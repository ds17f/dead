package com.deadarchive.core.database.v2.service

import android.util.Log
import com.deadarchive.core.database.v2.dao.SongSearchV2Dao
import com.deadarchive.core.database.v2.dao.VenueSearchV2Dao
import com.deadarchive.core.database.v2.dao.ShowSearchV2Dao
import com.deadarchive.core.database.v2.dao.MemberSearchV2Dao
import com.deadarchive.core.database.v2.entities.SongSearchV2Entity
import com.deadarchive.core.database.v2.entities.VenueSearchV2Entity
import com.deadarchive.core.database.v2.entities.ShowSearchV2Entity
import com.deadarchive.core.database.v2.entities.MemberSearchV2Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResultV2(
    val showId: String,
    val date: String,
    val venue: String,
    val location: String,
    val rating: Double,
    val rawRating: Double,
    val recordingCount: Int,
    val songCount: Int,
    val hasSetlist: Boolean,
    val relevanceScore: Int,
    val matchType: SearchMatchType,
    val matchContext: String? = null // Additional context about the match
)

enum class SearchMatchType {
    SONG_MATCH,
    VENUE_MATCH,
    SHOW_MATCH,
    MEMBER_MATCH,
    COMBINED_MATCH
}

data class SearchStatsV2(
    val totalSongs: Int,
    val totalVenues: Int,
    val totalShows: Int,
    val totalMembers: Int
)

