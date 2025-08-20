package com.deadly.v2.core.theme

import android.content.Context
import com.deadly.v2.core.theme.api.ThemeAssetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages theme switching and provides the current active theme provider.
 * 
 * Coordinates between built-in and ZIP-based theme providers, handling
 * initialization and switching between different themes.
 */
@Singleton
class ThemeManager @Inject constructor(
    private val builtinProvider: BuiltinThemeProvider,
    private val zipProviderFactory: ZipThemeProvider.Factory,
    private val context: Context
) {
    
    private val _currentProvider = MutableStateFlow<ThemeAssetProvider>(builtinProvider)
    val currentProvider: StateFlow<ThemeAssetProvider> = _currentProvider.asStateFlow()
    
    private val _availableThemes = MutableStateFlow<List<ThemeInfo>>(
        listOf(
            ThemeInfo(
                id = builtinProvider.getThemeId(),
                name = builtinProvider.getThemeName(),
                isBuiltin = true,
                zipPath = null
            )
        )
    )
    val availableThemes: StateFlow<List<ThemeInfo>> = _availableThemes.asStateFlow()
    
    /**
     * Switch to the built-in theme
     */
    fun useBuiltinTheme() {
        _currentProvider.value = builtinProvider
    }
    
    /**
     * Switch to a ZIP-based theme
     * 
     * @param zipPath Path to the theme ZIP file
     * @throws IllegalStateException if the ZIP theme fails to load
     */
    suspend fun useZipTheme(zipPath: String) {
        try {
            val zipProvider = zipProviderFactory.create(zipPath)
            zipProvider.initialize()
            
            _currentProvider.value = zipProvider
            
            // Add to available themes if not already present
            val currentThemes = _availableThemes.value.toMutableList()
            val themeInfo = ThemeInfo(
                id = zipProvider.getThemeId(),
                name = zipProvider.getThemeName(),
                isBuiltin = false,
                zipPath = zipPath
            )
            
            if (currentThemes.none { it.id == themeInfo.id }) {
                currentThemes.add(themeInfo)
                _availableThemes.value = currentThemes
            }
            
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load ZIP theme: $zipPath", e)
        }
    }
    
    /**
     * Get the currently active theme provider
     */
    fun getCurrentProvider(): ThemeAssetProvider = _currentProvider.value
    
    /**
     * MVP: Auto-initialize with newest theme or fallback to builtin
     * 
     * Scans for themes in app's files/themes directory and automatically
     * loads the newest ZIP theme if found, otherwise uses builtin theme.
     */
    suspend fun autoInitialize() {
        val themesDir = File(context.filesDir, "themes")
        if (!themesDir.exists()) {
            // No themes directory, use builtin theme
            useBuiltinTheme()
            return
        }
        
        // Find all .zip files in themes directory
        val zipFiles = themesDir.listFiles { _, name -> name.endsWith(".zip") }
        
        if (zipFiles.isNullOrEmpty()) {
            // No ZIP themes found, use builtin theme
            useBuiltinTheme()
            return
        }
        
        // Find the newest theme file by last modified timestamp
        val newestTheme = zipFiles.maxByOrNull { it.lastModified() }
        
        if (newestTheme != null) {
            try {
                // Try to load the newest theme
                useZipTheme(newestTheme.absolutePath)
            } catch (e: Exception) {
                // If ZIP theme fails, fallback to builtin
                useBuiltinTheme()
            }
        } else {
            // Fallback to builtin
            useBuiltinTheme()
        }
    }
    
    /**
     * Scan for theme files in the app's themes directory
     */
    suspend fun scanForThemes() {
        val themesDir = File(context.filesDir, "themes")
        if (!themesDir.exists()) return
        
        val themes = mutableListOf<ThemeInfo>()
        themes.add(ThemeInfo(
            id = builtinProvider.getThemeId(),
            name = builtinProvider.getThemeName(),
            isBuiltin = true,
            zipPath = null
        ))
        
        themesDir.listFiles { _, name -> name.endsWith(".zip") }?.forEach { zipFile ->
            try {
                val zipProvider = zipProviderFactory.create(zipFile.absolutePath)
                zipProvider.initialize()
                
                themes.add(ThemeInfo(
                    id = zipProvider.getThemeId(),
                    name = zipProvider.getThemeName(),
                    isBuiltin = false,
                    zipPath = zipFile.absolutePath
                ))
            } catch (e: Exception) {
                // Skip invalid theme files
            }
        }
        
        _availableThemes.value = themes
    }
}

/**
 * Information about an available theme
 */
data class ThemeInfo(
    val id: String,
    val name: String,
    val isBuiltin: Boolean,
    val zipPath: String?
)