package com.deadly.v2.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * SettingsViewModel - Business logic for V2 Settings screen
 * 
 * Handles theme import operations and coordinates with ThemeManager
 * to update available themes and potentially switch to imported themes.
 * 
 * Following V2 architecture patterns with minimal state management
 * since most UI feedback is handled directly by ThemeChooser component.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    /**
     * Handle theme file import completion
     * 
     * Called by ThemeChooser when a ZIP file has been successfully
     * copied to the themes directory. Triggers theme scanning to
     * make the new theme available.
     * 
     * @param themeFile The imported theme ZIP file
     */
    fun onThemeImported(themeFile: File) {
        viewModelScope.launch {
            try {
                // Trigger theme manager to scan for new themes
                themeManager.scanForThemes()
                
                // Future: Could automatically switch to the imported theme
                // or show a dialog asking if user wants to switch
                // For now, just make it available in the theme list
                
            } catch (e: Exception) {
                // ThemeChooser handles user feedback for import errors
                // Log error for debugging
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Clear all themes and restart the app
     * 
     * Deletes all imported theme files and exits the app so user can restart
     * with the builtin theme restored.
     */
    fun onClearThemes() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "onClearThemes: Clearing all themes")
                themeManager.clearAllThemes()
                Log.d(TAG, "onClearThemes: Themes cleared, exiting app")
                
                // Exit the app so user can restart with builtin theme
                exitProcess(0)
                
            } catch (e: Exception) {
                Log.e(TAG, "onClearThemes: Failed to clear themes", e)
                // Could show error toast here, but keeping it simple for now
            }
        }
    }
}