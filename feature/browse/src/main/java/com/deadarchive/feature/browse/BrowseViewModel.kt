package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.browse.service.BrowseSearchService
import com.deadarchive.feature.browse.service.BrowseLibraryService
import com.deadarchive.feature.browse.service.BrowseDownloadService
import com.deadarchive.feature.browse.service.BrowseDataService
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
    private val libraryService: BrowseLibraryService,
    private val downloadService: BrowseDownloadService,
    private val dataService: BrowseDataService
) : ViewModel() {
    
    // UI State (managed by ViewModel, updated by services)
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    // Expose service state flows
    val searchQuery: StateFlow<String> = searchService.searchQuery
    val isSearching: StateFlow<Boolean> = searchService.isSearching
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = downloadService.downloadStates
    val showConfirmationDialog: StateFlow<Show?> = downloadService.showConfirmationDialog
    
    init {
        // Load initial data on startup
        dataService.loadInitialData(
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            onSearchingStateChange = { /* handled by searchService */ }
        )
        // Start monitoring download states via service
        downloadService.startDownloadStateMonitoring(viewModelScope)
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
        libraryService.toggleLibrary(
            show = show,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
        )
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
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        downloadService.cancelShowDownloads(
            show = show,
            coroutineScope = viewModelScope
        )
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
        downloadService.downloadShow(
            show = show,
            coroutineScope = viewModelScope,
            onStateChange = { _uiState.value = it },
            currentState = _uiState.value
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
        downloadService.confirmRemoveDownload(viewModelScope)
    }
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val shows: List<Show>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}