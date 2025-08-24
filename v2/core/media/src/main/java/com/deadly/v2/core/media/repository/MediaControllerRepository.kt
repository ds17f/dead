package com.deadly.v2.core.media.repository

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.deadly.v2.core.media.service.DeadlyMediaSessionService
import com.deadly.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple MediaController repository for V2 playback
 * 
 * Provides clean interface to MediaSessionService using standard MediaController methods.
 * Uses async connection pattern to avoid blocking main thread.
 */
@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MediaControllerRepository"
    }
    
    // Connection state
    enum class ConnectionState {
        Disconnected, Connecting, Connected, Failed
    }
    
    private var mediaController: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    
    // Coroutine scope for async operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Command queue for operations while connecting
    private val pendingCommands = mutableListOf<suspend () -> Unit>()
    
    // Playback state flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentTrack = MutableStateFlow<MediaMetadata?>(null)
    val currentTrack: StateFlow<MediaMetadata?> = _currentTrack.asStateFlow()
    
    init {
        // Start async connection immediately
        connectToService()
    }
    
    /**
     * Play all tracks for a recording
     * Implements Play All button logic: toggle if same recording, replace if different
     */
    suspend fun playAll(recordingId: String, format: String) {
        Log.d(TAG, "playAll: $recordingId ($format)")
        
        executeWhenConnected {
            val controller = mediaController
            
            if (controller != null) {
                try {
                    Log.d(TAG, "Creating MediaItems for playback")
                    
                    // Create test tracks for this recording
                    val tracks = createTestTracks(recordingId)
                    
                    // Convert tracks to MediaItems
                    val mediaItems = tracks.map { track ->
                        val uri = track.streamingUrl ?: "https://archive.org/download/$recordingId/${track.filename}"
                        Log.d(TAG, "Creating MediaItem: ${track.displayTitle} -> $uri")
                        
                        androidx.media3.common.MediaItem.Builder()
                            .setUri(uri)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(track.displayTitle)
                                    .setArtist("Grateful Dead")
                                    .setAlbumTitle("Live Recording")
                                    .build()
                            )
                            .build()
                    } as List<androidx.media3.common.MediaItem>
                    
                    // Set media items and start playing
                    Log.d(TAG, "Setting ${mediaItems.size} media items to MediaController")
                    controller.setMediaItems(mediaItems)
                    controller.prepare()
                    controller.play()
                    
                    Log.d(TAG, "Playback started successfully")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in playAll", e)
                }
            } else {
                Log.w(TAG, "MediaController is null, cannot play")
            }
        }
    }
    
    /**
     * Play specific track within recording
     * Loads full recording queue if needed, then plays specified track
     */
    suspend fun playTrack(trackIndex: Int, recordingId: String, format: String) {
        Log.d(TAG, "playTrack: index=$trackIndex, recording=$recordingId ($format)")
        
        executeWhenConnected {
            val controller = mediaController
            
            if (controller != null) {
                try {
                    // Create test tracks for this recording
                    val tracks = createTestTracks(recordingId)
                    
                    // Convert tracks to MediaItems
                    val mediaItems = tracks.map { track ->
                        val uri = track.streamingUrl ?: "https://archive.org/download/$recordingId/${track.filename}"
                        
                        androidx.media3.common.MediaItem.Builder()
                            .setUri(uri)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(track.displayTitle)
                                    .setArtist("Grateful Dead")
                                    .setAlbumTitle("Live Recording")
                                    .build()
                            )
                            .build()
                    } as List<androidx.media3.common.MediaItem>
                    
                    // Set media items and play specific track
                    controller.setMediaItems(mediaItems, trackIndex, 0)
                    controller.prepare()
                    controller.play()
                    
                    Log.d(TAG, "Playing track $trackIndex successfully")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in playTrack", e)
                }
            }
        }
    }
    
    /**
     * Simple play/pause toggle
     */
    suspend fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause")
        
        executeWhenConnected {
            val controller = mediaController
            if (controller != null) {
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
        }
    }
    
    /**
     * Async connection to MediaSessionService
     */
    private fun connectToService() {
        if (_connectionState.value == ConnectionState.Connecting) {
            Log.d(TAG, "Connection already in progress")
            return
        }
        
        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "Connecting to MediaSessionService...")
        
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, DeadlyMediaSessionService::class.java)
            )
            
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future
            
            // Add async listener - runs on main executor
            future.addListener({
                try {
                    val controller = future.get()
                    mediaController = controller
                    _connectionState.value = ConnectionState.Connected
                    
                    // Set up player state listeners
                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }
                        
                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            _currentTrack.value = mediaMetadata
                        }
                    })
                    
                    Log.d(TAG, "MediaController connected successfully")
                    
                    // Execute any pending commands
                    repositoryScope.launch {
                        executePendingCommands()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect MediaController", e)
                    _connectionState.value = ConnectionState.Failed
                    mediaController = null
                }
            }, ContextCompat.getMainExecutor(context))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaController connection", e)
            _connectionState.value = ConnectionState.Failed
        }
    }
    
    /**
     * Execute command when connected, or queue if still connecting
     */
    private suspend fun executeWhenConnected(command: suspend () -> Unit) {
        when (_connectionState.value) {
            ConnectionState.Connected -> {
                command()
            }
            ConnectionState.Connecting -> {
                // Queue command for later execution
                synchronized(pendingCommands) {
                    pendingCommands.add(command)
                }
                Log.d(TAG, "Queued command - waiting for connection")
            }
            ConnectionState.Disconnected, ConnectionState.Failed -> {
                Log.d(TAG, "Attempting to reconnect...")
                connectToService()
                // Queue command for execution after connection
                synchronized(pendingCommands) {
                    pendingCommands.add(command)
                }
            }
        }
    }
    
    /**
     * Execute all pending commands after connection is established
     */
    private suspend fun executePendingCommands() {
        val commandsToExecute = synchronized(pendingCommands) {
            val commands = pendingCommands.toList()
            pendingCommands.clear()
            commands
        }
        
        Log.d(TAG, "Executing ${commandsToExecute.size} pending commands")
        commandsToExecute.forEach { command ->
            try {
                command()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing pending command", e)
            }
        }
    }
    
    /**
     * Create test tracks for a recording
     */
    private fun createTestTracks(recordingId: String): List<Track> {
        return listOf(
            Track(
                filename = "gd77-05-08eaton-d3t01.mp3",
                title = "Jack Straw",
                trackNumber = "1", 
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t01.mp3"
            ),
            Track(
                filename = "gd77-05-08eaton-d3t02.mp3",
                title = "Scarlet Begonias", 
                trackNumber = "2",
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t02.mp3"
            ),
            Track(
                filename = "gd77-05-08eaton-d3t03.mp3",
                title = "Fire on the Mountain", 
                trackNumber = "3",
                streamingUrl = "https://archive.org/download/gd77-05-08.sbd.hicks.4982.sbeok.shnf/gd77-05-08eaton-d3t03.mp3"
            )
        )
    }
    
    /**
     * Release resources
     */
    fun release() {
        mediaController?.release()
        mediaController = null
        controllerFuture?.cancel(true)
        repositoryScope.launch { /* scope will be cancelled by job */ }
        _connectionState.value = ConnectionState.Disconnected
    }
}