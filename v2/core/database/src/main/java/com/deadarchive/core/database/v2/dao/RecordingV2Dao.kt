package com.deadarchive.v2.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.RecordingV2Entity

@Dao
interface RecordingV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordings(recordings: List<RecordingV2Entity>): List<Long>
    
    @Query("SELECT * FROM recordings_v2 WHERE identifier = :identifier")
    suspend fun getRecordingById(identifier: String): RecordingV2Entity?
    
    @Query("SELECT * FROM recordings_v2 WHERE show_id = :showId ORDER BY rating DESC")
    suspend fun getRecordingsForShow(showId: String): List<RecordingV2Entity>
    
    @Query("SELECT * FROM recordings_v2 WHERE show_id = :showId ORDER BY rating DESC LIMIT 1")
    suspend fun getBestRecordingForShow(showId: String): RecordingV2Entity?
    
    @Query("SELECT * FROM recordings_v2 WHERE source_type = :sourceType ORDER BY rating DESC")
    suspend fun getRecordingsBySourceType(sourceType: String): List<RecordingV2Entity>
    
    @Query("SELECT COUNT(*) FROM recordings_v2")
    suspend fun getRecordingCount(): Int
    
    @Query("SELECT COUNT(*) FROM recordings_v2 WHERE show_id = :showId")
    suspend fun getRecordingCountForShow(showId: String): Int
    
    @Query("DELETE FROM recordings_v2 WHERE show_id = :showId")
    suspend fun deleteRecordingsForShow(showId: String)
    
    @Query("DELETE FROM recordings_v2")
    suspend fun deleteAllRecordings()
    
    /**
     * Get recordings with show context for a comprehensive view.
     * Includes show date, venue, and recording quality metrics.
     */
    @Query("""
        SELECT r.*, s.date as show_date, v.name as show_venue, s.city, s.state
        FROM recordings_v2 r
        JOIN shows_v2 s ON r.show_id = s.showId
        JOIN venues_v2 v ON s.venueId = v.venueId
        WHERE r.show_id = :showId
        ORDER BY r.rating DESC
    """)
    suspend fun getRecordingsWithShowContext(showId: String): List<RecordingWithShowContextV2>
    
    /**
     * Find top-rated recordings across all shows.
     * Useful for discovery and recommendation features.
     */
    @Query("""
        SELECT r.*, s.date as show_date, v.name as show_venue, s.city, s.state
        FROM recordings_v2 r
        JOIN shows_v2 s ON r.show_id = s.showId
        JOIN venues_v2 v ON s.venueId = v.venueId
        WHERE r.rating > :minRating AND r.review_count >= :minReviews
        ORDER BY r.rating DESC, r.review_count DESC
        LIMIT :limit
    """)
    suspend fun getTopRatedRecordings(
        minRating: Double = 2.0,
        minReviews: Int = 5,
        limit: Int = 50
    ): List<RecordingWithShowContextV2>
    
    /**
     * Get recordings by source type with quality filtering.
     * Essential for audiophile features and source preferences.
     */
    @Query("""
        SELECT r.*, s.date as show_date, v.name as show_venue, s.city, s.state
        FROM recordings_v2 r
        JOIN shows_v2 s ON r.show_id = s.showId
        JOIN venues_v2 v ON s.venueId = v.venueId
        WHERE r.source_type = :sourceType 
        AND r.rating >= :minRating
        ORDER BY r.rating DESC, s.date DESC
        LIMIT :limit
    """)
    suspend fun getRecordingsBySourceTypeWithQuality(
        sourceType: String,
        minRating: Double = 1.5,
        limit: Int = 100
    ): List<RecordingWithShowContextV2>
    
    /**
     * Search recordings by venue or location.
     * Enables venue-based discovery and filtering.
     */
    @Query("""
        SELECT r.*, s.date as show_date, v.name as show_venue, s.city, s.state
        FROM recordings_v2 r
        JOIN shows_v2 s ON r.show_id = s.showId
        JOIN venues_v2 v ON s.venueId = v.venueId
        WHERE (r.venue LIKE '%' || :searchQuery || '%' 
               OR r.location LIKE '%' || :searchQuery || '%'
               OR v.name LIKE '%' || :searchQuery || '%')
        ORDER BY r.rating DESC
        LIMIT :limit
    """)
    suspend fun searchRecordingsByVenue(searchQuery: String, limit: Int = 50): List<RecordingWithShowContextV2>
    
    /**
     * Get recording statistics for analysis and debugging.
     */
    @Query("""
        SELECT 
            source_type,
            COUNT(*) as count,
            AVG(rating) as avg_rating,
            AVG(review_count) as avg_reviews
        FROM recordings_v2 
        WHERE source_type IS NOT NULL
        GROUP BY source_type
        ORDER BY count DESC
    """)
    suspend fun getRecordingStatisticsBySourceType(): List<RecordingStatisticsV2>
    
    /**
     * Get recordings with track count for complete recording info.
     */
    @Query("""
        SELECT r.*, 
               COUNT(t.id) as track_count,
               SUM(t.duration) as total_duration
        FROM recordings_v2 r
        LEFT JOIN tracks_v2 t ON r.identifier = t.recording_id
        WHERE r.show_id = :showId
        GROUP BY r.identifier
        ORDER BY r.rating DESC
    """)
    suspend fun getRecordingsWithTrackInfo(showId: String): List<RecordingWithTrackInfoV2>
}

// Data classes for complex query results
data class RecordingWithShowContextV2(
    val identifier: String,
    @ColumnInfo(name = "show_id") val showId: String,
    val title: String?,
    @ColumnInfo(name = "source_type") val sourceType: String?,
    val lineage: String?,
    val taper: String?,
    val description: String?,
    val date: String,
    val venue: String,
    val location: String?,
    val rating: Double,
    @ColumnInfo(name = "raw_rating") val rawRating: Double,
    @ColumnInfo(name = "review_count") val reviewCount: Int,
    val confidence: Double,
    @ColumnInfo(name = "high_ratings") val highRatings: Int,
    @ColumnInfo(name = "low_ratings") val lowRatings: Int,
    @ColumnInfo(name = "collection_timestamp") val collectionTimestamp: Long,
    @ColumnInfo(name = "show_date") val showDate: String,
    @ColumnInfo(name = "show_venue") val showVenue: String,
    val city: String?,
    val state: String?
)

data class RecordingStatisticsV2(
    @ColumnInfo(name = "source_type") val sourceType: String,
    val count: Int,
    @ColumnInfo(name = "avg_rating") val avgRating: Double,
    @ColumnInfo(name = "avg_reviews") val avgReviews: Double
)

data class RecordingWithTrackInfoV2(
    val identifier: String,
    @ColumnInfo(name = "show_id") val showId: String,
    val title: String?,
    @ColumnInfo(name = "source_type") val sourceType: String?,
    val rating: Double,
    @ColumnInfo(name = "raw_rating") val rawRating: Double,
    @ColumnInfo(name = "review_count") val reviewCount: Int,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "total_duration") val totalDuration: Double?
)