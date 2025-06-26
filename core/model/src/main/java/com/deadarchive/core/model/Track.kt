package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    @SerialName("name")
    val filename: String,
    
    @SerialName("title")
    val title: String? = null,
    
    @SerialName("track")
    val trackNumber: String? = null,
    
    @SerialName("length")
    val durationSeconds: String? = null, // Duration in seconds as string
    
    val setNumber: Int? = null,
    val positionInSet: Int? = null,
    
    // Audio file reference
    val audioFile: AudioFile? = null,
    
    // Playback state
    val isCurrentlyPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val playbackPosition: Long = 0L // in milliseconds
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } 
            ?: filename.substringBeforeLast('.').let { name ->
                // Try to extract song name from filename patterns like "gd77-05-08d1t01.flac"
                val parts = name.split('t', 'd')
                if (parts.size > 1) {
                    "Track ${parts.last()}"
                } else {
                    name.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
                }
            }
    
    val displayTrackNumber: String
        get() = trackNumber ?: "?"
    
    val duration: Long
        get() = durationSeconds?.toDoubleOrNull()?.toLong() ?: 0L
    
    val formattedDuration: String
        get() {
            val totalSeconds = duration
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    
    val displayPosition: String
        get() = trackNumber?.let { "Track $it" } ?: ""
}