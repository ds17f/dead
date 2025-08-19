package com.deadarchive.v2.core.search

import android.util.Log
import com.deadarchive.v2.core.api.search.SearchService
import com.deadarchive.v2.core.api.search.SearchFilter
import com.deadarchive.v2.core.model.*
import com.deadarchive.v2.core.model.V2Database
import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.ShowSearchDao
import com.deadarchive.v2.core.database.entities.ShowEntity
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real FTS5 implementation of SearchService using Room database.
 * 
 * Core functionality only:
 * - FTS5 search for show IDs
 * - Batch lookup for full entities  
 * - Return results with FTS5 ranking preserved
 */
@Singleton
class SearchServiceImpl @Inject constructor(
    @V2Database private val showDao: ShowDao,
    @V2Database private val showSearchDao: ShowSearchDao
) : SearchService {
    
    companion object {
        private const val TAG = "SearchServiceImpl"
    }
    
    // Reactive state management
    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    private val _searchStatus = MutableStateFlow(SearchStatus.IDLE)
    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    private val _searchStats = MutableStateFlow(SearchStats(0, 0))
    
    // Public reactive flows
    override val currentQuery: Flow<String> = _currentQuery.asStateFlow()
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    override val searchStatus: Flow<SearchStatus> = _searchStatus.asStateFlow()
    override val recentSearches: Flow<List<RecentSearch>> = _recentSearches.asStateFlow()
    override val suggestedSearches: Flow<List<SuggestedSearch>> = MutableStateFlow<List<SuggestedSearch>>(emptyList()).asStateFlow()
    override val searchStats: Flow<SearchStats> = _searchStats.asStateFlow()
    
    override suspend fun updateSearchQuery(query: String): Result<Unit> {
        Log.d(TAG, "updateSearchQuery(query='$query') called")
        
        _currentQuery.value = query
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchStatus.value = SearchStatus.IDLE
            _searchStats.value = SearchStats(0, 0)
            return Result.success(Unit)
        }
        
        _searchStatus.value = SearchStatus.SEARCHING
        
        try {
            val startTime = System.currentTimeMillis()
            
            // FTS5 search for show IDs (BM25 ranking preserved in order)
            val showIds = showSearchDao.searchShows(query)
            Log.d(TAG, "FTS5 search returned ${showIds.size} show IDs")
            
            // Batch lookup for full show entities
            val shows = if (showIds.isNotEmpty()) {
                showDao.getShowsByIds(showIds)
            } else {
                emptyList<ShowEntity>()
            }
            
            Log.d(TAG, "Retrieved ${shows.size} full shows")
            
            // Convert to SearchResultShow preserving FTS5 ranking
            val results = shows.mapIndexed { index: Int, showEntity: ShowEntity ->
                val ftsRankScore = 1.0f - (index.toFloat() / showIds.size.coerceAtLeast(1))
                val matchType = determineMatchType(showEntity, query)
                val show = showEntity.toDomainShow()
                
                SearchResultShow(
                    show = show,
                    relevanceScore = ftsRankScore,
                    matchType = matchType,
                    hasDownloads = false
                )
            }
            
            val searchDuration = System.currentTimeMillis() - startTime
            
            _searchResults.value = results
            _searchStats.value = SearchStats(results.size, searchDuration)
            _searchStatus.value = if (results.isEmpty()) SearchStatus.NO_RESULTS else SearchStatus.SUCCESS
            
            Log.d(TAG, "Search completed - ${results.size} results in ${searchDuration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            _searchStatus.value = SearchStatus.ERROR
            return Result.failure(e)
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun clearSearch(): Result<Unit> {
        _currentQuery.value = ""
        _searchResults.value = emptyList()
        _searchStatus.value = SearchStatus.IDLE
        _searchStats.value = SearchStats(0, 0)
        return Result.success(Unit)
    }
    
    override suspend fun addRecentSearch(query: String): Result<Unit> {
        // TODO: Implement recent search persistence
        return Result.success(Unit)
    }
    
    override suspend fun clearRecentSearches(): Result<Unit> {
        _recentSearches.value = emptyList()
        return Result.success(Unit)
    }
    
    override suspend fun selectSuggestion(suggestion: SuggestedSearch): Result<Unit> {
        return updateSearchQuery(suggestion.query)
    }
    
    override suspend fun applyFilters(filters: List<SearchFilter>): Result<Unit> {
        // TODO: Implement filtering
        return Result.success(Unit)
    }
    
    override suspend fun clearFilters(): Result<Unit> {
        // TODO: Implement filtering
        return Result.success(Unit)
    }
    
    override suspend fun getSuggestions(partialQuery: String): Result<List<SuggestedSearch>> {
        return Result.success(emptyList())
    }
    
    private fun determineMatchType(showEntity: ShowEntity, query: String): SearchMatchType {
        val queryLower = query.lowercase()
        return when {
            showEntity.date.contains(queryLower) -> SearchMatchType.YEAR
            showEntity.venueName.lowercase().contains(queryLower) -> SearchMatchType.VENUE
            showEntity.city?.lowercase()?.contains(queryLower) == true -> SearchMatchType.LOCATION
            else -> SearchMatchType.GENERAL
        }
    }
}

/**
 * Extension function to convert ShowEntity to V2 Show domain model
 */
private fun ShowEntity.toDomainShow(): Show {
    return Show(
        id = showId,
        date = date,
        year = year,
        band = band,
        venue = Venue(venueName, city, state, country),
        location = Location.fromRaw(locationRaw, city, state),
        setlist = Setlist.parse(setlistRaw, setlistStatus),
        lineup = Lineup.parse(lineupRaw, lineupStatus),
        recordingIds = recordingsRaw?.parseRecordingIds() ?: emptyList(),
        bestRecordingId = bestRecordingId,
        recordingCount = recordingCount,
        averageRating = averageRating,
        totalReviews = totalReviews,
        isInLibrary = isInLibrary,
        libraryAddedAt = libraryAddedAt
    )
}

/**
 * Helper function to parse recording IDs from JSON string
 */
private fun String.parseRecordingIds(): List<String> {
    return try {
        Json.decodeFromString<List<String>>(this)
    } catch (e: Exception) {
        emptyList()
    }
}