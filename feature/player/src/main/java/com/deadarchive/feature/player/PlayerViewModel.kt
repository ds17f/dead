package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.media.player.MediaControllerRepository
import com.deadarchive.core.media.player.PlaybackEventTracker
import com.deadarchive.core.media.player.QueueManager
import com.deadarchive.core.media.player.QueueStateManager
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.network.mapper.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val mediaControllerRepository: MediaControllerRepository,
    private val queueManager: QueueManager,
    val queueStateManager: QueueStateManager,
    val playbackEventTracker: PlaybackEventTracker,
    private val showRepository: ShowRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: com.deadarchive.core.settings.data.SettingsRepository,
    private val ratingsRepository: com.deadarchive.core.data.repository.RatingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _currentRecording = MutableStateFlow<Recording?>(null)
    val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()
    
    // Download state tracking
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    // Track-level download states
    private val _trackDownloadStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val trackDownloadStates: StateFlow<Map<String, Boolean>> = _trackDownloadStates.asStateFlow()
    
    // Settings flow for debug panel access
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.deadarchive.core.settings.model.AppSettings()
        )
    
    // Navigation callbacks for show navigation with showId parameter
    var onNavigateToShow: ((showId: String, recordingId: String) -> Unit)? = null
    
    // Navigation state from QueueStateManager
    val hasNext = queueStateManager.hasNext.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val hasPrevious = queueStateManager.hasPrevious.stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    // Queue index sync for track highlighting
    init {
        viewModelScope.launch {
            queueStateManager.queueIndex.collect { queueIndex ->
                // Only sync track index if we're viewing the same recording that's in the queue
                val currentUrl = mediaControllerRepository.currentTrackUrl.value
                val currentRecording = _currentRecording.value
                
                if (currentUrl != null && currentRecording != null) {
                    val trackIndex = currentRecording.tracks.indexOfFirst { track ->
                        track.audioFile?.downloadUrl == currentUrl
                    }
                    
                    if (trackIndex >= 0 && trackIndex != _uiState.value.currentTrackIndex) {
                        Log.d(TAG, "Queue index changed ($queueIndex), syncing track index to $trackIndex")
                        Log.d(TAG, "  Recording: ${currentRecording.identifier}")
                        Log.d(TAG, "  Track: ${currentRecording.tracks[trackIndex].displayTitle}")
                        _uiState.value = _uiState.value.copy(currentTrackIndex = trackIndex)
                    }
                }
            }
        }
    }
    
    // Playlist management state - now derived from QueueManager
    val currentPlaylist: StateFlow<List<PlaylistItem>> = queueManager.getCurrentQueue()
        .map { queueItems: List<com.deadarchive.core.media.player.QueueItem> ->
            Log.d(TAG, "=== PLAYLIST FROM QUEUE MANAGER ===")
            Log.d(TAG, "queueItems.size: ${queueItems.size}")
            
            queueItems.mapIndexed { index: Int, queueItem: com.deadarchive.core.media.player.QueueItem ->
                val recording = _currentRecording.value
                val concertId = recording?.identifier ?: "unknown"
                
                // Create a Track object from QueueItem
                val track = Track(
                    filename = queueItem.mediaId.substringAfterLast("/"),
                    title = queueItem.title,
                    trackNumber = (index + 1).toString(),
                    durationSeconds = "0",
                    audioFile = AudioFile(
                        filename = queueItem.mediaId.substringAfterLast("/"),
                        format = queueItem.mediaId.substringAfterLast(".").uppercase(),
                        downloadUrl = queueItem.mediaId,
                        sizeBytes = "0"
                    )
                )
                
                PlaylistItem(
                    concertIdentifier = concertId,
                    track = track,
                    position = index
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _playlistTitle = MutableStateFlow<String?>(null)
    val playlistTitle: StateFlow<String?> = _playlistTitle.asStateFlow()
    
    // Navigation loading state
    private val _isNavigationLoading = MutableStateFlow(false)
    val isNavigationLoading: StateFlow<Boolean> = _isNavigationLoading.asStateFlow()
    
    init {
        Log.d(TAG, "PlayerViewModel: Initializing")
        try {
            // Observe player state changes
            viewModelScope.launch {
                Log.d(TAG, "PlayerViewModel: Setting up player state observation")
                combine(
                    mediaControllerRepository.isPlaying,
                    mediaControllerRepository.currentPosition,
                    mediaControllerRepository.duration,
                    mediaControllerRepository.playbackState,
                    mediaControllerRepository.currentTrackUrl
                ) { isPlaying, position, duration, state, currentTrackUrl ->
                    var updatedState = _uiState.value.copy(
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,
                        playbackState = state
                    )
                    
                    // Sync track index based on service's current track URL
                    currentTrackUrl?.let { url ->
                        val trackIndex = _uiState.value.tracks.indexOfFirst { track ->
                            track.audioFile?.downloadUrl == url
                        }
                        if (trackIndex >= 0 && trackIndex != updatedState.currentTrackIndex) {
                            Log.d(TAG, "Service track changed, syncing UI track index from ${updatedState.currentTrackIndex} to $trackIndex")
                            Log.d(TAG, "  Current URL: $url")
                            Log.d(TAG, "  Matched track: ${_uiState.value.tracks.getOrNull(trackIndex)?.displayTitle}")
                            Log.d(TAG, "  Recording: ${_currentRecording.value?.identifier}")
                            updatedState = updatedState.copy(currentTrackIndex = trackIndex)
                        } else if (trackIndex < 0) {
                            Log.d(TAG, "Track not found in current recording tracks - URL: $url")
                            Log.d(TAG, "  Current recording: ${_currentRecording.value?.identifier}")
                            Log.d(TAG, "  Available tracks: ${_uiState.value.tracks.size}")
                            // If track not found in current recording, keep current index
                        }
                    }
                    
                    updatedState
                }.collect { updatedState ->
                    _uiState.value = updatedState
                    Log.d(TAG, "PlayerViewModel: State updated - isPlaying: ${updatedState.isPlaying}, position: ${updatedState.currentPosition}, duration: ${updatedState.duration}, playbackState: ${updatedState.playbackState}, trackIndex: ${updatedState.currentTrackIndex}")
                }
            }
            
            // Start monitoring download states
            startDownloadStateMonitoring()
        } catch (e: Exception) {
            Log.e(TAG, "PlayerViewModel: Exception in init", e)
        }
    }
    
    fun loadRecording(recordingId: String) {
        Log.d(TAG, "loadRecording: Starting to load recording with ID: $recordingId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get user's audio format preferences
                val settings = settingsRepository.getSettings().firstOrNull()
                val formatPreferences = settings?.audioFormatPreference ?: com.deadarchive.core.model.AppConstants.PREFERRED_AUDIO_FORMATS
                
                Log.d(TAG, "loadRecording: Using format preferences: $formatPreferences")
                
                // Load recording with format filtering applied
                Log.d(TAG, "loadRecording: About to call getRecordingByIdWithFormatFilter with ID: '$recordingId'")
                val recording = showRepository.getRecordingByIdWithFormatFilter(recordingId, formatPreferences)
                
                Log.d(TAG, "loadRecording: Repository returned recording: ${recording != null}")
                if (recording != null) {
                    Log.d(TAG, "loadRecording: Found recording: ${recording.identifier} - ${recording.title}")
                    Log.d(TAG, "loadRecording: Recording found - title: ${recording.title}, filtered tracks count: ${recording.tracks.size}")
                    recording.tracks.forEachIndexed { index, track ->
                        Log.d(TAG, "loadRecording: Track $index - title: ${track.displayTitle}, format: ${track.audioFile?.format}")
                    }
                    
                    _currentRecording.value = recording
                    
                    // Create playlist from filtered recording tracks
                    val playlist = recording.tracks.mapIndexed { index, track ->
                        PlaylistItem(
                            concertIdentifier = recording.identifier,
                            track = track,
                            position = index
                        )
                    }
                    _playlistTitle.value = recording.displayTitle
                    
                    // Don't update the queue when loading a recording - the queue should only change
                    // when explicitly playing a track or show. This prevents browsing from affecting playback.
                    Log.d(TAG, "loadRecording: Not updating queue - queue only updates on explicit play actions")
                    
                    // Sync track index based on MediaController's current track URL to maintain continuity
                    val currentTrackUrl = mediaControllerRepository.currentTrackUrl.value
                    val syncedTrackIndex = if (currentTrackUrl != null) {
                        val trackIndex = recording.tracks.indexOfFirst { track ->
                            track.audioFile?.downloadUrl == currentTrackUrl
                        }
                        if (trackIndex >= 0) {
                            Log.d(TAG, "loadRecording: Syncing to currently playing track at index $trackIndex")
                            Log.d(TAG, "  Current URL: $currentTrackUrl")
                            Log.d(TAG, "  Matched track: ${recording.tracks[trackIndex].displayTitle}")
                            trackIndex
                        } else {
                            Log.d(TAG, "loadRecording: Current playing track not found in this recording, starting from beginning")
                            Log.d(TAG, "  Current URL: $currentTrackUrl")
                            Log.d(TAG, "  Recording: ${recording.identifier}")
                            0
                        }
                    } else {
                        Log.d(TAG, "loadRecording: No track currently playing, starting from beginning")
                        0
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tracks = recording.tracks,
                        currentTrackIndex = syncedTrackIndex,
                        isPlaylistMode = true,
                        playlistSize = playlist.size
                    )
                    
                    // Note: Removed auto-play behavior - recordings should only play when explicitly requested
                    Log.d(TAG, "loadRecording: Recording loaded successfully, ${recording.tracks.size} filtered tracks available")
                } else {
                    Log.w(TAG, "loadRecording: Recording not found for ID: $recordingId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Recording not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadRecording: Exception loading recording", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load recording: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Start playing the recording from the beginning (first track)
     * This clears the current queue and starts fresh with the current recording
     */
    fun playRecordingFromBeginning() {
        Log.d(TAG, "playRecordingFromBeginning: Starting recording from first track")
        val currentRecording = _currentRecording.value
        
        if (currentRecording != null) {
            Log.d(TAG, "playRecordingFromBeginning: Loading show into queue: ${currentRecording.title} with ${currentRecording.tracks.size} tracks")
            
            viewModelScope.launch {
                // Use QueueManager to load the entire show into the queue and start playback from beginning
                queueManager.loadShow(currentRecording, 0)
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    currentTrackIndex = 0,
                    tracks = currentRecording.tracks,
                    isPlaylistMode = true,
                    playlistSize = currentRecording.tracks.size
                )
            }
        } else {
            Log.w(TAG, "playRecordingFromBeginning: No recording available to play")
        }
    }
    
    /**
     * Update MediaControllerRepository with current queue context
     * The playlist should already contain filtered tracks (one format per song)
     * @param forceUpdate If true, updates queue even when music is currently playing
     */
    // Legacy method - to be removed after completing QueueManager migration
    private fun updateQueueContext(playlist: List<PlaylistItem>, forceUpdate: Boolean = false) {
        Log.d(TAG, "updateQueueContext: Legacy method called - will be removed after QueueManager migration")
        // For now, do nothing - queue operations should go through QueueManager
    }
    
    fun playTrack(trackIndex: Int) {
        Log.d(TAG, "playTrack: Attempting to play track at index $trackIndex")
        val tracks = _uiState.value.tracks
        Log.d(TAG, "playTrack: Total tracks available: ${tracks.size}")
        
        if (trackIndex in tracks.indices) {
            val track = tracks[trackIndex]
            Log.d(TAG, "playTrack: Selected track - title: ${track.displayTitle}, filename: ${track.filename}")
            
            val recording = _currentRecording.value
            
            if (recording != null) {
                Log.d(TAG, "playTrack: Playing track from recording: ${recording.title}")
                
                viewModelScope.launch {
                    try {
                        // Use QueueManager to play the track (which will load the entire show if needed)
                        queueManager.playTrack(track, recording)
                        
                        _uiState.value = _uiState.value.copy(
                            currentTrackIndex = trackIndex,
                            error = null
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "playTrack: Exception calling queueManager.playTrack", e)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to play track: ${e.localizedMessage}"
                        )
                    }
                }
            } else {
                Log.w(TAG, "playTrack: No recording available for context")
                _uiState.value = _uiState.value.copy(
                    error = "Recording context not available"
                )
            }
        } else {
            Log.e(TAG, "playTrack: Invalid track index $trackIndex for ${tracks.size} tracks")
        }
    }
    
    fun playPause() {
        Log.d(TAG, "playPause: isPlaying=${_uiState.value.isPlaying}, currentTrack=${_uiState.value.currentTrack?.displayTitle}")
        
        if (_uiState.value.isPlaying) {
            Log.d(TAG, "playPause: Pausing playback")
            mediaControllerRepository.pause()
        } else {
            // If no track is currently loaded, start the first track
            val currentTrack = _uiState.value.currentTrack
            if (currentTrack == null && _uiState.value.tracks.isNotEmpty()) {
                Log.d(TAG, "playPause: No current track, starting first track")
                playTrack(0)
            } else {
                Log.d(TAG, "playPause: Resuming playback")
                mediaControllerRepository.play()
            }
        }
    }
    
    fun skipToNext() {
        Log.d(TAG, "skipToNext: Skipping to next track using QueueManager")
        queueManager.skipToNext()
    }
    
    fun skipToPrevious() {
        Log.d(TAG, "skipToPrevious: Skipping to previous track using QueueManager")
        queueManager.skipToPrevious()
    }
    
    fun seekTo(position: Long) {
        mediaControllerRepository.seekTo(position)
    }
    
    fun updatePosition() {
        mediaControllerRepository.updatePosition()
    }
    
    // Playlist management methods
    
    /**
     * Set a custom playlist for playback
     * Note: The playlist should already have format filtering applied before calling this method
     * @param playlist List of PlaylistItem to set as current playlist
     * @param title Optional title for the playlist
     */
    fun setPlaylist(playlist: List<PlaylistItem>, title: String? = null) {
        Log.d(TAG, "setPlaylist: Setting playlist with ${playlist.size} items, title: $title")
        _playlistTitle.value = title
        
        // Log formats to verify filtering was applied
        playlist.take(3).forEach { item ->
            Log.d(TAG, "setPlaylist: Item - ${item.track.displayTitle} (${item.track.audioFile?.format})")
        }
        
        // TODO: Queue context update via QueueManager not yet implemented
        
        // Update UI state with tracks from playlist
        val tracks = playlist.map { it.track }
        _uiState.value = _uiState.value.copy(
            tracks = tracks,
            currentTrackIndex = 0,
            isPlaylistMode = playlist.isNotEmpty(),
            playlistSize = playlist.size
        )
    }
    
    /**
     * Get the current playlist
     * @return Current playlist items
     */
    fun getCurrentPlaylist(): List<PlaylistItem> {
        return currentPlaylist.value
    }
    
    /**
     * Navigate to a specific track in the current playlist
     * @param playlistIndex Index of the track in the playlist to navigate to
     */
    fun navigateToTrack(playlistIndex: Int) {
        Log.d(TAG, "navigateToTrack: Navigating to playlist index $playlistIndex")
        
        // Use QueueManager to skip to the specified index
        queueManager.skipToIndex(playlistIndex)
        
        // Update UI state to reflect the change
        _uiState.value = _uiState.value.copy(currentTrackIndex = playlistIndex)
    }
    
    /**
     * Add a track to the current playlist
     * @param playlistItem PlaylistItem to add to the playlist
     */
    fun addToPlaylist(playlistItem: PlaylistItem) {
        Log.d(TAG, "addToPlaylist: Adding track ${playlistItem.track.displayTitle}")
        val currentPlaylist = currentPlaylist.value.toMutableList()
        val newItem = playlistItem.copy(position = currentPlaylist.size)
        currentPlaylist.add(newItem)
        
        // TODO: Queue modification via QueueManager not yet implemented
        
        // Update UI state tracks
        val tracks = currentPlaylist.map { it.track }
        _uiState.value = _uiState.value.copy(
            tracks = tracks,
            isPlaylistMode = currentPlaylist.isNotEmpty(),
            playlistSize = currentPlaylist.size
        )
    }
    
    /**
     * Remove a track from the current playlist
     * @param playlistIndex Index of the track to remove from the playlist
     */
    fun removeFromPlaylist(playlistIndex: Int) {
        Log.d(TAG, "removeFromPlaylist: Removing track at index $playlistIndex")
        val currentPlaylist = currentPlaylist.value.toMutableList()
        
        if (playlistIndex in currentPlaylist.indices) {
            currentPlaylist.removeAt(playlistIndex)
            
            // Update positions for remaining items
            currentPlaylist.forEachIndexed { index, item ->
                currentPlaylist[index] = item.copy(position = index)
            }
            
            // TODO: Queue modification via QueueManager not yet implemented
            
            // Update UI state tracks
            val tracks = currentPlaylist.map { it.track }
            val currentIndex = _uiState.value.currentTrackIndex
            val newCurrentIndex = when {
                playlistIndex < currentIndex -> currentIndex - 1
                playlistIndex == currentIndex && currentIndex >= tracks.size -> maxOf(0, tracks.size - 1)
                else -> currentIndex
            }
            
            _uiState.value = _uiState.value.copy(
                tracks = tracks,
                currentTrackIndex = newCurrentIndex,
                isPlaylistMode = tracks.isNotEmpty(),
                playlistSize = tracks.size
            )
        } else {
            Log.e(TAG, "removeFromPlaylist: Invalid playlist index $playlistIndex for ${currentPlaylist.size} items")
        }
    }
    
    /**
     * Clear the current playlist
     */
    fun clearPlaylist() {
        Log.d(TAG, "clearPlaylist: Clearing current playlist")
        _playlistTitle.value = null
        
        // Use QueueManager to clear the queue
        queueManager.clearQueue()
        
        _uiState.value = _uiState.value.copy(
            tracks = emptyList(),
            currentTrackIndex = 0
        )
    }
    
    /**
     * Play a track from the current playlist
     * @param playlistItem PlaylistItem to play
     */
    private fun playTrackFromPlaylist(playlistItem: PlaylistItem) {
        val track = playlistItem.track
        Log.d(TAG, "playTrackFromPlaylist: Playing track ${track.displayTitle}")
        
        val audioFile = track.audioFile
        val downloadUrl = audioFile?.downloadUrl
        
        if (downloadUrl != null) {
            Log.d(TAG, "playTrackFromPlaylist: Playing track with URL: $downloadUrl")
            try {
                mediaControllerRepository.playTrack(
                    url = downloadUrl,
                    title = track.displayTitle,
                    artist = _playlistTitle.value ?: _currentRecording.value?.title
                )
                
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                Log.e(TAG, "playTrackFromPlaylist: Exception calling mediaControllerRepository.playTrack", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to play track: ${e.localizedMessage}"
                )
            }
        } else {
            Log.w(TAG, "playTrackFromPlaylist: No download URL available for track ${track.displayTitle}")
            _uiState.value = _uiState.value.copy(
                error = "Audio file not available for this track"
            )
        }
    }
    
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: PlayerViewModel cleared")
        // Don't release MediaControllerRepository - it should persist for background playback
        // The service should continue running independently of UI lifecycle
    }
    
    /**
     * Start monitoring download states for recording and tracks
     */
    private fun startDownloadStateMonitoring() {
        viewModelScope.launch {
            // Monitor all downloads and update states
            downloadRepository.getAllDownloads().collect { downloads ->
                val stateMap = mutableMapOf<String, ShowDownloadState>()
                val trackStatesMap = mutableMapOf<String, Boolean>()
                
                // Group downloads by recording ID
                val downloadsByRecording = downloads.groupBy { it.recordingId }
                
                downloadsByRecording.forEach { (recordingId, recordingDownloads) ->
                    val showDownloadState = when {
                        // If any download is marked for deletion, treat as not downloaded
                        recordingDownloads.any { it.isMarkedForDeletion } -> {
                            ShowDownloadState.NotDownloaded
                        }
                        // Handle failed downloads separately (show as failed)
                        recordingDownloads.any { it.status == DownloadStatus.FAILED } -> {
                            val failedTrack = recordingDownloads.first { it.status == DownloadStatus.FAILED }
                            ShowDownloadState.Failed(failedTrack.errorMessage)
                        }
                        // Filter out cancelled and failed downloads for status determination
                        else -> recordingDownloads.filter { it.status !in listOf(DownloadStatus.CANCELLED, DownloadStatus.FAILED) }.let { activeDownloads ->
                            when {
                                activeDownloads.all { it.status == DownloadStatus.COMPLETED } && activeDownloads.isNotEmpty() -> {
                                    ShowDownloadState.Downloaded
                                }
                                activeDownloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED } -> {
                                    // Calculate track-based progress (Spotify-style immediate feedback)
                                    val totalTracks = activeDownloads.size
                                    val completedTracks = activeDownloads.count { it.status == DownloadStatus.COMPLETED }
                                    
                                    // Get byte progress from actively downloading track if any
                                    val downloadingTrack = activeDownloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                                    val byteProgress = downloadingTrack?.progress ?: -1f
                                    val bytesDownloaded = downloadingTrack?.bytesDownloaded ?: 0L
                                    
                                    ShowDownloadState.Downloading(
                                        progress = byteProgress,
                                        bytesDownloaded = bytesDownloaded,
                                        completedTracks = completedTracks,
                                        totalTracks = totalTracks
                                    )
                                }
                                else -> {
                                    ShowDownloadState.NotDownloaded
                                }
                            }
                        }
                    }
                    
                    stateMap[recordingId] = showDownloadState
                    
                    // Update individual track download states
                    recordingDownloads.forEach { download ->
                        val trackKey = "${recordingId}_${download.trackFilename}"
                        trackStatesMap[trackKey] = download.status == DownloadStatus.COMPLETED && !download.isMarkedForDeletion
                    }
                }
                
                _downloadStates.value = stateMap
                _trackDownloadStates.value = trackStatesMap
            }
        }
    }
    
    /**
     * Start downloading the current recording
     */
    fun downloadRecording() {
        viewModelScope.launch {
            try {
                val recording = _currentRecording.value
                if (recording != null) {
                    // Provide immediate UI feedback
                    val currentDownloadStates = _downloadStates.value.toMutableMap()
                    currentDownloadStates[recording.identifier] = ShowDownloadState.Downloading(
                        progress = -1f, // -1 indicates "queued/starting"
                        bytesDownloaded = 0L,
                        completedTracks = 0,
                        totalTracks = 1 // Placeholder until actual track count is known
                    )
                    _downloadStates.value = currentDownloadStates
                    
                    // Start the actual download
                    downloadRepository.downloadRecording(recording)
                    
                    println("Downloading recording: ${recording.identifier}")
                } else {
                    println("No recording available to download")
                }
            } catch (e: Exception) {
                println("Failed to start download: ${e.message}")
                
                // On error, revert the optimistic UI state
                val recording = _currentRecording.value
                if (recording != null) {
                    val currentDownloadStates = _downloadStates.value.toMutableMap()
                    currentDownloadStates[recording.identifier] = ShowDownloadState.Failed("Failed to start download")
                    _downloadStates.value = currentDownloadStates
                }
            }
        }
    }
    
    /**
     * Cancel downloads for the current recording
     */
    fun cancelRecordingDownloads() {
        viewModelScope.launch {
            try {
                val recording = _currentRecording.value
                if (recording != null) {
                    downloadRepository.cancelRecordingDownloads(recording.identifier)
                } else {
                    println("No recording found to cancel downloads")
                }
            } catch (e: Exception) {
                println("Failed to cancel downloads: ${e.message}")
            }
        }
    }
    
    /**
     * Add/remove the current show from library
     */
    fun toggleLibrary() {
        viewModelScope.launch {
            try {
                val recording = _currentRecording.value
                if (recording != null) {
                    // TODO: Implement library toggle for recordings
                    // Need to implement a way to get Show from Recording
                    // or add recording-specific library functionality
                    println("Library toggle for recording not yet implemented: ${recording.identifier}")
                }
            } catch (e: Exception) {
                println("Failed to toggle library: ${e.message}")
            }
        }
    }
    
    /**
     * Get the current download state for the recording
     */
    fun getRecordingDownloadState(): ShowDownloadState {
        return try {
            val recording = _currentRecording.value
            if (recording != null) {
                _downloadStates.value[recording.identifier] ?: ShowDownloadState.NotDownloaded
            } else {
                ShowDownloadState.NotDownloaded
            }
        } catch (e: Exception) {
            ShowDownloadState.Failed("Failed to get download state")
        }
    }
    
    /**
     * Check if a track is downloaded
     */
    fun isTrackDownloaded(track: Track): Boolean {
        return try {
            val recording = _currentRecording.value
            if (recording != null) {
                val trackKey = "${recording.identifier}_${track.audioFile?.filename}"
                _trackDownloadStates.value[trackKey] ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Show removal confirmation for download
     */
    fun showRemoveDownloadConfirmation() {
        viewModelScope.launch {
            val recording = _currentRecording.value
            if (recording != null) {
                try {
                    // Soft delete the recording
                    downloadRepository.markRecordingForDeletion(recording.identifier)
                    println("🗑️ Recording ${recording.identifier} marked for soft deletion")
                } catch (e: Exception) {
                    println("Failed to mark recording for deletion: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Navigate to next show chronologically
     */
    fun navigateToNextShow() {
        Log.d(TAG, "navigateToNextShow: Button clicked - starting fast navigation")
        _isNavigationLoading.value = true
        viewModelScope.launch {
            try {
                val currentRecording = _currentRecording.value
                if (currentRecording != null) {
                    Log.d(TAG, "navigateToNextShow: Current recording date: ${currentRecording.concertDate}")
                    
                    // Use a more efficient approach: find shows around current date
                    val currentDate = currentRecording.concertDate
                    val nextShow = findNextShowByDate(currentDate)
                    
                    if (nextShow != null) {
                        Log.d(TAG, "navigateToNextShow: Found next show: ${nextShow.date} at ${nextShow.venue}")
                        
                        // Use getBestRecordingForShowId to respect user preferences
                        val nextRecording = getBestRecordingForShowId(nextShow.showId)
                        if (nextRecording != null) {
                            Log.d(TAG, "navigateToNextShow: Navigating to next show with showId: ${nextShow.showId}, recordingId: ${nextRecording.identifier}")
                            // Use navigation callback to preserve showId parameter
                            onNavigateToShow?.invoke(nextShow.showId, nextRecording.identifier) ?: run {
                                // Fallback to direct loading if no callback is set
                                Log.w(TAG, "navigateToNextShow: No navigation callback set, falling back to direct loading")
                                loadRecording(nextRecording.identifier)
                            }
                        } else {
                            Log.w(TAG, "navigateToNextShow: No recordings found for next show: ${nextShow.showId}")
                        }
                    } else {
                        Log.d(TAG, "navigateToNextShow: No next show available")
                    }
                } else {
                    Log.w(TAG, "navigateToNextShow: No current recording loaded")
                }
            } catch (e: Exception) {
                Log.e(TAG, "navigateToNextShow: Error navigating to next show", e)
            } finally {
                _isNavigationLoading.value = false
            }
        }
    }
    
    /**
     * Find the next show chronologically after the given date (newer shows)
     */
    private suspend fun findNextShowByDate(currentDate: String): Show? {
        return try {
            val nextShow = showRepository.getNextShowByDate(currentDate)
            Log.d(TAG, "findNextShowByDate: Current date: $currentDate, Found next show: ${nextShow?.date} at ${nextShow?.venue}")
            nextShow
        } catch (e: Exception) {
            Log.e(TAG, "findNextShowByDate: Error", e)
            null
        }
    }
    
    /**
     * Get the best recording for a show (first one, which is typically highest quality)
     */
    private suspend fun getBestRecordingForShow(show: Show): Recording? {
        return try {
            // If the show already has recordings loaded, use the first one
            if (show.recordings.isNotEmpty()) {
                show.recordings.first()
            } else {
                // Otherwise fetch from repository
                val recordings = showRepository.getRecordingsByShowId(show.showId)
                recordings.firstOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingForShow: Error", e)
            null
        }
    }
    
    /**
     * Navigate to previous show chronologically
     */
    fun navigateToPreviousShow() {
        Log.d(TAG, "navigateToPreviousShow: Button clicked - starting fast navigation")
        _isNavigationLoading.value = true
        viewModelScope.launch {
            try {
                val currentRecording = _currentRecording.value
                if (currentRecording != null) {
                    Log.d(TAG, "navigateToPreviousShow: Current recording date: ${currentRecording.concertDate}")
                    
                    // Use a more efficient approach: find shows around current date
                    val currentDate = currentRecording.concertDate
                    val previousShow = findPreviousShowByDate(currentDate)
                    
                    if (previousShow != null) {
                        Log.d(TAG, "navigateToPreviousShow: Found previous show: ${previousShow.date} at ${previousShow.venue}")
                        
                        // Use getBestRecordingForShowId to respect user preferences
                        val previousRecording = getBestRecordingForShowId(previousShow.showId)
                        if (previousRecording != null) {
                            Log.d(TAG, "navigateToPreviousShow: Navigating to previous show with showId: ${previousShow.showId}, recordingId: ${previousRecording.identifier}")
                            // Use navigation callback to preserve showId parameter
                            onNavigateToShow?.invoke(previousShow.showId, previousRecording.identifier) ?: run {
                                // Fallback to direct loading if no callback is set
                                Log.w(TAG, "navigateToPreviousShow: No navigation callback set, falling back to direct loading")
                                loadRecording(previousRecording.identifier)
                            }
                        } else {
                            Log.w(TAG, "navigateToPreviousShow: No recordings found for previous show: ${previousShow.showId}")
                        }
                    } else {
                        Log.d(TAG, "navigateToPreviousShow: No previous show available")
                    }
                } else {
                    Log.w(TAG, "navigateToPreviousShow: No current recording loaded")
                }
            } catch (e: Exception) {
                Log.e(TAG, "navigateToPreviousShow: Error navigating to previous show", e)
            } finally {
                _isNavigationLoading.value = false
            }
        }
    }
    
    /**
     * Find the previous show chronologically before the given date (older shows)
     */
    private suspend fun findPreviousShowByDate(currentDate: String): Show? {
        return try {
            val previousShow = showRepository.getPreviousShowByDate(currentDate)
            Log.d(TAG, "findPreviousShowByDate: Current date: $currentDate, Found previous show: ${previousShow?.date} at ${previousShow?.venue}")
            previousShow
        } catch (e: Exception) {
            Log.e(TAG, "findPreviousShowByDate: Error", e)
            null
        }
    }
    /**
     * Get alternative recordings for the current show
     */
    suspend fun getAlternativeRecordings(): List<Recording> {
        return try {
            val currentRecording = _currentRecording.value
            if (currentRecording != null) {
                // Get the show for the current recording
                val show = getShowByRecording(currentRecording)
                if (show != null) {
                    Log.d(TAG, "getAlternativeRecordings: Found show ${show.showId} with ${show.recordings.size} recordings")
                    // Return all recordings for this show
                    show.recordings
                } else {
                    Log.w(TAG, "getAlternativeRecordings: Could not find show for current recording")
                    emptyList()
                }
            } else {
                Log.w(TAG, "getAlternativeRecordings: No current recording loaded")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlternativeRecordings: Error fetching alternative recordings", e)
            emptyList()
        }
    }
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    /**
     * Get ShowEntity by showId for debug purposes
     */
    suspend fun getShowEntityById(showId: String): ShowEntity? {
        return try {
            showRepository.getShowEntityById(showId)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ShowEntity: $showId", e)
            null
        }
    }
    
    /**
     * Get Show object from Recording for debug purposes
     */
    suspend fun getShowByRecording(recording: Recording): Show? {
        return try {
            // Calculate showId from recording
            val normalizedDate = if (recording.concertDate.contains("T")) {
                recording.concertDate.substringBefore("T")
            } else {
                recording.concertDate
            }
            val normalizedVenue = recording.concertVenue
                ?.replace("'", "")
                ?.replace(".", "")
                ?.replace(" - ", "_")
                ?.replace(", ", "_")
                ?.replace(" & ", "_and_")
                ?.replace("&", "_and_")
                ?.replace(" University", "_U", true)
                ?.replace(" College", "_C", true)
                ?.replace("Memorial", "Mem", true)
                ?.replace("\\s+".toRegex(), "_")
                ?.replace("_+".toRegex(), "_")
                ?.trim('_')
                ?.lowercase()
                ?: "unknown"
            val showId = "${normalizedDate}_${normalizedVenue}"
            
            showRepository.getShowById(showId)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Show from recording", e)
            null
        }
    }
    
    /**
     * Save recording preference for a show
     */
    fun setRecordingPreference(showId: String, recordingId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting recording preference: showId=$showId, recordingId=$recordingId")
                settingsRepository.updateRecordingPreference(showId, recordingId)
                Log.d(TAG, "Recording preference saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set recording preference", e)
            }
        }
    }
    
    /**
     * Reset recording preference to the original ratings-based best recording
     */
    fun resetToRecommendedRecording(showId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Resetting to recommended recording for showId: $showId")
                // Remove user preference to fall back to ratings-based best recording
                settingsRepository.removeRecordingPreference(showId)
                
                // Load the recommended recording
                val recommendedRecordingId = getRecommendedRecordingId(showId)
                if (recommendedRecordingId != null) {
                    Log.d(TAG, "Loading recommended recording: $recommendedRecordingId")
                    loadRecording(recommendedRecordingId)
                } else {
                    Log.w(TAG, "No recommended recording found for showId: $showId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset to recommended recording", e)
            }
        }
    }
    
    /**
     * Get the best recording for a show ID based on user preferences
     */
    suspend fun getBestRecordingForShowId(showId: String): Recording? {
        return try {
            Log.d(TAG, "getBestRecordingForShowId: Getting best recording for showId: $showId")
            val show = showRepository.getShowById(showId)
            if (show != null) {
                Log.d(TAG, "getBestRecordingForShowId: Found show with ${show.recordings.size} recordings")
                // The show.bestRecording property already respects user preferences
                show.bestRecording
            } else {
                Log.w(TAG, "getBestRecordingForShowId: Show not found for showId: $showId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingForShowId: Error", e)
            null
        }
    }
    
    /**
     * Get alternative recordings for a specific show ID
     */
    suspend fun getAlternativeRecordingsById(showId: String): List<Recording> {
        return try {
            Log.d(TAG, "getAlternativeRecordingsById: Getting recordings for showId: $showId")
            val show = showRepository.getShowById(showId)
            if (show != null) {
                Log.d(TAG, "getAlternativeRecordingsById: Found ${show.recordings.size} recordings")
                show.recordings
            } else {
                Log.w(TAG, "getAlternativeRecordingsById: Show not found for showId: $showId")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlternativeRecordingsById: Error", e)
            emptyList()
        }
    }
    
    /**
     * Get the recommended recording ID for a show (ignoring user preferences, using algorithm-based selection)
     */
    suspend fun getRecommendedRecordingId(showId: String): String? {
        return try {
            Log.d(TAG, "getRecommendedRecordingId: Starting lookup for showId: $showId")
            
            // Get the full show with recordings to determine the best recording
            val show = showRepository.getShowById(showId)
            if (show != null) {
                Log.d(TAG, "getRecommendedRecordingId: Found show - date: ${show.date}, venue: ${show.venue}")
                Log.d(TAG, "getRecommendedRecordingId: Show has ${show.recordings.size} recordings")
                Log.d(TAG, "getRecommendedRecordingId: Show bestRecordingId: ${show.bestRecordingId}")
                
                // Use algorithm-based selection directly, ignoring bestRecordingId (which may be user preference)
                val algorithmRecording = show.recordings.minByOrNull { recording ->
                    // Same algorithm as Show.bestRecording but ignoring bestRecordingId
                    val ratingPriority = if (recording.hasRawRating) 0 else 1
                    val sourcePriority = when (recording.cleanSource?.uppercase()) {
                        "SBD" -> 1
                        "MATRIX" -> 2  
                        "FM" -> 3
                        "AUD" -> 4
                        else -> 5
                    }
                    val ratingValue = recording.rawRating ?: 0f
                    val ratingBonus = (5f - ratingValue) / 10f
                    
                    ratingPriority * 10 + sourcePriority + ratingBonus
                }
                Log.d(TAG, "getRecommendedRecordingId: Algorithm-based recommended recording: ${algorithmRecording?.identifier}")
                
                algorithmRecording?.identifier
            } else {
                Log.w(TAG, "getRecommendedRecordingId: Show not found for showId: $showId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRecommendedRecordingId: Error", e)
            null
        }
    }
}

data class PlayerUiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrackIndex: Int = 0,
    val tracks: List<Track> = emptyList(),
    val playbackState: Int = 1, // Player.STATE_IDLE
    val error: String? = null,
    // Playlist-specific UI state
    val isPlaylistMode: Boolean = false,
    val playlistSize: Int = 0
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentTrackIndex)
    
    val hasNextTrack: Boolean
        get() = currentTrackIndex < tracks.size - 1
    
    val hasPreviousTrack: Boolean
        get() = currentTrackIndex > 0
    
    val progress: Float
        get() = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    
    val isBuffering: Boolean
        get() = playbackState == 2 // Player.STATE_BUFFERING
}