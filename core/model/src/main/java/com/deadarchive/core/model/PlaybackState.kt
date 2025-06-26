package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L, // in milliseconds
    val duration: Long = 0L, // in milliseconds
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val playlist: List<PlaylistItem> = emptyList(),
    val currentPlaylistIndex: Int = -1,
    val error: String? = null
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    val formattedPosition: String
        get() = formatDuration(currentPosition)
    
    val formattedDuration: String
        get() = formatDuration(duration)
    
    val hasNext: Boolean
        get() = currentPlaylistIndex < playlist.size - 1
    
    val hasPrevious: Boolean
        get() = currentPlaylistIndex > 0
    
    val canPlay: Boolean
        get() = currentTrack != null && !isBuffering && error == null
    
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}