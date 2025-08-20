package com.deadly.core.settings.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.deadly.core.settings.SettingsUiState
import com.deadly.core.settings.BackupInfo
import com.deadly.core.backup.BackupService
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Service responsible for backup and restore operations in Settings feature.
 * Handles backup creation, restoration, and backup file management.
 */
@Singleton
class SettingsBackupService @Inject constructor(
    private val backupService: BackupService,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SettingsBackupService"
    }
    
    private val _backupJson = MutableStateFlow<String?>(null)
    val backupJson: StateFlow<String?> = _backupJson.asStateFlow()
    
    private val _backupFile = MutableStateFlow<java.io.File?>(null)
    val backupFile: StateFlow<java.io.File?> = _backupFile.asStateFlow()
    
    private val _latestBackupInfo = MutableStateFlow<BackupInfo?>(null)
    val latestBackupInfo: StateFlow<BackupInfo?> = _latestBackupInfo.asStateFlow()
    
    init {
        // Load latest backup info on startup
        loadLatestBackupInfo()
    }
    
    /**
     * Backup the user's library and settings
     */
    fun backupLibrary(
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting library backup...")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
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
                onStateChange(currentState.copy(
                    isLoading = false,
                    successMessage = successMessage
                ))
                Log.d(TAG, "Backup process completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create backup", e)
                Log.e(TAG, "Exception details: ${e.javaClass.simpleName} - ${e.message}")
                Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                
                // Clear backup data on error
                _backupJson.value = null
                _backupFile.value = null
                
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to create backup: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Restore library and settings from the latest backup file
     */
    fun restoreLibrary(
        coroutineScope: CoroutineScope,
        onStateChange: (SettingsUiState) -> Unit,
        currentState: SettingsUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting library restore from latest backup...")
                onStateChange(currentState.copy(isLoading = true, errorMessage = null))
                
                Log.d(TAG, "About to call backupService.findLatestBackupFile()")
                // Find the latest backup file
                val latestBackupFile = backupService.findLatestBackupFile()
                Log.d(TAG, "findLatestBackupFile() returned: $latestBackupFile")
                if (latestBackupFile == null) {
                    onStateChange(currentState.copy(
                        isLoading = false,
                        errorMessage = "No backup files found in Downloads directory"
                    ))
                    return@launch
                }
                
                Log.d(TAG, "Found backup file: ${latestBackupFile.absolutePath}")
                
                // Read and parse the backup file
                val backupJson = latestBackupFile.readText()
                val backupData = backupService.parseBackup(backupJson)
                
                if (backupData == null) {
                    onStateChange(currentState.copy(
                        isLoading = false,
                        errorMessage = "Failed to parse backup file: ${latestBackupFile.name}"
                    ))
                    return@launch
                }
                
                Log.d(TAG, "Parsed backup with ${backupData.libraryShows.size} shows")
                
                // Restore the backup
                val result = backupService.restoreFromBackup(backupData)
                
                when (result) {
                    is com.deadly.core.backup.RestoreResult.Success -> {
                        val message = if (result.skippedShows > 0) {
                            "Restored ${result.restoredShows} shows, skipped ${result.skippedShows} shows not in catalog. Settings: ${if (result.restoredSettings) "restored" else "failed"}"
                        } else {
                            "Successfully restored ${result.restoredShows} shows and settings from ${latestBackupFile.name}"
                        }
                        
                        // Refresh backup info after successful restore
                        loadLatestBackupInfo()
                        
                        onStateChange(currentState.copy(
                            isLoading = false,
                            successMessage = message
                        ))
                        Log.d(TAG, "Restore completed successfully - UI message set: $message")
                    }
                    
                    is com.deadly.core.backup.RestoreResult.Error -> {
                        onStateChange(currentState.copy(
                            isLoading = false,
                            errorMessage = "Restore failed: ${result.message}"
                        ))
                        Log.e(TAG, "Restore failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore backup", e)
                onStateChange(currentState.copy(
                    isLoading = false,
                    errorMessage = "Failed to restore backup: ${e.message}"
                ))
            }
        }
    }
    
    /**
     * Load information about the latest available backup
     */
    private fun loadLatestBackupInfo() {
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
    
    /**
     * Check if a backup is available for restore
     */
    fun hasBackupAvailable(): Boolean {
        return latestBackupInfo.value != null
    }
}