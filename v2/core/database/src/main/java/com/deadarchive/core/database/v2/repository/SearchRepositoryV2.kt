package com.deadarchive.v2.core.database.repository

import android.util.Log
import com.deadarchive.v2.core.database.service.SearchServiceV2
import com.deadarchive.v2.core.database.service.SearchResultV2
import com.deadarchive.v2.core.database.service.SearchMatchType
import com.deadarchive.v2.core.database.service.SearchStatsV2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class SearchQuery(
    val query: String,
    val filters: SearchFilters = SearchFilters()
)

data class SearchFilters(
    val yearRange: IntRange? = null,
    val includeTypes: Set<SearchMatchType> = SearchMatchType.values().toSet(),
    val minRating: Double? = null,
    val hasSetlist: Boolean? = null,
    val hasRecordings: Boolean? = null,
    val limit: Int = 50
)

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val results: List<SearchResultV2>, val query: String) : SearchState()
    data class Error(val message: String) : SearchState()
}

@Singleton
class SearchRepositoryV2 @Inject constructor(
    private val searchService: SearchServiceV2
) {
    
    companion object {
        private const val TAG = "SearchRepositoryV2"
        private const val MIN_SEARCH_LENGTH = 2
    }
    
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()
    
    private val _searchStats = MutableStateFlow<SearchStatsV2?>(null)
    val searchStats: StateFlow<SearchStatsV2?> = _searchStats.asStateFlow()
    
    /**
     * Perform search with advanced filtering
     */
    suspend fun search(searchQuery: SearchQuery): Flow<SearchState> = flow {
        val query = searchQuery.query.trim()
        
        if (query.length < MIN_SEARCH_LENGTH) {
            emit(SearchState.Error("Search query must be at least $MIN_SEARCH_LENGTH characters"))
            return@flow
        }
        
        emit(SearchState.Loading)
        _searchState.value = SearchState.Loading
        
        try {
            val results = searchService.searchAll(
                query = query,
                limit = searchQuery.filters.limit,
                includeTypes = searchQuery.filters.includeTypes
            )
            
            // Apply additional filters
            val filteredResults = applyFilters(results, searchQuery.filters)
            
            // Add to recent searches
            addToRecentSearches(query)
            
            val successState = SearchState.Success(filteredResults, query)
            emit(successState)
            _searchState.value = successState
            
            Log.d(TAG, "Search completed: ${filteredResults.size} results for '$query'")
            
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: '$query'", e)
            val errorState = SearchState.Error("Search failed: ${e.message}")
            emit(errorState)
            _searchState.value = errorState
        }
    }.catch { throwable ->
        Log.e(TAG, "Flow error during search", throwable)
        val errorState = SearchState.Error("Search error: ${throwable.message}")
        emit(errorState)
        _searchState.value = errorState
    }
    
    /**
     * Quick search with default filters
     */
    suspend fun quickSearch(query: String): Flow<SearchState> {
        return search(SearchQuery(query))
    }
    
    /**
     * Get popular shows (high rated shows)
     */
    suspend fun getPopularShows(limit: Int = 20): Flow<List<SearchResultV2>> = flow {
        try {
            val results = searchService.getHighRatedShows(minRating = 2.5, limit = limit)
            emit(results)
            Log.d(TAG, "Retrieved ${results.size} popular shows")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting popular shows", e)
            emit(emptyList())
        }
    }
    
    /**
     * Get shows by year
     */
    suspend fun getShowsByYear(year: Int): Flow<List<SearchResultV2>> = flow {
        try {
            val results = searchService.getShowsByYear(year)
            emit(results)
            Log.d(TAG, "Retrieved ${results.size} shows for year $year")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shows by year $year", e)
            emit(emptyList())
        }
    }
    
    /**
     * Get shows by date range
     */
    suspend fun getShowsByDateRange(startDate: String, endDate: String): Flow<List<SearchResultV2>> = flow {
        try {
            val results = searchService.getShowsByDateRange(startDate, endDate)
            emit(results)
            Log.d(TAG, "Retrieved ${results.size} shows for date range $startDate to $endDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shows by date range", e)
            emit(emptyList())
        }
    }
    
    /**
     * Get available years for filtering
     */
    suspend fun getAvailableYears(): Flow<List<Int>> = flow {
        try {
            val years = searchService.getAllYears()
            emit(years)
            Log.d(TAG, "Retrieved ${years.size} available years")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available years", e)
            emit(emptyList())
        }
    }
    
    /**
     * Load search statistics
     */
    suspend fun loadSearchStats(): Flow<SearchStatsV2> = flow {
        try {
            val stats = searchService.getSearchStats()
            emit(stats)
            _searchStats.value = stats
            Log.d(TAG, "Loaded search stats: $stats")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading search stats", e)
            val emptyStats = SearchStatsV2(0, 0, 0, 0)
            emit(emptyStats)
            _searchStats.value = emptyStats
        }
    }
    
    /**
     * Clear search state
     */
    fun clearSearch() {
        _searchState.value = SearchState.Idle
        Log.d(TAG, "Search state cleared")
    }
    
    /**
     * Clear recent searches
     */
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        Log.d(TAG, "Recent searches cleared")
    }
    
    /**
     * Advanced search with multiple criteria
     */
    suspend fun advancedSearch(
        query: String,
        yearRange: IntRange? = null,
        includeTypes: Set<SearchMatchType> = SearchMatchType.values().toSet(),
        minRating: Double? = null,
        hasSetlist: Boolean? = null,
        hasRecordings: Boolean? = null,
        limit: Int = 50
    ): Flow<SearchState> {
        val filters = SearchFilters(
            yearRange = yearRange,
            includeTypes = includeTypes,
            minRating = minRating,
            hasSetlist = hasSetlist,
            hasRecordings = hasRecordings,
            limit = limit
        )
        return search(SearchQuery(query, filters))
    }
    
    /**
     * Search suggestions based on partial query
     */
    suspend fun getSearchSuggestions(partialQuery: String): Flow<List<String>> = flow {
        try {
            if (partialQuery.length < 2) {
                emit(emptyList())
                return@flow
            }
            
            // For now, return recent searches that match
            // In the future, this could query song names, venue names, etc.
            val suggestions = _recentSearches.value
                .filter { it.contains(partialQuery, ignoreCase = true) }
                .take(5)
            
            emit(suggestions)
            Log.d(TAG, "Generated ${suggestions.size} suggestions for '$partialQuery'")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search suggestions", e)
            emit(emptyList())
        }
    }
    
    // Private helper methods
    
    private fun applyFilters(results: List<SearchResultV2>, filters: SearchFilters): List<SearchResultV2> {
        var filtered = results
        
        // Apply year range filter
        filters.yearRange?.let { range ->
            filtered = filtered.filter { result ->
                val year = result.date.substring(0, 4).toIntOrNull()
                year != null && year in range
            }
        }
        
        // Apply minimum rating filter
        filters.minRating?.let { minRating ->
            filtered = filtered.filter { it.rating >= minRating }
        }
        
        // Apply setlist filter
        filters.hasSetlist?.let { requireSetlist ->
            filtered = filtered.filter { it.hasSetlist == requireSetlist }
        }
        
        // Apply recordings filter
        filters.hasRecordings?.let { requireRecordings ->
            filtered = filtered.filter { (it.recordingCount > 0) == requireRecordings }
        }
        
        return filtered.take(filters.limit)
    }
    
    private fun addToRecentSearches(query: String) {
        val currentSearches = _recentSearches.value.toMutableList()
        
        // Remove if already exists
        currentSearches.remove(query)
        
        // Add to front
        currentSearches.add(0, query)
        
        // Keep only last 10 searches
        if (currentSearches.size > 10) {
            currentSearches.removeAt(currentSearches.size - 1)
        }
        
        _recentSearches.value = currentSearches
    }
}