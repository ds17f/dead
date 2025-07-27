package com.deadarchive.core.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.settings.api.SettingsRepository
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
import com.deadarchive.core.settings.service.SettingsConfigurationService
import com.deadarchive.core.settings.service.SettingsBackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Information about the latest available backup
 */
data class BackupInfo(
    val showCount: Int,
    val createdAt: Long,
    val fileName: String
)

/**
 * ViewModel for settings screen using service-oriented architecture.
 * Coordinates between UI and focused services for better maintainability.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val configurationService: SettingsConfigurationService,
    private val backupService: SettingsBackupService
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Expose service state flows
    val backupJson: StateFlow<String?> = backupService.backupJson
    val backupFile: StateFlow<java.io.File?> = backupService.backupFile
    val latestBackupInfo: StateFlow<BackupInfo?> = backupService.latestBackupInfo
    
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
    
    // Service initialization happens automatically via their constructors
    
    /**
     * Update the theme mode setting
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        configurationService.updateThemeMode(
            themeMode = themeMode,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the audio format preference order
     */
    fun updateAudioFormatPreference(formatOrder: List<String>) {
        configurationService.updateAudioFormatPreference(
            formatOrder = formatOrder,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the download WiFi-only setting
     */
    fun updateDownloadWifiOnly(wifiOnly: Boolean) {
        configurationService.updateDownloadWifiOnly(
            wifiOnly = wifiOnly,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the debug info visibility setting
     */
    fun updateShowDebugInfo(showDebugInfo: Boolean) {
        configurationService.updateShowDebugInfo(
            showDebugInfo = showDebugInfo,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Reset all settings to their default values
     */
    fun resetToDefaults() {
        configurationService.resetToDefaults(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
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
        configurationService.setDraggingFormats(
            dragging = dragging,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the deletion grace period in days
     */
    fun updateDeletionGracePeriod(days: Int) {
        configurationService.updateDeletionGracePeriod(
            days = days,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the low storage threshold in MB
     */
    fun updateLowStorageThreshold(thresholdMB: Long) {
        configurationService.updateLowStorageThreshold(
            thresholdMB = thresholdMB,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the preferred audio source for recording selection
     */
    fun updatePreferredAudioSource(source: String) {
        configurationService.updatePreferredAudioSource(
            source = source,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the minimum rating filter for recording selection
     */
    fun updateMinimumRating(rating: Float) {
        configurationService.updateMinimumRating(
            rating = rating,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the prefer higher rated setting
     */
    fun updatePreferHigherRated(prefer: Boolean) {
        configurationService.updatePreferHigherRated(
            prefer = prefer,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the use Library V2 setting
     */
    fun updateUseLibraryV2(enabled: Boolean) {
        configurationService.updateUseLibraryV2(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Backup the user's library and settings
     */
    fun backupLibrary() {
        backupService.backupLibrary(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Restore library and settings from the latest backup file
     */
    fun restoreLibrary() {
        backupService.restoreLibrary(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    // Backup info loading is now handled by SettingsBackupService
    
    /**
     * Check if a backup is available for restore
     */
    fun hasBackupAvailable(): Boolean {
        return backupService.hasBackupAvailable()
    }
}