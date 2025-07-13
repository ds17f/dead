package com.deadarchive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.sync.DataSyncService
import com.deadarchive.core.data.sync.SyncPhase
import com.deadarchive.core.data.sync.SyncProgress
import com.deadarchive.core.data.sync.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing splash screen state and initial data sync
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val dataSyncService: DataSyncService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    init {
        checkSyncStatusAndStart()
    }
    
    private fun checkSyncStatusAndStart() {
        viewModelScope.launch {
            try {
                val isInitialSyncComplete = dataSyncService.isInitialSyncComplete()
                
                if (isInitialSyncComplete) {
                    // Sync already completed, proceed to app
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.COMPLETED,
                        message = "Welcome back!"
                    )
                } else {
                    // Need to perform initial sync
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.REQUIRED,
                        message = "First time setup - downloading show catalog..."
                    )
                    startInitialSync()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncStatus = SyncStatus.ERROR,
                    errorMessage = "Failed to check sync status: ${e.message}"
                )
            }
        }
    }
    
    private fun startInitialSync() {
        viewModelScope.launch {
            // Collect sync progress
            launch {
                dataSyncService.getDownloadProgress().collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        syncProgress = progress,
                        message = when (progress.phase) {
                            SyncPhase.STARTING -> "Initializing download..."
                            SyncPhase.FETCHING -> "Fetching show list from Archive.org..."
                            SyncPhase.PROCESSING -> "Processing shows (${progress.processedItems}/${progress.totalItems})"
                            SyncPhase.FINALIZING -> "Finalizing database..."
                            SyncPhase.COMPLETED -> "Setup complete!"
                            SyncPhase.ERROR -> "Error: ${progress.error}"
                            SyncPhase.IDLE -> "Preparing..."
                        }
                    )
                }
            }
            
            // Start the sync
            val result = dataSyncService.downloadCompleteCatalog()
            
            when (result) {
                is SyncResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.COMPLETED,
                        message = "Setup complete! Downloaded ${result.recordingsProcessed} shows."
                    )
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        syncStatus = SyncStatus.ERROR,
                        errorMessage = result.error
                    )
                }
            }
        }
    }
    
    fun retrySync() {
        _uiState.value = _uiState.value.copy(
            syncStatus = SyncStatus.REQUIRED,
            errorMessage = null
        )
        startInitialSync()
    }
    
    fun skipSync() {
        // Allow user to skip initial sync and use API-only mode
        _uiState.value = _uiState.value.copy(
            syncStatus = SyncStatus.SKIPPED,
            message = "Skipped initial download - using online mode"
        )
    }
}

/**
 * UI state for the splash screen
 */
data class SplashUiState(
    val syncStatus: SyncStatus = SyncStatus.CHECKING,
    val syncProgress: SyncProgress = SyncProgress(
        phase = SyncPhase.IDLE,
        totalItems = 0,
        processedItems = 0,
        currentItem = ""
    ),
    val message: String = "Loading...",
    val errorMessage: String? = null
) {
    val isReady: Boolean
        get() = syncStatus in listOf(SyncStatus.COMPLETED, SyncStatus.SKIPPED)
        
    val showError: Boolean
        get() = syncStatus == SyncStatus.ERROR && errorMessage != null
        
    val showProgress: Boolean
        get() = syncStatus == SyncStatus.REQUIRED && syncProgress.isInProgress
}

/**
 * Status of the initial sync process
 */
enum class SyncStatus {
    CHECKING,    // Checking if sync is needed
    REQUIRED,    // Sync is needed and in progress
    COMPLETED,   // Sync completed successfully
    SKIPPED,     // User chose to skip sync
    ERROR        // Sync failed
}