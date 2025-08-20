package com.deadly.v2.feature.search.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.api.search.SearchService
import com.deadly.v2.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * SearchViewModel - State coordination for next-generation search interface
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
class SearchViewModel @Inject constructor(
    private val searchService: SearchService
) : ViewModel() {
    
    companion object {
        private const val TAG = "SearchViewModel"
    }
    
    // UI State 
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Debounced search query flow
    private val _searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    
    init {
        Log.d(TAG, "SearchViewModel initialized with SearchService")
        loadInitialState()
        observeServiceFlows()
        setupDebouncedSearch()
    }
    
    /**
     * Load initial state and populate test data
     */
    private fun loadInitialState() {
        Log.d(TAG, "Loading initial Search state")
        viewModelScope.launch {
            try {
                // Populate test data for immediate UI development
                searchService.populateTestData()
                Log.d(TAG, "Initial Search state loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load initial Search state", e)
            }
        }
    }
    
    /**
     * Handle search query changes from UI with debounced search execution
     */
    fun onSearchQueryChanged(query: String) {
        Log.d(TAG, "Search query changed: $query")
        
        // Update the debounced flow which will trigger search after delay
        _searchQueryFlow.value = query
    }
    
    /**
     * Setup debounced search to avoid hammering the search service
     */
    private fun setupDebouncedSearch() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(500) // Wait 500ms after typing stops
                .distinctUntilChanged() // Only trigger if query actually changed
                .collect { query ->
                    performDebouncedSearch(query)
                }
        }
    }
    
    /**
     * Perform the actual search after debounce delay
     */
    private suspend fun performDebouncedSearch(query: String) {
        Log.d(TAG, "Performing debounced search for: '$query'")
        
        try {
            // Cancel any previous search job
            searchJob?.cancel()
            
            // Start new search job
            searchJob = viewModelScope.launch {
                // Trigger search in service (this will update currentQuery and perform search)
                searchService.updateSearchQuery(query)
                
                // Add to recent searches only if it's a meaningful query (3+ chars)
                // and only after the search has been "committed" by the delay
                if (query.length >= 3) {
                    searchService.addRecentSearch(query)
                    Log.d(TAG, "Added '$query' to recent searches")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform debounced search for '$query'", e)
        }
    }
    
    /**
     * Handle recent search selection - execute immediately without debounce
     */
    fun onRecentSearchSelected(recentSearch: RecentSearch) {
        Log.d(TAG, "Recent search selected: ${recentSearch.query}")
        
        // Execute search immediately (no debounce for deliberate selections)
        viewModelScope.launch {
            try {
                searchService.updateSearchQuery(recentSearch.query)
                // Don't re-add to recent searches since it's already there
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute recent search", e)
            }
        }
    }
    
    /**
     * Handle suggested search selection - execute immediately without debounce
     */
    fun onSuggestionSelected(suggestion: SuggestedSearch) {
        Log.d(TAG, "Suggestion selected: ${suggestion.query}")
        
        // Execute search immediately (no debounce for deliberate selections)
        viewModelScope.launch {
            try {
                searchService.selectSuggestion(suggestion)
                // Add to recent searches since this is a deliberate action
                searchService.addRecentSearch(suggestion.query)
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
                searchService.clearSearch()
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
                searchService.clearRecentSearches()
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
            searchService.currentQuery.collect { query ->
                _uiState.value = _uiState.value.copy(searchQuery = query)
            }
        }
        
        viewModelScope.launch {
            searchService.searchResults.collect { results ->
                _uiState.value = _uiState.value.copy(searchResults = results)
            }
        }
        
        viewModelScope.launch {
            searchService.searchStatus.collect { status ->
                _uiState.value = _uiState.value.copy(
                    searchStatus = status,
                    isLoading = status == SearchStatus.SEARCHING,
                    error = if (status == SearchStatus.ERROR) "Search failed" else null
                )
            }
        }
        
        viewModelScope.launch {
            searchService.recentSearches.collect { recent ->
                _uiState.value = _uiState.value.copy(recentSearches = recent)
            }
        }
        
        viewModelScope.launch {
            searchService.suggestedSearches.collect { suggestions ->
                _uiState.value = _uiState.value.copy(suggestedSearches = suggestions)
            }
        }
        
        viewModelScope.launch {
            searchService.searchStats.collect { stats ->
                _uiState.value = _uiState.value.copy(searchStats = stats)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SearchViewModel cleared")
        
        // Cancel any pending search jobs
        searchJob?.cancel()
    }
}