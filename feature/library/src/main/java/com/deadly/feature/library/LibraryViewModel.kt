package com.deadly.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.core.model.LibraryItem
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import com.deadly.core.design.component.DownloadState
import com.deadly.core.design.component.ShowDownloadState
import com.deadly.feature.library.service.LibraryDataService
import com.deadly.core.data.download.DownloadService
import com.deadly.core.data.service.LibraryService
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class LibrarySortOption(val displayName: String) {
    DATE_ASCENDING("Show Date (Oldest First)"),
    DATE_DESCENDING("Show Date (Newest First)"), 
    ADDED_ASCENDING("Added (Oldest First)"),
    ADDED_DESCENDING("Added (Newest First)")
}

enum class DecadeFilter(val displayName: String, val decade: String?) {
    ALL("All", null),
    SIXTIES("60s", "196"),
    SEVENTIES("70s", "197"),
    EIGHTIES("80s", "198"), 
    NINETIES("90s", "199")
}

/**
 * ViewModel for library screen using service-oriented architecture.
 * Coordinates between UI and focused services for better maintainability.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dataService: LibraryDataService,
    private val downloadService: DownloadService,
    private val libraryService: LibraryService
) : ViewModel() {
    
    // UI State (managed by ViewModel, updated by services)
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    // Expose service state flows
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = downloadService.downloadStates
    val showConfirmationDialog: StateFlow<Show?> = downloadService.showConfirmationDialog
    val sortOption: StateFlow<LibrarySortOption> = dataService.sortOption
    val decadeFilter: StateFlow<DecadeFilter> = dataService.decadeFilter
    
    // Individual downloads for queue management
    val allDownloads = downloadService.getAllDownloads()
    
    /**
     * Get enriched downloads with show and track metadata
     */
    suspend fun getEnrichedDownloads() = downloadService.getEnrichedDownloads()
    
    init {
        // Load library data via service
        dataService.loadLibraryItems(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
        // Download state monitoring is automatically handled by shared DownloadService
    }
    
    // Data loading is now handled by LibraryDataService
    
    fun removeFromLibrary(libraryItem: LibraryItem) {
        viewModelScope.launch {
            try {
                libraryService.removeFromLibrary(libraryItem.showId)
                // UI updates automatically via reactive flows
            } catch (e: Exception) {
                // Handle error - could show snackbar or error state
                android.util.Log.e("LibraryViewModel", "Failed to remove library item ${libraryItem.id}: ${e.message}")
            }
        }
    }
    
    fun removeShowFromLibrary(showId: String) {
        viewModelScope.launch {
            try {
                libraryService.removeFromLibrary(showId)
                // UI updates automatically via reactive flows
            } catch (e: Exception) {
                // Handle error - could show snackbar or error state
                android.util.Log.e("LibraryViewModel", "Failed to remove show $showId from library: ${e.message}")
            }
        }
    }
    
    fun clearLibrary() {
        viewModelScope.launch {
            try {
                libraryService.clearLibrary()
                // UI updates automatically via reactive flows
            } catch (e: Exception) {
                // Handle error - could show snackbar or error state
                android.util.Log.e("LibraryViewModel", "Failed to clear library: ${e.message}")
            }
        }
    }
    
    fun retry() {
        dataService.loadLibraryItems(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
    }
    
    /**
     * Start downloading a recording
     */
    fun downloadRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                downloadService.downloadRecording(recording)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to start download: ${e.message}")
            }
        }
    }
    
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
                _uiState.value = LibraryUiState.Error("Failed to start download: ${e.message}")
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
                _uiState.value = LibraryUiState.Error(errorMessage)
            }
        )
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        viewModelScope.launch {
            try {
                downloadService.cancelShowDownloads(show)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to cancel downloads: ${e.message}")
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
                _uiState.value = LibraryUiState.Error("Failed to clear downloads: ${e.message}")
            }
        }
    }
    
    // Download state monitoring is now handled by LibraryDownloadService
    
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
                _uiState.value = LibraryUiState.Error("Failed to remove download: ${e.message}")
            }
        }
    }
    
    /**
     * Handle library button actions from unified LibraryButton component
     */
    fun handleLibraryAction(action: com.deadly.core.design.component.LibraryAction, show: Show) {
        viewModelScope.launch {
            try {
                when (action) {
                    com.deadly.core.design.component.LibraryAction.ADD_TO_LIBRARY -> {
                        libraryService.addToLibrary(show)
                    }
                    com.deadly.core.design.component.LibraryAction.REMOVE_FROM_LIBRARY -> {
                        libraryService.removeFromLibrary(show)
                    }
                    com.deadly.core.design.component.LibraryAction.REMOVE_WITH_DOWNLOADS -> {
                        libraryService.removeShowWithDownloadCleanup(show, alsoRemoveDownloads = true)
                    }
                }
                // UI updates automatically via reactive flows
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to update library: ${e.message}")
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
    fun getLibraryRemovalInfo(show: Show): com.deadly.core.data.service.LibraryRemovalInfo {
        return try {
            var result = com.deadly.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Checking...",
                downloadState = com.deadly.core.design.component.ShowDownloadState.NotDownloaded
            )
            
            viewModelScope.launch {
                result = libraryService.getDownloadInfoForShow(show)
            }
            
            // Return default while async operation completes
            result
        } catch (e: Exception) {
            com.deadly.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Error checking downloads",
                downloadState = com.deadly.core.design.component.ShowDownloadState.NotDownloaded
            )
        }
    }
    
    /**
     * Set the sort option
     */
    fun setSortOption(option: LibrarySortOption) {
        dataService.setSortOption(option)
    }
    
    /**
     * Set the decade filter
     */
    fun setDecadeFilter(filter: DecadeFilter) {
        dataService.setDecadeFilter(filter)
    }
    
    // Download Queue Management Methods
    
    /**
     * Cancel an individual download
     */
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.cancelDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to cancel download: ${e.message}")
            }
        }
    }
    
    /**
     * Retry a failed download
     */
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.retryDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to retry download: ${e.message}")
            }
        }
    }
    
    /**
     * Force start a queued download
     */
    fun forceDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.forceDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to force download: ${e.message}")
            }
        }
    }
    
    /**
     * Remove a download completely from the system
     */
    fun removeDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.removeDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to remove download: ${e.message}")
            }
        }
    }
    
    /**
     * Pause a download
     */
    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.pauseDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to pause download: ${e.message}")
            }
        }
    }
    
    /**
     * Resume a paused download
     */
    fun resumeDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadService.resumeDownload(downloadId)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to resume download: ${e.message}")
            }
        }
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val libraryItems: List<LibraryItem>,
        val shows: List<Show>
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}