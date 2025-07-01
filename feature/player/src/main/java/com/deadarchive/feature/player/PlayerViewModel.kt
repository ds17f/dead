package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.media.player.PlayerRepository
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val concertRepository: ConcertRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private val _currentConcert = MutableStateFlow<Concert?>(null)
    val currentConcert: StateFlow<Concert?> = _currentConcert.asStateFlow()
    
    // Playlist management state
    private val _currentPlaylist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val currentPlaylist: StateFlow<List<PlaylistItem>> = _currentPlaylist.asStateFlow()
    
    private val _playlistTitle = MutableStateFlow<String?>(null)
    val playlistTitle: StateFlow<String?> = _playlistTitle.asStateFlow()
    
    init {
        Log.d(TAG, "PlayerViewModel: Initializing")
        try {
            // Observe player state changes
            viewModelScope.launch {
                Log.d(TAG, "PlayerViewModel: Setting up player state observation")
                combine(
                    playerRepository.isPlaying,
                    playerRepository.currentPosition,
                    playerRepository.duration,
                    playerRepository.playbackState,
                    playerRepository.currentTrack
                ) { isPlaying, position, duration, state, currentMediaItem ->
                    _uiState.value.copy(
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,
                        playbackState = state
                    )
                }.collect { updatedState ->
                    _uiState.value = updatedState
                    Log.d(TAG, "PlayerViewModel: State updated - isPlaying: ${updatedState.isPlaying}, position: ${updatedState.currentPosition}, duration: ${updatedState.duration}, playbackState: ${updatedState.playbackState}")
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
                Log.d(TAG, "loadConcert: Calling repository.getConcertById($concertId)")
                val concert = concertRepository.getConcertById(concertId)
                
                if (concert != null) {
                    Log.d(TAG, "loadConcert: Concert found - title: ${concert.title}, tracks count: ${concert.tracks.size}")
                    concert.tracks.forEachIndexed { index, track ->
                        Log.d(TAG, "loadConcert: Track $index - title: ${track.displayTitle}, audioFile: ${track.audioFile?.filename}")
                    }
                    
                    _currentConcert.value = concert
                    
                    // Create playlist from concert tracks
                    val playlist = concert.tracks.mapIndexed { index, track ->
                        PlaylistItem(
                            concertIdentifier = concert.identifier,
                            track = track,
                            position = index
                        )
                    }
                    _currentPlaylist.value = playlist
                    _playlistTitle.value = concert.displayTitle
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tracks = concert.tracks,
                        currentTrackIndex = 0,
                        isPlaylistMode = true,
                        playlistSize = playlist.size
                    )
                    
                    // Automatically start playing the first track
                    if (concert.tracks.isNotEmpty()) {
                        Log.d(TAG, "loadConcert: Auto-playing first track")
                        playTrack(0)
                    }
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
                    playerRepository.playTrack(
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
            playerRepository.pause()
        } else {
            // If no track is currently loaded, start the first track
            val currentTrack = _uiState.value.currentTrack
            if (currentTrack == null && _uiState.value.tracks.isNotEmpty()) {
                Log.d(TAG, "playPause: No current track, starting first track")
                playTrack(0)
            } else {
                Log.d(TAG, "playPause: Resuming playback")
                playerRepository.play()
            }
        }
    }
    
    fun skipToNext() {
        val currentIndex = _uiState.value.currentTrackIndex
        val playlist = _currentPlaylist.value
        
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
        val playlist = _currentPlaylist.value
        
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
        playerRepository.seekTo(position)
    }
    
    fun updatePosition() {
        playerRepository.updatePosition()
    }
    
    // Playlist management methods
    
    /**
     * Set a custom playlist for playback
     * @param playlist List of PlaylistItem to set as current playlist
     * @param title Optional title for the playlist
     */
    fun setPlaylist(playlist: List<PlaylistItem>, title: String? = null) {
        Log.d(TAG, "setPlaylist: Setting playlist with ${playlist.size} items, title: $title")
        _currentPlaylist.value = playlist
        _playlistTitle.value = title
        
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
        return _currentPlaylist.value
    }
    
    /**
     * Navigate to a specific track in the current playlist
     * @param playlistIndex Index of the track in the playlist to navigate to
     */
    fun navigateToTrack(playlistIndex: Int) {
        Log.d(TAG, "navigateToTrack: Navigating to playlist index $playlistIndex")
        val playlist = _currentPlaylist.value
        
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
        val currentPlaylist = _currentPlaylist.value.toMutableList()
        val newItem = playlistItem.copy(position = currentPlaylist.size)
        currentPlaylist.add(newItem)
        
        _currentPlaylist.value = currentPlaylist
        
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
        val currentPlaylist = _currentPlaylist.value.toMutableList()
        
        if (playlistIndex in currentPlaylist.indices) {
            currentPlaylist.removeAt(playlistIndex)
            
            // Update positions for remaining items
            currentPlaylist.forEachIndexed { index, item ->
                currentPlaylist[index] = item.copy(position = index)
            }
            
            _currentPlaylist.value = currentPlaylist
            
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
        _currentPlaylist.value = emptyList()
        _playlistTitle.value = null
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
                playerRepository.playTrack(
                    url = downloadUrl,
                    title = track.displayTitle,
                    artist = _playlistTitle.value ?: _currentConcert.value?.title
                )
                
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                Log.e(TAG, "playTrackFromPlaylist: Exception calling playerRepository.playTrack", e)
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
        Log.d(TAG, "onCleared: Releasing player resources")
        playerRepository.release()
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