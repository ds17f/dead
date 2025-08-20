package com.deadly.v2.core.theme

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.deadly.v2.core.theme.api.ThemeAssetProvider
import com.deadly.v2.core.theme.api.ThemeManifest
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile

/**
 * Theme provider that loads assets from extracted ZIP theme packages.
 * 
 * Handles loading theme manifests and extracting assets from ZIP files
 * into the app's internal storage for use by Compose UI.
 */
class ZipThemeProvider @AssistedInject constructor(
    @Assisted private val themeZipPath: String,
    @ApplicationContext private val context: Context
) : ThemeAssetProvider {
    
    @AssistedFactory
    interface Factory {
        fun create(themeZipPath: String): ZipThemeProvider
    }
    
    private var _manifest: ThemeManifest? = null
    private var _extractedDir: File? = null
    
    private suspend fun ensureExtracted(): File = withContext(Dispatchers.IO) {
        _extractedDir?.let { return@withContext it }
        
        val themeFile = File(themeZipPath)
        if (!themeFile.exists()) {
            throw IllegalStateException("Theme file not found: $themeZipPath")
        }
        
        // Create extraction directory in internal storage
        val themeId = themeFile.nameWithoutExtension
        val extractDir = File(context.filesDir, "themes/$themeId")
        extractDir.mkdirs()
        
        // Extract ZIP contents
        ZipFile(themeFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val targetFile = File(extractDir, entry.name)
                targetFile.parentFile?.mkdirs()
                
                if (!entry.isDirectory) {
                    zip.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        
        _extractedDir = extractDir
        return@withContext extractDir
    }
    
    private suspend fun getManifest(): ThemeManifest = withContext(Dispatchers.IO) {
        _manifest?.let { return@withContext it }
        
        val extractDir = ensureExtracted()
        val manifestFile = File(extractDir, "theme.json")
        
        if (!manifestFile.exists()) {
            throw IllegalStateException("Theme manifest not found: theme.json")
        }
        
        val manifest = Json.decodeFromString<ThemeManifest>(manifestFile.readText())
        _manifest = manifest
        return@withContext manifest
    }
    
    @Composable
    private fun loadAssetPainter(assetFileName: String): Painter {
        val context = LocalContext.current
        
        return remember(assetFileName) {
            try {
                val extractDir = _extractedDir ?: error("Theme not extracted")
                val assetFile = File(extractDir, assetFileName)
                
                if (!assetFile.exists()) {
                    error("Asset file not found: $assetFileName")
                }
                
                val bitmap = BitmapFactory.decodeFile(assetFile.absolutePath)
                    ?: error("Failed to decode image: $assetFileName")
                
                BitmapPainter(bitmap.asImageBitmap())
            } catch (e: Exception) {
                // Fallback to a default painter or rethrow
                throw IllegalStateException("Failed to load theme asset: $assetFileName", e)
            }
        }
    }
    
    @Composable
    override fun primaryLogo(): Painter {
        val manifest = _manifest ?: error("Theme not loaded")
        return loadAssetPainter(manifest.assets.primaryLogo)
    }
    
    @Composable
    override fun splashLogo(): Painter {
        val manifest = _manifest ?: error("Theme not loaded")
        return loadAssetPainter(manifest.assets.splashLogo)
    }
    
    override fun getThemeId(): String {
        return _manifest?.id ?: "unknown"
    }
    
    override fun getThemeName(): String {
        return _manifest?.name ?: "Unknown Theme"
    }
    
    /**
     * Initialize the theme by loading manifest and extracting assets.
     * Must be called before using the provider.
     */
    suspend fun initialize() {
        getManifest() // This will extract and load the manifest
    }
}