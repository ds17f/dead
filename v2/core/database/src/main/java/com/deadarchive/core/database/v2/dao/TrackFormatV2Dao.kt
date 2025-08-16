package com.deadarchive.v2.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.TrackFormatV2Entity

@Dao
interface TrackFormatV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackFormat(trackFormat: TrackFormatV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackFormats(trackFormats: List<TrackFormatV2Entity>): List<Long>
    
    @Query("SELECT * FROM track_formats_v2 WHERE id = :formatId")
    suspend fun getTrackFormatById(formatId: Long): TrackFormatV2Entity?
    
    @Query("SELECT * FROM track_formats_v2 WHERE track_id = :trackId ORDER BY format")
    suspend fun getFormatsForTrack(trackId: Long): List<TrackFormatV2Entity>
    
    @Query("SELECT * FROM track_formats_v2 WHERE track_id = :trackId AND format = :format LIMIT 1")
    suspend fun getFormatForTrack(trackId: Long, format: String): TrackFormatV2Entity?
    
    @Query("SELECT COUNT(*) FROM track_formats_v2")
    suspend fun getTrackFormatCount(): Int
    
    @Query("SELECT COUNT(*) FROM track_formats_v2 WHERE track_id = :trackId")
    suspend fun getFormatCountForTrack(trackId: Long): Int
    
    @Query("DELETE FROM track_formats_v2 WHERE track_id = :trackId")
    suspend fun deleteFormatsForTrack(trackId: Long)
    
    @Query("DELETE FROM track_formats_v2")
    suspend fun deleteAllTrackFormats()
    
    /**
     * Get preferred format for track based on quality preference.
     * Returns best available format with preference order: Flac > VBR MP3 > others.
     */
    @Query("""
        SELECT * FROM track_formats_v2 
        WHERE track_id = :trackId 
        ORDER BY 
            CASE format 
                WHEN 'Flac' THEN 1 
                WHEN 'VBR MP3' THEN 2 
                ELSE 3 
            END,
            CAST(COALESCE(bitrate, '999') AS INTEGER) DESC
        LIMIT 1
    """)
    suspend fun getPreferredFormatForTrack(trackId: Long): TrackFormatV2Entity?
    
    /**
     * Get all FLAC formats for high-quality playback preference.
     */
    @Query("SELECT * FROM track_formats_v2 WHERE format = 'Flac' ORDER BY track_id")
    suspend fun getAllFlacFormats(): List<TrackFormatV2Entity>
    
    /**
     * Get formats with track context for media player file resolution.
     * Essential for building playback URLs with track metadata.
     */
    @Query("""
        SELECT tf.*, t.recording_id, t.track_number, t.title, t.duration
        FROM track_formats_v2 tf
        JOIN tracks_v2 t ON tf.track_id = t.id
        WHERE tf.track_id = :trackId
        ORDER BY 
            CASE tf.format 
                WHEN 'Flac' THEN 1 
                WHEN 'VBR MP3' THEN 2 
                ELSE 3 
            END
    """)
    suspend fun getFormatsWithTrackContext(trackId: Long): List<TrackFormatWithContextV2>
    
    /**
     * Get format statistics for analysis and debugging.
     * Useful for understanding format distribution across the archive.
     */
    @Query("""
        SELECT 
            format,
            COUNT(*) as count,
            AVG(CAST(COALESCE(bitrate, '0') AS INTEGER)) as avg_bitrate,
            COUNT(DISTINCT track_id) as unique_tracks
        FROM track_formats_v2 
        GROUP BY format
        ORDER BY count DESC
    """)
    suspend fun getFormatStatistics(): List<FormatStatisticsV2>
    
    /**
     * Find tracks with specific format available.
     * Useful for quality-based filtering and audiophile features.
     */
    @Query("""
        SELECT tf.*, t.recording_id, t.track_number, t.title, t.duration,
               r.source_type, r.rating, r.venue, r.date
        FROM track_formats_v2 tf
        JOIN tracks_v2 t ON tf.track_id = t.id
        JOIN recordings_v2 r ON t.recording_id = r.identifier
        WHERE tf.format = :format
        ORDER BY r.rating DESC, r.date DESC
        LIMIT :limit
    """)
    suspend fun getTracksWithFormat(format: String, limit: Int = 100): List<TrackFormatWithFullContextV2>
    
    /**
     * Get format availability for a complete recording.
     * Helps determine if entire recording is available in preferred format.
     */
    @Query("""
        SELECT tf.format, COUNT(*) as track_count
        FROM track_formats_v2 tf
        JOIN tracks_v2 t ON tf.track_id = t.id
        WHERE t.recording_id = :recordingId
        GROUP BY tf.format
        ORDER BY track_count DESC
    """)
    suspend fun getFormatAvailabilityForRecording(recordingId: String): List<FormatAvailabilityV2>
    
    /**
     * Get best bitrate for a specific format and track.
     * Useful for selecting highest quality compressed format.
     */
    @Query("""
        SELECT * FROM track_formats_v2 
        WHERE track_id = :trackId AND format = :format
        ORDER BY CAST(COALESCE(bitrate, '0') AS INTEGER) DESC
        LIMIT 1
    """)
    suspend fun getBestBitrateForFormat(trackId: Long, format: String): TrackFormatV2Entity?
}

// Data classes for complex query results
data class TrackFormatWithContextV2(
    val id: Long,
    @ColumnInfo(name = "track_id") val trackId: Long,
    val format: String,
    val filename: String,
    val bitrate: String?,
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "track_number") val trackNumber: String,
    val title: String,
    val duration: Double?
)

data class TrackFormatWithFullContextV2(
    val id: Long,
    @ColumnInfo(name = "track_id") val trackId: Long,
    val format: String,
    val filename: String,
    val bitrate: String?,
    @ColumnInfo(name = "recording_id") val recordingId: String,
    @ColumnInfo(name = "track_number") val trackNumber: String,
    val title: String,
    val duration: Double?,
    @ColumnInfo(name = "source_type") val sourceType: String?,
    val rating: Double,
    val venue: String,
    val date: String
)

data class FormatStatisticsV2(
    val format: String,
    val count: Int,
    @ColumnInfo(name = "avg_bitrate") val avgBitrate: Double,
    @ColumnInfo(name = "unique_tracks") val uniqueTracks: Int
)

data class FormatAvailabilityV2(
    val format: String,
    @ColumnInfo(name = "track_count") val trackCount: Int
)