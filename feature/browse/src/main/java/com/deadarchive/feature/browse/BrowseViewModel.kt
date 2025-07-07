package com.deadarchive.feature.browse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.download.worker.AudioDownloadWorker
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.browse.domain.SearchShowsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val searchShowsUseCase: SearchShowsUseCase,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val workManager = WorkManager.getInstance(context)
    
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    // Download state tracking
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    init {
        // Load popular shows on startup
        searchShows("grateful dead 1977")
        // Start monitoring download states
        startDownloadStateMonitoring()
    }
    
    fun updateSearchQuery(query: String) {
        println("📱 UI SEARCH: updateSearchQuery called with '$query'")
        _searchQuery.value = query
        // Auto-search when user types (with debouncing handled by UI)
        if (query.length >= 2) {
            println("📱 UI SEARCH: triggering search for '$query' (length ${query.length})")
            searchShows(query)
        } else if (query.isEmpty()) {
            println("📱 UI SEARCH: empty query, setting idle state")
            _uiState.value = BrowseUiState.Idle
        } else {
            println("📱 UI SEARCH: query too short '$query', not searching")
        }
    }
    
    
    fun searchShows(query: String = _searchQuery.value) {
        println("📱 VM SEARCH NEW: searchConcertsNew called with '$query'")
        if (query.isBlank()) {
            println("📱 VM SEARCH NEW: blank query, setting idle state")
            _uiState.value = BrowseUiState.Idle
            return
        }
        
        viewModelScope.launch {
            println("📱 VM SEARCH NEW: starting coroutine for '$query'")
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                val startTime = System.currentTimeMillis()
                searchShowsUseCase(query)
                    .catch { exception ->
                        val searchTime = System.currentTimeMillis() - startTime
                        println("📱 VM SEARCH NEW: error after ${searchTime}ms for '$query': ${exception.message}")
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to search concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { shows ->
                        val searchTime = System.currentTimeMillis() - startTime
                        println("📱 VM SEARCH NEW: success after ${searchTime}ms for '$query', got ${shows.size} shows")
                        shows.take(3).forEachIndexed { index, show ->
                            println("  📱 [$index] ${show.showId} - ${show.displayTitle} (${show.date}) - ${show.recordingCount} recordings")
                        }
                        _uiState.value = BrowseUiState.Success(shows)
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                println("📱 VM SEARCH NEW: exception for '$query': ${e.message}")
                _uiState.value = BrowseUiState.Error(
                    e.message ?: "Failed to search concerts"
                )
                _isSearching.value = false
            }
        }
    }
    
    fun toggleLibrary(show: Show) {
        viewModelScope.launch {
            try {
                // Add/remove the show to/from library
                val isInLibrary = libraryRepository.toggleShowLibrary(show)
                
                // Update the UI state locally instead of refreshing search
                val currentState = _uiState.value
                if (currentState is BrowseUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        if (existingShow.showId == show.showId) {
                            existingShow.copy(isInLibrary = isInLibrary)
                        } else {
                            existingShow
                        }
                    }
                    _uiState.value = BrowseUiState.Success(updatedShows)
                }
            } catch (e: Exception) {
                // Could add error handling/snackbar here
            }
        }
    }
    
    fun loadPopularShows() {
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                searchShowsUseCase.getPopularShows()
                    .catch { exception ->
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to load popular concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { shows ->
                        _uiState.value = BrowseUiState.Success(shows)
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error(
                    e.message ?: "Failed to load popular concerts"
                )
                _isSearching.value = false
            }
        }
    }
    
    fun loadRecentShows() {
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                searchShowsUseCase.getRecentShows()
                    .catch { exception ->
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to load recent concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { shows ->
                        _uiState.value = BrowseUiState.Success(shows)
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                _uiState.value = BrowseUiState.Error(
                    e.message ?: "Failed to load recent concerts"
                )
                _isSearching.value = false
            }
        }
    }
    
    /**
     * Start downloading a recording
     */
    fun downloadRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                downloadRepository.downloadRecording(recording)
            } catch (e: Exception) {
                // Could add error handling/snackbar here
                println("Failed to start download for recording ${recording.identifier}: ${e.message}")
            }
        }
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        viewModelScope.launch {
            try {
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    downloadRepository.cancelRecordingDownloads(bestRecording.identifier)
                } else {
                    println("No best recording found for show ${show.showId}")
                }
            } catch (e: Exception) {
                println("Failed to cancel downloads for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Start monitoring download states for all recordings
     */
    private fun startDownloadStateMonitoring() {
        viewModelScope.launch {
            // Monitor all downloads and update states
            downloadRepository.getAllDownloads().collect { downloads ->
                val stateMap = mutableMapOf<String, ShowDownloadState>()
                
                // Group downloads by recording ID
                val downloadsByRecording = downloads.groupBy { it.recordingId }
                
                downloadsByRecording.forEach { (recordingId, recordingDownloads) ->
                    val showDownloadState = when {
                        recordingDownloads.all { it.status == DownloadStatus.COMPLETED } -> {
                            ShowDownloadState.Downloaded
                        }
                        recordingDownloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED } -> {
                            // Calculate track-based progress (Spotify-style immediate feedback)
                            val totalTracks = recordingDownloads.size
                            val completedTracks = recordingDownloads.count { it.status == DownloadStatus.COMPLETED }
                            
                            // Get byte progress from actively downloading track if any
                            val downloadingTrack = recordingDownloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                            val byteProgress = downloadingTrack?.progress ?: -1f
                            val bytesDownloaded = downloadingTrack?.bytesDownloaded ?: 0L
                            
                            ShowDownloadState.Downloading(
                                progress = byteProgress,
                                bytesDownloaded = bytesDownloaded,
                                completedTracks = completedTracks,
                                totalTracks = totalTracks
                            )
                        }
                        recordingDownloads.any { it.status == DownloadStatus.FAILED } -> {
                            val failedTrack = recordingDownloads.first { it.status == DownloadStatus.FAILED }
                            ShowDownloadState.Failed(failedTrack.errorMessage)
                        }
                        else -> {
                            ShowDownloadState.NotDownloaded
                        }
                    }
                    
                    stateMap[recordingId] = showDownloadState
                }
                
                _downloadStates.value = stateMap
            }
        }
    }
    
    /**
     * Get the current download state for a recording
     */
    fun getDownloadState(recording: Recording): DownloadState {
        return try {
            // This is a simplified version for individual recording downloads
            // For now, return Available state as most UI uses show-level downloads
            DownloadState.Available
        } catch (e: Exception) {
            DownloadState.Error("Failed to get download state")
        }
    }
    
    /**
     * Start downloading the best recording of a show
     */
    fun downloadShow(show: Show) {
        viewModelScope.launch {
            try {
                // Get the best recording for this show
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    println("Downloading best recording for show ${show.showId}: ${bestRecording.identifier}")
                    downloadRepository.downloadRecording(bestRecording)
                } else {
                    println("No best recording available for show ${show.showId}")
                }
            } catch (e: Exception) {
                println("Failed to start download for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Get the current download state for a show (based on its best recording)
     */
    fun getShowDownloadState(show: Show): ShowDownloadState {
        return try {
            val bestRecording = show.bestRecording
            if (bestRecording != null) {
                // Get the current state from our monitored state map
                _downloadStates.value[bestRecording.identifier] ?: ShowDownloadState.NotDownloaded
            } else {
                ShowDownloadState.NotDownloaded
            }
        } catch (e: Exception) {
            ShowDownloadState.Failed("Failed to get download state")
        }
    }
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val shows: List<Show>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}