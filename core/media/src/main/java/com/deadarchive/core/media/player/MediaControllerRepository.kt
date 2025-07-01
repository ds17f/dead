package com.deadarchive.core.media.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
        Log.d(TAG, "Connecting to DeadArchivePlaybackService")
        
        val sessionToken = SessionToken(
            context, 
            ComponentName(context, DeadArchivePlaybackService::class.java)
        )
        
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
        
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                Log.d(TAG, "MediaController connected successfully")
                onControllerConnected()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect MediaController", e)
                onControllerConnectionFailed(e)
            }
        }, MoreExecutors.directExecutor())
    }
    
    /**
     * Called when MediaController successfully connects to service
     */
    private fun onControllerConnected() {
        val controller = mediaController ?: return
        
        _isConnected.value = true
        
        // Set up listener for player state changes
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                _isPlaying.value = isPlaying
                
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: $playbackState")
                _playbackState.value = playbackState
                
                // Update duration when ready
                if (playbackState == Player.STATE_READY) {
                    val duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                    _duration.value = duration
                    Log.d(TAG, "Duration updated: $duration")
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "onMediaItemTransition: ${mediaItem?.mediaId}")
                _currentTrack.value = mediaItem
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                _lastError.value = error
                stopPositionUpdates()
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
        _isConnected.value = false
        _lastError.value = PlaybackException(
            "Failed to connect to media service", 
            error, 
            PlaybackException.ERROR_CODE_REMOTE_ERROR
        )
        
        // Retry connection after delay
        coroutineScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            Log.d(TAG, "Retrying connection to service")
            connectToService()
        }
    }
    
    /**
     * Update StateFlow values from current MediaController state
     */
    private fun updateStateFromController() {
        val controller = mediaController ?: return
        
        _isPlaying.value = controller.isPlaying
        _playbackState.value = controller.playbackState
        _currentPosition.value = controller.currentPosition
        _duration.value = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        _currentTrack.value = controller.currentMediaItem
        
        if (controller.isPlaying) {
            startPositionUpdates()
        }
    }
    
    // Public API methods - identical interface to PlayerRepository
    
    /**
     * Play a single track from Archive.org
     */
    fun playTrack(url: String, title: String, artist: String? = null) {
        Log.d(TAG, "playTrack: URL=$url, title=$title")
        
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "MediaController not connected, cannot play track")
            return
        }
        
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(url)
                .build()
            
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
            
            Log.d(TAG, "Track queued for playback")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
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
        Log.d(TAG, "play() called")
        mediaController?.play() ?: Log.w(TAG, "MediaController not connected")
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        Log.d(TAG, "pause() called")
        mediaController?.pause() ?: Log.w(TAG, "MediaController not connected")
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
     * Start periodic position updates
     */
    private fun startPositionUpdates() {
        stopPositionUpdates() // Stop any existing job
        
        positionUpdateJob = coroutineScope.launch {
            while (isActive && _isPlaying.value) {
                val controller = mediaController
                if (controller != null) {
                    _currentPosition.value = controller.currentPosition
                }
                delay(1000L) // Update every second
            }
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