package com.deadly.feature.playlist.model

import com.deadly.core.model.CurrentTrackInfo

/**
 * MiniPlayerV2UiState - UI state representation for V2 mini-player
 * 
 * Clean V2 UI state following established patterns from other V2 components.
 * Isolates UI concerns from domain models and service layer.
 */
data class MiniPlayerV2UiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val trackInfo: CurrentTrackInfo? = null,
    val recordingId: String? = null,
    val error: String? = null,
    val isVisible: Boolean = false // Only show when there's current track info
) {
    /**
     * Computed property to determine if mini-player should be displayed
     */
    val shouldShow: Boolean
        get() = trackInfo != null && !isLoading && error == null
    
    /**
     * Formatted current position for display
     */
    val formattedPosition: String
        get() {
            val totalSeconds = currentPosition / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    
    /**
     * Formatted duration for display  
     */
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}