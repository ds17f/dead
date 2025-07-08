package com.deadarchive.core.data.repository

import android.content.Context
import com.deadarchive.core.database.RatingDao
import com.deadarchive.core.database.RecordingRatingEntity
import com.deadarchive.core.database.ShowRatingEntity
import com.deadarchive.core.model.RecordingRating
import com.deadarchive.core.model.ShowRating
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.RatedShow
import com.deadarchive.core.model.RatedRecording
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.util.zip.ZipInputStream

/**
 * Repository for managing show and recording ratings data.
 * 
 * This repository handles:
 * - Loading pre-computed ratings from assets
 * - Caching ratings in local database
 * - Providing rating data for shows and recordings
 * - Updating ratings when new data is available
 */
@Singleton
class RatingsRepository @Inject constructor(
    private val ratingDao: RatingDao,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "RatingsRepository"
        private const val RATINGS_ASSET_FILE = "ratings.json"
        private const val RATINGS_ZIP_FILE = "ratings.zip"
        private const val RATINGS_CACHE_EXPIRY = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    
    
    /**
     * Initialize ratings data from assets if database is empty.
     */
    suspend fun initializeRatingsIfNeeded() {
        try {
            val recordingCount = ratingDao.getRecordingRatingsCount()
            val showCount = ratingDao.getShowRatingsCount()
            
            if (recordingCount == 0 && showCount == 0) {
                Log.i(TAG, "Database is empty, loading ratings from assets...")
                loadRatingsFromAssets()
            } else {
                Log.i(TAG, "Ratings already loaded: $recordingCount recordings, $showCount shows")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ratings: ${e.message}", e)
        }
    }
    
    /**
     * Load and parse ratings data from the assets file.
     * First tries to load from compressed ZIP, then falls back to uncompressed JSON.
     */
    private suspend fun loadRatingsFromAssets() {
        try {
            val ratingsJson = extractRatingsFromAssets()
            parseAndSaveRatings(ratingsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ratings from assets: ${e.message}", e)
        }
    }
    
    /**
     * Extract ratings data from assets, preferring ZIP over JSON.
     */
    private suspend fun extractRatingsFromAssets(): String {
        return try {
            // First try to load from compressed ZIP file
            context.assets.open(RATINGS_ZIP_FILE).use { zipStream ->
                ZipInputStream(zipStream).use { zip ->
                    zip.nextEntry // Should be ratings.json
                    zip.readBytes().toString(Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load from ZIP, trying uncompressed JSON: ${e.message}")
            // Fallback to uncompressed JSON file
            context.assets.open(RATINGS_ASSET_FILE).use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        }
    }
    
    /**
     * Parse ratings JSON and save to database.
     */
    private suspend fun parseAndSaveRatings(ratingsJson: String) {
        val ratingsData = JSONObject(ratingsJson)
        
        // Parse recording ratings
        val recordingRatings = mutableListOf<RecordingRatingEntity>()
        if (ratingsData.has("recording_ratings")) {
            val recordingRatingsObj = ratingsData.getJSONObject("recording_ratings")
            val keys = recordingRatingsObj.keys()
            while (keys.hasNext()) {
                val identifier = keys.next()
                val ratingObj = recordingRatingsObj.getJSONObject(identifier)
                recordingRatings.add(
                    RecordingRatingEntity(
                        identifier = identifier,
                        rating = ratingObj.optDouble("rating", 0.0).toFloat(),
                        reviewCount = ratingObj.optInt("review_count", 0),
                        sourceType = ratingObj.optString("source_type", "UNKNOWN"),
                        confidence = ratingObj.optDouble("confidence", 0.0).toFloat()
                    )
                )
            }
        }
        
        // Parse show ratings  
        val showRatings = mutableListOf<ShowRatingEntity>()
        if (ratingsData.has("show_ratings")) {
            val showRatingsObj = ratingsData.getJSONObject("show_ratings")
            val keys = showRatingsObj.keys()
            while (keys.hasNext()) {
                val showKey = keys.next()
                val ratingObj = showRatingsObj.getJSONObject(showKey)
                showRatings.add(
                    ShowRatingEntity(
                        showKey = showKey,
                        date = ratingObj.optString("date", ""),
                        venue = ratingObj.optString("venue", ""),
                        rating = ratingObj.optDouble("rating", 0.0).toFloat(),
                        confidence = ratingObj.optDouble("confidence", 0.0).toFloat(),
                        bestRecordingId = ratingObj.optString("best_recording", ""),
                        recordingCount = ratingObj.optInt("recording_count", 0)
                    )
                )
            }
        }
        
        // Insert into database
        ratingDao.replaceAllRatings(recordingRatings, showRatings)
        
        Log.i(TAG, "Successfully loaded ${recordingRatings.size} recording ratings and ${showRatings.size} show ratings from assets")
    }
    
    /**
     * Get rating for a specific recording.
     */
    suspend fun getRecordingRating(identifier: String): RecordingRating? {
        return try {
            ratingDao.getRecordingRating(identifier)?.toRecordingRating()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recording rating for $identifier: ${e.message}")
            null
        }
    }
    
    /**
     * Get ratings for multiple recordings.
     */
    suspend fun getRecordingRatings(identifiers: List<String>): Map<String, RecordingRating> {
        return try {
            ratingDao.getRecordingRatings(identifiers)
                .associate { it.identifier to it.toRecordingRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recording ratings: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Get rating for a specific show.
     */
    suspend fun getShowRating(showKey: String): ShowRating? {
        return try {
            ratingDao.getShowRating(showKey)?.toShowRating()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get show rating for $showKey: ${e.message}")
            null
        }
    }
    
    /**
     * Get show rating by date and venue.
     */
    suspend fun getShowRatingByDateVenue(date: String, venue: String): ShowRating? {
        val showKey = generateShowKey(date, venue)
        return getShowRating(showKey)
    }
    
    /**
     * Get ratings for shows on a specific date.
     */
    suspend fun getShowRatingsByDate(date: String): List<ShowRating> {
        return try {
            ratingDao.getShowRatingsByDate(date).map { it.toShowRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get show ratings for date $date: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get ratings for shows on a specific month/day across all years.
     */
    suspend fun getShowRatingsByMonthDay(monthDay: String): List<ShowRating> {
        return try {
            ratingDao.getShowRatingsByMonthDay(monthDay).map { it.toShowRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get show ratings for month/day $monthDay: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get top-rated shows.
     */
    suspend fun getTopShows(minRating: Float = 4.0f, minConfidence: Float = 0.7f, limit: Int = 100): List<ShowRating> {
        return try {
            ratingDao.getTopShowRatings(minRating, minConfidence, limit).map { it.toShowRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top shows: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get top-rated recordings.
     */
    suspend fun getTopRecordings(minRating: Float = 4.0f): List<RecordingRating> {
        return try {
            ratingDao.getTopRecordingRatings(minRating).map { it.toRecordingRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top recordings: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get recordings by source type.
     */
    suspend fun getRecordingsBySourceType(sourceType: String): List<RecordingRating> {
        return try {
            ratingDao.getRecordingRatingsBySource(sourceType).map { it.toRecordingRating() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recordings by source type $sourceType: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Create a RatedShow by combining Show and ShowRating data.
     */
    suspend fun createRatedShow(show: Show): RatedShow {
        val showKey = generateShowKey(show.date, show.venue ?: "")
        val rating = getShowRating(showKey)
        return RatedShow(show, rating)
    }
    
    /**
     * Create RatedShows for a list of shows.
     */
    suspend fun createRatedShows(shows: List<Show>): List<RatedShow> {
        return shows.map { createRatedShow(it) }
    }
    
    /**
     * Create a RatedRecording by combining Recording and RecordingRating data.
     */
    suspend fun createRatedRecording(recording: Recording): RatedRecording {
        val rating = getRecordingRating(recording.identifier)
        return RatedRecording(recording, rating)
    }
    
    /**
     * Create RatedRecordings for a list of recordings.
     */
    suspend fun createRatedRecordings(recordings: List<Recording>): List<RatedRecording> {
        return recordings.map { createRatedRecording(it) }
    }
    
    /**
     * Get Flow of top shows for reactive UI updates.
     */
    fun getTopShowsFlow(minRating: Float = 4.0f, limit: Int = 100): Flow<List<ShowRating>> {
        return ratingDao.getTopShowRatingsFlow(minRating, limit)
            .map { entities -> entities.map { it.toShowRating() } }
    }
    
    /**
     * Get Flow of show ratings by month/day for reactive UI updates.
     */
    fun getShowRatingsByMonthDayFlow(monthDay: String): Flow<List<ShowRating>> {
        return ratingDao.getShowRatingsByMonthDayFlow(monthDay)
            .map { entities -> entities.map { it.toShowRating() } }
    }
    
    /**
     * Clean up old ratings data.
     */
    suspend fun cleanupOldRatings() {
        try {
            val cutoffTime = System.currentTimeMillis() - RATINGS_CACHE_EXPIRY
            ratingDao.deleteOldRecordingRatings(cutoffTime)
            ratingDao.deleteOldShowRatings(cutoffTime)
            Log.i(TAG, "Cleaned up old ratings data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old ratings: ${e.message}")
        }
    }
    
    /**
     * Generate a show key from date and venue.
     */
    private fun generateShowKey(date: String, venue: String): String {
        val cleanVenue = venue.replace(" ", "_")
            .replace(",", "")
            .replace("&", "and")
            .replace("'", "")
        return "${date}_${cleanVenue}"
    }
    
    /**
     * Get rating statistics.
     */
    suspend fun getRatingStatistics(): Map<String, Any> {
        return try {
            val recordingCount = ratingDao.getRecordingRatingsCount()
            val showCount = ratingDao.getShowRatingsCount()
            val avgShowRating = ratingDao.getAverageShowRating() ?: 0f
            val sourceStats = ratingDao.getRatingStatsBySource()
            
            mapOf(
                "recording_count" to recordingCount,
                "show_count" to showCount,
                "average_show_rating" to avgShowRating,
                "source_statistics" to sourceStats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get rating statistics: ${e.message}")
            emptyMap()
        }
    }
}