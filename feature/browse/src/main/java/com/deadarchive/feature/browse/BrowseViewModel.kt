package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.FavoriteRepository
import com.deadarchive.core.model.Concert
import com.deadarchive.feature.browse.domain.SearchConcertsUseCase
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
    private val searchConcertsUseCase: SearchConcertsUseCase,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Idle)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    init {
        // Load popular concerts on startup
        searchConcerts("grateful dead 1977")
    }
    
    fun updateSearchQuery(query: String) {
        println("📱 UI SEARCH: updateSearchQuery called with '$query'")
        _searchQuery.value = query
        // Auto-search when user types (with debouncing handled by UI)
        if (query.length >= 2) {
            println("📱 UI SEARCH: triggering search for '$query' (length ${query.length})")
            searchConcerts(query)
        } else if (query.isEmpty()) {
            println("📱 UI SEARCH: empty query, setting idle state")
            _uiState.value = BrowseUiState.Idle
        } else {
            println("📱 UI SEARCH: query too short '$query', not searching")
        }
    }
    
    fun searchConcerts(query: String = _searchQuery.value) {
        println("📱 VM SEARCH: searchConcerts called with '$query'")
        if (query.isBlank()) {
            println("📱 VM SEARCH: blank query, setting idle state")
            _uiState.value = BrowseUiState.Idle
            return
        }
        
        viewModelScope.launch {
            println("📱 VM SEARCH: starting coroutine for '$query'")
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                val startTime = System.currentTimeMillis()
                searchConcertsUseCase(query)
                    .catch { exception ->
                        val searchTime = System.currentTimeMillis() - startTime
                        println("📱 VM SEARCH: error after ${searchTime}ms for '$query': ${exception.message}")
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to search concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { concerts ->
                        val searchTime = System.currentTimeMillis() - startTime
                        println("📱 VM SEARCH: success after ${searchTime}ms for '$query', got ${concerts.size} concerts")
                        concerts.take(3).forEachIndexed { index, concert ->
                            println("  📱 [$index] ${concert.identifier} - ${concert.title} (${concert.date})")
                        }
                        _uiState.value = BrowseUiState.Success(concerts)
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                println("📱 VM SEARCH: exception for '$query': ${e.message}")
                _uiState.value = BrowseUiState.Error(
                    e.message ?: "Failed to search concerts"
                )
                _isSearching.value = false
            }
        }
    }
    
    fun toggleFavorite(concert: Concert) {
        viewModelScope.launch {
            try {
                if (concert.isFavorite) {
                    favoriteRepository.removeConcertFromFavorites(concert.identifier)
                } else {
                    favoriteRepository.addConcertToFavorites(concert)
                }
                // Refresh search to update favorite status
                searchConcerts()
            } catch (e: Exception) {
                // Could add error handling/snackbar here
            }
        }
    }
    
    fun loadPopularConcerts() {
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                searchConcertsUseCase.getPopularConcerts()
                    .catch { exception ->
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to load popular concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { concerts ->
                        _uiState.value = BrowseUiState.Success(concerts)
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
    
    fun loadRecentConcerts() {
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                searchConcertsUseCase.getRecentConcerts()
                    .catch { exception ->
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to load recent concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { concerts ->
                        _uiState.value = BrowseUiState.Success(concerts)
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
    data class Success(val concerts: List<Concert>) : BrowseUiState()
    data class Error(val message: String) : BrowseUiState()
}