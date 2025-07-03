package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.FavoriteRepository
import com.deadarchive.core.model.Show
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
    private val favoriteRepository: FavoriteRepository
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
    
    fun toggleFavorite(show: Show) {
        viewModelScope.launch {
            try {
                // For Show, we favorite the best recording
                show.bestRecording?.let { recording ->
                    if (show.isFavorite) {
                        favoriteRepository.removeRecordingFromFavorites(recording.identifier)
                    } else {
                        favoriteRepository.addRecordingToFavorites(recording)
                    }
                }
                // Refresh search to update favorite status
                searchShows()
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
}

sealed class BrowseUiState {
    object Idle : BrowseUiState()
    object Loading : BrowseUiState()
    data class Success(val shows: List<Show>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}