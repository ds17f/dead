package com.deadarchive.feature.browse.domain

import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.model.Concert
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching concerts with business logic
 */
class SearchConcertsUseCase @Inject constructor(
    private val concertRepository: ConcertRepository
) {
    
    /**
     * Search for concerts with the given query
     * Handles empty queries and adds default search terms if needed
     */
    operator fun invoke(query: String): Flow<List<Concert>> {
        val searchQuery = when {
            query.isBlank() -> "grateful dead"
            // Check if it's a date pattern (1977, 1977-05, 1977-05-08)
            query.matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?")) -> "grateful dead $query"
            // Check if it's a short search that might need context
            query.length < 3 && !query.matches(Regex("\\d+")) -> "grateful dead $query"
            else -> {
                // If it doesn't contain "grateful dead" and isn't obviously a date/venue, add it
                if (!query.contains("grateful", ignoreCase = true) && 
                    !query.contains("dead", ignoreCase = true) &&
                    !query.matches(Regex(".*\\d{4}.*"))) {
                    "grateful dead $query"
                } else {
                    query
                }
            }
        }
        
        return concertRepository.searchConcerts(searchQuery)
    }
    
    /**
     * Get popular concerts (predefined search)
     */
    fun getPopularConcerts(): Flow<List<Concert>> {
        return concertRepository.searchConcerts("grateful dead 1977")
    }
    
    /**
     * Get recent concerts (predefined search)
     */
    fun getRecentConcerts(): Flow<List<Concert>> {
        return concertRepository.searchConcerts("grateful dead")
    }
}