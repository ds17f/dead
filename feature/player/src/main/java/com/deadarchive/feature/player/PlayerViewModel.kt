package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.SetlistRepository
import com.deadarchive.core.media.player.MediaControllerRepository
import com.deadarchive.core.media.player.PlaybackEventTracker
import com.deadarchive.core.media.player.QueueManager
import com.deadarchive.core.media.player.QueueStateManager
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Setlist
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.model.util.VenueUtil
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.design.component.DownloadAction
import com.deadarchive.core.data.download.DownloadService
import com.deadarchive.core.data.service.LibraryService
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val mediaControllerRepository: MediaControllerRepository,
    private val queueManager: QueueManager,
    val queueStateManager: QueueStateManager,
    val playbackEventTracker: PlaybackEventTracker,
    private val setlistRepository: SetlistRepository,
    private val settingsRepository: com.deadarchive.core.settings.api.SettingsRepository,
    private val downloadService: DownloadService,
    private val playerDataService: com.deadarchive.feature.player.service.PlayerDataService,
    private val playerPlaylistService: com.deadarchive.feature.player.service.PlayerPlaylistService,
    private val libraryService: LibraryService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _currentRecording = MutableStateFlow<Recording?>(null)
    val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()
    
    // Download state tracking - delegated to shared service
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = downloadService.downloadStates
    
    // Track-level download states - delegated to shared service
    val trackDownloadStates: StateFlow<Map<String, Boolean>> = downloadService.trackDownloadStates
    
    // Download confirmation dialog state (passthrough to DownloadService)
    val showConfirmationDialog: StateFlow<Show?> = downloadService.showConfirmationDialog
    
    // Settings flow for debug panel access
    val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.deadarchive.core.settings.api.model.AppSettings()
        )
    
    // Navigation callbacks for show navigation with showId parameter
    var onNavigateToShow: ((showId: String, recordingId: String) -> Unit)? = null
    
    // Navigation state from QueueStateManager
    val hasNext = queueStateManager.hasNext.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val hasPrevious = queueStateManager.hasPrevious.stateIn(viewModelScope, SharingStarted.Lazily, false)
    
    // Track UI sync handled by MediaController combine flow below - removed duplicate QueueStateManager flow
    
    // Playlist management state - delegated to service
    val currentPlaylist: StateFlow<List<PlaylistItem>> = playerPlaylistService.currentPlaylist
    val playlistTitle: StateFlow<String?> = playerPlaylistService.playlistTitle
    
    // Navigation loading state
    private val _isNavigationLoading = MutableStateFlow(false)
    val isNavigationLoading: StateFlow<Boolean> = _isNavigationLoading.asStateFlow()
    
    // Setlist state management
    private val _setlistState = MutableStateFlow<SetlistState>(SetlistState.Initial)
    val setlistState: StateFlow<SetlistState> = _setlistState.asStateFlow()
    
    // Library state management - reactive flow from LibraryService
    // This automatically updates when library status changes (e.g., when downloads add shows to library)
    val isInLibraryFlow: StateFlow<Boolean> = currentRecording
        .filterNotNull()
        .flatMapLatest { recording ->
            val showId = playerDataService.generateShowId(recording)
            libraryService.isShowInLibraryFlow(showId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
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
                    mediaControllerRepository.currentTrackMediaId
                ) { isPlaying, position, duration, state, currentMediaId ->
                    var updatedState = _uiState.value.copy(
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,
                        playbackState = state
                    )
                    
                    // SAFE TRACK MATCHING USING MEDIAID
                    currentMediaId?.let { mediaId ->
                        // SAFETY CHECK: Only try to match if we have a recording and tracks loaded
                        val currentRecording = _currentRecording.value
                        val availableTracks = _uiState.value.tracks
                        
                        if (currentRecording == null || availableTracks.isEmpty()) {
                            Log.d(TAG, "=== SKIPPING TRACK MATCHING (NO DATA) ===")
                            Log.d(TAG, "MediaId: $mediaId")
                            Log.d(TAG, "Recording loaded: ${currentRecording != null}")
                            Log.d(TAG, "Tracks available: ${availableTracks.size}")
                            Log.d(TAG, "Reason: App startup or no recording loaded yet")
                            return@let // Skip matching, don't crash
                        }
                        
                        Log.d(TAG, "=== TRACK MATCHING WITH MEDIAID ===")
                        Log.d(TAG, "MediaId to match: $mediaId")
                        Log.d(TAG, "Current recording: ${currentRecording.identifier}")
                        Log.d(TAG, "Available tracks: ${availableTracks.size}")
                        
                        // Log all available tracks with new MediaId format for debugging
                        availableTracks.forEachIndexed { i, track ->
                            val expectedMediaId = "${currentRecording.identifier}_${track.filename}"
                            Log.d(TAG, "  Track[$i]: ${track.displayTitle}")
                            Log.d(TAG, "    Expected MediaId: $expectedMediaId")
                            Log.d(TAG, "    Filename: ${track.filename}")
                            Log.d(TAG, "    DownloadURL: ${track.audioFile?.downloadUrl}")
                        }
                        
                        // Find track by exact MediaId match (MediaId is recordingId_filename)
                        val trackIndex = availableTracks.indexOfFirst { track ->
                            "${currentRecording.identifier}_${track.filename}" == mediaId
                        }
                        
                        if (trackIndex >= 0) {
                            if (trackIndex != updatedState.currentTrackIndex) {
                                Log.w(TAG, "✅ TRACK MATCH SUCCESS: syncing UI track index from ${updatedState.currentTrackIndex} to $trackIndex")
                                Log.w(TAG, "  Matched track: ${availableTracks[trackIndex].displayTitle}")
                                Log.w(TAG, "  Setting currentTrackIndex to: $trackIndex")
                                updatedState = updatedState.copy(currentTrackIndex = trackIndex)
                                Log.w(TAG, "  Updated state currentTrackIndex: ${updatedState.currentTrackIndex}")
                            } else {
                                Log.d(TAG, "✅ Track already matched at index $trackIndex: ${availableTracks[trackIndex].displayTitle}")
                            }
                        } else {
                            // LOG WARNING: Track matching failed - likely during show transition
                            Log.w(TAG, "⚠️ Track matching failed - likely during show transition")
                            Log.w(TAG, "MediaId: $mediaId")  
                            Log.w(TAG, "Recording: ${currentRecording.identifier}")
                            Log.w(TAG, "Available tracks: ${availableTracks.size}")
                            Log.w(TAG, "ENTERING TRACK MATCHING FAILURE LOGIC")
                            
                            // Check if MediaId contains current recording identifier
                            val mediaIdBelongsToCurrentRecording = mediaId.contains(currentRecording.identifier)
                            Log.w(TAG, "MediaId belongs to current recording: $mediaIdBelongsToCurrentRecording")
                            
                            if (mediaIdBelongsToCurrentRecording) {
                                // MediaId belongs to current recording but no match found - genuine issue
                                Log.e(TAG, "❌ GENUINE TRACK MATCHING FAILURE!")
                                availableTracks.forEachIndexed { i, track ->
                                    Log.e(TAG, "  [$i] ${track.displayTitle} -> ${track.audioFile?.downloadUrl}")
                                }
                                // Reset track index only for genuine failures
                                updatedState = updatedState.copy(currentTrackIndex = -1)
                            } else {
                                // MediaId is from different recording - need to load the correct recording
                                Log.w(TAG, "📱 Different recording detected - extracting recording ID from MediaId")
                                val recordingIdFromMediaId = mediaId.substringBefore("_")
                                Log.w(TAG, "  Extracted recording ID: $recordingIdFromMediaId")
                                Log.w(TAG, "  Current recording ID: ${currentRecording.identifier}")
                                Log.w(TAG, "  Are they different? ${recordingIdFromMediaId != currentRecording.identifier}")
                                Log.w(TAG, "  Is extracted ID not blank? ${recordingIdFromMediaId.isNotBlank()}")
                                
                                if (recordingIdFromMediaId != currentRecording.identifier && recordingIdFromMediaId.isNotBlank()) {
                                    Log.w(TAG, "🚨 Different recording playing but NOT auto-loading to avoid navigation interference")
                                    Log.w(TAG, "  Playing: $recordingIdFromMediaId")
                                    Log.w(TAG, "  UI showing: ${currentRecording.identifier}")
                                    // Don't auto-load - let user navigate freely
                                } else {
                                    Log.w(TAG, "📱 Show transition in progress - keeping current track index")
                                }
                            }
                        }
                    }
                    
                    updatedState
                }.collect { updatedState ->
                    _uiState.value = updatedState
                    Log.d(TAG, "PlayerViewModel: State updated - isPlaying: ${updatedState.isPlaying}, position: ${updatedState.currentPosition}, duration: ${updatedState.duration}, playbackState: ${updatedState.playbackState}, trackIndex: ${updatedState.currentTrackIndex}")
                }
            }
            
            // Download state monitoring starts automatically in shared DownloadService
            
            // Auto-load current recording from MediaController if available
            viewModelScope.launch {
                mediaControllerRepository.currentRecordingId.collect { recordingId ->
                    if (recordingId != null && _currentRecording.value?.identifier != recordingId) {
                        Log.d(TAG, "PlayerViewModel: Auto-loading recording from MediaController: $recordingId")
                        loadRecording(recordingId)
                    }
                }
            }
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
                val formatPreferences = settings?.audioFormatPreferences ?: com.deadarchive.core.model.AppConstants.PREFERRED_AUDIO_FORMATS
                
                Log.d(TAG, "loadRecording: Using format preferences: $formatPreferences")
                
                // Load recording with format filtering applied
                Log.d(TAG, "loadRecording: About to call playerDataService.loadRecording with ID: '$recordingId'")
                val recording = playerDataService.loadRecording(recordingId, formatPreferences)
                
                Log.d(TAG, "loadRecording: Repository returned recording: ${recording != null}")
                if (recording != null) {
                    Log.d(TAG, "loadRecording: Found recording: ${recording.identifier} - ${recording.title}")
                    Log.d(TAG, "loadRecording: Recording found - title: ${recording.title}, filtered tracks count: ${recording.tracks.size}")
                    recording.tracks.forEachIndexed { index, track ->
                        Log.d(TAG, "loadRecording: Track $index - title: ${track.displayTitle}, format: ${track.audioFile?.format}")
                    }
                    
                    _currentRecording.value = recording
                    
                    // Library status will be automatically updated via reactive flow
                    
                    // Create playlist from filtered recording tracks
                    val playlist = recording.tracks.mapIndexed { index, track ->
                        PlaylistItem(
                            concertIdentifier = recording.identifier,
                            track = track,
                            position = index
                        )
                    }
                    playerPlaylistService.setPlaylist(playlist, recording.displayTitle)
                    
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
                queueManager.loadShow(currentRecording, 0, 0, true)
                
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
        playerPlaylistService.setPlaylist(playlist, title)
        
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
        return playerPlaylistService.getCurrentPlaylist()
    }
    
    /**
     * Navigate to a specific track in the current playlist
     * @param playlistIndex Index of the track in the playlist to navigate to
     */
    fun navigateToTrack(playlistIndex: Int) {
        viewModelScope.launch {
            playerPlaylistService.navigateToTrack(playlistIndex)
            // Update UI state to reflect the change
            _uiState.value = _uiState.value.copy(currentTrackIndex = playlistIndex)
        }
    }
    
    /**
     * Add a track to the current playlist
     * @param playlistItem PlaylistItem to add to the playlist
     */
    fun addToPlaylist(playlistItem: PlaylistItem) {
        playerPlaylistService.addToPlaylist(playlistItem)
        
        // Update UI state tracks
        val tracks = playerPlaylistService.getCurrentPlaylist().map { it.track }
        _uiState.value = _uiState.value.copy(
            tracks = tracks,
            isPlaylistMode = tracks.isNotEmpty(),
            playlistSize = tracks.size
        )
    }
    
    /**
     * Remove a track from the current playlist
     * @param playlistIndex Index of the track to remove from the playlist
     */
    fun removeFromPlaylist(playlistIndex: Int) {
        playerPlaylistService.removeFromPlaylist(playlistIndex)
        
        // Update UI state tracks
        val tracks = playerPlaylistService.getCurrentPlaylist().map { it.track }
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
    }
    
    /**
     * Clear the current playlist
     */
    fun clearPlaylist() {
        playerPlaylistService.clearPlaylist()
        
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
                    artist = playerPlaylistService.playlistTitle.value ?: _currentRecording.value?.title
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
    
    /**
     * Start downloading the current recording
     */
    fun downloadRecording() {
        viewModelScope.launch {
            try {
                val recording = _currentRecording.value
                if (recording != null) {
                    Log.d(TAG, "downloadRecording: Starting download for recording ${recording.identifier}")
                    
                    // Check current download state
                    val currentState = downloadService.getRecordingDownloadState(recording)
                    Log.d(TAG, "downloadRecording: Current state: $currentState")
                    
                    when (currentState) {
                        is ShowDownloadState.NotDownloaded -> {
                            // Start new download
                            downloadService.downloadRecording(recording)
                            Log.d(TAG, "downloadRecording: Started download for recording ${recording.identifier}")
                        }
                        is ShowDownloadState.Downloaded -> {
                            // Show confirmation dialog for removal - use the centralized confirmation system
                            Log.d(TAG, "downloadRecording: Recording already downloaded, showing confirmation dialog")
                            // Create a minimal Show object for the confirmation dialog
                            val show = Show(
                                date = recording.title?.substringBefore(" ") ?: "Unknown Date",
                                venue = "Recording: ${recording.identifier}",
                                recordings = listOf(recording)
                            )
                            downloadService.showRemoveDownloadConfirmation(show)
                        }
                        is ShowDownloadState.Downloading -> {
                            // Cancel in-progress download
                            Log.d(TAG, "downloadRecording: Canceling in-progress download")
                            downloadService.cancelRecordingDownloads(recording)
                        }
                        is ShowDownloadState.Paused -> {
                            // Resume paused download
                            Log.d(TAG, "downloadRecording: Resuming paused download")
                            downloadService.resumeRecordingDownloads(recording.identifier)
                        }
                        is ShowDownloadState.Cancelled -> {
                            // Restart cancelled download
                            Log.d(TAG, "downloadRecording: Restarting cancelled download")
                            downloadService.downloadRecording(recording)
                        }
                        is ShowDownloadState.Failed -> {
                            // Retry failed download
                            Log.d(TAG, "downloadRecording: Retrying failed download")
                            downloadService.downloadRecording(recording)
                        }
                    }
                } else {
                    Log.w(TAG, "downloadRecording: No recording available to download")
                }
            } catch (e: Exception) {
                Log.e(TAG, "downloadRecording: Failed to start download", e)
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
                    downloadService.cancelRecordingDownloads(recording)
                    Log.d(TAG, "cancelRecordingDownloads: Canceled downloads for recording ${recording.identifier}")
                } else {
                    Log.w(TAG, "cancelRecordingDownloads: No recording found to cancel downloads")
                }
            } catch (e: Exception) {
                Log.e(TAG, "cancelRecordingDownloads: Failed to cancel downloads", e)
            }
        }
    }
    
    /**
     * Handle download button click with smart state-based logic using centralized DownloadService
     */
    fun handleDownloadButtonClick() {
        val recording = _currentRecording.value
        if (recording != null) {
            // Create a minimal Show object for the DownloadService
            val show = Show(
                date = recording.concertDate,
                venue = recording.concertVenue,
                location = recording.concertLocation,
                recordings = listOf(recording)
            )
            
            downloadService.handleDownloadButtonClick(
                show = show,
                coroutineScope = viewModelScope,
                onError = { errorMessage ->
                    Log.e(TAG, "handleDownloadButtonClick: $errorMessage")
                }
            )
            
            // Library status will be automatically updated via reactive flow when downloads add shows to library
        } else {
            Log.w(TAG, "handleDownloadButtonClick: No recording available")
        }
    }
    
    /**
     * Handle long-press actions from the unified DownloadButton component
     */
    fun handleDownloadAction(action: DownloadAction) {
        viewModelScope.launch {
            try {
                val recording = _currentRecording.value
                if (recording != null) {
                    // Create a minimal Show object for the DownloadService
                    val show = Show(
                        date = recording.title?.substringBefore(" ") ?: "Unknown Date",
                        venue = "Recording: ${recording.identifier}",
                        recordings = listOf(recording)
                    )
                    
                    downloadService.handleDownloadAction(
                        show = show,
                        action = action,
                        coroutineScope = viewModelScope,
                        onError = { errorMessage ->
                            Log.e(TAG, "handleDownloadAction: $errorMessage")
                        }
                    )
                } else {
                    Log.w(TAG, "handleDownloadAction: No recording available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleDownloadAction: Failed to handle action $action", e)
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
                downloadService.getRecordingDownloadState(recording)
            } else {
                ShowDownloadState.NotDownloaded
            }
        } catch (e: Exception) {
            ShowDownloadState.NotDownloaded
        }
    }
    
    /**
     * Check if a track is downloaded
     */
    suspend fun isTrackDownloaded(track: Track): Boolean {
        return try {
            downloadService.isTrackDownloaded(track)
        } catch (e: Exception) {
            Log.e(TAG, "isTrackDownloaded: Error checking if track is downloaded", e)
            false
        }
    }
    
    /**
     * Show removal confirmation for download
     * TODO: Implement show-based confirmation dialog
     */
    fun showRemoveDownloadConfirmation() {
        Log.d(TAG, "showRemoveDownloadConfirmation: Feature temporarily disabled during refactoring")
        // TODO: Need to get Show from Recording to call downloadService.showRemoveDownloadConfirmation(show)
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
                    val nextShow = playerDataService.findNextShowByDate(currentRecording)
                    
                    if (nextShow != null) {
                        Log.d(TAG, "navigateToNextShow: Found next show: ${nextShow.date} at ${nextShow.venue}")
                        
                        // Use getBestRecordingForShow to respect user preferences
                        val nextRecording = playerDataService.getBestRecordingForShow(nextShow)
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
                    val previousShow = playerDataService.findPreviousShowByDate(currentRecording)
                    
                    if (previousShow != null) {
                        Log.d(TAG, "navigateToPreviousShow: Found previous show: ${previousShow.date} at ${previousShow.venue}")
                        
                        // Use getBestRecordingForShow to respect user preferences
                        val previousRecording = playerDataService.getBestRecordingForShow(previousShow)
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
     * Get alternative recordings for the current show
     */
    suspend fun getAlternativeRecordings(): List<Recording> {
        return try {
            val currentRecording = _currentRecording.value
            if (currentRecording != null) {
                playerDataService.getAlternativeRecordings(currentRecording)
            } else {
                Log.w(TAG, "getAlternativeRecordings: No current recording loaded")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlternativeRecordings: Error fetching alternative recordings", e)
            emptyList()
        }
    }
    
    // Legacy API methods - delegate to services for backward compatibility
    suspend fun getAlternativeRecordingsById(showId: String): List<Recording> {
        return try {
            val currentRecording = _currentRecording.value
            if (currentRecording != null) {
                playerDataService.getAlternativeRecordings(currentRecording)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAlternativeRecordingsById: Error", e)
            emptyList()
        }
    }
    
    suspend fun getBestRecordingForShowId(showId: String): Recording? {
        return try {
            Log.d(TAG, "getBestRecordingForShowId: Delegating to playerDataService for showId: $showId")
            playerDataService.getBestRecordingByShowId(showId)
        } catch (e: Exception) {
            Log.e(TAG, "getBestRecordingForShowId: Error", e)
            null
        }
    }
    
    suspend fun getRecommendedRecordingId(showId: String): String? {
        return try {
            val bestRecording = getBestRecordingForShowId(showId)
            bestRecording?.identifier
        } catch (e: Exception) {
            Log.e(TAG, "getRecommendedRecordingId: Error", e)
            null
        }
    }

    companion object {
        private const val TAG = "PlayerViewModel"
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
                
                // Load the recommended recording using the service
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
     * Load setlist data for a show
     */
    fun loadSetlist(showId: String) {
        Log.d(TAG, "Loading setlist for showId: $showId")
        _setlistState.value = SetlistState.Loading
        
        viewModelScope.launch {
            try {
                val setlist = setlistRepository.getSetlist(showId)
                Log.d(TAG, "Setlist loaded: ${setlist?.hasSongs == true} (${setlist?.totalSongs ?: 0} songs)")
                _setlistState.value = SetlistState.Success(setlist)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load setlist for showId: $showId", e)
                _setlistState.value = SetlistState.Error("Failed to load setlist: ${e.message}")
            }
        }
    }
    
    /**
     * Clear setlist state
     */
    fun clearSetlist() {
        _setlistState.value = SetlistState.Initial
    }
    
    
    /**
     * Toggle library status for current show
     */
    fun toggleLibrary() {
        viewModelScope.launch {
            try {
                val currentRecording = _currentRecording.value
                if (currentRecording != null) {
                    val showId = playerDataService.generateShowId(currentRecording)
                    val currentStatus = libraryService.isShowInLibrary(showId)
                    
                    // Create a minimal Show object for the LibraryService
                    val show = Show(
                        date = currentRecording.concertDate ?: "Unknown Date",
                        venue = currentRecording.concertVenue ?: "Unknown Venue",
                        location = currentRecording.concertLocation ?: "",
                        recordings = listOf(currentRecording)
                    )
                    
                    if (currentStatus) {
                        libraryService.removeFromLibrary(showId)
                    } else {
                        libraryService.addToLibrary(show)
                    }
                    
                    Log.d(TAG, "toggleLibrary: Toggled library for show $showId")
                } else {
                    Log.w(TAG, "toggleLibrary: No current recording to toggle library status for")
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleLibrary: Failed to toggle library status", e)
            }
        }
    }
    
    /**
     * Hide download confirmation dialog (passthrough to DownloadService)
     */
    fun hideConfirmationDialog() {
        downloadService.hideConfirmationDialog()
    }
    
    /**
     * Confirm removal of download (passthrough to DownloadService)
     */
    fun confirmRemoveDownload() {
        viewModelScope.launch {
            downloadService.confirmRemoveDownload()
        }
    }
    
    /**
     * Handle library actions from LibraryButton component
     */
    fun handleLibraryAction(action: com.deadarchive.core.design.component.LibraryAction) {
        viewModelScope.launch {
            try {
                val currentRecording = _currentRecording.value
                if (currentRecording != null) {
                    val showId = playerDataService.generateShowId(currentRecording)
                    // Create a minimal show object from recording
                    val show = Show(
                        date = currentRecording.concertDate,
                        venue = currentRecording.concertVenue,
                        location = currentRecording.concertLocation,
                        recordings = listOf(currentRecording)
                    )
                    
                    when (action) {
                        com.deadarchive.core.design.component.LibraryAction.ADD_TO_LIBRARY -> {
                            libraryService.addToLibrary(show)
                        }
                        com.deadarchive.core.design.component.LibraryAction.REMOVE_FROM_LIBRARY -> {
                            libraryService.removeFromLibrary(show)
                        }
                        com.deadarchive.core.design.component.LibraryAction.REMOVE_WITH_DOWNLOADS -> {
                            libraryService.removeShowWithDownloadCleanup(show, alsoRemoveDownloads = true)
                        }
                    }
                    Log.d(TAG, "handleLibraryAction: Handled action $action for show $showId")
                } else {
                    Log.w(TAG, "handleLibraryAction: No current recording to handle library action for")
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleLibraryAction: Failed to handle library action $action", e)
            }
        }
    }
    
    /**
     * Get library removal info for LibraryButton component
     */
    fun getLibraryRemovalInfo(show: com.deadarchive.core.model.Show): com.deadarchive.core.data.service.LibraryRemovalInfo {
        return try {
            // Use the shared library service to get download info
            runBlocking {
                libraryService.getDownloadInfoForShow(show)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getLibraryRemovalInfo: Error getting download info", e)
            com.deadarchive.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "",
                downloadState = com.deadarchive.core.design.component.ShowDownloadState.NotDownloaded
            )
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

sealed class SetlistState {
    object Initial : SetlistState()
    object Loading : SetlistState()
    data class Success(val setlist: Setlist?) : SetlistState()
    data class Error(val message: String) : SetlistState()
}