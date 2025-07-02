package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.media.player.MediaControllerRepository
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val mediaControllerRepository: MediaControllerRepository,
    private val concertRepository: ConcertRepository,
    private val settingsRepository: com.deadarchive.core.settings.data.SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _currentConcert = MutableStateFlow<Concert?>(null)
    val currentConcert: StateFlow<Concert?> = _currentConcert.asStateFlow()
    
    // Playlist management state - now derived from MediaControllerRepository
    val currentPlaylist: StateFlow<List<PlaylistItem>> = combine(
        mediaControllerRepository.queueUrls,
        mediaControllerRepository.queueMetadata,
        _currentConcert
    ) { queueUrls, queueMetadata, concert ->
        Log.d(TAG, "=== PLAYLIST COMBINE FLOW ===")
        Log.d(TAG, "queueUrls.size: ${queueUrls.size}")
        Log.d(TAG, "queueMetadata.size: ${queueMetadata.size}")
        Log.d(TAG, "concert: ${concert?.title}")
        queueUrls.forEachIndexed { index, url ->
            Log.d(TAG, "queueUrls[$index]: $url")
        }
        
        if (queueUrls.isNotEmpty()) {
            if (concert != null) {
                // Map queue URLs back to PlaylistItems using concert tracks (preferred)
                val playlist = queueUrls.mapIndexedNotNull { index, url ->
                    val track = concert.tracks.find { it.audioFile?.downloadUrl == url }
                    track?.let { 
                        Log.d(TAG, "Mapped URL to track: ${it.displayTitle}")
                        PlaylistItem(
                            concertIdentifier = concert.identifier,
                            track = it,
                            position = index
                        )
                    }
                }
                Log.d(TAG, "Final playlist size: ${playlist.size}")
                playlist
            } else {
                // Concert is null, use metadata from MediaControllerRepository (proper track titles)
                Log.d(TAG, "Concert is null, using metadata from MediaControllerRepository")
                val playlist = queueUrls.mapIndexed { index, url ->
                    val filename = url.substringAfterLast("/")
                    // Use metadata title if available, otherwise extract from filename
                    val trackTitle = queueMetadata.find { it.first == url }?.second 
                        ?: filename.substringBeforeLast(".")
                    
                    Log.d(TAG, "Using metadata title for index $index: $trackTitle")
                    
                    // Create a minimal Track object for display
                    val fallbackTrack = Track(
                        filename = filename,
                        title = trackTitle,
                        trackNumber = (index + 1).toString(),
                        durationSeconds = "0",
                        audioFile = AudioFile(
                            filename = filename,
                            format = filename.substringAfterLast(".").uppercase(),
                            downloadUrl = url,
                            sizeBytes = "0"
                        )
                    )
                    
                    PlaylistItem(
                        concertIdentifier = "metadata",
                        track = fallbackTrack,
                        position = index
                    )
                }
                Log.d(TAG, "Created metadata-based playlist size: ${playlist.size}")
                playlist
            }
        } else {
            Log.d(TAG, "Returning empty playlist - queueUrls empty: ${queueUrls.isEmpty()}, concert null: ${concert == null}")
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _playlistTitle = MutableStateFlow<String?>(null)
    val playlistTitle: StateFlow<String?> = _playlistTitle.asStateFlow()
    
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
                            updatedState = updatedState.copy(currentTrackIndex = trackIndex)
                        }
                    }
                    
                    updatedState
                }.collect { updatedState ->
                    _uiState.value = updatedState
                    Log.d(TAG, "PlayerViewModel: State updated - isPlaying: ${updatedState.isPlaying}, position: ${updatedState.currentPosition}, duration: ${updatedState.duration}, playbackState: ${updatedState.playbackState}, trackIndex: ${updatedState.currentTrackIndex}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PlayerViewModel: Exception in init", e)
        }
    }
    
    fun loadConcert(concertId: String) {
        Log.d(TAG, "loadConcert: Starting to load concert with ID: $concertId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get user's audio format preferences
                val settings = settingsRepository.getSettings().firstOrNull()
                val formatPreferences = settings?.audioFormatPreference ?: com.deadarchive.core.model.AppConstants.PREFERRED_AUDIO_FORMATS
                
                Log.d(TAG, "loadConcert: Using format preferences: $formatPreferences")
                
                // Load concert with format filtering applied
                val concert = concertRepository.getConcertByIdWithFormatFilter(concertId, formatPreferences)
                
                if (concert != null) {
                    Log.d(TAG, "loadConcert: Concert found - title: ${concert.title}, filtered tracks count: ${concert.tracks.size}")
                    concert.tracks.forEachIndexed { index, track ->
                        Log.d(TAG, "loadConcert: Track $index - title: ${track.displayTitle}, format: ${track.audioFile?.format}")
                    }
                    
                    _currentConcert.value = concert
                    
                    // Create playlist from filtered concert tracks
                    val playlist = concert.tracks.mapIndexed { index, track ->
                        PlaylistItem(
                            concertIdentifier = concert.identifier,
                            track = track,
                            position = index
                        )
                    }
                    _playlistTitle.value = concert.displayTitle
                    
                    // Update queue context for UI synchronization (this populates currentPlaylist via StateFlow)
                    // Note: This doesn't start playback, it just makes the queue data available
                    updateQueueContext(playlist)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tracks = concert.tracks,
                        currentTrackIndex = 0,
                        isPlaylistMode = true,
                        playlistSize = playlist.size
                    )
                    
                    // Note: Removed auto-play behavior - concerts should only play when explicitly requested
                    Log.d(TAG, "loadConcert: Concert loaded successfully, ${concert.tracks.size} filtered tracks available")
                } else {
                    Log.w(TAG, "loadConcert: Concert not found for ID: $concertId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Concert not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadConcert: Exception loading concert", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load concert: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Start playing the concert from the beginning (first track)
     */
    fun playConcertFromBeginning() {
        Log.d(TAG, "playConcertFromBeginning: Starting concert from first track")
        val tracks = _uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playTrack(0)
        } else {
            Log.w(TAG, "playConcertFromBeginning: No tracks available to play")
        }
    }
    
    /**
     * Update MediaControllerRepository with current queue context
     * The playlist should already contain filtered tracks (one format per song)
     */
    private fun updateQueueContext(playlist: List<PlaylistItem>) {
        val queueUrls = playlist.mapNotNull { it.track.audioFile?.downloadUrl }
        val trackTitles = playlist.map { it.track.displayTitle }
        val trackArtists = playlist.map { _currentConcert.value?.displayTitle ?: "Unknown Artist" }
        
        if (queueUrls.isNotEmpty()) {
            // Only update queue context if nothing is currently playing to avoid interrupting playback
            val isCurrentlyPlaying = mediaControllerRepository.isPlaying.value
            if (!isCurrentlyPlaying) {
                Log.d(TAG, "updateQueueContext: Updating MediaController with ${queueUrls.size} filtered queue items (not currently playing)")
                Log.d(TAG, "updateQueueContext: Sample titles: ${trackTitles.take(3)}")
                // Log the formats being queued to verify filtering worked
                playlist.take(3).forEach { item ->
                    Log.d(TAG, "updateQueueContext: Queue item - ${item.track.displayTitle} (${item.track.audioFile?.format})")
                }
                mediaControllerRepository.updateQueueContext(
                    queueUrls = queueUrls,
                    trackTitles = trackTitles,
                    trackArtists = trackArtists,
                    currentIndex = _uiState.value.currentTrackIndex
                )
            } else {
                Log.d(TAG, "updateQueueContext: Skipping queue update - music is currently playing")
            }
        } else {
            Log.w(TAG, "updateQueueContext: No valid URLs found in playlist")
        }
    }
    
    fun playTrack(trackIndex: Int) {
        Log.d(TAG, "playTrack: Attempting to play track at index $trackIndex")
        val tracks = _uiState.value.tracks
        Log.d(TAG, "playTrack: Total tracks available: ${tracks.size}")
        
        if (trackIndex in tracks.indices) {
            val track = tracks[trackIndex]
            Log.d(TAG, "playTrack: Selected track - title: ${track.displayTitle}, filename: ${track.filename}")
            
            val audioFile = track.audioFile
            Log.d(TAG, "playTrack: AudioFile - ${audioFile?.filename}, downloadUrl: ${audioFile?.downloadUrl}")
            
            val downloadUrl = audioFile?.downloadUrl
            
            if (downloadUrl != null) {
                Log.d(TAG, "playTrack: Playing track with URL: $downloadUrl")
                try {
                    // Update queue context with current track index before playing
                    val currentPlaylist = currentPlaylist.value
                    if (currentPlaylist.isNotEmpty()) {
                        updateQueueContext(currentPlaylist)
                    }
                    
                    mediaControllerRepository.playTrack(
                        url = downloadUrl,
                        title = track.displayTitle,
                        artist = _currentConcert.value?.title
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        currentTrackIndex = trackIndex,
                        error = null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "playTrack: Exception calling playerRepository.playTrack", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to play track: ${e.localizedMessage}"
                    )
                }
            } else {
                Log.w(TAG, "playTrack: No download URL available for track ${track.displayTitle}")
                _uiState.value = _uiState.value.copy(
                    error = "Audio file not available for this track"
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
        val currentIndex = _uiState.value.currentTrackIndex
        val playlist = currentPlaylist.value
        
        if (playlist.isNotEmpty()) {
            // Use playlist-aware navigation if playlist is available
            if (currentIndex < playlist.size - 1) {
                navigateToTrack(currentIndex + 1)
            }
        } else {
            // Fallback to track-based navigation for backward compatibility
            val tracks = _uiState.value.tracks
            if (currentIndex < tracks.size - 1) {
                playTrack(currentIndex + 1)
            }
        }
    }
    
    fun skipToPrevious() {
        val currentIndex = _uiState.value.currentTrackIndex
        val playlist = currentPlaylist.value
        
        if (playlist.isNotEmpty()) {
            // Use playlist-aware navigation if playlist is available
            if (currentIndex > 0) {
                navigateToTrack(currentIndex - 1)
            }
        } else {
            // Fallback to track-based navigation for backward compatibility
            if (currentIndex > 0) {
                playTrack(currentIndex - 1)
            }
        }
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
        
        // Update MediaControllerRepository with queue context (this will populate currentPlaylist via StateFlow)
        updateQueueContext(playlist)
        
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
        val playlist = currentPlaylist.value
        
        if (playlistIndex in playlist.indices) {
            val playlistItem = playlist[playlistIndex]
            val track = playlistItem.track
            
            Log.d(TAG, "navigateToTrack: Selected track - title: ${track.displayTitle}")
            
            // Update current track index in UI state
            _uiState.value = _uiState.value.copy(currentTrackIndex = playlistIndex)
            
            // Play the track
            playTrackFromPlaylist(playlistItem)
        } else {
            Log.e(TAG, "navigateToTrack: Invalid playlist index $playlistIndex for ${playlist.size} items")
        }
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
        
        // Update MediaControllerRepository with queue context (this will update currentPlaylist via StateFlow)
        updateQueueContext(currentPlaylist)
        
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
            
            // Update MediaControllerRepository with queue context (this will update currentPlaylist via StateFlow)
            updateQueueContext(currentPlaylist)
            
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
        
        // Clear MediaControllerRepository queue context (this will clear currentPlaylist via StateFlow)
        mediaControllerRepository.updateQueueContext(emptyList(), 0)
        
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
                    artist = _playlistTitle.value ?: _currentConcert.value?.title
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
    
    companion object {
        private const val TAG = "PlayerViewModel"
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