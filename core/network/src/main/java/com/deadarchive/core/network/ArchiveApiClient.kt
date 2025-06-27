package com.deadarchive.core.network

import com.deadarchive.core.model.Concert
import com.deadarchive.core.network.interceptor.ArchiveApiException
import com.deadarchive.core.network.interceptor.NetworkException
import com.deadarchive.core.network.mapper.ArchiveMapper.toConcert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level client for Archive.org API operations
 * Provides a clean interface for the rest of the application
 */
@Singleton
class ArchiveApiClient @Inject constructor(
    private val apiService: ArchiveApiService
) {
    
    /**
     * Search for Grateful Dead concerts
     * 
     * @param query Search query (artist, venue, date, etc.)
     * @param limit Maximum number of results
     * @param offset Starting index for pagination
     * @return Result with list of concerts or error
     */
    suspend fun searchConcerts(
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): ApiResult<List<Concert>> = withContext(Dispatchers.IO) {
        try {
            val searchQuery = if (query.isBlank()) {
                "Grateful Dead"
            } else {
                "Grateful Dead $query"
            }
            
            val response = apiService.searchConcerts(
                query = searchQuery,
                rows = limit,
                start = offset
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toConcert() }
                ApiResult.Success(concerts)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
    
    /**
     * Search concerts by date range
     * 
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format  
     * @param limit Maximum number of results
     * @return Result with list of concerts or error
     */
    suspend fun searchConcertsByDateRange(
        startDate: String,
        endDate: String,
        limit: Int = 100
    ): ApiResult<List<Concert>> = withContext(Dispatchers.IO) {
        try {
            val dateFilter = "date:[$startDate TO $endDate]"
            
            val response = apiService.searchConcertsByDateRange(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toConcert() }
                ApiResult.Success(concerts)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
    
    /**
     * Search concerts by venue
     * 
     * @param venue Venue name (e.g., "Fillmore West")
     * @param limit Maximum number of results
     * @return Result with list of concerts or error
     */
    suspend fun searchConcertsByVenue(
        venue: String,
        limit: Int = 100
    ): ApiResult<List<Concert>> = withContext(Dispatchers.IO) {
        try {
            val venueQuery = "venue:\"$venue\""
            
            val response = apiService.searchConcertsByVenue(
                query = "collection:GratefulDead AND venue:\"$venue\"",
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toConcert() }
                ApiResult.Success(concerts)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
    
    /**
     * Get detailed concert information including files and tracks
     * 
     * @param identifier Archive.org item identifier
     * @return Result with detailed concert or error
     */
    suspend fun getConcertDetails(
        identifier: String
    ): ApiResult<Concert> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConcertMetadata(identifier)
            
            if (response.isSuccessful && response.body() != null) {
                val concert = response.body()!!.toConcert()
                ApiResult.Success(concert)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
    
    /**
     * Get popular/highly-rated concerts
     * 
     * @param minRating Minimum average rating (1.0 to 5.0)
     * @param minReviews Minimum number of reviews
     * @param limit Maximum number of results
     * @return Result with list of popular concerts or error
     */
    suspend fun getPopularConcerts(
        minRating: Double = 4.0,
        minReviews: Int = 5,
        limit: Int = 50
    ): ApiResult<List<Concert>> = withContext(Dispatchers.IO) {
        try {
            val filters = "avg_rating:[$minRating TO *] AND num_reviews:[$minReviews TO *]"
            
            val response = apiService.getPopularConcerts(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toConcert() }
                ApiResult.Success(concerts)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
    
    /**
     * Get recently added concerts
     * 
     * @param days Number of days to look back
     * @param limit Maximum number of results
     * @return Result with list of recent concerts or error
     */
    suspend fun getRecentConcerts(
        days: Int = 30,
        limit: Int = 50
    ): ApiResult<List<Concert>> = withContext(Dispatchers.IO) {
        try {
            val dateFilter = "addeddate:[NOW-${days}DAYS TO NOW]"
            
            val response = apiService.getRecentConcerts(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toConcert() }
                ApiResult.Success(concerts)
            } else {
                ApiResult.Error(
                    ArchiveApiException(
                        code = response.code(),
                        message = response.message(),
                        url = response.raw().request.url.toString()
                    )
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}

/**
 * Sealed class representing API operation results
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> exception
    }
}