package com.deadarchive.core.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.settings.api.SettingsRepository
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
import com.deadarchive.core.settings.service.SettingsConfigurationService
import com.deadarchive.core.settings.service.SettingsBackupService
import com.deadarchive.core.data.service.UpdateService
import com.deadarchive.core.data.service.GlobalUpdateManager
import com.deadarchive.core.model.AppUpdate
import com.deadarchive.core.model.UpdateStatus
import com.deadarchive.core.model.UpdateDownloadState
import com.deadarchive.core.model.UpdateInstallationState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
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
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val configurationService: SettingsConfigurationService,
    private val backupService: SettingsBackupService,
    private val updateService: UpdateService,
    private val globalUpdateManager: GlobalUpdateManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Update-related state
    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()
    
    private val _currentUpdate = MutableStateFlow<AppUpdate?>(null)
    val currentUpdate: StateFlow<AppUpdate?> = _currentUpdate.asStateFlow()
    
    private val _downloadState = MutableStateFlow(UpdateDownloadState())
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()
    
    // Installation status tracking
    val installationStatus: StateFlow<UpdateInstallationState> = updateService.getInstallationStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UpdateInstallationState()
        )
    
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
    
    init {
        // Observe global update manager for startup-detected updates
        viewModelScope.launch {
            globalUpdateManager.updateStatus.collect { globalStatus ->
                if (globalStatus != null && globalStatus.isUpdateAvailable) {
                    Log.d(TAG, "🎉 Global update detected, updating SettingsViewModel state")
                    _updateStatus.value = globalStatus
                    _currentUpdate.value = globalStatus.update
                    
                    // Clear the global state after we've received it
                    globalUpdateManager.clearUpdateStatus()
                } else {
                    Log.d(TAG, "No global update status or not available")
                }
            }
        }
    }
    
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
     * Update the use Player V2 setting
     */
    fun updateUsePlayerV2(enabled: Boolean) {
        configurationService.updateUsePlayerV2(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the use Search V2 setting
     */
    fun updateUseSearchV2(enabled: Boolean) {
        configurationService.updateUseSearchV2(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the use Home V2 setting
     */
    fun updateUseHomeV2(enabled: Boolean) {
        configurationService.updateUseHomeV2(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the use Playlist V2 setting
     */
    fun updateUsePlaylistV2(enabled: Boolean) {
        configurationService.updateUsePlaylistV2(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Update the use MiniPlayer V2 setting
     */
    fun updateUseMiniPlayerV2(enabled: Boolean) {
        configurationService.updateUseMiniPlayerV2(
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
    
    // Update-related methods
    
    /**
     * Check for app updates manually
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get current app version for display
                val currentVersion = try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName ?: "unknown"
                } catch (e: Exception) {
                    "unknown"
                }
                
                Log.d(TAG, "Starting update check - current version: $currentVersion")
                
                val result = updateService.checkForUpdates()
                result.fold(
                    onSuccess = { status ->
                        _updateStatus.value = status
                        
                        val message = if (status.isUpdateAvailable) {
                            val update = status.update!!
                            _currentUpdate.value = update
                            "Update available!\n" +
                            "Current: $currentVersion\n" +
                            "Latest: ${update.version}\n" +
                            "Released: ${formatUpdateDate(update.publishedAt)}"
                        } else {
                            "You're on the latest version!\n" +
                            "Current: $currentVersion\n" +
                            "Checked: GitHub ds17f/dead repository\n" +
                            "Status: Up to date"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = message
                        )
                        
                        Log.d(TAG, "Update check completed: ${if (status.isUpdateAvailable) "update available" else "up to date"}")
                    },
                    onFailure = { error ->
                        val detailedError = "Failed to check for updates from GitHub repository ds17f/dead.\n\n" +
                                          "Current version: $currentVersion\n" +
                                          "Error: ${error.message}\n\n" +
                                          "Please check your internet connection and try again."
                        
                        Log.e(TAG, "Failed to check for updates", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = detailedError
                        )
                    }
                )
            } catch (e: Exception) {
                val detailedError = "Error checking for updates.\n\n" +
                                  "Repository: ds17f/dead\n" +
                                  "Error: ${e.message}\n\n" +
                                  "This might be a network issue or the GitHub API might be temporarily unavailable."
                
                Log.e(TAG, "Error checking for updates", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = detailedError
                )
            }
        }
    }
    
    /**
     * Download the current update
     */
    fun downloadUpdate() {
        val update = _currentUpdate.value ?: return
        
        viewModelScope.launch {
            try {
                // Start monitoring download progress
                updateService.getDownloadProgress(update).collect { progress ->
                    _downloadState.value = progress
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring download progress", e)
            }
        }
        
        viewModelScope.launch {
            try {
                val result = updateService.downloadUpdate(update)
                result.fold(
                    onSuccess = { apkFile ->
                        Log.d(TAG, "Update downloaded successfully: ${apkFile.absolutePath}")
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Update downloaded successfully"
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to download update", error)
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to download update: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error downloading update: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Install the downloaded update
     */
    fun installUpdate() {
        val downloadedFile = _downloadState.value.downloadedFile ?: return
        
        viewModelScope.launch {
            try {
                val apkFile = java.io.File(downloadedFile)
                val result = updateService.installUpdate(apkFile)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Update installation initiated successfully")
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Update installation started"
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to install update", error)
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to install update: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error installing update", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error installing update: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Skip the current update version
     */
    fun skipUpdate() {
        val update = _currentUpdate.value ?: return
        
        viewModelScope.launch {
            try {
                updateService.skipUpdate(update.version)
                _currentUpdate.value = null
                _updateStatus.value = null
                _downloadState.value = UpdateDownloadState()
                
                _uiState.value = _uiState.value.copy(
                    successMessage = "Update skipped"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping update", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error skipping update: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Set auto update check enabled/disabled
     */
    fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        configurationService.setAutoUpdateCheckEnabled(
            enabled = enabled,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Clear update-related state
     */
    fun clearUpdateState() {
        _currentUpdate.value = null
        _updateStatus.value = null
        _downloadState.value = UpdateDownloadState()
    }
    
    /**
     * Format ISO 8601 date string for display
     */
    private fun formatUpdateDate(isoDateString: String): String {
        return try {
            val instant = Instant.parse(isoDateString)
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date.from(instant))
        } catch (e: Exception) {
            isoDateString
        }
    }
}