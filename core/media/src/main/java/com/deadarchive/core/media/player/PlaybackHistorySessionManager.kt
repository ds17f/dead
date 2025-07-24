package com.deadarchive.core.media.player

import com.deadarchive.core.data.repository.PlaybackHistoryRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent session management system for playback history tracking.
 * 
 * This is the core coordinator that connects Media3 events to persistent history storage.
 * It manages playback sessions, determines when to record events, and handles the timing
 * complexities of tracking during active playback transitions.
 * 
 * Key responsibilities:
 * - Create and manage playback sessions
 * - Convert Media3 events to history records
 * - Handle session completion and cleanup
 * - Apply business logic for meaningful tracking
 * - Coordinate between PlaybackEventTracker and PlaybackHistoryRepository
 */
@Singleton
class PlaybackHistorySessionManager @Inject constructor(
    private val playbackEventTracker: PlaybackEventTracker,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Current session state
    private var currentSessionId: String? = null
    private var currentTrackHistoryId: String? = null
    private var sessionStartTime: Long? = null
    private var trackStartTime: Long? = null
    private var isSessionActive = false
    
    // Configuration
    companion object {
        private const val MIN_TRACK_DURATION_MS = 30_000L // 30 seconds minimum
        private const val COMPLETION_THRESHOLD = 0.9f // 90% = completed
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        private const val TAG = "PlaybackHistorySessionManager"
    }
    
    init {
        // Start monitoring playback events
        startEventMonitoring()
    }
    
    // ========================================
    // Public API
    // ========================================
    
    /**
     * Start monitoring playback events and automatically manage sessions
     */
    fun startTracking() {
        if (!isSessionActive) {
            startEventMonitoring()
        }
    }
    
    /**
     * Stop monitoring and complete any active session
     */
    suspend fun stopTracking() {
        if (isSessionActive) {
            completeCurrentSession("TRACKING_STOPPED")
        }
        scope.cancel()
    }
    
    /**
     * Force complete the current session (useful for app shutdown)
     */
    suspend fun forceCompleteSession() {
        if (isSessionActive) {
            completeCurrentSession("FORCED_COMPLETION")
        }
    }
    
    /**
     * Get current session information for debugging
     */
    fun getCurrentSessionInfo(): SessionInfo? {
        return if (isSessionActive) {
            SessionInfo(
                sessionId = currentSessionId!!,
                startTime = sessionStartTime!!,
                currentTrackId = currentTrackHistoryId,
                trackStartTime = trackStartTime,
                isActive = true
            )
        } else null
    }
    
    // ========================================
    // Core Event Monitoring
    // ========================================
    
    private fun startEventMonitoring() {
        scope.launch {
            playbackEventTracker.events
                .collect { events ->
                    if (events.isNotEmpty()) {
                        val latestEvent = events.last()
                        handlePlaybackEvent(latestEvent)
                    }
                }
        }
        
        // Monitor session state changes
        scope.launch {
            playbackEventTracker.isSessionActive
                .collect { eventTrackerSessionActive ->
                    if (!eventTrackerSessionActive && isSessionActive) {
                        // Event tracker session ended, complete our history session
                        completeCurrentSession("EVENT_TRACKER_SESSION_ENDED")
                    }
                }
        }
    }
    
    private suspend fun handlePlaybackEvent(event: PlaybackEventTracker.PlaybackEvent) {
        when (event.eventType) {
            PlaybackEventTracker.EventType.SESSION_START -> {
                handleSessionStart(event)
            }
            
            PlaybackEventTracker.EventType.MEDIA_ITEM_TRANSITION -> {
                handleTrackTransition(event)
            }
            
            PlaybackEventTracker.EventType.IS_PLAYING_CHANGED -> {
                handlePlayingStateChange(event)
            }
            
            PlaybackEventTracker.EventType.SESSION_END -> {
                handleSessionEnd(event)
            }
            
            PlaybackEventTracker.EventType.PLAYER_ERROR -> {
                handlePlayerError(event)
            }
            
            else -> {
                // Other events don't require special handling
            }
        }
    }
    
    // ========================================
    // Session Management
    // ========================================
    
    private suspend fun handleSessionStart(event: PlaybackEventTracker.PlaybackEvent) {
        // Complete any existing session first
        if (isSessionActive) {
            completeCurrentSession("NEW_SESSION_STARTED")
        }
        
        // Start new session
        currentSessionId = UUID.randomUUID().toString()
        sessionStartTime = event.timestamp
        isSessionActive = true
        
        // If we have track info, start tracking the first track
        if (event.trackUrl != null) {
            startTrackHistory(event)
        }
    }
    
    private suspend fun handleTrackTransition(event: PlaybackEventTracker.PlaybackEvent) {
        // Complete current track if one is active
        if (currentTrackHistoryId != null) {
            completeCurrentTrack(event, event.reasonName ?: "MEDIA_ITEM_TRANSITION")
        }
        
        // Start new track if session is active and we have track info
        if (isSessionActive && event.trackUrl != null) {
            startTrackHistory(event)
        }
    }
    
    private suspend fun handlePlayingStateChange(event: PlaybackEventTracker.PlaybackEvent) {
        if (!event.isPlaying && currentTrackHistoryId != null) {
            // Paused - complete current track history
            completeCurrentTrack(event, "PLAYBACK_PAUSED")
        } else if (event.isPlaying && currentTrackHistoryId == null && event.trackUrl != null) {
            // Resumed - start new track history if we don't have one
            startTrackHistory(event)
        }
    }
    
    private suspend fun handleSessionEnd(event: PlaybackEventTracker.PlaybackEvent) {
        completeCurrentSession("SESSION_END_EVENT")
    }
    
    private suspend fun handlePlayerError(event: PlaybackEventTracker.PlaybackEvent) {
        if (currentTrackHistoryId != null) {
            completeCurrentTrack(event, "PLAYER_ERROR")
        }
    }
    
    // ========================================
    // Track History Management
    // ========================================
    
    private suspend fun startTrackHistory(event: PlaybackEventTracker.PlaybackEvent) {
        if (!isSessionActive || event.trackUrl == null) return
        
        // Ensure we have a session
        if (currentSessionId == null) {
            currentSessionId = UUID.randomUUID().toString()
            sessionStartTime = event.timestamp
            isSessionActive = true
        }
        
        // Extract recording ID from track URL or track title
        val recordingId = extractRecordingId(event.trackUrl, event.trackTitle)
        if (recordingId == null) {
            return // Can't track without recording ID
        }
        
        val historyId = UUID.randomUUID().toString()
        currentTrackHistoryId = historyId
        trackStartTime = event.timestamp
        
        try {
            playbackHistoryRepository.recordPlaybackEvent(
                recordingId = recordingId,
                trackFilename = extractFilename(event.trackUrl),
                trackUrl = event.trackUrl,
                trackTitle = event.trackTitle ?: "Unknown Track",
                trackNumber = extractTrackNumber(event.trackTitle),
                playbackTimestamp = event.timestamp,
                playbackSource = determinePlaybackSource(event.trackUrl),
                sessionId = currentSessionId,
                playbackContext = "MEDIA3_SESSION"
            )
        } catch (e: Exception) {
            // Log error but continue
            currentTrackHistoryId = null
            trackStartTime = null
        }
    }
    
    private suspend fun completeCurrentTrack(
        event: PlaybackEventTracker.PlaybackEvent, 
        reason: String
    ) {
        val historyId = currentTrackHistoryId ?: return
        val startTime = trackStartTime ?: return
        
        val playbackDuration = event.timestamp - startTime
        val finalPosition = event.position
        val trackDuration = mediaControllerRepository.duration.value
        
        // Only record meaningful listens
        if (playbackDuration >= MIN_TRACK_DURATION_MS || 
            playbackHistoryRepository.isPlaybackMeaningful(playbackDuration, trackDuration, finalPosition)) {
            
            val wasCompleted = playbackHistoryRepository.isTrackCompleted(finalPosition, trackDuration)
            
            try {
                playbackHistoryRepository.updatePlaybackCompletion(
                    playbackId = historyId,
                    completionTimestamp = event.timestamp,
                    finalPosition = finalPosition,
                    playbackDuration = playbackDuration,
                    wasCompleted = wasCompleted,
                    transitionReason = reason
                )
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        
        // Reset current track
        currentTrackHistoryId = null
        trackStartTime = null
    }
    
    private suspend fun completeCurrentSession(reason: String) {
        if (!isSessionActive) return
        
        // Complete current track if active
        if (currentTrackHistoryId != null) {
            val currentEvent = playbackEventTracker.currentEvent.value
            if (currentEvent != null) {
                completeCurrentTrack(currentEvent, reason)
            }
        }
        
        // Complete session in repository
        currentSessionId?.let { sessionId ->
            try {
                playbackHistoryRepository.completeSession(sessionId)
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        
        // Reset session state
        currentSessionId = null
        sessionStartTime = null
        isSessionActive = false
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    /**
     * Extract recording ID from track URL or title
     * Assumes URL format like: /recordings/{recordingId}/tracks/{filename}
     */
    private fun extractRecordingId(trackUrl: String, trackTitle: String?): String? {
        // Try to extract from URL first
        val urlPattern = """/recordings/([^/]+)/""".toRegex()
        val urlMatch = urlPattern.find(trackUrl)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }
        
        // Try to extract from current track info
        return try {
            mediaControllerRepository.currentRecordingId.value
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract filename from track URL
     */
    private fun extractFilename(trackUrl: String): String {
        return trackUrl.substringAfterLast("/").ifEmpty { trackUrl }
    }
    
    /**
     * Extract track number from track title if present
     */
    private fun extractTrackNumber(trackTitle: String?): Int? {
        if (trackTitle == null) return null
        
        // Look for patterns like "01 - Title" or "Track 1"
        val patterns = listOf(
            """^(\d{1,2})\s*[-\s]""".toRegex(),
            """Track\s+(\d+)""".toRegex(RegexOption.IGNORE_CASE),
            """^(\d+)\.?\s""".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(trackTitle)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        
        return null
    }
    
    /**
     * Determine playback source from track URL
     */
    private fun determinePlaybackSource(trackUrl: String): String {
        return when {
            trackUrl.startsWith("file://") -> "LOCAL"
            trackUrl.startsWith("content://") -> "LOCAL"
            trackUrl.contains("localhost") -> "LOCAL"
            trackUrl.startsWith("http://") -> "STREAM"
            trackUrl.startsWith("https://") -> "STREAM"
            else -> "UNKNOWN"
        }
    }
    
    // ========================================
    // Data Classes
    // ========================================
    
    data class SessionInfo(
        val sessionId: String,
        val startTime: Long,
        val currentTrackId: String?,
        val trackStartTime: Long?,
        val isActive: Boolean
    )
    
    // ========================================
    // Lifecycle Management
    // ========================================
    
    /**
     * Clean up resources when manager is destroyed
     */
    fun cleanup() {
        scope.launch {
            if (isSessionActive) {
                completeCurrentSession("MANAGER_CLEANUP")
            }
            scope.cancel()
        }
    }
}