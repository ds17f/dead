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
import com.deadly.v2.core.media.exception.FormatNotAvailableException
import com.deadly.v2.core.model.Track as V2Track
import com.deadly.v2.core.network.archive.service.ArchiveService
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
 * V2 MediaController repository for playback state management
 * 
 * Loads tracks directly from ArchiveService and maintains centralized playback state.
 * All V2 screens observe this repository for consistent playback information.
 */
@Singleton
class MediaControllerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archiveService: ArchiveService
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
     * Loads tracks from ArchiveService and starts playback
     */
    suspend fun playAll(recordingId: String, format: String, startPosition: Long = 0L) {
        Log.d(TAG, "playAll: $recordingId ($format) at position $startPosition")
        
        executeWhenConnected {
            val controller = mediaController
            
            if (controller != null) {
                try {
                    Log.d(TAG, "Loading tracks from ArchiveService for recording: $recordingId")
                    
                    // Get raw V2 Track models from ArchiveService
                    val result = archiveService.getRecordingTracks(recordingId)
                    if (result.isSuccess) {
                        val rawTracks = result.getOrNull() ?: emptyList()
                        Log.d(TAG, "Loaded ${rawTracks.size} raw tracks from ArchiveService")
                        
                        // Filter by format - explicit failure if not found
                        val filteredTracks = rawTracks.filter { track ->
                            track.format.equals(format, ignoreCase = true)
                        }
                        
                        if (filteredTracks.isEmpty()) {
                            // Explicit failure - throw exception with debugging info
                            val availableFormats = rawTracks.map { it.format }.distinct()
                            throw FormatNotAvailableException(
                                recordingId = recordingId,
                                requestedFormat = format,
                                availableFormats = availableFormats
                            )
                        }
                        
                        Log.d(TAG, "Found ${filteredTracks.size} tracks for format: $format")
                        
                        // Convert to MediaItems
                        val mediaItems = convertToMediaItems(recordingId, filteredTracks)
                        
                        // Set media items and start playing at position
                        Log.d(TAG, "Setting ${mediaItems.size} media items to MediaController")
                        controller.setMediaItems(mediaItems, 0, startPosition)
                        controller.prepare()
                        controller.play()
                        
                        Log.d(TAG, "Playback started successfully")
                        
                    } else {
                        Log.e(TAG, "Failed to load tracks: ${result.exceptionOrNull()}")
                    }
                    
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
     * Loads full recording queue, then plays specified track at specified position
     */
    suspend fun playTrack(trackIndex: Int, recordingId: String, format: String, position: Long = 0L) {
        Log.d(TAG, "playTrack: index=$trackIndex, recording=$recordingId ($format) at position $position")
        
        executeWhenConnected {
            val controller = mediaController
            
            if (controller != null) {
                try {
                    Log.d(TAG, "Loading tracks from ArchiveService for playTrack")
                    
                    // Get raw V2 Track models from ArchiveService
                    val result = archiveService.getRecordingTracks(recordingId)
                    if (result.isSuccess) {
                        val rawTracks = result.getOrNull() ?: emptyList()
                        Log.d(TAG, "Loaded ${rawTracks.size} raw tracks from ArchiveService")
                        
                        // Filter by format - explicit failure if not found
                        val filteredTracks = rawTracks.filter { track ->
                            track.format.equals(format, ignoreCase = true)
                        }
                        
                        if (filteredTracks.isEmpty()) {
                            // Explicit failure - throw exception with debugging info
                            val availableFormats = rawTracks.map { it.format }.distinct()
                            throw FormatNotAvailableException(
                                recordingId = recordingId,
                                requestedFormat = format,
                                availableFormats = availableFormats
                            )
                        }
                        
                        Log.d(TAG, "Found ${filteredTracks.size} tracks for format: $format")
                        
                        // Validate track index
                        if (trackIndex >= 0 && trackIndex < filteredTracks.size) {
                            // Convert to MediaItems
                            val mediaItems = convertToMediaItems(recordingId, filteredTracks)
                            
                            // Set media items and play specific track at position
                            controller.setMediaItems(mediaItems, trackIndex, position)
                            controller.prepare()
                            controller.play()
                            
                            Log.d(TAG, "Playing track $trackIndex at position $position successfully")
                        } else {
                            Log.e(TAG, "Invalid track index: $trackIndex (available: 0-${filteredTracks.size - 1})")
                        }
                        
                    } else {
                        Log.e(TAG, "Failed to load tracks: ${result.exceptionOrNull()}")
                    }
                    
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
     * Convert V2 Track models to MediaItems for ExoPlayer
     */
    private fun convertToMediaItems(recordingId: String, tracks: List<V2Track>): List<androidx.media3.common.MediaItem> {
        return tracks.mapIndexed { index, track ->
            // Use track name/filename for URL construction if no direct URL available
            val uri = generateArchiveUrl(recordingId, track)
            Log.d(TAG, "Converting track ${index + 1}: ${track.title ?: track.name} -> $uri")
            
            androidx.media3.common.MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title ?: track.name)
                        .setArtist("Grateful Dead")
                        .setAlbumTitle("Live Recording")
                        .setTrackNumber(track.trackNumber)
                        .build()
                )
                .build()
        }
    }
    
    /**
     * Generate Archive.org URL from V2 Track model
     */
    private fun generateArchiveUrl(recordingId: String, track: V2Track): String {
        // V2 Track should have the filename/name that corresponds to actual Archive.org files
        return "https://archive.org/download/${recordingId}/${track.name}"
    }
    
    // /**
    //  * Extract recording identifier from track name pattern
    //  */
    // private fun extractRecordingIdFromTrack(track: V2Track): String {
    //     // Extract recording ID from track name pattern like "gd77-05-08d1t01.mp3"
    //     val name = track.name
    //     return when {
    //         name.contains("gd") && name.contains("d") -> {
    //             // Pattern: gd77-05-08d1t01.mp3 -> need the full recording identifier
    //             // This is a simplified approach - real implementation may need more logic
    //             "gd77-05-08.sbd.hicks.4982.sbeok.shnf" // Fallback for now
    //         }
    //         else -> "unknown-recording"
    //     }
    // }
    
    /**
     * Parse duration string to milliseconds
     */
    private fun parseDuration(duration: String?): Long? {
        return duration?.let { durationStr ->
            try {
                // Handle MM:SS format
                if (durationStr.contains(":")) {
                    val parts = durationStr.split(":")
                    if (parts.size == 2) {
                        val minutes = parts[0].toIntOrNull() ?: 0
                        val seconds = parts[1].toIntOrNull() ?: 0
                        return (minutes * 60 + seconds) * 1000L
                    }
                }
                // Handle seconds as string
                durationStr.toDoubleOrNull()?.let { (it * 1000).toLong() }
            } catch (e: Exception) {
                null
            }
        }
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