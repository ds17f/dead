package com.deadarchive.core.data.repository

import com.deadarchive.core.database.PlaybackHistoryDao
import com.deadarchive.core.database.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * Repository for managing playback history data operations.
 * Provides business logic layer between the database and application components.
 * 
 * Handles playback event persistence, analytics queries, and data cleanup
 * while abstracting database implementation details from the rest of the app.
 */
@Singleton
class PlaybackHistoryRepository @Inject constructor(
    private val playbackHistoryDao: PlaybackHistoryDao
) {
    
    // ========================================
    // Playback Event Recording
    // ========================================
    
    /**
     * Record a new playback event to the history database.
     * This is the primary method for tracking when tracks are played.
     */
    suspend fun recordPlaybackEvent(
        recordingId: String,
        trackFilename: String,
        trackUrl: String,
        trackTitle: String,
        trackNumber: Int? = null,
        playbackTimestamp: Long = System.currentTimeMillis(),
        completionTimestamp: Long? = null,
        playbackDuration: Long? = null,
        finalPosition: Long = 0L,
        trackDuration: Long? = null,
        wasCompleted: Boolean = false,
        playbackSource: String = "UNKNOWN",
        sessionId: String? = null,
        transitionReason: String? = null,
        playbackContext: String? = null
    ) {
        val playbackHistory = PlaybackHistoryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            trackFilename = trackFilename,
            trackUrl = trackUrl,
            trackTitle = trackTitle,
            trackNumber = trackNumber,
            playbackTimestamp = playbackTimestamp,
            completionTimestamp = completionTimestamp,
            playbackDuration = playbackDuration,
            finalPosition = finalPosition,
            trackDuration = trackDuration,
            wasCompleted = wasCompleted,
            playbackSource = playbackSource,
            sessionId = sessionId,
            transitionReason = transitionReason,
            playbackContext = playbackContext,
            trackingVersion = 1
        )
        
        playbackHistoryDao.insert(playbackHistory)
    }
    
    /**
     * Update an existing playback event with completion information.
     * Used when a track finishes playing to record final statistics.
     */
    suspend fun updatePlaybackCompletion(
        playbackId: String,
        completionTimestamp: Long,
        finalPosition: Long,
        playbackDuration: Long,
        wasCompleted: Boolean,
        transitionReason: String? = null
    ) {
        // Fetch existing record
        val existingRecords = playbackHistoryDao.getRecentPlaybackHistory(limit = 1000)
        existingRecords.collect { records ->
            val record = records.find { it.id == playbackId }
            if (record != null) {
                val updatedRecord = record.copy(
                    completionTimestamp = completionTimestamp,
                    finalPosition = finalPosition,
                    playbackDuration = playbackDuration,
                    wasCompleted = wasCompleted,
                    transitionReason = transitionReason
                )
                playbackHistoryDao.update(updatedRecord)
            }
        }
    }
    
    // ========================================
    // History Queries
    // ========================================
    
    /**
     * Get recent playback history for display in UI
     */
    fun getRecentPlaybackHistory(limit: Int = 100): Flow<List<PlaybackHistoryEntity>> {
        return playbackHistoryDao.getRecentPlaybackHistory(limit)
    }
    
    /**
     * Get playback history for a specific date range
     */
    fun getPlaybackHistoryByDateRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<PlaybackHistoryEntity>> {
        return playbackHistoryDao.getPlaybackHistoryByDateRange(startTimestamp, endTimestamp)
    }
    
    /**
     * Get playback history for a specific recording
     */
    fun getPlaybackHistoryForRecording(recordingId: String): Flow<List<PlaybackHistoryEntity>> {
        return playbackHistoryDao.getPlaybackHistoryForRecording(recordingId)
    }
    
    /**
     * Get playback history for a specific session
     */
    fun getPlaybackHistoryForSession(sessionId: String): Flow<List<PlaybackHistoryEntity>> {
        return playbackHistoryDao.getPlaybackHistoryForSession(sessionId)
    }
    
    // ========================================
    // Analytics and Statistics
    // ========================================
    
    /**
     * Get most played recordings with play counts
     */
    fun getMostPlayedRecordings(limit: Int = 20): Flow<List<PlaybackHistoryDao.RecordingPlayCount>> {
        return playbackHistoryDao.getMostPlayedRecordings(limit)
    }
    
    /**
     * Get most played tracks with play counts
     */
    fun getMostPlayedTracks(limit: Int = 20): Flow<List<PlaybackHistoryDao.TrackPlayCount>> {
        return playbackHistoryDao.getMostPlayedTracks(limit)
    }
    
    /**
     * Get total listening time across all completed tracks
     */
    suspend fun getTotalListeningTime(): Long {
        return playbackHistoryDao.getTotalListeningTime() ?: 0L
    }
    
    /**
     * Get listening statistics for the last N days
     */
    suspend fun getListeningStats(dayCount: Int = 7): PlaybackHistoryDao.ListeningStats? {
        val cutoffTime = System.currentTimeMillis() - (dayCount * 24 * 60 * 60 * 1000L)
        return playbackHistoryDao.getListeningStats(cutoffTime)
    }
    
    /**
     * Get listening statistics for today
     */
    suspend fun getTodayListeningStats(): PlaybackHistoryDao.ListeningStats? {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        return playbackHistoryDao.getListeningStats(todayStart)
    }
    
    /**
     * Get weekly listening statistics
     */
    suspend fun getWeeklyListeningStats(): PlaybackHistoryDao.ListeningStats? {
        return getListeningStats(7)
    }
    
    /**
     * Get monthly listening statistics
     */
    suspend fun getMonthlyListeningStats(): PlaybackHistoryDao.ListeningStats? {
        return getListeningStats(30)
    }
    
    // ========================================
    // Resume Functionality
    // ========================================
    
    /**
     * Get the last track that was interrupted and could be resumed
     * Returns null if no suitable track found or last track was completed
     */
    suspend fun getLastIncompleteTrack(): PlaybackHistoryEntity? {
        return playbackHistoryDao.getLastIncompleteTrack()
    }
    
    // ========================================
    // Session Management
    // ========================================
    
    /**
     * Get all incomplete playback sessions that need cleanup
     */
    suspend fun getIncompleteSessionIds(): List<String> {
        return playbackHistoryDao.getIncompleteSessionIds()
    }
    
    /**
     * Mark session as completed by updating all incomplete records in that session
     */
    suspend fun completeSession(sessionId: String, completionTimestamp: Long = System.currentTimeMillis()) {
        val sessionHistory = playbackHistoryDao.getPlaybackHistoryForSession(sessionId)
        sessionHistory.collect { records ->
            records.filter { it.completionTimestamp == null }.forEach { record ->
                val completedRecord = record.copy(
                    completionTimestamp = completionTimestamp,
                    // If no playback duration was recorded, estimate it from timestamps
                    playbackDuration = record.playbackDuration ?: (completionTimestamp - record.playbackTimestamp)
                )
                playbackHistoryDao.update(completedRecord)
            }
        }
    }
    
    // ========================================
    // Data Management
    // ========================================
    
    /**
     * Get total count of playback events for debugging
     */
    fun getPlaybackHistoryCount(): Flow<Int> {
        return playbackHistoryDao.getPlaybackHistoryCount()
    }
    
    /**
     * Get total count of playback events
     */
    suspend fun getTotalPlaybackCount(): Int {
        return playbackHistoryDao.getTotalPlaybackCount()
    }
    
    /**
     * Clean up old playback history beyond retention period.
     * Default retention is 365 days.
     */
    suspend fun cleanupOldHistory(retentionDays: Int = 365) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        playbackHistoryDao.deleteOldHistory(cutoffTime)
    }
    
    /**
     * Clear all playback history (use with caution)
     */
    suspend fun clearAllHistory() {
        playbackHistoryDao.deleteAll()
    }
    
    /**
     * Delete a specific playback event
     */
    suspend fun deletePlaybackEvent(playbackId: String) {
        playbackHistoryDao.deleteById(playbackId)
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    /**
     * Check if a track playback qualifies as a "meaningful listen"
     * Based on duration and completion percentage
     */
    fun isPlaybackMeaningful(
        playbackDuration: Long?,
        trackDuration: Long?,
        finalPosition: Long
    ): Boolean {
        val minDuration = 30_000L // 30 seconds
        val actualDuration = playbackDuration ?: 0L
        
        // Meaningful if played for at least 30 seconds
        if (actualDuration >= minDuration) return true
        
        // Or if played for at least 25% of track duration
        if (trackDuration != null && trackDuration > 0) {
            val completionPercent = finalPosition.toFloat() / trackDuration.toFloat()
            if (completionPercent >= 0.25f) return true
        }
        
        return false
    }
    
    /**
     * Calculate completion percentage for a playback event
     */
    fun calculateCompletionPercentage(finalPosition: Long, trackDuration: Long?): Float {
        return if (trackDuration != null && trackDuration > 0) {
            (finalPosition.toFloat() / trackDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Determine if a track was "completed" based on position and duration
     * A track is considered completed if played to >90% of its duration
     */
    fun isTrackCompleted(finalPosition: Long, trackDuration: Long?): Boolean {
        val completionPercent = calculateCompletionPercentage(finalPosition, trackDuration)
        return completionPercent >= 0.9f
    }
}