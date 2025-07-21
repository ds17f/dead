package com.deadarchive.core.data.api.repository

import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for show-related operations
 */
interface ShowRepository {
    // Show-based methods
    fun searchShows(query: String): Flow<List<Show>>
    fun searchShowsLimited(query: String, limit: Int): Flow<List<Show>>
    fun getAllShows(): Flow<List<Show>>
    fun getLibraryShows(): Flow<List<Show>>
    suspend fun getLibraryShowsList(): List<Show>
    suspend fun getRecordingsByShowId(showId: String): List<Recording>
    
    // Recording-based methods (individual recordings)
    fun searchRecordings(query: String): Flow<List<Recording>>
    suspend fun getRecordingById(id: String): Recording?
    suspend fun getRecordingByIdWithFormatFilter(id: String, formatPreferences: List<String>): Recording?
    fun getAllCachedRecordings(): Flow<List<Recording>>
    
    // Ratings-enhanced methods
    suspend fun getTopRatedShows(limit: Int = 50): List<Show>
    suspend fun getTopRatedRecordings(limit: Int = 50): List<Recording>
    suspend fun getShowsWithRatings(minRating: Float = 4.0f, limit: Int = 100): List<Show>
    
    // Streaming URL generation methods
    suspend fun getStreamingUrl(identifier: String, filename: String): String?
    suspend fun getTrackStreamingUrls(identifier: String): List<Pair<com.deadarchive.core.model.AudioFile, String>>
    suspend fun getPreferredStreamingUrl(identifier: String): String?
    suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String?
    
    // Debug methods
    suspend fun debugDatabaseState(): String
    suspend fun getShowById(showId: String): Show?
    
    // Navigation methods for efficient show navigation
    suspend fun getNextShowByDate(currentDate: String): Show?
    suspend fun getPreviousShowByDate(currentDate: String): Show?
}