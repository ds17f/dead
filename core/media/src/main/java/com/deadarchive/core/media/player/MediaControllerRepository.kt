package com.deadarchive.core.media.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.deadarchive.core.media.service.DeadArchivePlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that provides a MediaController-based interface for media playback.
 * This replaces direct ExoPlayer access with service communication through MediaController.
 * Maintains the same StateFlow interface as the original PlayerRepository for UI compatibility.
 * 
 * *** SINGLE SOURCE OF TRUTH ***
 * All UI state is synchronized from the service via MediaController listeners.
 * UI components should ONLY observe these StateFlows, never maintain separate state.
 * Commands are sent to service, state changes come back via listeners.
 */
@UnstableApi
@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "MediaControllerRepository"
        private const val CONNECTION_TIMEOUT_MS = 5000L
    }
    
    // MediaController and connection state
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    // StateFlow properties - identical interface to PlayerRepository
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()
    
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    private val _lastError = MutableStateFlow<PlaybackException?>(null)
    val lastError: StateFlow<PlaybackException?> = _lastError.asStateFlow()
    
    // Connection state for debugging and error handling
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    init {
        Log.d(TAG, "MediaControllerRepository initializing")
        connectToService()
    }
    
    /**
     * Connect to the MediaSessionService using MediaController
     */
    private fun connectToService() {
        Log.d(TAG, "=== ATTEMPTING TO CONNECT TO SERVICE ===")
        Log.d(TAG, "Package name: ${context.packageName}")
        Log.d(TAG, "Service component: ${ComponentName(context, DeadArchivePlaybackService::class.java)}")
        
        val sessionToken = SessionToken(
            context, 
            ComponentName(context, DeadArchivePlaybackService::class.java)
        )
        Log.d(TAG, "SessionToken created: $sessionToken")
        
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
        Log.d(TAG, "MediaController.Builder.buildAsync() called")
        
        controllerFuture?.addListener({
            Log.d(TAG, "MediaController connection callback triggered")
            try {
                mediaController = controllerFuture?.get()
                Log.d(TAG, "=== MEDIACONTROLLER CONNECTED SUCCESSFULLY ===")
                Log.d(TAG, "Controller available commands: ${mediaController?.availableCommands}")
                Log.d(TAG, "Controller instance: ${mediaController?.javaClass?.simpleName}")
                onControllerConnected()
            } catch (e: Exception) {
                Log.e(TAG, "=== MEDIACONTROLLER CONNECTION FAILED ===", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                onControllerConnectionFailed(e)
            }
        }, MoreExecutors.directExecutor())
        
        Log.d(TAG, "Connection listener added, waiting for callback...")
    }
    
    /**
     * Called when MediaController successfully connects to service
     */
    private fun onControllerConnected() {
        val controller = mediaController ?: return
        
        _isConnected.value = true
        
        // Set up listener for player state changes - THIS IS THE SINGLE SOURCE OF TRUTH
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "=== SERVICE STATE CHANGE: onIsPlayingChanged ===")
                Log.d(TAG, "Service isPlaying changed: $isPlaying")
                _isPlaying.value = isPlaying
                
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
                Log.d(TAG, "UI StateFlow updated - isPlaying: ${_isPlaying.value}")
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "=== SERVICE STATE CHANGE: onPlaybackStateChanged ===")
                Log.d(TAG, "Service playbackState changed: $playbackState")
                val stateNames = mapOf(
                    Player.STATE_IDLE to "IDLE",
                    Player.STATE_BUFFERING to "BUFFERING", 
                    Player.STATE_READY to "READY",
                    Player.STATE_ENDED to "ENDED"
                )
                Log.d(TAG, "State name: ${stateNames[playbackState] ?: "UNKNOWN"}")
                
                _playbackState.value = playbackState
                
                // Update duration when ready
                if (playbackState == Player.STATE_READY) {
                    val duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    _duration.value = duration
                    Log.d(TAG, "Duration updated from service: $duration")
                }
                Log.d(TAG, "UI StateFlow updated - playbackState: ${_playbackState.value}")
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "=== SERVICE STATE CHANGE: onMediaItemTransition ===")
                Log.d(TAG, "Service track changed to: ${mediaItem?.mediaId}")
                Log.d(TAG, "Track title: ${mediaItem?.mediaMetadata?.title}")
                Log.d(TAG, "Track artist: ${mediaItem?.mediaMetadata?.artist}")
                Log.d(TAG, "Transition reason: $reason")
                
                val reasonNames = mapOf(
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT to "REPEAT",
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO to "AUTO", 
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK to "SEEK",
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED to "PLAYLIST_CHANGED"
                )
                Log.d(TAG, "Reason name: ${reasonNames[reason] ?: "UNKNOWN"}")
                
                _currentTrack.value = mediaItem
                Log.d(TAG, "UI StateFlow updated - currentTrack: ${_currentTrack.value?.mediaId}")
            }
            
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                Log.d(TAG, "=== SERVICE STATE CHANGE: onPositionDiscontinuity ===")
                Log.d(TAG, "Position changed from ${oldPosition.positionMs} to ${newPosition.positionMs}")
                _currentPosition.value = newPosition.positionMs
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "=== SERVICE STATE CHANGE: onPlayerError ===", error)
                Log.e(TAG, "Service error: ${error.message}")
                _lastError.value = error
                stopPositionUpdates()
                Log.d(TAG, "UI StateFlow updated - error: ${_lastError.value?.message}")
            }
        }
        
        controller.addListener(listener)
        
        // Initialize current state from controller
        updateStateFromController()
    }
    
    /**
     * Called when MediaController connection fails
     */
    private fun onControllerConnectionFailed(error: Exception) {
        Log.d(TAG, "=== CONNECTION FAILED - RESETTING STATE ===")
        _isConnected.value = false
        
        // Reset all StateFlows to default values since we lost connection to service
        _isPlaying.value = false
        _playbackState.value = Player.STATE_IDLE
        _currentPosition.value = 0L
        _duration.value = 0L
        _currentTrack.value = null
        stopPositionUpdates()
        
        _lastError.value = PlaybackException(
            "Failed to connect to media service", 
            error, 
            PlaybackException.ERROR_CODE_REMOTE_ERROR
        )
        
        Log.d(TAG, "State reset to defaults due to connection failure")
        
        // Retry connection after delay
        coroutineScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            Log.d(TAG, "Retrying connection to service")
            connectToService()
        }
    }
    
    /**
     * Update StateFlow values from current MediaController state
     * This ensures UI immediately reflects the current service state on connection
     */
    private fun updateStateFromController() {
        val controller = mediaController ?: return
        
        Log.d(TAG, "=== SYNCING INITIAL STATE FROM SERVICE ===")
        Log.d(TAG, "Service current state:")
        Log.d(TAG, "  - isPlaying: ${controller.isPlaying}")
        Log.d(TAG, "  - playbackState: ${controller.playbackState}")
        Log.d(TAG, "  - currentPosition: ${controller.currentPosition}")
        Log.d(TAG, "  - duration: ${controller.duration}")
        Log.d(TAG, "  - currentMediaItem: ${controller.currentMediaItem?.mediaId}")
        Log.d(TAG, "  - currentTrack title: ${controller.currentMediaItem?.mediaMetadata?.title}")
        
        // Sync all StateFlows with service state
        _isPlaying.value = controller.isPlaying
        _playbackState.value = controller.playbackState
        _currentPosition.value = controller.currentPosition
        _duration.value = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        _currentTrack.value = controller.currentMediaItem
        
        Log.d(TAG, "UI StateFlows updated to match service state:")
        Log.d(TAG, "  - _isPlaying: ${_isPlaying.value}")
        Log.d(TAG, "  - _playbackState: ${_playbackState.value}")
        Log.d(TAG, "  - _currentPosition: ${_currentPosition.value}")
        Log.d(TAG, "  - _duration: ${_duration.value}")
        Log.d(TAG, "  - _currentTrack: ${_currentTrack.value?.mediaId}")
        
        if (controller.isPlaying) {
            startPositionUpdates()
        }
        
        Log.d(TAG, "=== STATE SYNCHRONIZATION COMPLETE ===")
    }
    
    // Public API methods - identical interface to PlayerRepository
    
    /**
     * Play a single track from Archive.org
     */
    fun playTrack(url: String, title: String, artist: String? = null) {
        Log.d(TAG, "=== PLAY TRACK COMMAND ===")
        Log.d(TAG, "URL: $url")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Artist: $artist")
        Log.d(TAG, "Controller connected: ${_isConnected.value}")
        
        val controller = mediaController
        if (controller == null) {
            Log.e(TAG, "=== COMMAND FAILED: MediaController not connected ===")
            Log.e(TAG, "Connection state: ${_isConnected.value}")
            Log.e(TAG, "ControllerFuture: $controllerFuture")
            
            // Try to reconnect if not connected
            if (!_isConnected.value) {
                Log.d(TAG, "Attempting to reconnect to service...")
                connectToService()
            }
            return
        }
        
        // Verify controller is in a good state
        Log.d(TAG, "MediaController available, checking state...")
        Log.d(TAG, "Controller playback state: ${controller.playbackState}")
        Log.d(TAG, "Controller isPlaying: ${controller.isPlaying}")
        Log.d(TAG, "Controller available commands: ${controller.availableCommands}")
        
        try {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()
            
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(url)
                .setMediaMetadata(mediaMetadata)
                .build()
            
            Log.d(TAG, "Created MediaItem: ${mediaItem.mediaId} with metadata: ${mediaItem.mediaMetadata.title}")
            
            // Stop any current playback first
            if (controller.isPlaying) {
                Log.d(TAG, "Stopping current playback...")
                controller.stop()
            }
            
            Log.d(TAG, "Calling controller.setMediaItem()...")
            controller.setMediaItem(mediaItem)
            
            Log.d(TAG, "Calling controller.prepare()...")
            controller.prepare()
            
            // Wait a moment for prepare to complete
            Log.d(TAG, "Calling controller.play()...")
            controller.play()
            
            Log.d(TAG, "=== ALL COMMANDS SENT SUCCESSFULLY ===")
            Log.d(TAG, "Controller state after commands: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
            
            // Log the current MediaItem to verify it was set
            Log.d(TAG, "Current MediaItem after commands: ${controller.currentMediaItem?.mediaId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR SENDING COMMANDS ===", e)
            _lastError.value = PlaybackException(
                "Failed to play track: ${e.message}",
                e,
                PlaybackException.ERROR_CODE_UNSPECIFIED
            )
        }
    }
    
    /**
     * Play a playlist of tracks
     */
    fun playPlaylist(urls: List<String>) {
        Log.d(TAG, "playPlaylist: ${urls.size} tracks")
        
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "MediaController not connected, cannot play playlist")
            return
        }
        
        try {
            val mediaItems = urls.map { url ->
                MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(url)
                    .build()
            }
            
            controller.setMediaItems(mediaItems)
            controller.prepare()
            controller.play()
            
            Log.d(TAG, "Playlist queued for playback")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing playlist", e)
            _lastError.value = PlaybackException(
                "Failed to play playlist: ${e.message}",
                e,
                PlaybackException.ERROR_CODE_UNSPECIFIED
            )
        }
    }
    
    /**
     * Resume/start playback
     */
    fun play() {
        Log.d(TAG, "=== PLAY COMMAND ===")
        Log.d(TAG, "Controller connected: ${_isConnected.value}")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Controller state before play: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
            controller.play()
            Log.d(TAG, "Controller state after play: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
        } else {
            Log.w(TAG, "=== PLAY COMMAND FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        Log.d(TAG, "=== PAUSE COMMAND ===")
        Log.d(TAG, "Controller connected: ${_isConnected.value}")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Controller state before pause: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
            controller.pause()
            Log.d(TAG, "Controller state after pause: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
        } else {
            Log.w(TAG, "=== PAUSE COMMAND FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        Log.d(TAG, "stop() called")
        mediaController?.stop() ?: Log.w(TAG, "MediaController not connected")
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Long) {
        Log.d(TAG, "seekTo: $position")
        mediaController?.seekTo(position) ?: Log.w(TAG, "MediaController not connected")
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        Log.d(TAG, "skipToNext() called")
        val controller = mediaController
        if (controller != null && controller.hasNextMediaItem()) {
            controller.seekToNext()
        } else {
            Log.w(TAG, "Cannot skip to next - MediaController not connected or no next item")
        }
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        Log.d(TAG, "skipToPrevious() called")
        val controller = mediaController
        if (controller != null && controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
        } else {
            Log.w(TAG, "Cannot skip to previous - MediaController not connected or no previous item")
        }
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: Int) {
        Log.d(TAG, "setRepeatMode: $repeatMode")
        mediaController?.repeatMode = repeatMode
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleMode: Boolean) {
        Log.d(TAG, "setShuffleMode: $shuffleMode")
        mediaController?.shuffleModeEnabled = shuffleMode
    }
    
    /**
     * Update position manually (for UI components that need current position)
     */
    fun updatePosition() {
        val controller = mediaController
        if (controller != null) {
            _currentPosition.value = controller.currentPosition
        }
    }
    
    /**
     * Start periodic position updates synchronized with service state
     */
    private fun startPositionUpdates() {
        stopPositionUpdates() // Stop any existing job
        
        Log.d(TAG, "Starting position updates synchronized with service")
        positionUpdateJob = coroutineScope.launch {
            while (isActive && _isPlaying.value) {
                val controller = mediaController
                if (controller != null) {
                    val servicePosition = controller.currentPosition
                    _currentPosition.value = servicePosition
                    // Log position sync every 5 seconds to avoid spam
                    if (servicePosition % 5000 < 1000) {
                        Log.d(TAG, "Position synced from service: ${servicePosition}ms")
                    }
                }
                delay(1000L) // Update every second
            }
            Log.d(TAG, "Position updates stopped")
        }
    }
    
    /**
     * Stop periodic position updates
     */
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    /**
     * Release resources and disconnect from service
     */
    fun release() {
        Log.d(TAG, "Releasing MediaControllerRepository")
        
        stopPositionUpdates()
        
        mediaController?.let { controller ->
            // Remove listeners and release controller
            controllerFuture?.let { future ->
                MediaController.releaseFuture(future)
            }
        }
        
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
    }
}