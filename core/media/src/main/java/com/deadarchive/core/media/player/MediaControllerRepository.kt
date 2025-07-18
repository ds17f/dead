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
import com.deadarchive.core.model.CurrentTrackInfo
import com.deadarchive.core.data.repository.ShowRepository
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
    @ApplicationContext private val context: Context,
    private val localFileResolver: LocalFileResolver,
    private val showRepository: ShowRepository
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
    
    private val _currentTrackUrl = MutableStateFlow<String?>(null)
    val currentTrackUrl: StateFlow<String?> = _currentTrackUrl.asStateFlow()
    
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    private val _lastError = MutableStateFlow<PlaybackException?>(null)
    val lastError: StateFlow<PlaybackException?> = _lastError.asStateFlow()
    
    // Connection state for debugging and error handling
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    // Queue context for proper MediaController synchronization
    private var currentQueueUrls: List<String> = emptyList()
    private var currentQueueIndex: Int = 0
    
    // Queue state exposed to UI
    private val _queueUrls = MutableStateFlow<List<String>>(emptyList())
    val queueUrls: StateFlow<List<String>> = _queueUrls.asStateFlow()
    
    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()
    
    // Queue metadata for proper track title display
    private val _queueMetadata = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // URL to Title pairs
    val queueMetadata: StateFlow<List<Pair<String, String>>> = _queueMetadata.asStateFlow()
    
    // Current concert ID for UI access
    private val _currentRecordingId = MutableStateFlow<String?>(null)
    val currentRecordingIdFlow: StateFlow<String?> = _currentRecordingId.asStateFlow()
    
    // Complete current track information
    private val _currentTrackInfo = MutableStateFlow<CurrentTrackInfo?>(null)
    val currentTrackInfo: StateFlow<CurrentTrackInfo?> = _currentTrackInfo.asStateFlow()
    
    fun getCurrentRecordingId(): String? = currentRecordingId
    
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
                
                // Update CurrentTrackInfo with new playing state
                _currentTrackInfo.value?.let { trackInfo ->
                    _currentTrackInfo.value = trackInfo.copy(isPlaying = isPlaying)
                }
                
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
                    
                    // Update CurrentTrackInfo with new duration
                    _currentTrackInfo.value?.let { trackInfo ->
                        _currentTrackInfo.value = trackInfo.copy(duration = duration)
                    }
                    
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
                _currentTrackUrl.value = mediaItem?.mediaId // mediaId is the URL
                
                // Update enriched track info when track changes
                mediaItem?.mediaId?.let { trackUrl ->
                    coroutineScope.launch {
                        updateCurrentTrackInfo(trackUrl)
                    }
                }
                
                Log.d(TAG, "UI StateFlow updated - currentTrack: ${_currentTrack.value?.mediaId}")
                Log.d(TAG, "Current track URL updated: ${_currentTrackUrl.value}")
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
        
        // Sync any existing queue context to the newly connected controller
        if (currentQueueUrls.isNotEmpty()) {
            Log.d(TAG, "Syncing existing queue context to newly connected controller")
            syncQueueToMediaController()
        }
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
        _currentTrackUrl.value = null
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
        _currentTrackUrl.value = controller.currentMediaItem?.mediaId
        
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
     * Update queue context for proper MediaController synchronization
     */
    // Store track metadata for proper display in media controls
    private var queueTrackTitles: List<String> = emptyList()
    private var queueTrackArtists: List<String> = emptyList()
    private var currentRecordingId: String? = null
    
    fun updateQueueContext(queueUrls: List<String>, currentIndex: Int = 0) {
        updateQueueContext(queueUrls, emptyList(), emptyList(), null, currentIndex)
    }
    
    fun updateQueueContext(
        queueUrls: List<String>, 
        trackTitles: List<String> = emptyList(),
        trackArtists: List<String> = emptyList(),
        concertId: String? = null,
        currentIndex: Int = 0
    ) {
        Log.d(TAG, "=== UPDATING QUEUE CONTEXT ===")
        Log.d(TAG, "Queue size: ${queueUrls.size}")
        Log.d(TAG, "Track titles: ${trackTitles.size}")
        Log.d(TAG, "Current index: $currentIndex")
        Log.d(TAG, "Recording ID: $concertId")
        
        currentQueueUrls = queueUrls
        currentQueueIndex = currentIndex
        queueTrackTitles = trackTitles
        queueTrackArtists = trackArtists
        currentRecordingId = concertId
        
        // Update StateFlow for UI
        _queueUrls.value = queueUrls
        _queueIndex.value = currentIndex
        _currentRecordingId.value = concertId
        
        // Update queue metadata for UI display
        val metadata = queueUrls.mapIndexed { index, url ->
            val title = queueTrackTitles.getOrNull(index) ?: url.substringAfterLast("/").substringBeforeLast(".")
            url to title
        }
        _queueMetadata.value = metadata
        
        Log.d(TAG, "=== QUEUE STATE UPDATED ===")
        Log.d(TAG, "StateFlow queueUrls updated: ${queueUrls.size} items")
        Log.d(TAG, "StateFlow queueIndex updated: $currentIndex")
        Log.d(TAG, "StateFlow queueMetadata updated: ${metadata.size} items")
        
        // If we have a queue and controller is connected, sync the queue to MediaController
        if (queueUrls.isNotEmpty() && mediaController != null) {
            syncQueueToMediaController()
        }
    }
    
    /**
     * Sync current queue to MediaController using setMediaItems()
     * Now resolves local files for offline playback
     */
    private fun syncQueueToMediaController() {
        val controller = mediaController ?: return
        
        if (currentQueueUrls.isEmpty()) {
            Log.d(TAG, "No queue to sync to MediaController")
            return
        }
        
        Log.d(TAG, "=== SYNCING QUEUE TO MEDIACONTROLLER ===")
        Log.d(TAG, "Queue size: ${currentQueueUrls.size}")
        Log.d(TAG, "Current index: $currentQueueIndex")
        
        try {
            // Resolve local files for each queue item asynchronously
            coroutineScope.launch {
                val mediaItems = mutableListOf<MediaItem>()
                
                for ((index, url) in currentQueueUrls.withIndex()) {
                    val resolvedUrl = resolvePlaybackUrl(url) ?: url
                    val title = queueTrackTitles.getOrNull(index) ?: url.substringAfterLast("/").substringBeforeLast(".")
                    val filename = url.substringAfterLast("/")
                    val trackNumber = index + 1 // Use queue position as track number
                    
                    // Create enriched MediaItem for better notifications
                    val mediaItem = createEnrichedMediaItem(
                        trackUrl = url,
                        resolvedUrl = resolvedUrl,
                        songTitle = title,
                        trackNumber = trackNumber,
                        filename = filename
                    )
                    
                    mediaItems.add(mediaItem)
                }
                
                Log.d(TAG, "Setting MediaItems with ${mediaItems.size} items (with local file resolution)")
                controller.setMediaItems(mediaItems, currentQueueIndex, 0L)
                controller.prepare()
                
                Log.d(TAG, "Queue synced to MediaController successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing queue to MediaController", e)
        }
    }
    
    /**
     * Play a single track from Archive.org with queue context.
     * Now supports offline playback by checking for local files first.
     */
    fun playTrack(url: String, title: String, artist: String? = null) {
        Log.d(TAG, "=== PLAY TRACK COMMAND ===")
        Log.d(TAG, "Original URL: $url")
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
        
        // Check for local file first (offline playback support)
        coroutineScope.launch {
            val finalUrl = resolvePlaybackUrl(url) ?: url
            playTrackWithResolvedUrl(finalUrl, url, title, artist, controller)
        }
    }
    
    /**
     * Resolve the best URL for playback (local file if available, otherwise streaming URL)
     */
    private suspend fun resolvePlaybackUrl(originalUrl: String): String? {
        return try {
            // Use current recording ID if available for better resolution
            val recordingId = currentRecordingId
            val localFileUri = localFileResolver.resolveLocalFile(originalUrl, recordingId)
            if (localFileUri != null) {
                Log.i(TAG, "🎵 OFFLINE PLAYBACK: Using local file for $originalUrl")
                localFileUri
            } else {
                Log.d(TAG, "📡 STREAMING PLAYBACK: No local file found for $originalUrl")
                null // Will use original URL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local file for $originalUrl", e)
            null // Fallback to streaming
        }
    }
    
    /**
     * Play track with the resolved URL (either local file or streaming URL)
     */
    private fun playTrackWithResolvedUrl(
        finalUrl: String,
        originalUrl: String,
        title: String,
        artist: String?,
        controller: MediaController
    ) {
        Log.d(TAG, "Final URL for playback: $finalUrl")
        Log.d(TAG, "Original URL (for queue matching): $originalUrl")
        
        // Verify controller is in a good state
        Log.d(TAG, "MediaController available, checking state...")
        Log.d(TAG, "Controller playback state: ${controller.playbackState}")
        Log.d(TAG, "Controller isPlaying: ${controller.isPlaying}")
        Log.d(TAG, "Controller available commands: ${controller.availableCommands}")
        
        try {
            // Check if we have queue context and use it for proper media control navigation
            if (currentQueueUrls.isNotEmpty() && originalUrl in currentQueueUrls) {
                // Use queue-based playback for proper forward/back button support
                Log.d(TAG, "Playing track with queue context (${currentQueueUrls.size} items)")
                
                val trackIndex = currentQueueUrls.indexOf(originalUrl)
                if (trackIndex >= 0) {
                    currentQueueIndex = trackIndex
                    _queueIndex.value = currentQueueIndex
                    Log.d(TAG, "Track found at queue index: $trackIndex")
                    
                    // Create MediaItems with metadata for the entire queue
                    // NOTE: We need to resolve local files for each queue item too
                    coroutineScope.launch {
                        val mediaItems = mutableListOf<MediaItem>()
                        
                        for ((index, queueUrl) in currentQueueUrls.withIndex()) {
                            val resolvedUrl = resolvePlaybackUrl(queueUrl) ?: queueUrl
                            val queueFilename = queueUrl.substringAfterLast("/")
                            val queueTrackNumber = index + 1 // Use queue position as track number
                            val queueTitle = queueTrackTitles.getOrNull(index) 
                                ?: com.deadarchive.core.model.Track.extractSongFromFilename(queueFilename)
                            
                            // Create enriched MediaItem for each queue item
                            val mediaItem = createEnrichedMediaItem(
                                trackUrl = queueUrl,
                                resolvedUrl = resolvedUrl,
                                songTitle = if (queueUrl == originalUrl) title else queueTitle,
                                trackNumber = queueTrackNumber,
                                filename = queueFilename
                            )
                            
                            mediaItems.add(mediaItem)
                        }
                        
                        Log.d(TAG, "Setting queue with ${mediaItems.size} items (with local file resolution), starting at index $trackIndex")
                        controller.setMediaItems(mediaItems, trackIndex, 0L)
                        controller.prepare()
                        controller.play()
                        
                        Log.d(TAG, "=== QUEUE-BASED PLAYBACK STARTED ===")
                    }
                    return
                }
            }
            
            // Fallback to single track playback if no queue context
            Log.d(TAG, "Playing single track without queue context")
            val filename = originalUrl.substringAfterLast("/")
            val trackNumber = 1 // Single track playback is always track 1
            
            // Create enriched MediaItem for better notifications
            coroutineScope.launch {
                val mediaItem = createEnrichedMediaItem(
                    trackUrl = originalUrl,
                    resolvedUrl = finalUrl,
                    songTitle = title,
                    trackNumber = trackNumber,
                    filename = filename
                )
                
                Log.d(TAG, "Created MediaItem: ${mediaItem.mediaId} with metadata: ${mediaItem.mediaMetadata.title}")
                Log.d(TAG, "Using URI: ${mediaItem.localConfiguration?.uri}")
                
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
                
                Log.d(TAG, "=== SINGLE TRACK PLAYBACK STARTED ===")
                Log.d(TAG, "Controller state after commands: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
                
                // Log the current MediaItem to verify it was set
                Log.d(TAG, "Current MediaItem after commands: ${controller.currentMediaItem?.mediaId}")
            }
            
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
     * Play a playlist/queue of tracks
     */
    fun playPlaylist(urls: List<String>, startIndex: Int = 0) {
        Log.d(TAG, "=== PLAY PLAYLIST/QUEUE COMMAND ===")
        Log.d(TAG, "Queue size: ${urls.size} tracks")
        Log.d(TAG, "Start index: $startIndex")
        
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "MediaController not connected, cannot play playlist")
            return
        }
        
        // Update our queue context
        currentQueueUrls = urls
        currentQueueIndex = startIndex
        
        // Update StateFlow for UI
        _queueUrls.value = urls
        _queueIndex.value = startIndex
        
        try {
            val mediaItems = urls.map { url ->
                MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(url)
                    .build()
            }
            
            Log.d(TAG, "Setting MediaItems with ${mediaItems.size} items, starting at index $startIndex")
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
            
            Log.d(TAG, "=== PLAYLIST/QUEUE PLAYBACK STARTED ===")
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
     * Skip to next track (queue-aware)
     */
    fun skipToNext() {
        Log.d(TAG, "=== SKIP TO NEXT COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Current queue index: $currentQueueIndex, queue size: ${currentQueueUrls.size}")
            Log.d(TAG, "Controller hasNextMediaItem: ${controller.hasNextMediaItem()}")
            
            if (controller.hasNextMediaItem()) {
                controller.seekToNext()
                // Update our queue index tracking
                if (currentQueueIndex < currentQueueUrls.size - 1) {
                    currentQueueIndex++
                    _queueIndex.value = currentQueueIndex
                    Log.d(TAG, "Updated queue index to: $currentQueueIndex")
                }
            } else {
                Log.w(TAG, "No next item available in queue")
            }
        } else {
            Log.w(TAG, "Cannot skip to next - MediaController not connected")
        }
    }
    
    /**
     * Skip to previous track (queue-aware)
     */
    fun skipToPrevious() {
        Log.d(TAG, "=== SKIP TO PREVIOUS COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Current queue index: $currentQueueIndex, queue size: ${currentQueueUrls.size}")
            Log.d(TAG, "Controller hasPreviousMediaItem: ${controller.hasPreviousMediaItem()}")
            
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPrevious()
                // Update our queue index tracking
                if (currentQueueIndex > 0) {
                    currentQueueIndex--
                    _queueIndex.value = currentQueueIndex
                    Log.d(TAG, "Updated queue index to: $currentQueueIndex")
                }
            } else {
                Log.w(TAG, "No previous item available in queue")
            }
        } else {
            Log.w(TAG, "Cannot skip to previous - MediaController not connected")
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
                    
                    // Update CurrentTrackInfo with new position
                    _currentTrackInfo.value?.let { trackInfo ->
                        _currentTrackInfo.value = trackInfo.copy(position = servicePosition)
                    }
                    
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
     * Create CurrentTrackInfo with enriched metadata from show data
     */
    private suspend fun createCurrentTrackInfo(
        trackUrl: String,
        recordingId: String,
        songTitle: String,
        trackNumber: Int?,
        filename: String
    ): CurrentTrackInfo? {
        return try {
            // Get recording data to find show information
            val recording = showRepository.getRecordingById(recordingId)
            if (recording == null) {
                Log.w(TAG, "Could not find recording data for ID: $recordingId")
                return null
            }
            
            // Create Show model to get proper showId
            val show = com.deadarchive.core.model.Show(
                date = recording.concertDate,
                venue = recording.concertVenue,
                location = recording.concertLocation
            )
            
            CurrentTrackInfo(
                trackUrl = trackUrl,
                recordingId = recordingId,
                showId = show.showId, // Use proper showId from Show model
                showDate = recording.concertDate,
                venue = recording.concertVenue,
                location = recording.concertLocation,
                songTitle = songTitle,
                trackNumber = trackNumber,
                filename = filename,
                isPlaying = _isPlaying.value,
                position = _currentPosition.value,
                duration = _duration.value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating CurrentTrackInfo for recording $recordingId", e)
            null
        }
    }
    
    /**
     * Create MediaItem with enriched metadata for notifications
     */
    private suspend fun createEnrichedMediaItem(
        trackUrl: String,
        resolvedUrl: String,
        songTitle: String,
        trackNumber: Int?,
        filename: String
    ): MediaItem {
        android.util.Log.d("MediaController", "=== createEnrichedMediaItem Debug ===")
        android.util.Log.d("MediaController", "trackUrl: '$trackUrl'")
        android.util.Log.d("MediaController", "songTitle parameter: '$songTitle'")
        android.util.Log.d("MediaController", "filename: '$filename'")
        
        val recordingId = localFileResolver.extractRecordingIdFromUrl(trackUrl)
        
        // Try to get enriched metadata
        val trackInfo = if (recordingId != null) {
            createCurrentTrackInfo(trackUrl, recordingId, songTitle, trackNumber, filename)
        } else null
        
        android.util.Log.d("MediaController", "trackInfo: $trackInfo")
        android.util.Log.d("MediaController", "trackInfo.songTitle: '${trackInfo?.songTitle}'")
        android.util.Log.d("MediaController", "trackInfo.displayTitle: '${trackInfo?.displayTitle}'")
        
        val metadata = if (trackInfo != null) {
            // Use enriched metadata for notifications
            MediaMetadata.Builder()
                .setTitle(trackInfo.displayTitle)        // "Song Title"
                .setArtist(trackInfo.displayArtist)      // "City, State"
                .setAlbumTitle(trackInfo.displaySubtitle) // "Date - Venue"
                .setDisplayTitle(trackInfo.songTitle)    // Just the song name
                .build()
        } else {
            // Fallback to basic metadata
            MediaMetadata.Builder()
                .setTitle(songTitle)
                .setArtist("Grateful Dead")
                .setAlbumTitle("Dead Archive")
                .build()
        }
        
        android.util.Log.d("MediaController", "MediaItem metadata title: '${metadata.title}'")
        android.util.Log.d("MediaController", "MediaItem metadata displayTitle: '${metadata.displayTitle}'")
        
        return MediaItem.Builder()
            .setUri(resolvedUrl)
            .setMediaId(trackUrl)
            .setMediaMetadata(metadata)
            .build()
    }
    
    /**
     * Update the current track info StateFlow when track changes
     */
    private suspend fun updateCurrentTrackInfo(trackUrl: String) {
        val recordingId = localFileResolver.extractRecordingIdFromUrl(trackUrl)
        if (recordingId == null) {
            _currentTrackInfo.value = null
            return
        }
        
        // Extract track info - try multiple sources in order of preference
        val trackFilename = trackUrl.substringAfterLast("/")
        val queueMetadataEntry = queueMetadata.value.find { it.first == trackUrl }
        
        // Try to get track title from recording data if queue metadata is stale
        var songTitle = queueMetadataEntry?.second
        android.util.Log.d("MediaController", "=== songTitle Debug ===")
        android.util.Log.d("MediaController", "trackUrl: '$trackUrl'")
        android.util.Log.d("MediaController", "trackFilename: '$trackFilename'")
        android.util.Log.d("MediaController", "queueMetadataEntry: $queueMetadataEntry")
        android.util.Log.d("MediaController", "songTitle from queue: '$songTitle'")
        
        if (songTitle == null) {
            // Queue metadata is stale or missing, try to get title from recording data
            try {
                val recording = showRepository.getRecordingById(recordingId)
                if (recording != null) {
                    // URL-decode the filename for proper matching with recording data
                    val decodedTrackFilename = java.net.URLDecoder.decode(trackFilename, "UTF-8")
                    android.util.Log.d("MediaController", "Decoded trackFilename: '$decodedTrackFilename'")
                    
                    // Find matching track in recording data
                    val matchingTrack = recording.tracks.find { track ->
                        track.audioFile?.filename == decodedTrackFilename
                    }
                    songTitle = matchingTrack?.displayTitle
                    android.util.Log.d("MediaController", "Found track in recording data: '${matchingTrack?.displayTitle}'")
                    android.util.Log.d("MediaController", "Updated songTitle: '$songTitle'")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get track from recording data", e)
            }
        }
        
        android.util.Log.d("MediaController", "songTitle after recording lookup: '$songTitle'")
        
        // Final fallback to filename parsing
        if (songTitle == null) {
            songTitle = com.deadarchive.core.model.Track.extractSongFromFilename(trackFilename)
            android.util.Log.d("MediaController", "Final fallback songTitle: '$songTitle'")
        }
        
        Log.d(TAG, "=== TRACK TITLE EXTRACTION DEBUG ===")
        Log.d(TAG, "Track URL: $trackUrl")
        Log.d(TAG, "Track filename: $trackFilename")
        Log.d(TAG, "Queue metadata entries: ${queueMetadata.value.size}")
        Log.d(TAG, "Found queue metadata entry: ${queueMetadataEntry?.let { "${it.first} -> ${it.second}" } ?: "NONE"}")
        Log.d(TAG, "Final song title: $songTitle")
        Log.d(TAG, "Parsed from filename would be: ${com.deadarchive.core.model.Track.extractSongFromFilename(trackFilename)}")
        queueMetadata.value.take(3).forEach { entry ->
            Log.d(TAG, "Sample queue entry: ${entry.first} -> ${entry.second}")
        }
        
        // If queue metadata doesn't contain the current track, it's stale and needs to be cleared
        if (queueMetadataEntry == null && queueMetadata.value.isNotEmpty()) {
            Log.w(TAG, "Queue metadata is stale! Current track not found in metadata. Clearing stale metadata.")
            _queueMetadata.value = emptyList()
        }
        
        // Use queue position as track number (1-based) instead of filename parsing
        val trackNumber = currentQueueUrls.indexOf(trackUrl).let { index ->
            if (index >= 0) index + 1 else null
        }
        
        val trackInfo = createCurrentTrackInfo(
            trackUrl = trackUrl,
            recordingId = recordingId,
            songTitle = songTitle,
            trackNumber = trackNumber,
            filename = trackFilename
        )
        
        _currentTrackInfo.value = trackInfo
        
        // Update currentRecordingId to keep navigation consistent
        if (recordingId != _currentRecordingId.value) {
            Log.d(TAG, "Updating currentRecordingId from ${_currentRecordingId.value} to $recordingId")
            _currentRecordingId.value = recordingId
        }
        
        Log.d(TAG, "Updated CurrentTrackInfo: ${trackInfo?.displayTitle ?: "null"}")
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