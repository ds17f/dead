package com.deadly.v2.feature.player.screens.main.models

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * V2 Player ViewModel - Phase 2 Mock Implementation
 * 
 * Provides static mock data to test UI layout and interactions.
 * Will be replaced with real service integration in Phase 3.
 */
class PlayerViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "PlayerViewModel"
    }
    
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            // Mock track data
            trackDisplayInfo = TrackDisplayInfo(
                title = "Scarlet Begonias",
                artist = "Grateful Dead",
                album = "May 8, 1977 - Barton Hall",
                duration = "11:23",
                artwork = null // No artwork for Phase 2
            ),
            // Mock progress data
            progressDisplayInfo = ProgressDisplayInfo(
                currentPosition = "4:32",
                totalDuration = "11:23",
                progressPercentage = 0.4f
            ),
            // Mock playback state
            isPlaying = false,
            isLoading = false,
            hasNext = true,
            hasPrevious = true,
            error = null
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    /**
     * Load recording - Phase 2 just updates mock data
     */
    fun loadRecording(recordingId: String) {
        Log.d(TAG, "Loading recording: $recordingId (mock)")
        
        // Update mock data based on recordingId
        _uiState.value = _uiState.value.copy(
            trackDisplayInfo = _uiState.value.trackDisplayInfo.copy(
                title = "Mock Track for $recordingId",
                album = "Mock Album - Test Date"
            )
        )
    }
    
    /**
     * Toggle play/pause
     */
    fun onPlayPauseClicked() {
        Log.d(TAG, "Play/pause clicked - mock")
        _uiState.value = _uiState.value.copy(
            isPlaying = !_uiState.value.isPlaying
        )
    }
    
    /**
     * Seek to next track
     */
    fun onNextClicked() {
        Log.d(TAG, "Next clicked - mock")
        // Mock: Change track info
        _uiState.value = _uiState.value.copy(
            trackDisplayInfo = _uiState.value.trackDisplayInfo.copy(
                title = "Next Mock Track",
                duration = "8:45"
            ),
            progressDisplayInfo = _uiState.value.progressDisplayInfo.copy(
                currentPosition = "0:00",
                totalDuration = "8:45",
                progressPercentage = 0.0f
            )
        )
    }
    
    /**
     * Seek to previous track  
     */
    fun onPreviousClicked() {
        Log.d(TAG, "Previous clicked - mock")
        // Mock: Change track info
        _uiState.value = _uiState.value.copy(
            trackDisplayInfo = _uiState.value.trackDisplayInfo.copy(
                title = "Previous Mock Track",
                duration = "12:15"
            ),
            progressDisplayInfo = _uiState.value.progressDisplayInfo.copy(
                currentPosition = "0:00", 
                totalDuration = "12:15",
                progressPercentage = 0.0f
            )
        )
    }
    
    /**
     * Seek to position
     */
    fun onSeek(position: Float) {
        Log.d(TAG, "Seek to $position - mock")
        val totalSeconds = parseDurationToSeconds(_uiState.value.progressDisplayInfo.totalDuration)
        val newPositionSeconds = (totalSeconds * position).toInt()
        val newPositionString = formatSecondsToString(newPositionSeconds)
        
        _uiState.value = _uiState.value.copy(
            progressDisplayInfo = _uiState.value.progressDisplayInfo.copy(
                currentPosition = newPositionString,
                progressPercentage = position
            )
        )
    }
    
    /**
     * Helper: Parse MM:SS to seconds
     */
    private fun parseDurationToSeconds(duration: String): Int {
        return try {
            val parts = duration.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toInt()
                val seconds = parts[1].toInt()
                minutes * 60 + seconds
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Helper: Format seconds to MM:SS string
     */
    private fun formatSecondsToString(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
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