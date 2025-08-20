package com.deadly.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ratings operations.
 */
@Dao
interface RatingDao {
    
    // Recording Ratings
    
    @Query("SELECT * FROM recording_ratings WHERE identifier = :identifier")
    suspend fun getRecordingRating(identifier: String): RecordingRatingEntity?
    
    @Query("SELECT * FROM recording_ratings WHERE identifier IN (:identifiers)")
    suspend fun getRecordingRatings(identifiers: List<String>): List<RecordingRatingEntity>
    
    @Query("SELECT * FROM recording_ratings WHERE rating >= :minRating ORDER BY rating DESC")
    suspend fun getTopRecordingRatings(minRating: Float = 4.0f): List<RecordingRatingEntity>
    
    @Query("SELECT * FROM recording_ratings WHERE sourceType = :sourceType ORDER BY rating DESC")
    suspend fun getRecordingRatingsBySource(sourceType: String): List<RecordingRatingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordingRating(rating: RecordingRatingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordingRatings(ratings: List<RecordingRatingEntity>)
    
    @Query("DELETE FROM recording_ratings WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldRecordingRatings(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM recording_ratings")
    suspend fun getRecordingRatingsCount(): Int
    
    // Show Ratings
    
    @Query("SELECT * FROM show_ratings WHERE showKey = :showKey")
    suspend fun getShowRating(showKey: String): ShowRatingEntity?
    
    @Query("SELECT * FROM show_ratings WHERE date = :date ORDER BY rating DESC")
    suspend fun getShowRatingsByDate(date: String): List<ShowRatingEntity>
    
    @Query("SELECT * FROM show_ratings WHERE SUBSTR(date, 6, 5) = :monthDay ORDER BY rating DESC")
    suspend fun getShowRatingsByMonthDay(monthDay: String): List<ShowRatingEntity>
    
    @Query("SELECT * FROM show_ratings WHERE rating >= :minRating AND confidence >= :minConfidence ORDER BY rating DESC LIMIT :limit")
    suspend fun getTopShowRatings(minRating: Float = 4.0f, minConfidence: Float = 0.7f, limit: Int = 100): List<ShowRatingEntity>
    
    @Query("SELECT * FROM show_ratings WHERE rating >= :minRating ORDER BY rating DESC")
    suspend fun getHighRatedShows(minRating: Float): List<ShowRatingEntity>
    
    @Query("SELECT * FROM show_ratings WHERE venue LIKE '%' || :venueQuery || '%' ORDER BY rating DESC")
    suspend fun getShowRatingsByVenue(venueQuery: String): List<ShowRatingEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowRating(rating: ShowRatingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowRatings(ratings: List<ShowRatingEntity>)
    
    @Query("DELETE FROM show_ratings WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldShowRatings(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM show_ratings")
    suspend fun getShowRatingsCount(): Int
    
    // Combined queries
    
    @Query("""
        SELECT sr.* FROM show_ratings sr
        INNER JOIN recording_ratings rr ON sr.bestRecordingId = rr.identifier
        WHERE rr.sourceType = :sourceType AND sr.rating >= :minRating
        ORDER BY sr.rating DESC
        LIMIT :limit
    """)
    suspend fun getTopShowsBySourceType(sourceType: String, minRating: Float = 4.0f, limit: Int = 50): List<ShowRatingEntity>
    
    // Reactive queries with Flow
    
    @Query("SELECT * FROM show_ratings WHERE rating >= :minRating ORDER BY rating DESC LIMIT :limit")
    fun getTopShowRatingsFlow(minRating: Float = 4.0f, limit: Int = 100): Flow<List<ShowRatingEntity>>
    
    @Query("SELECT * FROM show_ratings WHERE SUBSTR(date, 6, 5) = :monthDay ORDER BY rating DESC")
    fun getShowRatingsByMonthDayFlow(monthDay: String): Flow<List<ShowRatingEntity>>
    
    // Bulk operations
    
    @Transaction
    suspend fun replaceAllRatings(
        recordingRatings: List<RecordingRatingEntity>,
        showRatings: List<ShowRatingEntity>
    ) {
        // Clear existing data
        clearAllRatings()
        
        // Insert new data
        insertRecordingRatings(recordingRatings)
        insertShowRatings(showRatings)
    }
    
    @Query("DELETE FROM recording_ratings")
    suspend fun clearRecordingRatings()
    
    @Query("DELETE FROM show_ratings")
    suspend fun clearShowRatings()
    
    @Transaction
    suspend fun clearAllRatings() {
        clearRecordingRatings()
        clearShowRatings()
    }
    
    // Statistics queries
    
    @Query("SELECT AVG(rating) FROM recording_ratings WHERE sourceType = :sourceType")
    suspend fun getAverageRatingBySource(sourceType: String): Float?
    
    @Query("SELECT AVG(rating) FROM show_ratings WHERE confidence >= :minConfidence")
    suspend fun getAverageShowRating(minConfidence: Float = 0.5f): Float?
    
    @Query("""
        SELECT sourceType, COUNT(*) as count, AVG(rating) as avgRating 
        FROM recording_ratings 
        GROUP BY sourceType 
        ORDER BY avgRating DESC
    """)
    suspend fun getRatingStatsBySource(): List<RatingSourceStats>
}

/**
 * Data class for rating statistics by source type.
 */
data class RatingSourceStats(
    val sourceType: String,
    val count: Int,
    val avgRating: Float
)