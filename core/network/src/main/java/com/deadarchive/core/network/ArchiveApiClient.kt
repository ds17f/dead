package com.deadarchive.core.network

import com.deadarchive.core.model.Recording
import com.deadarchive.core.network.interceptor.ArchiveApiException
import com.deadarchive.core.network.interceptor.NetworkException
import com.deadarchive.core.network.mapper.ArchiveMapper.toRecording
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
    suspend fun searchRecordings(
        query: String,
        limit: Int = 50,
        offset: Int = 0
    ): ApiResult<List<Recording>> = withContext(Dispatchers.IO) {
        try {
            val searchQuery = if (query.isBlank()) {
                "Grateful Dead"
            } else {
                "Grateful Dead $query"
            }
            
            val response = apiService.searchRecordings(
                query = searchQuery,
                rows = limit,
                start = offset
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toRecording() }
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
    suspend fun searchRecordingsByDateRange(
        startDate: String,
        endDate: String,
        limit: Int = 100
    ): ApiResult<List<Recording>> = withContext(Dispatchers.IO) {
        try {
            val dateFilter = "date:[$startDate TO $endDate]"
            
            val response = apiService.searchRecordingsByDateRange(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toRecording() }
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
    suspend fun searchRecordingsByVenue(
        venue: String,
        limit: Int = 100
    ): ApiResult<List<Recording>> = withContext(Dispatchers.IO) {
        try {
            val venueQuery = "venue:\"$venue\""
            
            val response = apiService.searchRecordingsByVenue(
                query = "collection:GratefulDead AND venue:\"$venue\"",
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toRecording() }
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
    suspend fun getRecordingDetails(
        identifier: String
    ): ApiResult<Recording> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecordingMetadata(identifier)
            
            if (response.isSuccessful && response.body() != null) {
                val concert = response.body()!!.toRecording()
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
    suspend fun getPopularRecordings(
        minRating: Double = 4.0,
        minReviews: Int = 5,
        limit: Int = 50
    ): ApiResult<List<Recording>> = withContext(Dispatchers.IO) {
        try {
            val filters = "avg_rating:[$minRating TO *] AND num_reviews:[$minReviews TO *]"
            
            val response = apiService.getPopularRecordings(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toRecording() }
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
    suspend fun getRecentRecordings(
        days: Int = 30,
        limit: Int = 50
    ): ApiResult<List<Recording>> = withContext(Dispatchers.IO) {
        try {
            val dateFilter = "addeddate:[NOW-${days}DAYS TO NOW]"
            
            val response = apiService.getRecentRecordings(
                rows = limit
            )
            
            if (response.isSuccessful && response.body() != null) {
                val concerts = response.body()!!.response.docs.map { it.toRecording() }
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