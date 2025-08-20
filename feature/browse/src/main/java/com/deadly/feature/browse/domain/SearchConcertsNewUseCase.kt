package com.deadly.feature.browse.domain

import com.deadly.core.data.api.repository.ShowRepository
import com.deadly.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for searching shows with the new Show/Recording structure
 * Groups individual recordings into shows by date and venue
 */
class SearchShowsUseCase @Inject constructor(
    private val showRepository: ShowRepository
) {
    
    /**
     * Search for shows with intelligent query processing
     * Returns Show objects with grouped recordings
     */
    operator fun invoke(query: String): Flow<List<Show>> {
        android.util.Log.d("SearchShowsUseCase", "🔎 SearchShowsUseCase invoke called with query: '$query'")
        
        if (query.isBlank()) {
            android.util.Log.d("SearchShowsUseCase", "🔎 Blank query, using default 'grateful dead'")
            return showRepository.searchShows("grateful dead")
        }
        
        val searchQuery = processSearchQuery(query.trim())
        android.util.Log.d("SearchShowsUseCase", "🔎 Processed query: '$query' → '$searchQuery'")
        android.util.Log.d("SearchShowsUseCase", "🔎 Calling showRepository.searchShows with '$searchQuery'")
        return showRepository.searchShows(searchQuery)
    }
    
    /**
     * Search for shows with intelligent query processing and limit results
     * Optimized for cases where only top results are needed (like era filtering)
     */
    fun searchLimited(query: String, limit: Int): Flow<List<Show>> {
        android.util.Log.d("SearchShowsUseCase", "🔎 SearchShowsUseCase searchLimited called with query: '$query', limit: $limit")
        
        if (query.isBlank()) {
            android.util.Log.d("SearchShowsUseCase", "🔎 Blank query, using default 'grateful dead'")
            return showRepository.searchShowsLimited("grateful dead", limit)
        }
        
        val searchQuery = processSearchQuery(query.trim())
        android.util.Log.d("SearchShowsUseCase", "🔎 Processed limited query: '$query' → '$searchQuery'")
        android.util.Log.d("SearchShowsUseCase", "🔎 Calling showRepository.searchShowsLimited with '$searchQuery', limit: $limit")
        return showRepository.searchShowsLimited(searchQuery, limit)
    }
    
    /**
     * Process the search query to handle different search patterns
     */
    private fun processSearchQuery(query: String): String {
        return when {
            // Handle full date patterns: 1977-05-08, 1977-05, 1977
            query.matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")) -> {
                // For pure date searches, search both with and without "grateful dead"
                // This allows finding concerts by date alone
                query
            }
            
            // Handle "grateful dead YEAR" patterns - extract just the year
            query.matches(Regex("(?i)grateful\\s+dead\\s+(\\d{4})")) -> {
                val year = Regex("(\\d{4})").find(query)?.value ?: ""
                year
            }
            
            // Handle partial date patterns: 05-08, 05/08, 5/8
            query.matches(Regex("\\d{1,2}[-/]\\d{1,2}")) -> {
                // Convert to standard format and search
                val parts = query.split("[-/]".toRegex())
                val month = parts[0].padStart(2, '0')
                val day = parts[1].padStart(2, '0')
                "-$month-$day"
            }
            
            // Handle month names: May 1977, may 8
            query.matches(Regex("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*")) -> {
                query // Search as-is for month names
            }
            
            // Handle venue/location searches: Winterland, Boston, Berkeley
            query.length >= 3 && !query.contains("grateful", ignoreCase = true) && 
            !query.contains("dead", ignoreCase = true) -> {
                // For venue/location searches, don't add "grateful dead" prefix
                // Let the comprehensive database search handle it
                query
            }
            
            // Handle very short queries that might need context
            query.length < 3 -> {
                "grateful dead $query"
            }
            
            // Default: use query as-is
            else -> query
        }
    }
    
    /**
     * Get popular shows (top-rated shows using weighted ratings for ranking) 
     */
    fun getPopularShows(): Flow<List<Show>> {
        android.util.Log.d("SearchShowsUseCase", "🔎 getPopularShows called - getting all shows sorted by weighted rating")
        return showRepository.getAllShows().map { shows ->
            shows.filter { it.hasRating }
                .sortedWith(
                    compareByDescending<Show> { it.rating ?: 0f }  // Use weighted rating for internal ranking
                        .thenByDescending { it.date }
                )
                .take(50) // Top 50 shows by weighted rating
        }
    }
    
    /**
     * Get recent shows 
     */
    fun getRecentShows(): Flow<List<Show>> {
        android.util.Log.d("SearchShowsUseCase", "🔎 getRecentShows called - calling getAllShows()")
        return showRepository.getAllShows()
    }
}