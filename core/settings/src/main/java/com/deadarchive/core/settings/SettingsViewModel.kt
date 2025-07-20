package com.deadarchive.core.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.settings.api.SettingsRepository
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
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
 * Information about the latest available backup
 */
data class BackupInfo(
    val showCount: Int,
    val createdAt: Long,
    val fileName: String
)

/**
 * ViewModel for settings screen following established patterns
 * Manages settings state with reactive updates and proper error handling
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupService: com.deadarchive.core.backup.BackupService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _backupJson = MutableStateFlow<String?>(null)
    val backupJson: StateFlow<String?> = _backupJson.asStateFlow()
    
    private val _backupFile = MutableStateFlow<java.io.File?>(null)
    val backupFile: StateFlow<java.io.File?> = _backupFile.asStateFlow()
    
    private val _latestBackupInfo = MutableStateFlow<BackupInfo?>(null)
    val latestBackupInfo: StateFlow<BackupInfo?> = _latestBackupInfo.asStateFlow()
    
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
    
    init {
        // Load latest backup info on startup
        loadLatestBackupInfo()
    }
    
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
     * Update the debug info visibility setting
     */
    fun updateShowDebugInfo(showDebugInfo: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating show debug info to: $showDebugInfo")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateShowDebugInfo(showDebugInfo)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (showDebugInfo) "Debug panels enabled" else "Debug panels disabled"
                )
                Log.d(TAG, "Show debug info setting updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update show debug info setting", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update debug setting: ${e.message}"
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
    
    /**
     * Update the deletion grace period in days
     */
    fun updateDeletionGracePeriod(days: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating deletion grace period to: $days days")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateDeletionGracePeriod(days)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Grace period updated to $days days"
                )
                Log.d(TAG, "Deletion grace period updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update deletion grace period", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update grace period: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the low storage threshold in MB
     */
    fun updateLowStorageThreshold(thresholdMB: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating low storage threshold to: ${thresholdMB}MB")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateLowStorageThreshold(thresholdMB)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Storage threshold updated to ${thresholdMB}MB"
                )
                Log.d(TAG, "Low storage threshold updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update low storage threshold", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update storage threshold: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the preferred audio source for recording selection
     */
    fun updatePreferredAudioSource(source: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating preferred audio source to: $source")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updatePreferredAudioSource(source)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Audio source preference updated to $source"
                )
                Log.d(TAG, "Preferred audio source updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update preferred audio source", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update audio source preference: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the minimum rating filter for recording selection
     */
    fun updateMinimumRating(rating: Float) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating minimum rating to: $rating")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updateMinimumRating(rating)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Minimum rating updated to ${rating}★"
                )
                Log.d(TAG, "Minimum rating updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update minimum rating", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update minimum rating: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update the prefer higher rated setting
     */
    fun updatePreferHigherRated(prefer: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating prefer higher rated to: $prefer")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                settingsRepository.updatePreferHigherRated(prefer)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = if (prefer) "Higher rated recordings will be preferred" else "Higher rated preference disabled"
                )
                Log.d(TAG, "Prefer higher rated updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update prefer higher rated", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update higher rated preference: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Backup the user's library and settings
     */
    fun backupLibrary() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting library backup...")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                Log.d(TAG, "Calling backupService.createBackup...")
                // Create backup
                val backup = backupService.createBackup(appVersion = "1.0") // TODO: Get actual app version
                Log.d(TAG, "Backup created, calling exportBackup...")
                
                val (backupJson, backupFile) = backupService.exportBackup(context, backup)
                Log.d(TAG, "Export completed, storing backup data...")
                
                // Store backup JSON and file for debug display
                Log.d(TAG, "Setting backup JSON - length: ${backupJson.length}")
                _backupJson.value = backupJson
                Log.d(TAG, "Setting backup file: ${backupFile?.absolutePath ?: "null"}")
                _backupFile.value = backupFile
                
                Log.d(TAG, "Backup created successfully: ${backup.libraryShows.size} shows")
                Log.d(TAG, "Backup JSON length: ${backupJson.length} characters")
                
                val successMessage = if (backupFile != null) {
                    "Backup saved to Downloads: ${backupFile.name} (${backup.libraryShows.size} shows, ${backupJson.length} chars)"
                } else {
                    "Backup created in memory only (${backup.libraryShows.size} shows, ${backupJson.length} chars) - file save failed"
                }
                
                // Refresh backup info after successful backup
                loadLatestBackupInfo()
                
                Log.d(TAG, "Setting success state with message: $successMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = successMessage
                )
                Log.d(TAG, "Backup process completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create backup", e)
                Log.e(TAG, "Exception details: ${e.javaClass.simpleName} - ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                
                // Clear backup data on error
                _backupJson.value = null
                _backupFile.value = null
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create backup: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Restore library and settings from the latest backup file
     */
    fun restoreLibrary() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting library restore from latest backup...")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                Log.d(TAG, "About to call backupService.findLatestBackupFile()")
                // Find the latest backup file
                val latestBackupFile = backupService.findLatestBackupFile()
                Log.d(TAG, "findLatestBackupFile() returned: $latestBackupFile")
                if (latestBackupFile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No backup files found in Downloads directory"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Found backup file: ${latestBackupFile.absolutePath}")
                
                // Read and parse the backup file
                val backupJson = latestBackupFile.readText()
                val backupData = backupService.parseBackup(backupJson)
                
                if (backupData == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to parse backup file: ${latestBackupFile.name}"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Parsed backup with ${backupData.libraryShows.size} shows")
                
                // Restore the backup
                val result = backupService.restoreFromBackup(backupData)
                
                when (result) {
                    is com.deadarchive.core.backup.RestoreResult.Success -> {
                        val message = if (result.skippedShows > 0) {
                            "Restored ${result.restoredShows} shows, skipped ${result.skippedShows} shows not in catalog. Settings: ${if (result.restoredSettings) "restored" else "failed"}"
                        } else {
                            "Successfully restored ${result.restoredShows} shows and settings from ${latestBackupFile.name}"
                        }
                        
                        // Refresh backup info after successful restore
                        loadLatestBackupInfo()
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = message
                        )
                        Log.d(TAG, "Restore completed successfully - UI message set: $message")
                    }
                    
                    is com.deadarchive.core.backup.RestoreResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Restore failed: ${result.message}"
                        )
                        Log.e(TAG, "Restore failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore backup", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to restore backup: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load information about the latest available backup
     */
    private fun loadLatestBackupInfo() {
        viewModelScope.launch {
            try {
                val latestBackupFile = backupService.findLatestBackupFile()
                if (latestBackupFile != null) {
                    val backupJson = latestBackupFile.readText()
                    val backupData = backupService.parseBackup(backupJson)
                    
                    if (backupData != null) {
                        _latestBackupInfo.value = BackupInfo(
                            showCount = backupData.libraryShows.size,
                            createdAt = backupData.createdAt,
                            fileName = latestBackupFile.name
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load latest backup info", e)
                _latestBackupInfo.value = null
            }
        }
    }
    
    /**
     * Check if a backup is available for restore
     */
    fun hasBackupAvailable(): Boolean {
        return latestBackupInfo.value != null
    }
}