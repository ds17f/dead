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
            query.length < 3 -> "grateful dead $query"
            else -> query
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