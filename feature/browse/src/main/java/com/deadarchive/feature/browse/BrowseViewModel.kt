package com.deadarchive.feature.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.browse.service.BrowseSearchService
import com.deadarchive.core.data.service.LibraryService
import com.deadarchive.core.data.download.DownloadService
import com.deadarchive.feature.browse.service.BrowseDataService
import com.deadarchive.core.data.service.GlobalUpdateManager
import com.deadarchive.core.model.UpdateStatus
import com.deadarchive.core.model.AppUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for browse screen using service-oriented architecture.
 * Coordinates between UI and focused services for better maintainability.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val searchService: BrowseSearchService,
    private val libraryService: LibraryService,
    private val downloadService: DownloadService,
    private val dataService: BrowseDataService,
    private val globalUpdateManager: GlobalUpdateManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "BrowseViewModel"
    }
    
    // UI State (managed by ViewModel, updated by services)
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    // Expose service state flows
    val searchQuery: StateFlow<String> = searchService.searchQuery
    val isSearching: StateFlow<Boolean> = searchService.isSearching
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = downloadService.downloadStates
    val showConfirmationDialog: StateFlow<Show?> = downloadService.showConfirmationDialog
    
    // Update-related state
    private val _updateStatus = MutableStateFlow<UpdateStatus?>(null)
    val updateStatus: StateFlow<UpdateStatus?> = _updateStatus.asStateFlow()
    
    private val _currentUpdate = MutableStateFlow<AppUpdate?>(null)
    val currentUpdate: StateFlow<AppUpdate?> = _currentUpdate.asStateFlow()
    
    init {
        // Load initial data on startup
        dataService.loadInitialData(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            onSearchingStateChange = { /* handled by searchService */ }
        )
        // Download state monitoring is automatically handled by shared DownloadService
        
        // Observe global update manager for startup-detected updates
        viewModelScope.launch {
            globalUpdateManager.updateStatus.collect { globalStatus ->
                if (globalStatus != null && globalStatus.isUpdateAvailable) {
                    Log.d(TAG, "🎉 Global update detected in Browse screen, updating state")
                    _updateStatus.value = globalStatus
                    _currentUpdate.value = globalStatus.update
                    
                    // Clear the global state after we've received it
                    globalUpdateManager.clearUpdateStatus()
                } else {
                    Log.d(TAG, "No global update status or not available in Browse")
                }
            }
        }
    }
    
    fun updateSearchQuery(query: String) {
        searchService.updateSearchQuery(
            query = query,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
    }
    
    fun searchShows(query: String = searchQuery.value) {
        searchService.searchShows(
            query = query,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
    }
    
    fun filterByEra(era: String) {
        searchService.filterByEra(
            era = era,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
    }
    
    fun toggleLibrary(show: Show) {
        viewModelScope.launch {
            try {
                libraryService.toggleLibrary(show)
                // UI updates automatically via reactive search results
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to update library: ${e.message}")
            }
        }
    }
    
    fun loadPopularShows() {
        dataService.loadPopularShows(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            onSearchingStateChange = { /* handled by searchService */ }
        )
    }
    
    fun loadRecentShows() {
        dataService.loadRecentShows(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            onSearchingStateChange = { /* handled by searchService */ }
        )
    }
    
    /**
     * Start downloading a recording and automatically add its show to library
     */
    fun downloadRecording(recording: Recording) {
        Log.d(TAG, "Starting download for recording ${recording.identifier}")
        viewModelScope.launch {
            try {
                // Start the download
                downloadService.downloadRecording(recording)
                
                // Find the show this recording belongs to and add it to library automatically
                val currentState = _uiState.value
                if (currentState is BrowseUiState.Success) {
                    val showForRecording = currentState.shows.find { show ->
                        show.recordings.any { it.identifier == recording.identifier }
                    }
                    
                    // Add show to library if not already there
                    showForRecording?.let { show ->
                        if (!show.isInLibrary) {
                            Log.d(TAG, "Download started - adding show ${show.showId} to library")
                            libraryService.addToLibrary(show)
                            // UI updates automatically via reactive search results
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadRecording: ${e.message}", e)
                _uiState.value = BrowseUiState.Error("Failed to start download: ${e.message}")
            }
        }
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        viewModelScope.launch {
            try {
                downloadService.cancelShowDownloads(show)
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to cancel downloads: ${e.message}")
            }
        }
    }
    
    /**
     * Clear/remove all downloads for a show (completely delete from system)
     * This is what users expect when they "uncheck" a downloaded show
     */
    fun clearShowDownloads(show: Show) {
        viewModelScope.launch {
            try {
                downloadService.clearShowDownloads(show)
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to clear downloads: ${e.message}")
            }
        }
    }
    
    // Download state monitoring is now handled by BrowseDownloadService in init{}
    
    /**
     * Get the current download state for a recording
     */
    fun getDownloadState(recording: Recording): DownloadState {
        return downloadService.getDownloadState(recording)
    }
    
    /**
     * Start downloading the best recording of a show
     */
    fun downloadShow(show: Show) {
        viewModelScope.launch {
            try {
                downloadService.downloadShow(show)
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to start download: ${e.message}")
            }
        }
    }
    
    /**
     * Handle download button click with smart state-based logic
     */
    fun handleDownloadButtonClick(show: Show) {
        downloadService.handleDownloadButtonClick(
            show = show,
            coroutineScope = viewModelScope,
            onError = { errorMessage ->
                _uiState.value = BrowseUiState.Error(errorMessage)
            }
        )
    }
    
    /**
     * Get the current download state for a show (based on its best recording)
     */
    fun getShowDownloadState(show: Show): ShowDownloadState {
        return downloadService.getShowDownloadState(show)
    }
    
    /**
     * Show confirmation dialog for removing download
     */
    fun showRemoveDownloadConfirmation(show: Show) {
        downloadService.showRemoveDownloadConfirmation(show)
    }
    
    /**
     * Hide confirmation dialog
     */
    fun hideConfirmationDialog() {
        downloadService.hideConfirmationDialog()
    }
    
    /**
     * Confirm removal of download (soft delete)
     */
    fun confirmRemoveDownload() {
        viewModelScope.launch {
            try {
                downloadService.confirmRemoveDownload()
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to remove download: ${e.message}")
            }
        }
    }
    
    /**
     * Handle library button actions from unified LibraryButton component
     */
    fun handleLibraryAction(action: com.deadarchive.core.design.component.LibraryAction, show: Show) {
        viewModelScope.launch {
            try {
                when (action) {
                    com.deadarchive.core.design.component.LibraryAction.ADD_TO_LIBRARY -> {
                        libraryService.addToLibrary(show)
                    }
                    com.deadarchive.core.design.component.LibraryAction.REMOVE_FROM_LIBRARY -> {
                        libraryService.removeFromLibrary(show)
                    }
                    com.deadarchive.core.design.component.LibraryAction.REMOVE_WITH_DOWNLOADS -> {
                        libraryService.removeShowWithDownloadCleanup(show, alsoRemoveDownloads = true)
                    }
                }
                // UI updates automatically via reactive search results
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error("Failed to update library: ${e.message}")
            }
        }
    }
    
    /**
     * Get reactive library status for a specific show.
     * Returns a Flow that automatically updates when library status changes.
     */
    fun getLibraryStatusFlow(show: Show): kotlinx.coroutines.flow.Flow<Boolean> {
        return libraryService.isShowInLibraryFlow(show.showId)
    }
    
    /**
     * Get download information for library removal confirmation dialog
     */
    fun getLibraryRemovalInfo(show: Show): com.deadarchive.core.data.service.LibraryRemovalInfo {
        return try {
            var result = com.deadarchive.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Checking...",
                downloadState = com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded
            )
            
            viewModelScope.launch {
                result = libraryService.getDownloadInfoForShow(show)
            }
            
            // Return default while async operation completes
            result
        } catch (e: Exception) {
            com.deadarchive.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Error checking downloads",
                downloadState = com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded
            )
        }
    }
    
    // Update-related methods for in-app update dialog
    
    /**
     * Clear update-related state
     */
    fun clearUpdateState() {
        _currentUpdate.value = null
        _updateStatus.value = null
    }
    
    /**
     * Skip the current update version
     */
    fun skipUpdate() {
        val update = _currentUpdate.value ?: return
        
        viewModelScope.launch {
            try {
                // Clear local state immediately
                clearUpdateState()
                Log.d(TAG, "Update skipped from Browse screen")
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping update", e)
            }
        }
    }
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val shows: List<Show>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}