package com.deadarchive.feature.browse.service

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadarchive.feature.browse.domain.SearchShowsUseCase
import com.deadarchive.feature.browse.BrowseUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for search functionality in the Browse feature.
 * Handles search query management, show searching, and era filtering.
 */
@Singleton
class BrowseSearchService @Inject constructor(
    private val searchShowsUseCase: SearchShowsUseCase
) {
    
    companion object {
        private const val TAG = "BrowseSearchService"
    }
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    // Track current search job to cancel previous operations
    private var currentSearchJob: Job? = null
    
    /**
     * Update the search query and optionally trigger search
     */
    fun updateSearchQuery(query: String, coroutineScope: CoroutineScope, onStateChange: (BrowseUiState) -> Unit) {
        Log.d(TAG, "updateSearchQuery called with '$query'")
        _searchQuery.value = query
        
        // Auto-search when user types (with debouncing handled by UI)
        if (query.length >= 2) {
            Log.d(TAG, "triggering search for '$query' (length ${query.length})")
            searchShows(query, coroutineScope, onStateChange)
        } else if (query.isEmpty()) {
            Log.d(TAG, "empty query, setting idle state")
            onStateChange(BrowseUiState.Idle)
        } else {
            Log.d(TAG, "query too short '$query', not searching")
        }
    }
    
    /**
     * Search for shows with the given query
     */
    fun searchShows(
        query: String = _searchQuery.value,
        coroutineScope: CoroutineScope,
        onStateChange: (BrowseUiState) -> Unit
    ) {
        Log.d(TAG, "searchShows called with '$query'")
        if (query.isBlank()) {
            Log.d(TAG, "blank query, setting idle state")
            onStateChange(BrowseUiState.Idle)
            return
        }
        
        // Cancel any previous search operation
        currentSearchJob?.cancel()
        
        currentSearchJob = coroutineScope.launch {
            Log.d(TAG, "starting coroutine for '$query'")
            _isSearching.value = true
            onStateChange(BrowseUiState.Loading)
            
            try {
                val startTime = System.currentTimeMillis()
                searchShowsUseCase(query)
                    .catch { exception ->
                        val searchTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "error after ${searchTime}ms for '$query': ${exception.message}")
                        onStateChange(BrowseUiState.Error(
                            exception.message ?: "Failed to search concerts"
                        ))
                        _isSearching.value = false
                    }
                    .collect { shows ->
                        val searchTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "success after ${searchTime}ms for '$query', got ${shows.size} shows")
                        shows.take(3).forEachIndexed { index, show ->
                            Log.d(TAG, "[$index] ${show.showId} - ${show.displayTitle} (${show.date}) - ${show.recordingCount} recordings")
                        }
                        onStateChange(BrowseUiState.Success(shows))
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "exception for '$query': ${e.message}")
                onStateChange(BrowseUiState.Error(
                    e.message ?: "Failed to search concerts"
                ))
                _isSearching.value = false
            }
        }
    }
    
    /**
     * Filter shows by era (decade)
     */
    fun filterByEra(
        era: String,
        coroutineScope: CoroutineScope,
        onStateChange: (BrowseUiState) -> Unit
    ) {
        Log.d(TAG, "filtering by era '$era'")
        
        // Cancel any previous search/filter operation
        currentSearchJob?.cancel()
        
        currentSearchJob = coroutineScope.launch {
            _isSearching.value = true
            onStateChange(BrowseUiState.Loading)
            
            try {
                val startTime = System.currentTimeMillis()
                
                // Map era to search query pattern
                val eraQuery = when (era.lowercase()) {
                    "1970s" -> "197"  // Will match 1970-1979 dates
                    "1980s" -> "198"  // Will match 1980-1989 dates  
                    "1990s" -> "199"  // Will match 1990-1999 dates
                    else -> era.lowercase()
                }
                
                Log.d(TAG, "searching with era query: '$eraQuery' for era '$era'")
                
                searchShowsUseCase(eraQuery)
                    .catch { exception ->
                        val searchTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "era filter error after ${searchTime}ms for '$era': ${exception.message}")
                        onStateChange(BrowseUiState.Error(
                            exception.message ?: "Failed to filter by era"
                        ))
                        _isSearching.value = false
                    }
                    .collect { shows ->
                        val searchTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "era filter success after ${searchTime}ms for '$era', got ${shows.size} shows")
                        
                        // Additional client-side filtering to ensure accuracy
                        val filteredShows = shows.filter { show ->
                            when (era.lowercase()) {
                                "1970s" -> show.date.startsWith("197")
                                "1980s" -> show.date.startsWith("198")
                                "1990s" -> show.date.startsWith("199")
                                else -> true
                            }
                        }
                        
                        Log.d(TAG, "after client-side filtering: ${filteredShows.size} shows for era '$era'")
                        onStateChange(BrowseUiState.Success(filteredShows))
                        _isSearching.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "era filter exception for '$era': ${e.message}")
                onStateChange(BrowseUiState.Error(
                    e.message ?: "Failed to filter by era"
                ))
                _isSearching.value = false
            }
        }
    }
    
    /**
     * Cancel any current search operation
     */
    fun cancelCurrentSearch() {
        currentSearchJob?.cancel()
        _isSearching.value = false
    }
}