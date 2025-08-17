package com.deadarchive.v2.core.database.service

import android.util.Log
import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.ShowFtsDao
import com.deadarchive.v2.core.database.entities.ShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AwesomeBarResult(
    val show: ShowEntity,
    val matchType: AwesomeBarMatchType,
    val relevanceScore: Float
)

enum class AwesomeBarMatchType {
    DATE_MATCH,      // Matched on date/year
    VENUE_MATCH,     // Matched on venue name
    LOCATION_MATCH,  // Matched on city/state
    SONG_MATCH,      // Matched on song in setlist
    FULL_TEXT_MATCH  // General FTS match
}

@Singleton
class AwesomeBarService @Inject constructor(
    private val showDao: ShowDao,
    private val showFtsDao: ShowFtsDao
) {
    
    companion object {
        private const val TAG = "AwesomeBarService"
        private const val DEFAULT_LIMIT = 50
    }
    
    /**
     * Unified "Awesome Bar" search across all show data
     * Handles: dates, venues, locations, songs, general text
     */
    suspend fun search(
        query: String,
        limit: Int = DEFAULT_LIMIT
    ): List<AwesomeBarResult> = withContext(Dispatchers.IO) {
        
        if (query.isBlank()) {
            Log.d(TAG, "Empty search query provided")
            return@withContext emptyList()
        }
        
        val normalizedQuery = query.trim()
        Log.d(TAG, "Awesome Bar search: '$normalizedQuery'")
        
        try {
            // Try different search strategies and combine results
            val results = mutableListOf<AwesomeBarResult>()
            
            // 1. Date searches (exact and partial)
            results.addAll(searchByDate(normalizedQuery))
            
            // 2. Venue searches
            results.addAll(searchByVenue(normalizedQuery))
            
            // 3. Location searches
            results.addAll(searchByLocation(normalizedQuery))
            
            // 4. Song searches
            results.addAll(searchBySong(normalizedQuery))
            
            // 5. FTS search for everything else
            results.addAll(searchByFts(normalizedQuery))
            
            // Remove duplicates and sort by relevance
            val deduplicatedResults = results
                .groupBy { it.show.showId }
                .mapValues { (_, matches) ->
                    // If multiple matches for same show, take highest relevance
                    matches.maxByOrNull { it.relevanceScore } ?: matches.first()
                }
                .values
                .sortedByDescending { it.relevanceScore }
                .take(limit)
            
            Log.d(TAG, "Awesome Bar search completed: ${deduplicatedResults.size} results")
            deduplicatedResults
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Awesome Bar search", e)
            emptyList()
        }
    }
    
    /**
     * Quick autocomplete suggestions for Awesome Bar
     */
    suspend fun getSuggestions(
        query: String,
        limit: Int = 10
    ): List<String> = withContext(Dispatchers.IO) {
        
        if (query.length < 2) return@withContext emptyList()
        
        try {
            // Get FTS results for prefix matching
            val rowIds = showFtsDao.searchShowsPrefix(query, limit * 2)
            val shows = rowIds.mapNotNull { rowId ->
                showDao.getAllShows().getOrNull(rowId.toInt())
            }
            
            // Extract suggestion strings
            val suggestions = mutableSetOf<String>()
            
            shows.forEach { show ->
                // Add venue name if it matches
                if (show.venueName.contains(query, ignoreCase = true)) {
                    suggestions.add(show.venueName)
                }
                
                // Add city if it matches
                show.city?.let { city ->
                    if (city.contains(query, ignoreCase = true)) {
                        suggestions.add("$city, ${show.state ?: ""}")
                    }
                }
                
                // Add song names if they match
                show.songList?.split(",")?.forEach { song ->
                    val cleanSong = song.trim()
                    if (cleanSong.contains(query, ignoreCase = true)) {
                        suggestions.add(cleanSong)
                    }
                }
            }
            
            suggestions.take(limit).toList()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting suggestions", e)
            emptyList()
        }
    }
    
    private suspend fun searchByDate(query: String): List<AwesomeBarResult> {
        val shows = mutableListOf<ShowEntity>()
        
        // Try different date formats
        when {
            query.matches(Regex("\\d{4}")) -> {
                // Year search: "1977"
                shows.addAll(showDao.getShowsByYear(query.toInt()))
            }
            query.matches(Regex("\\d{4}-\\d{2}")) -> {
                // Year-month search: "1977-05"
                shows.addAll(showDao.getShowsByYearMonth(query))
            }
            query.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                // Exact date search: "1977-05-08"
                shows.addAll(showDao.getShowsByDate(query))
            }
        }
        
        return shows.map { show ->
            AwesomeBarResult(
                show = show,
                matchType = AwesomeBarMatchType.DATE_MATCH,
                relevanceScore = 10f // High relevance for exact date matches
            )
        }
    }
    
    private suspend fun searchByVenue(query: String): List<AwesomeBarResult> {
        val shows = showDao.getShowsByVenue(query)
        return shows.map { show ->
            AwesomeBarResult(
                show = show,
                matchType = AwesomeBarMatchType.VENUE_MATCH,
                relevanceScore = 8f
            )
        }
    }
    
    private suspend fun searchByLocation(query: String): List<AwesomeBarResult> {
        val showsByCity = showDao.getShowsByCity(query)
        val showsByState = showDao.getShowsByState(query)
        
        val allShows = (showsByCity + showsByState).distinctBy { it.showId }
        
        return allShows.map { show ->
            AwesomeBarResult(
                show = show,
                matchType = AwesomeBarMatchType.LOCATION_MATCH,
                relevanceScore = 7f
            )
        }
    }
    
    private suspend fun searchBySong(query: String): List<AwesomeBarResult> {
        val shows = showDao.getShowsBySong(query)
        return shows.map { show ->
            AwesomeBarResult(
                show = show,
                matchType = AwesomeBarMatchType.SONG_MATCH,
                relevanceScore = 9f // High relevance for song matches
            )
        }
    }
    
    private suspend fun searchByFts(query: String): List<AwesomeBarResult> {
        try {
            val rowIds = showFtsDao.searchShows(query)
            val shows = rowIds.mapNotNull { rowId ->
                showDao.getAllShows().getOrNull(rowId.toInt())
            }
            
            return shows.mapIndexed { index, show ->
                AwesomeBarResult(
                    show = show,
                    matchType = AwesomeBarMatchType.FULL_TEXT_MATCH,
                    relevanceScore = 6f - (index * 0.1f) // Decreasing relevance by result order
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "FTS search failed, falling back to basic search", e)
            return emptyList()
        }
    }
}