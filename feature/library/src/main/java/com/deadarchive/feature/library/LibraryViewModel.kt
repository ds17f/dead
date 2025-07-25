package com.deadarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.library.service.LibraryDataService
import com.deadarchive.core.data.download.DownloadService
import kotlinx.coroutines.launch
import com.deadarchive.feature.library.service.LibraryManagementService
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
    private val managementService: LibraryManagementService
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
        managementService.removeFromLibrary(
            libraryItem = libraryItem,
            coroutineScope = viewModelScope
        )
    }
    
    fun removeShowFromLibrary(showId: String) {
        managementService.removeShowFromLibrary(
            showId = showId,
            coroutineScope = viewModelScope
        )
    }
    
    fun clearLibrary() {
        managementService.clearLibrary(viewModelScope)
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