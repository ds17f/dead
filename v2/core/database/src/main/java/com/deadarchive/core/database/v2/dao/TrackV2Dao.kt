package com.deadarchive.v2.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.TrackV2Entity

@Dao
interface TrackV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackV2Entity>): List<Long>
    
    @Query("SELECT * FROM tracks_v2 WHERE id = :trackId")
    suspend fun getTrackById(trackId: Long): TrackV2Entity?
    
    @Query("SELECT * FROM tracks_v2 WHERE recording_id = :recordingId ORDER BY CAST(track_number AS INTEGER)")
    suspend fun getTracksForRecording(recordingId: String): List<TrackV2Entity>
    
    @Query("SELECT * FROM tracks_v2 WHERE title LIKE '%' || :searchQuery || '%'")
    suspend fun searchTracksByTitle(searchQuery: String): List<TrackV2Entity>
    
    @Query("SELECT COUNT(*) FROM tracks_v2")
    suspend fun getTrackCount(): Int
    
    @Query("SELECT COUNT(*) FROM tracks_v2 WHERE recording_id = :recordingId")
    suspend fun getTrackCountForRecording(recordingId: String): Int
    
    @Query("SELECT SUM(duration) FROM tracks_v2 WHERE recording_id = :recordingId AND duration IS NOT NULL")
    suspend fun getTotalDurationForRecording(recordingId: String): Double?
    
    @Query("DELETE FROM tracks_v2 WHERE recording_id = :recordingId")
    suspend fun deleteTracksForRecording(recordingId: String)
    
    @Query("DELETE FROM tracks_v2")
    suspend fun deleteAllTracks()
    
    /**
     * Get tracks with format information for media player integration.
     * Returns tracks with all available formats for quality selection.
     */
    @Query("""
        SELECT t.*, 
               GROUP_CONCAT(tf.format || ':' || tf.filename || ':' || COALESCE(tf.bitrate, '')) as formats_info
        FROM tracks_v2 t
        LEFT JOIN track_formats_v2 tf ON t.id = tf.track_id
        WHERE t.recording_id = :recordingId
        GROUP BY t.id
        ORDER BY CAST(t.track_number AS INTEGER)
    """)
    suspend fun getTracksWithFormats(recordingId: String): List<TrackWithFormatsV2>
    
    /**
     * Get tracks with recording context for complete playback information.
     * Essential for media player to display track info with show context.
     */
    @Query("""
        SELECT t.*, r.source_type, r.rating, r.venue, r.date, r.show_id,
               s.city, s.state
        FROM tracks_v2 t
        JOIN recordings_v2 r ON t.recording_id = r.identifier
        JOIN shows_v2 s ON r.show_id = s.showId
        WHERE t.recording_id = :recordingId
        ORDER BY CAST(t.track_number AS INTEGER)
    """)
    suspend fun getTracksWithRecordingContext(recordingId: String): List<TrackWithRecordingContextV2>
    
    /**
     * Find all performances of a specific song across recordings.
     * Critical for "Find all Dark Star performances" functionality.
     */
    @Query("""
        SELECT t.*, r.source_type, r.rating, r.venue, r.date, r.show_id,
               s.city, s.state
        FROM tracks_v2 t
        JOIN recordings_v2 r ON t.recording_id = r.identifier
        JOIN shows_v2 s ON r.show_id = s.showId
        WHERE t.title LIKE '%' || :songTitle || '%'
        ORDER BY r.date DESC, r.rating DESC
    """)
    suspend fun findSongPerformances(songTitle: String): List<TrackWithRecordingContextV2>
    
    /**
     * Get track by recording and track number for direct access.
     * Used for playlist and playback navigation.
     */
    @Query("""
        SELECT * FROM tracks_v2 
        WHERE recording_id = :recordingId AND track_number = :trackNumber 
        LIMIT 1
    """)
    suspend fun getTrackByRecordingAndNumber(recordingId: String, trackNumber: String): TrackV2Entity?
    
    /**
     * Get the longest tracks for discovery features.
     * Useful for finding epic jams and extended performances.
     */
    @Query("""
        SELECT t.*, r.source_type, r.rating, r.venue, r.date, r.show_id,
               s.city, s.state
        FROM tracks_v2 t
        JOIN recordings_v2 r ON t.recording_id = r.identifier
        JOIN shows_v2 s ON r.show_id = s.showId
        WHERE t.duration IS NOT NULL AND t.duration > :minDuration
        ORDER BY t.duration DESC
        LIMIT :limit
    """)
    suspend fun getLongestTracks(minDuration: Double = 600.0, limit: Int = 50): List<TrackWithRecordingContextV2>
    
    /**
     * Get track statistics for analysis.
     */
    @Query("""
        SELECT 
            title,
            COUNT(*) as performance_count,
            AVG(duration) as avg_duration,
            MAX(duration) as max_duration,
            MIN(duration) as min_duration
        FROM tracks_v2 
        WHERE duration IS NOT NULL
        GROUP BY title
        HAVING COUNT(*) >= :minPerformances
        ORDER BY performance_count DESC
        LIMIT :limit
    """)
    suspend fun getTrackStatistics(minPerformances: Int = 5, limit: Int = 100): List<TrackStatisticsV2>
}

// Data classes for complex query results
data class TrackWithFormatsV2(
    val id: Long,
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "track_number") val trackNumber: String,
    val title: String,
    val duration: Double?,
    @ColumnInfo(name = "formats_info") val formatsInfo: String? // Concatenated format info
)

data class TrackWithRecordingContextV2(
    val id: Long,
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "track_number") val trackNumber: String,
    val title: String,
    val duration: Double?,
    @ColumnInfo(name = "source_type") val sourceType: String?,
    val rating: Double,
    val venue: String,
    val date: String,
    @ColumnInfo(name = "show_id") val showId: String? = null,
    val city: String?,
    val state: String?
)

data class TrackStatisticsV2(
    val title: String,
    @ColumnInfo(name = "performance_count") val performanceCount: Int,
    @ColumnInfo(name = "avg_duration") val avgDuration: Double,
    @ColumnInfo(name = "max_duration") val maxDuration: Double,
    @ColumnInfo(name = "min_duration") val minDuration: Double
)