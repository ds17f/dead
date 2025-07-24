package com.deadarchive.core.media.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated tracker for Media3 playback events, focused on capturing data needed
 * for playback history tracking. This complements MediaControllerRepository by
 * providing specialized event monitoring for history purposes.
 * 
 * Key events tracked:
 * - onMediaItemTransition: Critical for detecting track changes during playback
 * - onIsPlayingChanged: For session start/stop detection
 * - onPlaybackStateChanged: For buffering/ready state tracking
 * - onPositionDiscontinuity: For seek event detection
 * 
 * This tracker focuses on the timing and sequencing issues identified in the
 * playback history analysis, particularly the missing track transitions during
 * active playback sessions.
 */
@Singleton
class PlaybackEventTracker @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    companion object {
        private const val TAG = "PlaybackEventTracker"
    }
    
    /**
     * Represents a captured Media3 playback event with timestamp and context
     */
    data class PlaybackEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val eventType: EventType,
        val mediaItem: MediaItem? = null,
        val position: Long = 0L,
        val isPlaying: Boolean = false,
        val playbackState: Int = Player.STATE_IDLE,
        val reason: Int? = null,
        val reasonName: String? = null,
        val trackUrl: String? = null,
        val trackTitle: String? = null,
        val sessionContext: String? = null
    )
    
    /**
     * Types of events we track for playback history
     */
    enum class EventType {
        MEDIA_ITEM_TRANSITION,
        PLAYBACK_STATE_CHANGED,
        IS_PLAYING_CHANGED,
        POSITION_DISCONTINUITY,
        PLAYER_ERROR,
        SESSION_START,
        SESSION_END
    }
    
    // Event tracking state
    private val _events = MutableStateFlow<List<PlaybackEvent>>(emptyList())
    val events: StateFlow<List<PlaybackEvent>> = _events.asStateFlow()
    
    private val _currentEvent = MutableStateFlow<PlaybackEvent?>(null)
    val currentEvent: StateFlow<PlaybackEvent?> = _currentEvent.asStateFlow()
    
    // Session tracking
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    
    private val _sessionStartTime = MutableStateFlow<Long?>(null)
    val sessionStartTime: StateFlow<Long?> = _sessionStartTime.asStateFlow()
    
    // Statistics
    private val _totalEventsTracked = MutableStateFlow(0)
    val totalEventsTracked: StateFlow<Int> = _totalEventsTracked.asStateFlow()
    
    private val _transitionEventsTracked = MutableStateFlow(0)
    val transitionEventsTracked: StateFlow<Int> = _transitionEventsTracked.asStateFlow()
    
    private var mediaController: MediaController? = null
    private var listener: Player.Listener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        Log.d(TAG, "PlaybackEventTracker initializing")
        startMonitoringConnection()
    }
    
    /**
     * Monitor MediaController connection state and connect automatically when ready
     */
    private fun startMonitoringConnection() {
        coroutineScope.launch {
            mediaControllerRepository.isConnected.collect { isConnected ->
                Log.d(TAG, "MediaController connection state changed: $isConnected")
                if (isConnected && mediaController == null) {
                    Log.d(TAG, "MediaController became available, attempting to set up listener")
                    setupMediaControllerListener()
                } else if (!isConnected && mediaController != null) {
                    Log.d(TAG, "MediaController disconnected, cleaning up listener")
                    cleanupListener()
                }
            }
        }
    }
    
    /**
     * Set up listener on MediaController to capture Media3 events
     */
    private fun setupMediaControllerListener() {
        // Get MediaController from repository and add our specialized listener
        mediaController = mediaControllerRepository.getMediaController()
        
        if (mediaController == null) {
            Log.d(TAG, "MediaController not available yet, will retry when connectToMediaController is called")
            return
        }
        
        addPlaybackEventListener()
    }
    
    /**
     * Add our specialized Player.Listener to capture events for history tracking
     */
    private fun addPlaybackEventListener() {
        val controller = mediaController ?: return
        
        listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "=== PLAYBACK EVENT: onMediaItemTransition ===")
                Log.d(TAG, "MediaItem: ${mediaItem?.mediaId}")
                Log.d(TAG, "Transition reason: $reason")
                
                val reasonName = getTransitionReasonName(reason)
                val event = PlaybackEvent(
                    eventType = EventType.MEDIA_ITEM_TRANSITION,
                    mediaItem = mediaItem,
                    position = controller.currentPosition,
                    isPlaying = controller.isPlaying,
                    playbackState = controller.playbackState,
                    reason = reason,
                    reasonName = reasonName,
                    trackUrl = mediaItem?.mediaId,
                    trackTitle = mediaItem?.mediaMetadata?.title?.toString(),
                    sessionContext = generateSessionContext()
                )
                
                trackEvent(event)
                _transitionEventsTracked.value = _transitionEventsTracked.value + 1
                
                Log.d(TAG, "Track transition recorded: ${event.trackTitle} (reason: $reasonName)")
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "=== PLAYBACK EVENT: onIsPlayingChanged ===")
                Log.d(TAG, "Is playing: $isPlaying")
                
                val event = PlaybackEvent(
                    eventType = EventType.IS_PLAYING_CHANGED,
                    mediaItem = controller.currentMediaItem,
                    position = controller.currentPosition,
                    isPlaying = isPlaying,
                    playbackState = controller.playbackState,
                    trackUrl = controller.currentMediaItem?.mediaId,
                    trackTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString(),
                    sessionContext = generateSessionContext()
                )
                
                trackEvent(event)
                
                // Session management
                if (isPlaying && !_isSessionActive.value) {
                    startSession()
                } else if (!isPlaying && _isSessionActive.value) {
                    // Don't immediately end session on pause - wait for actual stop/end
                    Log.d(TAG, "Playback paused, session remains active")
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "=== PLAYBACK EVENT: onPlaybackStateChanged ===")
                Log.d(TAG, "Playback state: $playbackState")
                
                val stateName = getPlaybackStateName(playbackState)
                val event = PlaybackEvent(
                    eventType = EventType.PLAYBACK_STATE_CHANGED,
                    mediaItem = controller.currentMediaItem,
                    position = controller.currentPosition,
                    isPlaying = controller.isPlaying,
                    playbackState = playbackState,
                    reasonName = stateName,
                    trackUrl = controller.currentMediaItem?.mediaId,
                    trackTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString(),
                    sessionContext = generateSessionContext()
                )
                
                trackEvent(event)
                
                // Session management based on playback state
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "Playback ended, ending session")
                        endSession()
                    }
                    Player.STATE_IDLE -> {
                        if (_isSessionActive.value) {
                            Log.d(TAG, "Player went idle, ending session")
                            endSession()
                        }
                    }
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo, 
                newPosition: Player.PositionInfo, 
                reason: Int
            ) {
                Log.d(TAG, "=== PLAYBACK EVENT: onPositionDiscontinuity ===")
                Log.d(TAG, "Position changed from ${oldPosition.positionMs} to ${newPosition.positionMs}")
                Log.d(TAG, "Reason: $reason")
                
                val event = PlaybackEvent(
                    eventType = EventType.POSITION_DISCONTINUITY,
                    mediaItem = controller.currentMediaItem,
                    position = newPosition.positionMs,
                    isPlaying = controller.isPlaying,
                    playbackState = controller.playbackState,
                    reason = reason,
                    trackUrl = controller.currentMediaItem?.mediaId,
                    trackTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString(),
                    sessionContext = generateSessionContext()
                )
                
                trackEvent(event)
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "=== PLAYBACK EVENT: onPlayerError ===", error)
                
                val event = PlaybackEvent(
                    eventType = EventType.PLAYER_ERROR,
                    mediaItem = controller.currentMediaItem,
                    position = controller.currentPosition,
                    isPlaying = controller.isPlaying,
                    playbackState = controller.playbackState,
                    reasonName = error.message,
                    trackUrl = controller.currentMediaItem?.mediaId,
                    trackTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString(),
                    sessionContext = generateSessionContext()
                )
                
                trackEvent(event)
                
                // End session on error
                if (_isSessionActive.value) {
                    Log.d(TAG, "Player error occurred, ending session")
                    endSession()
                }
            }
        }
        
        controller.addListener(listener!!)
        Log.d(TAG, "PlaybackEventTracker listener added to MediaController successfully")
    }
    
    /**
     * Record a playback event and update state flows
     */
    private fun trackEvent(event: PlaybackEvent) {
        _currentEvent.value = event
        
        // Add to events list (keep last 100 events for debug purposes)
        val currentEvents = _events.value.toMutableList()
        currentEvents.add(event)
        if (currentEvents.size > 100) {
            currentEvents.removeAt(0)
        }
        _events.value = currentEvents
        
        _totalEventsTracked.value = _totalEventsTracked.value + 1
        
        Log.d(TAG, "Event tracked: ${event.eventType} at ${event.timestamp}")
    }
    
    /**
     * Start a new playback session
     */
    private fun startSession() {
        if (_isSessionActive.value) {
            Log.d(TAG, "Session already active, not starting new one")
            return
        }
        
        val startTime = System.currentTimeMillis()
        _isSessionActive.value = true
        _sessionStartTime.value = startTime
        
        val event = PlaybackEvent(
            eventType = EventType.SESSION_START,
            sessionContext = "Session started at $startTime"
        )
        trackEvent(event)
        
        Log.d(TAG, "Playback session started at $startTime")
    }
    
    /**
     * End the current playback session
     */
    private fun endSession() {
        if (!_isSessionActive.value) {
            Log.d(TAG, "No active session to end")
            return
        }
        
        val endTime = System.currentTimeMillis()
        val startTime = _sessionStartTime.value ?: endTime
        val duration = endTime - startTime
        
        _isSessionActive.value = false
        
        val event = PlaybackEvent(
            eventType = EventType.SESSION_END,
            sessionContext = "Session ended at $endTime, duration: ${duration}ms"
        )
        trackEvent(event)
        
        Log.d(TAG, "Playback session ended at $endTime, duration: ${duration}ms")
    }
    
    /**
     * Generate context information for the current session
     */
    private fun generateSessionContext(): String {
        val sessionActive = _isSessionActive.value
        val sessionStart = _sessionStartTime.value
        val currentTime = System.currentTimeMillis()
        
        return if (sessionActive && sessionStart != null) {
            val duration = currentTime - sessionStart
            "Active session, duration: ${duration}ms"
        } else {
            "No active session"
        }
    }
    
    /**
     * Get human-readable name for transition reason
     */
    private fun getTransitionReasonName(reason: Int): String {
        return when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
            else -> "UNKNOWN($reason)"
        }
    }
    
    /**
     * Get human-readable name for playback state
     */
    private fun getPlaybackStateName(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN($state)"
        }
    }
    
    /**
     * Clear all tracked events (for debugging)
     */
    fun clearEvents() {
        _events.value = emptyList()
        _totalEventsTracked.value = 0
        _transitionEventsTracked.value = 0
        Log.d(TAG, "All tracked events cleared")
    }
    
    /**
     * Get current event statistics
     */
    fun getEventStats(): Map<String, Any> {
        return mapOf(
            "totalEvents" to _totalEventsTracked.value,
            "transitionEvents" to _transitionEventsTracked.value,
            "sessionActive" to _isSessionActive.value,
            "sessionStartTime" to (_sessionStartTime.value ?: 0L),
            "currentEventsInMemory" to _events.value.size
        )
    }
    
    /**
     * Public method to manually connect to MediaController (called by dependency injection)
     */
    fun connectToMediaController() {
        setupMediaControllerListener()
    }
    
    /**
     * Clean up listener when MediaController disconnects
     */
    private fun cleanupListener() {
        listener?.let { 
            mediaController?.removeListener(it)
            Log.d(TAG, "PlaybackEventTracker listener removed from MediaController")
        }
        mediaController = null
        listener = null
        
        if (_isSessionActive.value) {
            endSession()
        }
    }
    
    /**
     * Clean up when service is destroyed
     */
    fun cleanup() {
        cleanupListener()
        coroutineScope.cancel()
        Log.d(TAG, "PlaybackEventTracker cleaned up completely")
    }
}