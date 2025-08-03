package com.deadarchive.feature.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.search.api.SearchV2Service
import com.deadarchive.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * SearchV2ViewModel - State coordination for next-generation search interface
 * 
 * This ViewModel follows the V2 architecture pattern established by PlayerV2:
 * 1. UI drives the requirements (service interface discovered through UI building)
 * 2. ViewModel coordinates between UI and services
 * 3. Single service dependency with clean separation
 * 4. Reactive state flows for UI updates
 * 
 * The ViewModel provides basic state management foundation ready for
 * UI-first development where building UI components will discover the
 * exact service interface requirements.
 */
@HiltViewModel
class SearchV2ViewModel @Inject constructor(
    @Named("stub") private val searchV2Service: SearchV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchV2ViewModel"
    }
    
    // UI State 
    private val _uiState = MutableStateFlow(SearchV2UiState())
    val uiState: StateFlow<SearchV2UiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "SearchV2ViewModel initialized with SearchV2Service")
        loadInitialState()
        observeServiceFlows()
    }
    
    /**
     * Load initial state and populate test data
     */
    private fun loadInitialState() {
        Log.d(TAG, "Loading initial SearchV2 state")
        viewModelScope.launch {
            try {
                // Populate test data for immediate UI development
                searchV2Service.populateTestData()
                Log.d(TAG, "Initial SearchV2 state loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load initial SearchV2 state", e)
            }
        }
    }
    
    /**
     * Handle search query changes from UI
     */
    fun onSearchQueryChanged(query: String) {
        Log.d(TAG, "Search query changed: $query")
        viewModelScope.launch {
            try {
                // Add to recent searches if not empty
                if (query.isNotBlank()) {
                    searchV2Service.addRecentSearch(query)
                }
                // Trigger search
                searchV2Service.updateSearchQuery(query)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update search query", e)
            }
        }
    }
    
    /**
     * Handle recent search selection
     */
    fun onRecentSearchSelected(recentSearch: RecentSearch) {
        Log.d(TAG, "Recent search selected: ${recentSearch.query}")
        onSearchQueryChanged(recentSearch.query)
    }
    
    /**
     * Handle suggested search selection
     */
    fun onSuggestionSelected(suggestion: SuggestedSearch) {
        Log.d(TAG, "Suggestion selected: ${suggestion.query}")
        viewModelScope.launch {
            try {
                searchV2Service.selectSuggestion(suggestion)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to select suggestion", e)
            }
        }
    }
    
    /**
     * Clear search query and results
     */
    fun onClearSearch() {
        Log.d(TAG, "Clearing search")
        viewModelScope.launch {
            try {
                searchV2Service.clearSearch()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear search", e)
            }
        }
    }
    
    /**
     * Clear recent search history
     */
    fun onClearRecentSearches() {
        Log.d(TAG, "Clearing recent searches")
        viewModelScope.launch {
            try {
                searchV2Service.clearRecentSearches()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear recent searches", e)
            }
        }
    }
    
    /**
     * Observe service flows and update UI state
     */
    private fun observeServiceFlows() {
        viewModelScope.launch {
            searchV2Service.currentQuery.collect { query ->
                _uiState.value = _uiState.value.copy(searchQuery = query)
            }
        }
        
        viewModelScope.launch {
            searchV2Service.searchResults.collect { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
        
        viewModelScope.launch {
            searchV2Service.searchStatus.collect { status ->
                _uiState.value = _uiState.value.copy(
                    searchStatus = status,
                    isLoading = status == SearchStatus.SEARCHING,
                    error = if (status == SearchStatus.ERROR) "Search failed" else null
                )
            }
        }
        
        viewModelScope.launch {
            searchV2Service.recentSearches.collect { recent ->
                _uiState.value = _uiState.value.copy(recentSearches = recent)
            }
        }
        
        viewModelScope.launch {
            searchV2Service.suggestedSearches.collect { suggestions ->
                _uiState.value = _uiState.value.copy(suggestedSearches = suggestions)
            }
        }
        
        viewModelScope.launch {
            searchV2Service.searchStats.collect { stats ->
                _uiState.value = _uiState.value.copy(searchStats = stats)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SearchV2ViewModel cleared")
    }
}

/**
 * UI State for SearchV2Screen
 * 
 * Comprehensive state model discovered through UI-first development
 * and coordinated with SearchV2Service reactive flows.
 */
data class SearchV2UiState(
    val searchQuery: String = "",
    val searchResults: List<SearchResultShow> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus.IDLE,
    val recentSearches: List<RecentSearch> = emptyList(),
    val suggestedSearches: List<SuggestedSearch> = emptyList(),
    val searchStats: SearchStats = SearchStats(0, 0),
    val isLoading: Boolean = false,
    val error: String? = null
)