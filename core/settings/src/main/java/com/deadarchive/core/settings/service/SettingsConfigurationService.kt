package com.deadarchive.core.settings.service

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadarchive.core.settings.api.SettingsRepository
import com.deadarchive.core.settings.api.model.ThemeMode
import com.deadarchive.core.settings.SettingsUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for app configuration management in Settings feature.
 * Handles all settings operations with proper error handling and state updates.
 */
@Singleton
class SettingsConfigurationService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    companion object {
        private const val TAG = "SettingsConfigurationService"
    }
    
    /**
     * Update the theme mode setting
     */
    fun updateThemeMode(
        themeMode: ThemeMode,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating theme mode to: $themeMode")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateThemeMode(themeMode)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Theme updated to ${themeMode.displayName}"
                ))
                Log.d(TAG, "Theme mode updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update theme mode", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update theme: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the audio format preference order
     */
    fun updateAudioFormatPreference(
        formatOrder: List<String>,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating audio format preference: $formatOrder")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateAudioFormatPreference(formatOrder)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Audio format preferences updated"
                ))
                Log.d(TAG, "Audio format preference updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update audio format preference", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update audio preferences: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the download WiFi-only setting
     */
    fun updateDownloadWifiOnly(
        wifiOnly: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating download WiFi-only to: $wifiOnly")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateDownloadWifiOnly(wifiOnly)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (wifiOnly) "Downloads restricted to WiFi" else "Downloads allowed on cellular"
                ))
                Log.d(TAG, "Download WiFi-only setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update download WiFi-only setting", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update download setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the debug info visibility setting
     */
    fun updateShowDebugInfo(
        showDebugInfo: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating show debug info to: $showDebugInfo")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateShowDebugInfo(showDebugInfo)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (showDebugInfo) "Debug panels enabled" else "Debug panels disabled"
                ))
                Log.d(TAG, "Show debug info setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update show debug info setting", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update debug setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the deletion grace period in days
     */
    fun updateDeletionGracePeriod(
        days: Int,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating deletion grace period to: $days days")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateDeletionGracePeriod(days)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Grace period updated to $days days"
                ))
                Log.d(TAG, "Deletion grace period updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update deletion grace period", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update grace period: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the low storage threshold in MB
     */
    fun updateLowStorageThreshold(
        thresholdMB: Long,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating low storage threshold to: ${thresholdMB}MB")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateLowStorageThreshold(thresholdMB)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Storage threshold updated to ${thresholdMB}MB"
                ))
                Log.d(TAG, "Low storage threshold updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update low storage threshold", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update storage threshold: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the preferred audio source for recording selection
     */
    fun updatePreferredAudioSource(
        source: String,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating preferred audio source to: $source")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updatePreferredAudioSource(source)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Audio source preference updated to $source"
                ))
                Log.d(TAG, "Preferred audio source updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update preferred audio source", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update audio source preference: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the minimum rating filter for recording selection
     */
    fun updateMinimumRating(
        rating: Float,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating minimum rating to: $rating")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateMinimumRating(rating)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "Minimum rating updated to ${rating}★"
                ))
                Log.d(TAG, "Minimum rating updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update minimum rating", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update minimum rating: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the prefer higher rated setting
     */
    fun updatePreferHigherRated(
        prefer: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating prefer higher rated to: $prefer")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updatePreferHigherRated(prefer)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (prefer) "Higher rated recordings will be preferred" else "Higher rated preference disabled"
                ))
                Log.d(TAG, "Prefer higher rated updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update prefer higher rated", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update higher rated preference: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Reset all settings to their default values
     */
    fun resetToDefaults(
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Resetting all settings to defaults")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.resetToDefaults()
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = "All settings reset to defaults"
                ))
                Log.d(TAG, "Settings reset to defaults successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings to defaults", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to reset settings: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use Library V2 setting
     */
    fun updateUseLibraryV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use Library V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUseLibraryV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Library V2 enabled" else "Library V2 disabled"
                ))
                Log.d(TAG, "Use Library V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use Library V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update Library V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use Player V2 setting
     */
    fun updateUsePlayerV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use Player V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUsePlayerV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Player V2 enabled 🚀" else "Player V2 disabled"
                ))
                Log.d(TAG, "Use Player V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use Player V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update Player V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use Search V2 setting
     */
    fun updateUseSearchV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use Search V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUseSearchV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Search V2 enabled 🚀" else "Search V2 disabled"
                ))
                Log.d(TAG, "Use Search V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use Search V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update Search V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use Home V2 setting
     */
    fun updateUseHomeV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use Home V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUseHomeV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Home V2 enabled 🚀" else "Home V2 disabled"
                ))
                Log.d(TAG, "Use Home V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use Home V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update Home V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use Playlist V2 setting
     */
    fun updateUsePlaylistV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use Playlist V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUsePlaylistV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Playlist V2 enabled 🚀" else "Playlist V2 disabled"
                ))
                Log.d(TAG, "Use Playlist V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use Playlist V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update Playlist V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Update the use MiniPlayer V2 setting
     */
    fun updateUseMiniPlayerV2(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Updating use MiniPlayer V2 to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.updateUseMiniPlayerV2(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "MiniPlayer V2 enabled 🚀" else "MiniPlayer V2 disabled"
                ))
                Log.d(TAG, "Use MiniPlayer V2 updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update use MiniPlayer V2", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update MiniPlayer V2 setting: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Set drag state for audio format reordering
     */
    fun setDraggingFormats(
        dragging: Boolean,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        onStateChange(currentState.copy(isDraggingFormats = dragging))
    }
    
    /**
     * Set auto update check enabled/disabled
     */
    fun setAutoUpdateCheckEnabled(
        enabled: Boolean,
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Setting auto update check enabled to: $enabled")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                settingsRepository.setAutoUpdateCheckEnabled(enabled)
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Auto update check enabled" else "Auto update check disabled"
                ))
                Log.d(TAG, "Auto update check setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update auto update check setting", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to update auto update check setting: ${e.message}"
                ))
            }
        }
    }
}