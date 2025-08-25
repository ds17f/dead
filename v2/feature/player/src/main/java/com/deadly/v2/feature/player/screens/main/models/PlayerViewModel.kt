package com.deadly.v2.feature.player.screens.main.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.api.player.PlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * V2 Player ViewModel - Real MediaController Integration
 * 
 * Uses PlayerService to provide reactive state from MediaControllerRepository.
 * Handles all player UI interactions and delegates to centralized media control.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerService: PlayerService
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    // Reactive UI state from PlayerService flows
    val uiState: StateFlow<PlayerUiState> = combine(
        playerService.currentTrackTitle,
        playerService.currentAlbum,
        playerService.currentPosition,
        playerService.duration,
        playerService.progress,
        playerService.isPlaying,
        playerService.hasNext,
        playerService.hasPrevious
    ) { flows ->
        val title = flows[0] as String?
        val album = flows[1] as String?
        val position = flows[2] as Long
        val duration = flows[3] as Long
        val progress = flows[4] as Float
        val isPlaying = flows[5] as Boolean
        val hasNext = flows[6] as Boolean
        val hasPrevious = flows[7] as Boolean
        PlayerUiState(
            trackDisplayInfo = TrackDisplayInfo(
                title = title ?: "Unknown Track",
                artist = "Grateful Dead", // TODO: Extract from metadata
                album = album ?: "Unknown Album",
                duration = playerService.formatDuration(duration),
                artwork = null // TODO: Add artwork support
            ),
            progressDisplayInfo = ProgressDisplayInfo(
                currentPosition = playerService.formatPosition(position),
                totalDuration = playerService.formatDuration(duration),
                progressPercentage = progress
            ),
            isPlaying = isPlaying,
            isLoading = false, // TODO: Add loading state from service
            hasNext = hasNext,
            hasPrevious = hasPrevious,
            error = null // TODO: Add error state from service
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState(
            trackDisplayInfo = TrackDisplayInfo(
                title = "Loading...",
                artist = "Grateful Dead",
                album = "Loading...",
                duration = "0:00",
                artwork = null
            ),
            progressDisplayInfo = ProgressDisplayInfo(
                currentPosition = "0:00",
                totalDuration = "0:00",
                progressPercentage = 0.0f
            ),
            isPlaying = false,
            isLoading = true,
            hasNext = false,
            hasPrevious = false,
            error = null
        )
    )
    
    /**
     * Load recording - No-op since state comes from MediaController
     */
    fun loadRecording(recordingId: String) {
        Log.d(TAG, "Load recording: $recordingId - state comes from MediaController")
        // MediaController handles track loading, we just observe state
    }
    
    /**
     * Toggle play/pause
     */
    fun onPlayPauseClicked() {
        Log.d(TAG, "Play/pause clicked")
        viewModelScope.launch {
            try {
                playerService.togglePlayPause()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling play/pause", e)
            }
        }
    }
    
    /**
     * Seek to next track
     */
    fun onNextClicked() {
        Log.d(TAG, "Next clicked")
        viewModelScope.launch {
            try {
                playerService.seekToNext()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to next", e)
            }
        }
    }
    
    /**
     * Seek to previous track  
     */
    fun onPreviousClicked() {
        Log.d(TAG, "Previous clicked")
        viewModelScope.launch {
            try {
                playerService.seekToPrevious()
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to previous", e)
            }
        }
    }
    
    /**
     * Seek to position
     */
    fun onSeek(position: Float) {
        Log.d(TAG, "Seek to $position")
        viewModelScope.launch {
            try {
                // Get current duration and convert percentage to milliseconds
                val durationMs = playerService.duration.value
                val positionMs = (durationMs * position).toLong()
                playerService.seekToPosition(positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking to position", e)
            }
        }
    }
    
    /**
     * Get debug metadata for inspection panel
     */
    suspend fun getDebugMetadata(): Map<String, String?> {
        return try {
            playerService.getDebugMetadata()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting debug metadata", e)
            mapOf("error" to "Failed to get debug metadata: ${e.message}")
        }
    }
}

/**
 * UI State for Player screen
 */
data class PlayerUiState(
    val trackDisplayInfo: TrackDisplayInfo,
    val progressDisplayInfo: ProgressDisplayInfo,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val error: String? = null
)

/**
 * Display model for track information
 */
data class TrackDisplayInfo(
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val artwork: String? = null
)

/**
 * Display model for progress information
 */
data class ProgressDisplayInfo(
    val currentPosition: String,
    val totalDuration: String,
    val progressPercentage: Float // 0.0 to 1.0
)