package com.deadarchive.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for playback history operations.
 * Provides reactive queries for UI and efficient access patterns for analytics.
 * 
 * Follows existing DAO patterns with Flow-based reactive queries, bulk operations,
 * and proper indexing utilization for performance.
 */
@Dao
interface PlaybackHistoryDao {
    
    // ========================================
    // Basic CRUD Operations
    // ========================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playbackHistory: PlaybackHistoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playbackHistories: List<PlaybackHistoryEntity>)
    
    @Update
    suspend fun update(playbackHistory: PlaybackHistoryEntity)
    
    @Delete
    suspend fun delete(playbackHistory: PlaybackHistoryEntity)
    
    @Query("DELETE FROM playback_history WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM playback_history")
    suspend fun deleteAll()
    
    // ========================================
    // Recent Playback History
    // ========================================
    
    /**
     * Get recent playback history ordered by timestamp (most recent first)
     * Returns Flow for reactive UI updates
     */
    @Query("""
        SELECT * FROM playback_history 
        ORDER BY playbackTimestamp DESC 
        LIMIT :limit
    """)
    fun getRecentPlaybackHistory(limit: Int = 100): Flow<List<PlaybackHistoryEntity>>
    
    /**
     * Get playback history for a specific date range
     */
    @Query("""
        SELECT * FROM playback_history 
        WHERE playbackTimestamp BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY playbackTimestamp DESC
    """)
    fun getPlaybackHistoryByDateRange(
        startTimestamp: Long, 
        endTimestamp: Long
    ): Flow<List<PlaybackHistoryEntity>>
    
    /**
     * Get playback history for a specific recording
     */
    @Query("""
        SELECT * FROM playback_history 
        WHERE recordingId = :recordingId
        ORDER BY playbackTimestamp DESC
    """)
    fun getPlaybackHistoryForRecording(recordingId: String): Flow<List<PlaybackHistoryEntity>>
    
    // ========================================
    // Analytics and Statistics
    // ========================================
    
    /**
     * Get most played recordings with play counts
     */
    @Query("""
        SELECT recordingId, COUNT(*) as playCount
        FROM playback_history 
        WHERE wasCompleted = 1
        GROUP BY recordingId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedRecordings(limit: Int = 20): Flow<List<RecordingPlayCount>>
    
    /**
     * Get most played tracks with play counts
     */
    @Query("""
        SELECT recordingId, trackFilename, trackTitle, COUNT(*) as playCount
        FROM playback_history 
        WHERE wasCompleted = 1
        GROUP BY recordingId, trackFilename 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedTracks(limit: Int = 20): Flow<List<TrackPlayCount>>
    
    /**
     * Get total listening time (completed tracks only)
     */
    @Query("""
        SELECT SUM(COALESCE(playbackDuration, 0)) as totalTime
        FROM playback_history 
        WHERE wasCompleted = 1
    """)
    suspend fun getTotalListeningTime(): Long?
    
    /**
     * Get listening statistics for the last N days
     */
    @Query("""
        SELECT 
            COUNT(*) as totalTracks,
            COUNT(CASE WHEN wasCompleted = 1 THEN 1 END) as completedTracks,
            SUM(COALESCE(playbackDuration, 0)) as totalDuration,
            COUNT(DISTINCT recordingId) as uniqueRecordings,
            COUNT(DISTINCT sessionId) as uniqueSessions
        FROM playback_history 
        WHERE playbackTimestamp >= :sinceTimestamp
    """)
    suspend fun getListeningStats(sinceTimestamp: Long): ListeningStats?
    
    // ========================================
    // Session Management
    // ========================================
    
    /**
     * Get all playback events for a specific session
     */
    @Query("""
        SELECT * FROM playback_history 
        WHERE sessionId = :sessionId
        ORDER BY playbackTimestamp ASC
    """)
    fun getPlaybackHistoryForSession(sessionId: String): Flow<List<PlaybackHistoryEntity>>
    
    /**
     * Get incomplete playback sessions (no completion timestamp)
     */
    @Query("""
        SELECT DISTINCT sessionId FROM playback_history 
        WHERE sessionId IS NOT NULL 
        AND completionTimestamp IS NULL
        ORDER BY playbackTimestamp DESC
    """)
    suspend fun getIncompleteSessionIds(): List<String>
    
    // ========================================
    // Maintenance Operations
    // ========================================
    
    /**
     * Delete old playback history beyond retention period
     */
    @Query("""
        DELETE FROM playback_history 
        WHERE playbackTimestamp < :cutoffTimestamp
    """)
    suspend fun deleteOldHistory(cutoffTimestamp: Long)
    
    /**
     * Get count of total playback events
     */
    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getTotalPlaybackCount(): Int
    
    /**
     * Get playback history count for debugging
     */
    @Query("SELECT COUNT(*) FROM playback_history")
    fun getPlaybackHistoryCount(): Flow<Int>
    
    // ========================================
    // Data Classes for Query Results
    // ========================================
    
    data class RecordingPlayCount(
        val recordingId: String,
        val playCount: Int
    )
    
    data class TrackPlayCount(
        val recordingId: String,
        val trackFilename: String,
        val trackTitle: String,
        val playCount: Int
    )
    
    data class ListeningStats(
        val totalTracks: Int,
        val completedTracks: Int,
        val totalDuration: Long,
        val uniqueRecordings: Int,
        val uniqueSessions: Int
    ) {
        val completionRate: Float
            get() = if (totalTracks > 0) completedTracks.toFloat() / totalTracks.toFloat() else 0f
        
        val averageSessionLength: Float
            get() = if (uniqueSessions > 0) totalTracks.toFloat() / uniqueSessions.toFloat() else 0f
    }
}