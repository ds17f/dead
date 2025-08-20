package com.deadly.core.media.player.service

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.deadly.core.data.api.repository.ShowRepository
import com.deadly.core.model.CurrentTrackInfo
import com.deadly.core.model.Recording
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
 * Service responsible for StateFlow synchronization and position updates.
 * Maintains the single source of truth for all playback state coming from the MediaController.
 */
@UnstableApi
@Singleton
class PlaybackStateSync @Inject constructor(
    private val showRepository: ShowRepository
) {
    
    companion object {
        private const val TAG = "PlaybackStateSync"
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    // Current recording for enriched track metadata
    private var currentRecording: Recording? = null
    
    // StateFlow properties - identical interface to MediaControllerRepository
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
    
    private val _currentTrackMediaId = MutableStateFlow<String?>(null)
    val currentTrackMediaId: StateFlow<String?> = _currentTrackMediaId.asStateFlow()
    
    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()
    
    private val _lastError = MutableStateFlow<PlaybackException?>(null)
    val lastError: StateFlow<PlaybackException?> = _lastError.asStateFlow()
    
    private val _currentRecordingId = MutableStateFlow<String?>(null)
    val currentRecordingId: StateFlow<String?> = _currentRecordingId.asStateFlow()
    
    private val _currentTrackInfo = MutableStateFlow<CurrentTrackInfo?>(null)
    val currentTrackInfo: StateFlow<CurrentTrackInfo?> = _currentTrackInfo.asStateFlow()
    
    // Queue state flows
    private val _queueUrls = MutableStateFlow<List<String>>(emptyList())
    val queueUrls: StateFlow<List<String>> = _queueUrls.asStateFlow()
    
    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()
    
    private val _queueMetadata = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val queueMetadata: StateFlow<List<Pair<String, String>>> = _queueMetadata.asStateFlow()
    
    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()
    
    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()
    
    /**
     * Set up MediaController listener to sync state changes
     * This is the single source of truth - all state comes from the service
     */
    fun setupMediaControllerListener(controller: MediaController) {
        Log.d(TAG, "Setting up MediaController state synchronization")
        
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
                    startPositionUpdates(controller)
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
                Log.d(TAG, "Service mediaItem changed: ${mediaItem?.localConfiguration?.uri}")
                Log.d(TAG, "Transition reason: $reason")
                
                _currentTrack.value = mediaItem
                
                mediaItem?.let { item ->
                    val trackUrl = item.localConfiguration?.uri?.toString()
                    val mediaId = item.mediaId
                    
                    Log.d(TAG, "MediaItem details - URL: $trackUrl, MediaId: $mediaId")
                    
                    if (trackUrl != null) {
                        _currentTrackUrl.value = trackUrl
                        _currentTrackMediaId.value = mediaId
                        
                        // Launch coroutine to update track info
                        coroutineScope.launch {
                            updateCurrentTrackInfo(trackUrl)
                        }
                    }
                } ?: run {
                    // MediaItem is null, clear both URL and MediaId
                    _currentTrackUrl.value = null
                    _currentTrackMediaId.value = null
                }
                Log.d(TAG, "UI StateFlow updated - currentTrack: ${_currentTrack.value?.localConfiguration?.uri}")
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "=== SERVICE STATE CHANGE: onPlayerError ===", error)
                Log.e(TAG, "Service error type: ${error.errorCode}")
                Log.e(TAG, "Service error message: ${error.message}")
                _lastError.value = error
                Log.d(TAG, "UI StateFlow updated - lastError: ${_lastError.value?.message}")
            }
        }
        
        controller.addListener(listener)
        
        // Initial state sync
        updateStateFromController(controller)
        
        Log.d(TAG, "MediaController listener setup complete")
    }
    
    /**
     * Manually update all state from the current MediaController state
     * Used for initial synchronization and manual refresh
     */
    fun updateStateFromController(controller: MediaController) {
        Log.d(TAG, "=== MANUAL STATE SYNC FROM SERVICE ===")
        
        val isPlaying = controller.isPlaying
        val playbackState = controller.playbackState
        val currentTrack = controller.currentMediaItem
        val duration = controller.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        val position = controller.currentPosition
        
        Log.d(TAG, "Service state - isPlaying: $isPlaying, state: $playbackState, duration: $duration, position: $position")
        Log.d(TAG, "Service current track: ${currentTrack?.localConfiguration?.uri}")
        
        // Update all state flows
        _isPlaying.value = isPlaying
        _playbackState.value = playbackState
        _currentTrack.value = currentTrack
        _duration.value = duration
        _currentPosition.value = position
        
        // Update track URL, MediaId and info
        currentTrack?.let { item ->
            val trackUrl = item.localConfiguration?.uri?.toString()
            val mediaId = item.mediaId
            
            Log.d(TAG, "Manual sync - URL: $trackUrl, MediaId: $mediaId")
            
            if (trackUrl != null) {
                _currentTrackUrl.value = trackUrl
                _currentTrackMediaId.value = mediaId
                
                // Launch coroutine to update track info
                coroutineScope.launch {
                    updateCurrentTrackInfo(trackUrl)
                }
            }
        } ?: run {
            // No current track, clear both URL and MediaId
            _currentTrackUrl.value = null
            _currentTrackMediaId.value = null
        }
        
        // Start position updates if playing
        if (isPlaying) {
            startPositionUpdates(controller)
        }
        
        Log.d(TAG, "Manual state sync complete")
    }
    
    /**
     * Start periodic position updates synchronized with service
     */
    private fun startPositionUpdates(controller: MediaController) {
        stopPositionUpdates() // Stop any existing job
        
        Log.d(TAG, "Starting position updates synchronized with service")
        positionUpdateJob = coroutineScope.launch {
            while (isActive && _isPlaying.value) {
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
     * Update queue state flows
     */
    fun updateQueueState(urls: List<String>, index: Int, metadata: List<Pair<String, String>>) {
        updateQueueState(urls, index, metadata, null)
    }
    
    /**
     * Update queue state flows with Recording data for enriched metadata
     */
    fun updateQueueState(urls: List<String>, index: Int, metadata: List<Pair<String, String>>, recording: Recording?) {
        _queueUrls.value = urls
        _queueIndex.value = index
        _queueMetadata.value = metadata
        _hasNext.value = index < urls.size - 1
        _hasPrevious.value = index > 0
        
        // Store recording for enriched track info
        this.currentRecording = recording
        
        Log.d(TAG, "Queue state updated - ${urls.size} tracks, index: $index, recording: ${recording?.identifier}")
    }
    
    /**
     * Update CurrentTrackInfo with enriched metadata using Recording data
     */
    private suspend fun updateCurrentTrackInfo(trackUrl: String) {
        try {
            Log.d(TAG, "Updating CurrentTrackInfo for track: $trackUrl")
            
            val recording = currentRecording
            if (recording == null) {
                Log.w(TAG, "No recording data available for track: $trackUrl")
                // Fallback to URL parsing for backward compatibility
                updateCurrentTrackInfoFromUrl(trackUrl)
                return
            }
            
            // Find the track in the recording by matching URL (handle both streaming and downloaded)
            val track = recording.tracks.find { track ->
                val downloadUrl = track.audioFile?.downloadUrl
                if (downloadUrl != null) {
                    // For downloaded tracks, trackUrl is file:// but we need to match against original downloadUrl
                    downloadUrl == trackUrl || trackUrl.endsWith(track.filename)
                } else {
                    false
                }
            }
            
            if (track == null) {
                Log.w(TAG, "Track not found in recording for URL: $trackUrl")
                // Fallback to URL parsing
                updateCurrentTrackInfoFromUrl(trackUrl)
                return
            }
            
            Log.d(TAG, "Found track in recording: ${track.displayTitle}")
            
            // Use rich Recording and Track data
            _currentRecordingId.value = recording.identifier
            
            val trackInfo = CurrentTrackInfo(
                trackUrl = trackUrl,
                recordingId = recording.identifier,
                showId = recording.identifier, // Use recording ID as show ID for now
                showDate = recording.concertDate,
                venue = recording.concertVenue,
                location = recording.concertLocation,
                songTitle = track.displayTitle,
                trackNumber = track.trackNumber?.toIntOrNull(),
                filename = track.audioFile?.filename ?: trackUrl.substringAfterLast("/"),
                isPlaying = _isPlaying.value,
                position = _currentPosition.value,
                duration = _duration.value
            )
            
            _currentTrackInfo.value = trackInfo
            
            Log.d(TAG, "CurrentTrackInfo updated successfully with rich metadata")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating CurrentTrackInfo", e)
        }
    }
    
    /**
     * Fallback method: Update CurrentTrackInfo using URL parsing (legacy)
     */
    private suspend fun updateCurrentTrackInfoFromUrl(trackUrl: String) {
        try {
            Log.d(TAG, "Falling back to URL parsing for track: $trackUrl")
            
            // Extract recording ID from URL
            val recordingId = extractRecordingIdFromUrl(trackUrl)
            if (recordingId == null) {
                Log.w(TAG, "Could not extract recording ID from URL: $trackUrl")
                return
            }
            _currentRecordingId.value = recordingId
            
            // Parse filename and track info from URL
            val filename = trackUrl.substringAfterLast("/")
            val songTitle = extractSongTitleFromFilename(filename)
            val trackNumber = extractTrackNumberFromFilename(filename)
            
            // Create basic CurrentTrackInfo
            val trackInfo = createCurrentTrackInfo(trackUrl, recordingId, songTitle, trackNumber, filename)
            _currentTrackInfo.value = trackInfo
            
            Log.d(TAG, "CurrentTrackInfo updated successfully via URL parsing")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating CurrentTrackInfo from URL", e)
        }
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
            val show = com.deadly.core.model.Show(
                date = recording.concertDate,
                venue = recording.concertVenue,
                location = recording.concertLocation
            )
            
            CurrentTrackInfo(
                trackUrl = trackUrl,
                songTitle = songTitle,
                showDate = recording.concertDate ?: "Unknown Date",
                venue = recording.concertVenue ?: "Unknown Venue",
                location = recording.concertLocation,
                recordingId = recordingId,
                showId = show.showId,
                trackNumber = trackNumber,
                filename = filename,
                position = _currentPosition.value,
                duration = _duration.value,
                isPlaying = _isPlaying.value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating CurrentTrackInfo", e)
            null
        }
    }
    
    /**
     * Extract recording ID from streaming URL
     */
    private fun extractRecordingIdFromUrl(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            val pathSegments = uri.pathSegments
            
            // Handle Archive.org URLs: https://ia800805.us.archive.org/9/items/{recordingId}/...
            if (pathSegments.size >= 3 && pathSegments[1] == "items") {
                pathSegments[2] // The recording identifier
            }
            // Handle direct download URLs: /download/{recordingId}/...
            else if (pathSegments.size >= 2 && pathSegments[0] == "download") {
                pathSegments[1] // The recording identifier
            } else {
                Log.w(TAG, "Unrecognized URL format: $url")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recording ID from URL: $url", e)
            null
        }
    }
    
    /**
     * Extract song title from filename
     */
    private fun extractSongTitleFromFilename(filename: String): String {
        return try {
            val baseName = filename.substringBeforeLast(".")
            // Remove track number prefix if present (e.g., "01 - Song Name" -> "Song Name")
            val titlePart = baseName.replace(Regex("^\\d+\\s*[-.]\\s*"), "")
            titlePart.ifEmpty { baseName }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting song title from filename: $filename", e)
            filename
        }
    }
    
    /**
     * Extract track number from filename
     */
    private fun extractTrackNumberFromFilename(filename: String): Int? {
        return try {
            val baseName = filename.substringBeforeLast(".")
            val trackNumberMatch = Regex("^(\\d+)").find(baseName)
            trackNumberMatch?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting track number from filename: $filename", e)
            null
        }
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        Log.d(TAG, "Releasing PlaybackStateSync resources")
        stopPositionUpdates()
    }
}