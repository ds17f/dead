package com.deadarchive.feature.browse.domain

import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.model.ConcertNew
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching concerts with the new Concert/Recording structure
 * Groups individual recordings into concerts by date and venue
 */
class SearchConcertsNewUseCase @Inject constructor(
    private val concertRepository: ConcertRepository
) {
    
    /**
     * Search for concerts with intelligent query processing
     * Returns ConcertNew objects with grouped recordings
     */
    operator fun invoke(query: String): Flow<List<ConcertNew>> {
        if (query.isBlank()) {
            return concertRepository.searchConcertsNew("grateful dead")
        }
        
        val searchQuery = processSearchQuery(query.trim())
        return concertRepository.searchConcertsNew(searchQuery)
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
     * Get popular concerts (famous shows)
     */
    fun getPopularConcerts(): Flow<List<ConcertNew>> {
        return concertRepository.searchConcertsNew("1977")
    }
    
    /**
     * Get recent concerts 
     */
    fun getRecentConcerts(): Flow<List<ConcertNew>> {
        return concertRepository.searchConcertsNew("grateful dead")
    }
}