@Singleton
class SearchServiceV2 @Inject constructor(
    private val songSearchDao: SongSearchV2Dao,
    private val venueSearchDao: VenueSearchV2Dao,
    private val showSearchDao: ShowSearchV2Dao,
    private val memberSearchDao: MemberSearchV2Dao
) {
    
    companion object {
        private const val TAG = "SearchServiceV2"
        private const val MAX_RESULTS_PER_TYPE = 100
        private const val DEFAULT_SEARCH_LIMIT = 50
    }
    
    /**
     * Unified search across all search tables
     */
    suspend fun searchAll(
        query: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        includeTypes: Set<SearchMatchType> = SearchMatchType.values().toSet()
    ): List<SearchResultV2> = withContext(Dispatchers.IO) {
        
        if (query.isBlank()) {
            Log.d(TAG, "Empty search query provided")
            return@withContext emptyList()
        }
        
        val normalizedQuery = query.trim().lowercase()
        Log.d(TAG, "Performing unified search for: '$normalizedQuery'")
        
        val results = mutableListOf<SearchResultV2>()
        
        try {
            // Run searches in parallel for better performance
            val songSearchDeferred = if (SearchMatchType.SONG_MATCH in includeTypes) {
                async { searchSongs(normalizedQuery) }
            } else null
            
            val venueSearchDeferred = if (SearchMatchType.VENUE_MATCH in includeTypes) {
                async { searchVenues(normalizedQuery) }
            } else null
            
            val showSearchDeferred = if (SearchMatchType.SHOW_MATCH in includeTypes) {
                async { searchShows(normalizedQuery) }
            } else null
            
            val memberSearchDeferred = if (SearchMatchType.MEMBER_MATCH in includeTypes) {
                async { searchMembers(normalizedQuery) }
            } else null
            
            // Collect results
            songSearchDeferred?.await()?.let { songResults ->
                results.addAll(songResults.map { convertSongToSearchResult(it) })
            }
            
            venueSearchDeferred?.await()?.let { venueResults ->
                results.addAll(venueResults.map { convertVenueToSearchResult(it) })
            }
            
            showSearchDeferred?.await()?.let { showResults ->
                results.addAll(showResults.map { convertShowToSearchResult(it) })
            }
            
            memberSearchDeferred?.await()?.let { memberResults ->
                results.addAll(memberResults.map { convertMemberToSearchResult(it) })
            }
            
            // Remove duplicates by showId and sort by relevance + rating
            val deduplicatedResults = results
                .groupBy { it.showId }
                .mapValues { (_, matches) ->
                    // If multiple matches for same show, combine relevance scores
                    val combinedRelevance = matches.sumOf { it.relevanceScore }
                    val bestMatch = matches.maxByOrNull { it.relevanceScore } ?: matches.first()
                    
                    bestMatch.copy(
                        relevanceScore = combinedRelevance,
                        matchType = if (matches.size > 1) SearchMatchType.COMBINED_MATCH else bestMatch.matchType,
                        matchContext = if (matches.size > 1) {
                            matches.joinToString(", ") { "${it.matchType.name}: ${it.matchContext}" }
                        } else {
                            bestMatch.matchContext
                        }
                    )
                }
                .values
                .sortedWith(compareByDescending<SearchResultV2> { it.relevanceScore }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.date })
                .take(limit)
            
            Log.d(TAG, "Search completed: ${deduplicatedResults.size} unique results from ${results.size} total matches")
            deduplicatedResults
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing unified search", e)
            emptyList()
        }
    }
    
    /**
     * Search songs by name or key
     */
    suspend fun searchSongs(query: String): List<SongSearchV2Entity> = withContext(Dispatchers.IO) {
        try {
            songSearchDao.searchSongs(query).take(MAX_RESULTS_PER_TYPE)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching songs", e)
            emptyList()
        }
    }
    
    /**
     * Search venues by name, city, or location
     */
    suspend fun searchVenues(query: String): List<VenueSearchV2Entity> = withContext(Dispatchers.IO) {
        try {
            venueSearchDao.searchVenues(query).take(MAX_RESULTS_PER_TYPE)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching venues", e)
            emptyList()
        }
    }
    
    /**
     * Search shows by search text or other attributes
     */
    suspend fun searchShows(query: String): List<ShowSearchV2Entity> = withContext(Dispatchers.IO) {
        try {
            showSearchDao.searchShows(query).take(MAX_RESULTS_PER_TYPE)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching shows", e)
            emptyList()
        }
    }
    
    /**
     * Search members by name or instruments
     */
    suspend fun searchMembers(query: String): List<MemberSearchV2Entity> = withContext(Dispatchers.IO) {
        try {
            memberSearchDao.searchMembers(query).take(MAX_RESULTS_PER_TYPE)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching members", e)
            emptyList()
        }
    }
    
    /**
     * Get search statistics
     */
    suspend fun getSearchStats(): SearchStatsV2 = withContext(Dispatchers.IO) {
        try {
            val songCount = songSearchDao.getCount()
            val venueCount = venueSearchDao.getTotalShowCount()
            val showCount = showSearchDao.getCount()
            val memberCount = memberSearchDao.getTotalMemberShowCount()
            
            SearchStatsV2(
                totalSongs = songCount,
                totalVenues = venueCount,
                totalShows = showCount,
                totalMembers = memberCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search stats", e)
            SearchStatsV2(0, 0, 0, 0)
        }
    }
    
    /**
     * Get shows by year
     */
    suspend fun getShowsByYear(year: Int): List<SearchResultV2> = withContext(Dispatchers.IO) {
        try {
            showSearchDao.getShowsByYear(year).map { convertShowToSearchResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shows by year", e)
            emptyList()
        }
    }
    
    /**
     * Get shows by date range
     */
    suspend fun getShowsByDateRange(startDate: String, endDate: String): List<SearchResultV2> = withContext(Dispatchers.IO) {
        try {
            showSearchDao.getShowsInDateRange(startDate, endDate).map { convertShowToSearchResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shows by date range", e)
            emptyList()
        }
    }
    
    /**
     * Get high rated shows
     */
    suspend fun getHighRatedShows(minRating: Double = 2.0, limit: Int = DEFAULT_SEARCH_LIMIT): List<SearchResultV2> = withContext(Dispatchers.IO) {
        try {
            showSearchDao.getHighRatedShows(minRating).take(limit).map { convertShowToSearchResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting high rated shows", e)
            emptyList()
        }
    }
    
    /**
     * Get all available years
     */
    suspend fun getAllYears(): List<Int> = withContext(Dispatchers.IO) {
        try {
            showSearchDao.getAllYears()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all years", e)
            emptyList()
        }
    }
    
    // Conversion functions
    
    private fun convertSongToSearchResult(song: SongSearchV2Entity): SearchResultV2 {
        return SearchResultV2(
            showId = song.showId,
            date = song.date,
            venue = song.venue,
            location = song.location,
            rating = song.rating,
            rawRating = song.rawRating,
            recordingCount = 0, // Not available in song search
            songCount = 0, // Not available in song search
            hasSetlist = true, // Implied since song exists
            relevanceScore = calculateSongRelevanceScore(song),
            matchType = SearchMatchType.SONG_MATCH,
            matchContext = "${song.songName} (${song.setName ?: "Unknown Set"})"
        )
    }
    
    private fun convertVenueToSearchResult(venue: VenueSearchV2Entity): SearchResultV2 {
        return SearchResultV2(
            showId = venue.showId,
            date = venue.date,
            venue = venue.venueName,
            location = venue.location,
            rating = venue.rating,
            rawRating = venue.rawRating,
            recordingCount = venue.recordingCount,
            songCount = 0, // Not available in venue search
            hasSetlist = false, // Not available in venue search
            relevanceScore = calculateVenueRelevanceScore(venue),
            matchType = SearchMatchType.VENUE_MATCH,
            matchContext = "${venue.venueName}, ${venue.city}"
        )
    }
    
    private fun convertShowToSearchResult(show: ShowSearchV2Entity): SearchResultV2 {
        return SearchResultV2(
            showId = show.showId,
            date = show.date,
            venue = show.venue,
            location = show.location,
            rating = show.rating,
            rawRating = show.rawRating,
            recordingCount = show.recordingCount,
            songCount = show.songCount,
            hasSetlist = show.hasSetlist,
            relevanceScore = calculateShowRelevanceScore(show),
            matchType = SearchMatchType.SHOW_MATCH,
            matchContext = "${show.venue}, ${show.city}"
        )
    }
    
    private fun convertMemberToSearchResult(member: MemberSearchV2Entity): SearchResultV2 {
        return SearchResultV2(
            showId = member.showId,
            date = member.date,
            venue = member.venue,
            location = member.location,
            rating = member.rating,
            rawRating = 0.0, // Not available in member search
            recordingCount = 0, // Not available in member search
            songCount = 0, // Not available in member search
            hasSetlist = false, // Not available in member search
            relevanceScore = calculateMemberRelevanceScore(member),
            matchType = SearchMatchType.MEMBER_MATCH,
            matchContext = "${member.memberName} (${member.instruments})"
        )
    }
    
    // Relevance scoring functions
    
    private fun calculateSongRelevanceScore(song: SongSearchV2Entity): Int {
        var score = 10 // Base score for song matches
        if (song.rating > 2.0) score += 5 // Bonus for well-rated shows
        if (song.position != null && song.position <= 3) score += 2 // Bonus for early songs in set
        return score
    }
    
    private fun calculateVenueRelevanceScore(venue: VenueSearchV2Entity): Int {
        var score = 8 // Base score for venue matches
        if (venue.rating > 2.0) score += 5 // Bonus for well-rated shows
        if (venue.recordingCount > 5) score += 2 // Bonus for well-documented shows
        return score
    }
    
    private fun calculateShowRelevanceScore(show: ShowSearchV2Entity): Int {
        var score = 6 // Base score for show matches
        if (show.rating > 2.0) score += 5 // Bonus for well-rated shows
        if (show.hasSetlist) score += 3 // Bonus for shows with setlists
        if (show.recordingCount > 3) score += 2 // Bonus for well-documented shows
        return score
    }
    
    private fun calculateMemberRelevanceScore(member: MemberSearchV2Entity): Int {
        var score = 7 // Base score for member matches
        if (member.rating > 2.0) score += 5 // Bonus for well-rated shows
        return score
    }
}