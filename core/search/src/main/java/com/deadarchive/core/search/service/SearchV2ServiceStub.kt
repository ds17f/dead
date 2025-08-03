package com.deadarchive.core.search.service

import android.util.Log
import com.deadarchive.core.search.api.SearchV2Service
import com.deadarchive.core.search.api.SearchFilter
import com.deadarchive.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive stub implementation of SearchV2Service with realistic Dead show data.
 * 
 * This stub provides:
 * - Rich mock data spanning decades of Grateful Dead concerts
 * - Realistic search filtering and relevance scoring
 * - Search suggestions based on popular venues, years, and songs
 * - Recent search history management
 * - Proper search status management with loading states
 * 
 * Enables immediate UI development with comprehensive test data while validating
 * the V2 architecture patterns and service integration.
 */
@Singleton
class SearchV2ServiceStub @Inject constructor() : SearchV2Service {
    
    companion object {
        private const val TAG = "SearchV2ServiceStub"
    }
    
    // Reactive state management
    private val _currentQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<SearchResultShow>>(emptyList())
    private val _searchStatus = MutableStateFlow(SearchStatus.IDLE)
    private val _recentSearches = MutableStateFlow<List<RecentSearch>>(emptyList())
    private val _suggestedSearches = MutableStateFlow<List<SuggestedSearch>>(emptyList())
    private val _searchStats = MutableStateFlow(SearchStats(0, 0))
    private val _appliedFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())
    
    // Public reactive flows
    override val currentQuery: Flow<String> = _currentQuery.asStateFlow()
    override val searchResults: Flow<List<SearchResultShow>> = _searchResults.asStateFlow()
    override val searchStatus: Flow<SearchStatus> = _searchStatus.asStateFlow()
    override val recentSearches: Flow<List<RecentSearch>> = _recentSearches.asStateFlow()
    override val suggestedSearches: Flow<List<SuggestedSearch>> = _suggestedSearches.asStateFlow()
    override val searchStats: Flow<SearchStats> = _searchStats.asStateFlow()
    
    // Comprehensive mock show data spanning decades
    private val mockShows = listOf(
        // Cornell 5/8/77 - The legendary show
        Show(
            date = "1977-05-08",
            venue = "Barton Hall",
            location = "Ithaca, NY",
            recordings = listOf(
                Recording(
                    identifier = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                    title = "Cornell 5/8/77 - Soundboard",
                    concertDate = "1977-05-08",
                    concertVenue = "Barton Hall",
                    concertLocation = "Ithaca, NY",
                    source = "Soundboard",
                    lineage = "SBD > Cassette > DAT > CDR > FLAC",
                    transferer = "David Hicks",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // Europe '72 Classic
        Show(
            date = "1972-05-03",
            venue = "Olympia Theatre",
            location = "Paris, France",
            recordings = listOf(
                Recording(
                    identifier = "gd72-05-03.sbd.unknown.30057.sbeok.shnf",
                    title = "Europe '72 - Paris",
                    concertDate = "1972-05-03",
                    concertVenue = "Olympia Theatre",
                    concertLocation = "Paris, France",
                    source = "Soundboard",
                    lineage = "SBD > Cassette > FLAC",
                    transferer = "Unknown",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // Woodstock 1969
        Show(
            date = "1969-08-16",
            venue = "Woodstock Music & Art Fair",
            location = "Bethel, NY",
            recordings = listOf(
                Recording(
                    identifier = "gd69-08-16.aud.vernon.16793.sbeok.shnf",
                    title = "Woodstock '69 - Audience",
                    concertDate = "1969-08-16",
                    concertVenue = "Woodstock Music & Art Fair",
                    concertLocation = "Bethel, NY",
                    source = "Audience",
                    lineage = "AUD > Cassette > DAT > CDR > FLAC",
                    transferer = "Vernon",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // Dick's Picks era
        Show(
            date = "1973-06-10",
            venue = "RFK Stadium",
            location = "Washington, DC",
            recordings = listOf(
                Recording(
                    identifier = "dp12",
                    title = "Dick's Picks Vol. 12",
                    concertDate = "1973-06-10",
                    concertVenue = "RFK Stadium",
                    concertLocation = "Washington, DC",
                    source = "Soundboard",
                    lineage = "SBD > Reel > DAT > FLAC",
                    transferer = "Dick Latvala",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // 1990s era
        Show(
            date = "1995-07-09",
            venue = "Soldier Field",
            location = "Chicago, IL",
            recordings = listOf(
                Recording(
                    identifier = "gd95-07-09.sbd.miller.97483.flac1644",
                    title = "Jerry's Last Show",
                    concertDate = "1995-07-09",
                    concertVenue = "Soldier Field",
                    concertLocation = "Chicago, IL",
                    source = "Soundboard",
                    lineage = "SBD > DAT > FLAC",
                    transferer = "Miller",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // Fillmore East classics
        Show(
            date = "1970-02-13",
            venue = "Fillmore East",
            location = "New York, NY",
            recordings = listOf(
                Recording(
                    identifier = "gd70-02-13.sbd.16332.sbeok.shnf",
                    title = "Fillmore East Classic",
                    concertDate = "1970-02-13",
                    concertVenue = "Fillmore East",
                    concertLocation = "New York, NY",
                    source = "Soundboard",
                    lineage = "SBD > Reel > FLAC",
                    transferer = "Unknown",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // Fillmore West
        Show(
            date = "1969-02-27",
            venue = "Fillmore West",
            location = "San Francisco, CA",
            recordings = listOf(
                Recording(
                    identifier = "gd69-02-27.sbd.vernon.87915.flac1644",
                    title = "Live/Dead Era",
                    concertDate = "1969-02-27",
                    concertVenue = "Fillmore West",
                    concertLocation = "San Francisco, CA",
                    source = "Soundboard",
                    lineage = "SBD > Reel > FLAC",
                    transferer = "Vernon",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        ),
        
        // More 1977 shows
        Show(
            date = "1977-05-22",
            venue = "The Sportatorium",
            location = "Pembroke Pines, FL",
            recordings = listOf(
                Recording(
                    identifier = "gd77-05-22.sbd.hicks.32928.sbeok.shnf",
                    title = "Florida '77",
                    concertDate = "1977-05-22",
                    concertVenue = "The Sportatorium",
                    concertLocation = "Pembroke Pines, FL",
                    source = "Soundboard",
                    lineage = "SBD > Cassette > FLAC",
                    transferer = "David Hicks",
                    tracks = emptyList(),
                    audioFiles = emptyList()
                )
            )
        )
    )
    
    override suspend fun updateSearchQuery(query: String): Result<Unit> {
        Log.d(TAG, "STUB: updateSearchQuery(query='$query') called")
        
        _currentQuery.value = query
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchStatus.value = SearchStatus.IDLE
            _suggestedSearches.value = emptyList()
            _searchStats.value = SearchStats(0, 0)
            return Result.success(Unit)
        }
        
        // Simulate search loading
        _searchStatus.value = SearchStatus.SEARCHING
        delay(300) // Realistic search delay
        
        try {
            // Perform smart search with relevance scoring
            val startTime = System.currentTimeMillis()
            val results = performSearch(query)
            val searchDuration = System.currentTimeMillis() - startTime
            
            _searchResults.value = results
            _searchStats.value = SearchStats(
                totalResults = results.size,
                searchDuration = searchDuration,
                appliedFilters = _appliedFilters.value.map { it.displayName }
            )
            
            _searchStatus.value = if (results.isEmpty()) SearchStatus.NO_RESULTS else SearchStatus.SUCCESS
            
            // Generate suggestions based on query
            _suggestedSearches.value = generateSuggestions(query)
            
            Log.d(TAG, "STUB: Search completed - found ${results.size} results in ${searchDuration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "STUB: Search failed", e)
            _searchStatus.value = SearchStatus.ERROR
            return Result.failure(e)
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun clearSearch(): Result<Unit> {
        Log.d(TAG, "STUB: clearSearch() called")
        
        _currentQuery.value = ""
        _searchResults.value = emptyList()
        _searchStatus.value = SearchStatus.IDLE
        _suggestedSearches.value = emptyList()
        _searchStats.value = SearchStats(0, 0)
        
        return Result.success(Unit)
    }
    
    override suspend fun addRecentSearch(query: String): Result<Unit> {
        Log.d(TAG, "STUB: addRecentSearch(query='$query') called")
        
        if (query.isBlank()) return Result.success(Unit)
        
        val currentRecents = _recentSearches.value.toMutableList()
        
        // Remove existing entry if present
        currentRecents.removeAll { it.query == query }
        
        // Add to front
        currentRecents.add(0, RecentSearch(query, System.currentTimeMillis()))
        
        // Keep only last 10
        if (currentRecents.size > 10) {
            currentRecents.removeAt(currentRecents.size - 1)
        }
        
        _recentSearches.value = currentRecents
        
        return Result.success(Unit)
    }
    
    override suspend fun clearRecentSearches(): Result<Unit> {
        Log.d(TAG, "STUB: clearRecentSearches() called")
        _recentSearches.value = emptyList()
        return Result.success(Unit)
    }
    
    override suspend fun selectSuggestion(suggestion: SuggestedSearch): Result<Unit> {
        Log.d(TAG, "STUB: selectSuggestion(suggestion='${suggestion.query}') called")
        return updateSearchQuery(suggestion.query)
    }
    
    override suspend fun applyFilters(filters: List<SearchFilter>): Result<Unit> {
        Log.d(TAG, "STUB: applyFilters(filters=$filters) called")
        
        _appliedFilters.value = filters.toSet()
        
        // Re-run search with filters applied
        if (_currentQuery.value.isNotBlank()) {
            return updateSearchQuery(_currentQuery.value)
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun clearFilters(): Result<Unit> {
        Log.d(TAG, "STUB: clearFilters() called")
        
        _appliedFilters.value = emptySet()
        
        // Re-run search without filters
        if (_currentQuery.value.isNotBlank()) {
            return updateSearchQuery(_currentQuery.value)
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun getSuggestions(partialQuery: String): Result<List<SuggestedSearch>> {
        Log.d(TAG, "STUB: getSuggestions(partialQuery='$partialQuery') called")
        
        val suggestions = generateSuggestions(partialQuery)
        return Result.success(suggestions)
    }
    
    override suspend fun populateTestData(): Result<Unit> {
        Log.d(TAG, "STUB: populateTestData() called")
        
        // Populate with sample recent searches
        _recentSearches.value = listOf(
            RecentSearch("Cornell 5/8/77", System.currentTimeMillis() - 3600000),
            RecentSearch("1977", System.currentTimeMillis() - 7200000),
            RecentSearch("Fillmore", System.currentTimeMillis() - 10800000),
            RecentSearch("Dick's Picks", System.currentTimeMillis() - 14400000)
        )
        
        Log.d(TAG, "STUB: Populated ${_recentSearches.value.size} recent searches")
        
        return Result.success(Unit)
    }
    
    /**
     * Perform intelligent search with relevance scoring
     */
    private fun performSearch(query: String): List<SearchResultShow> {
        val queryLower = query.lowercase()
        val results = mutableListOf<SearchResultShow>()
        
        for (show in mockShows) {
            val matchType = determineMatchType(show, queryLower)
            val relevanceScore = calculateRelevanceScore(show, queryLower, matchType)
            
            if (relevanceScore > 0.1f) {
                val hasDownloads = listOf("gd1977-05-08", "gd72-05-03", "dp12").any { 
                    show.recordings.any { recording -> recording.identifier.contains(it) }
                }
                
                results.add(SearchResultShow(
                    show = show,
                    relevanceScore = relevanceScore,
                    matchType = matchType,
                    hasDownloads = hasDownloads
                ))
            }
        }
        
        // Apply filters
        val filteredResults = applySearchFilters(results)
        
        // Sort by relevance score descending
        return filteredResults.sortedByDescending { it.relevanceScore }
    }
    
    private fun determineMatchType(show: Show, query: String): SearchMatchType {
        return when {
            show.date.contains(query) || show.year?.contains(query) == true -> SearchMatchType.YEAR
            show.venue?.lowercase()?.contains(query) == true -> SearchMatchType.VENUE
            show.location?.lowercase()?.contains(query) == true -> SearchMatchType.LOCATION
            show.recordings.any { it.title?.lowercase()?.contains(query) == true } -> SearchMatchType.TITLE
            else -> SearchMatchType.GENERAL
        }
    }
    
    private fun calculateRelevanceScore(show: Show, query: String, matchType: SearchMatchType): Float {
        var score = 0f
        
        // Base scoring by match type
        when (matchType) {
            SearchMatchType.TITLE -> score += 1.0f
            SearchMatchType.VENUE -> score += 0.9f
            SearchMatchType.YEAR -> score += 0.8f
            SearchMatchType.LOCATION -> score += 0.7f
            SearchMatchType.SETLIST -> score += 0.6f
            SearchMatchType.GENERAL -> score += 0.5f
        }
        
        // Bonus for exact matches
        if (show.venue?.lowercase() == query) score += 0.5f
        if (show.date.contains(query)) score += 0.3f
        
        // Popular show bonuses
        when (show.date) {
            "1977-05-08" -> score += 0.3f // Cornell
            "1972-05-03" -> score += 0.2f // Europe '72
            "1969-08-16" -> score += 0.2f // Woodstock
            "1995-07-09" -> score += 0.2f // Jerry's last show
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun applySearchFilters(results: List<SearchResultShow>): List<SearchResultShow> {
        var filtered = results
        
        for (filter in _appliedFilters.value) {
            filtered = when (filter) {
                SearchFilter.HAS_DOWNLOADS -> filtered.filter { it.hasDownloads }
                SearchFilter.SOUNDBOARD -> filtered.filter { show ->
                    show.show.recordings.any { it.source?.contains("Soundboard", ignoreCase = true) == true }
                }
                SearchFilter.AUDIENCE -> filtered.filter { show ->
                    show.show.recordings.any { it.source?.contains("Audience", ignoreCase = true) == true }
                }
                SearchFilter.POPULAR -> filtered.filter { it.relevanceScore > 0.8f }
                else -> filtered
            }
        }
        
        return filtered
    }
    
    private fun generateSuggestions(query: String): List<SuggestedSearch> {
        if (query.isBlank()) return emptyList()
        
        val suggestions = mutableListOf<SuggestedSearch>()
        val queryLower = query.lowercase()
        
        // Year suggestions
        if (queryLower.matches(Regex("19\\d{0,2}"))) {
            suggestions.addAll(listOf(
                SuggestedSearch("1977", 25, SuggestionType.YEAR),
                SuggestedSearch("1972", 18, SuggestionType.YEAR),
                SuggestedSearch("1969", 12, SuggestionType.YEAR),
                SuggestedSearch("1995", 8, SuggestionType.YEAR)
            ).filter { it.query.startsWith(queryLower) })
        }
        
        // Venue suggestions
        val venues = listOf("Fillmore", "Cornell", "Woodstock", "Madison Square Garden", "Soldier Field")
        suggestions.addAll(venues
            .filter { it.lowercase().contains(queryLower) }
            .map { SuggestedSearch(it, 15, SuggestionType.VENUE) }
        )
        
        // Location suggestions
        val locations = listOf("New York", "California", "Chicago", "Boston", "Philadelphia")
        suggestions.addAll(locations
            .filter { it.lowercase().contains(queryLower) }
            .map { SuggestedSearch(it, 10, SuggestionType.LOCATION) }
        )
        
        // Popular search terms
        val popularTerms = listOf("Dick's Picks", "Europe '72", "Skull & Roses", "Live/Dead")
        suggestions.addAll(popularTerms
            .filter { it.lowercase().contains(queryLower) }
            .map { SuggestedSearch(it, 20, SuggestionType.GENERAL) }
        )
        
        return suggestions.take(6) // Limit to 6 suggestions
    }
}