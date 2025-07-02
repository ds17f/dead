package com.deadarchive.core.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.settings.data.SettingsRepository
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.model.RepeatMode
import com.deadarchive.core.settings.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen following established patterns
 * Manages settings state with reactive updates and proper error handling
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    /**
     * Reactive settings state that updates UI automatically when settings change
     */
    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .catch { exception ->
            Log.e(TAG, "Error loading settings", exception)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to load settings: ${exception.message}"
            )
            emit(AppSettings()) // Emit default settings on error
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )
    
    /**
     * Update the theme mode setting
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating theme mode to: $themeMode")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateThemeMode(themeMode)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Theme updated to ${themeMode.displayName}"
                )
                Log.d(TAG, "Theme mode updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update theme mode", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update theme: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the audio format preference order
     */
    fun updateAudioFormatPreference(formatOrder: List<String>) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating audio format preference: $formatOrder")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateAudioFormatPreference(formatOrder)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Audio format preferences updated"
                )
                Log.d(TAG, "Audio format preference updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update audio format preference", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update audio preferences: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the download WiFi-only setting
     */
    fun updateDownloadWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating download WiFi-only to: $wifiOnly")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateDownloadWifiOnly(wifiOnly)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (wifiOnly) "Downloads restricted to WiFi" else "Downloads allowed on cellular"
                )
                Log.d(TAG, "Download WiFi-only setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update download WiFi-only setting", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update download setting: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the repeat mode setting
     */
    fun updateRepeatMode(repeatMode: RepeatMode) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating repeat mode to: $repeatMode")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateRepeatMode(repeatMode)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Repeat mode set to ${repeatMode.displayName}"
                )
                Log.d(TAG, "Repeat mode updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update repeat mode", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update repeat mode: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the shuffle enabled setting
     */
    fun updateShuffleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating shuffle enabled to: $enabled")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateShuffleEnabled(enabled)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (enabled) "Shuffle enabled" else "Shuffle disabled"
                )
                Log.d(TAG, "Shuffle enabled setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update shuffle enabled setting", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update shuffle setting: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset all settings to their default values
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Resetting all settings to defaults")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.resetToDefaults()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "All settings reset to defaults"
                )
                Log.d(TAG, "Settings reset to defaults successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset settings to defaults", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to reset settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear any displayed message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    /**
     * Set drag state for audio format reordering
     */
    fun setDraggingFormats(dragging: Boolean) {
        _uiState.value = _uiState.value.copy(isDraggingFormats = dragging)
    }
}