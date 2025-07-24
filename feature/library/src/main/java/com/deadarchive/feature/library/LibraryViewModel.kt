package com.deadarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.library.service.LibraryDataService
import com.deadarchive.feature.library.service.LibraryDownloadService
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
    private val downloadService: LibraryDownloadService,
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
    
    init {
        // Load library data via service
        dataService.loadLibraryItems(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it }
        )
        // Start monitoring download states via service
        downloadService.startDownloadStateMonitoring(viewModelScope)
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
        downloadService.downloadRecording(
            recording = recording,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
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
        downloadService.downloadShow(
            show = show,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        downloadService.cancelShowDownloads(
            show = show,
            coroutineScope = viewModelScope
        )
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
        downloadService.confirmRemoveDownload(viewModelScope)
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
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val libraryItems: List<LibraryItem>,
        val shows: List<Show>
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}