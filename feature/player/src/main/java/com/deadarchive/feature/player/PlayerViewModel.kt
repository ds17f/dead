package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.SetlistRepository
import com.deadarchive.core.media.player.MediaControllerRepositoryRefactored
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
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val mediaControllerRepository: MediaControllerRepositoryRefactored,
    private val queueManager: QueueManager,
    val queueStateManager: QueueStateManager,
    val playbackEventTracker: PlaybackEventTracker,
    private val setlistRepository: SetlistRepository,
    private val settingsRepository: com.deadarchive.core.settings.api.SettingsRepository,
    private val playerDataService: com.deadarchive.feature.player.service.PlayerDataService,
    private val playerPlaylistService: com.deadarchive.feature.player.service.PlayerPlaylistService,
    private val playerDownloadService: com.deadarchive.feature.player.service.PlayerDownloadService,
    private val playerLibraryService: com.deadarchive.feature.player.service.PlayerLibraryService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _currentRecording = MutableStateFlow<Recording?>(null)
    val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()
    
    // Download state tracking - delegated to service
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = playerDownloadService.downloadStates
    
    // Track-level download states - delegated to service
    val trackDownloadStates: StateFlow<Map<String, Boolean>> = playerDownloadService.trackDownloadStates
    
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
    
    // Playlist management state - delegated to service
    val currentPlaylist: StateFlow<List<PlaylistItem>> = playerPlaylistService.currentPlaylist
    val playlistTitle: StateFlow<String?> = playerPlaylistService.playlistTitle
    
    // Navigation loading state
    private val _isNavigationLoading = MutableStateFlow(false)
    val isNavigationLoading: StateFlow<Boolean> = _isNavigationLoading.asStateFlow()
    
    // Setlist state management
    private val _setlistState = MutableStateFlow<SetlistState>(SetlistState.Initial)
    val setlistState: StateFlow<SetlistState> = _setlistState.asStateFlow()
    
    // Library state management - delegated to service
    val isInLibrary: StateFlow<Boolean> = playerLibraryService.isInLibrary
    
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
            playerDownloadService.startDownloadStateMonitoring()
            
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
                    
                    // Update library status for this recording's show
                    val showId = playerDataService.generateShowId(recording)
                    playerLibraryService.checkLibraryStatus(showId)
                    
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
     * Update MediaControllerRepositoryRefactored with current queue context
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
        // Don't release MediaControllerRepositoryRefactored - it should persist for background playback
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
                    playerDownloadService.downloadRecording(recording)
                    Log.d(TAG, "downloadRecording: Started download for recording ${recording.identifier}")
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
                    playerDownloadService.cancelRecordingDownloads(recording)
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
     * Get the current download state for the recording
     */
    fun getRecordingDownloadState(): ShowDownloadState {
        return try {
            val recording = _currentRecording.value
            if (recording != null) {
                playerDownloadService.getRecordingDownloadState(recording)
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
            playerDownloadService.isTrackDownloaded(track)
        } catch (e: Exception) {
            Log.e(TAG, "isTrackDownloaded: Error checking if track is downloaded", e)
            false
        }
    }
    
    /**
     * Show removal confirmation for download
     */
    fun showRemoveDownloadConfirmation() {
        val recording = _currentRecording.value
        if (recording != null) {
            playerDownloadService.showRemoveDownloadConfirmation(recording)
        } else {
            Log.w(TAG, "showRemoveDownloadConfirmation: No recording available")
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
     * Set current show ID for library status observation
     */
    fun checkLibraryStatus(showId: String) {
        viewModelScope.launch {
            playerLibraryService.checkLibraryStatus(showId)
        }
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
                    val currentStatus = playerLibraryService.isInLibrary.value
                    
                    if (currentStatus) {
                        playerLibraryService.removeFromLibrary(showId)
                    } else {
                        playerLibraryService.addToLibrary(showId)
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