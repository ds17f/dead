package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.ConcertRepository
import com.deadarchive.core.media.player.PlayerRepository
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
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
                    playerRepository.playbackState
                ) { isPlaying, position, duration, state ->
                    _uiState.value = _uiState.value.copy(
                        isPlaying = isPlaying,
                        currentPosition = position,
                        duration = duration,
                        playbackState = state
                    )
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tracks = concert.tracks,
                        currentTrackIndex = 0
                    )
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
        if (_uiState.value.isPlaying) {
            playerRepository.pause()
        } else {
            playerRepository.play()
        }
    }
    
    fun skipToNext() {
        val currentIndex = _uiState.value.currentTrackIndex
        val tracks = _uiState.value.tracks
        
        if (currentIndex < tracks.size - 1) {
            playTrack(currentIndex + 1)
        }
    }
    
    fun skipToPrevious() {
        val currentIndex = _uiState.value.currentTrackIndex
        
        if (currentIndex > 0) {
            playTrack(currentIndex - 1)
        }
    }
    
    fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }
    
    fun updatePosition() {
        playerRepository.updatePosition()
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
    val error: String? = null
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
}