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
        _searchQuery.value = query
        // Auto-search when user types (with debouncing handled by UI)
        if (query.length >= 2) {
            searchConcerts(query)
        } else if (query.isEmpty()) {
            _uiState.value = BrowseUiState.Idle
        }
    }
    
    fun searchConcerts(query: String = _searchQuery.value) {
        if (query.isBlank()) {
            _uiState.value = BrowseUiState.Idle
            return
        }
        
        viewModelScope.launch {
            _isSearching.value = true
            _uiState.value = BrowseUiState.Loading
            
            try {
                searchConcertsUseCase(query)
                    .catch { exception ->
                        _uiState.value = BrowseUiState.Error(
                            exception.message ?: "Failed to search concerts"
                        )
                        _isSearching.value = false
                    }
                    .collect { concerts ->
                        _uiState.value = BrowseUiState.Success(concerts)
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
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