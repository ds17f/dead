package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.browse.domain.SearchShowsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val searchShowsUseCase: SearchShowsUseCase,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    init {
        // Load popular shows on startup
        searchShows("grateful dead 1977")
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
     * Get the current download state for a recording
     */
    fun getDownloadState(recording: Recording): DownloadState {
        return try {
            // For now, return Available state as a placeholder
            // In Task 5, we'll implement proper download progress tracking
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
            // For now, return NotDownloaded state as a placeholder
            // In Task 5, we'll implement proper download progress tracking
            // This would check the download state of the show's best recording
            ShowDownloadState.NotDownloaded
        } catch (e: Exception) {
            ShowDownloadState.Failed
        }
    }
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val shows: List<Show>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}