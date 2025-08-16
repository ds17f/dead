package com.deadarchive.v2.core.database.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DataManifest(
    @SerialName("package")
    val packageInfo: PackageInfo
)

@Serializable
data class PackageInfo(
    val name: String,
    val version: String,
    @SerialName("version_type")
    val versionType: String,
    val description: String,
    val created: String
)

@Singleton
class AssetManagerV2 @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AssetManagerV2"
        private const val DATA_ZIP_FILENAME = "data-v2.0.2.zip"
        private const val TEMP_DIR_NAME = "v2_data_import"
        private const val MANIFEST_FILENAME = "manifest.json"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Extract the data zip file to a temporary directory in cache
     * Returns the temporary directory containing extracted files
     */
    suspend fun extractDataZip(): File = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting extraction of $DATA_ZIP_FILENAME")
        
        // Create temp directory in cache
        val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
        tempDir.mkdirs()
        
        // Open assets zip file
        context.assets.open(DATA_ZIP_FILENAME).use { assetStream ->
            ZipInputStream(assetStream).use { zipStream ->
                var entry = zipStream.nextEntry
                var extractedCount = 0
                
                while (entry != null) {
                    val entryFile = File(tempDir, entry.name)
                    
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        // Ensure parent directories exist
                        entryFile.parentFile?.mkdirs()
                        
                        // Extract file
                        FileOutputStream(entryFile).use { outputStream ->
                            zipStream.copyTo(outputStream)
                        }
                        extractedCount++
                    }
                    
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
                
                Log.d(TAG, "Extracted $extractedCount files to ${tempDir.absolutePath}")
            }
        }
        
        tempDir
    }
    
    /**
     * Get the data version from the manifest.json file in the zip
     */
    suspend fun getDataVersion(tempDir: File): String = withContext(Dispatchers.IO) {
        val manifestFile = File(tempDir, MANIFEST_FILENAME)
        if (!manifestFile.exists()) {
            Log.w(TAG, "Manifest file not found in extracted data")
            return@withContext "unknown"
        }
        
        try {
            val manifestContent = manifestFile.readText()
            val manifest = json.decodeFromString<DataManifest>(manifestContent)
            Log.d(TAG, "Data version: ${manifest.packageInfo.version}")
            manifest.packageInfo.version
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse manifest.json", e)
            "unknown"
        }
    }
    
    /**
     * Get the full manifest data for detailed import tracking
     */
    suspend fun getManifest(tempDir: File): DataManifest? = withContext(Dispatchers.IO) {
        val manifestFile = File(tempDir, MANIFEST_FILENAME)
        if (!manifestFile.exists()) {
            Log.w(TAG, "Manifest file not found in extracted data")
            return@withContext null
        }
        
        try {
            val manifestContent = manifestFile.readText()
            json.decodeFromString<DataManifest>(manifestContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse manifest.json", e)
            null
        }
    }
    
    /**
     * Clean up temporary files after import
     */
    suspend fun cleanupTempFiles(tempDir: File): Unit = withContext(Dispatchers.IO) {
        try {
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                Log.d(TAG, "Cleaned up temp directory: ${tempDir.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup temp directory", e)
        }
    }
    
    /**
     * Check if the data zip exists in assets
     */
    fun isDataZipAvailable(): Boolean {
        return try {
            context.assets.open(DATA_ZIP_FILENAME).use { 
                true 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Data zip file not found in assets", e)
            false
        }
    }
}