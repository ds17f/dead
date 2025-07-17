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
    
    // Streaming URL for playback/download
    val streamingUrl: String? = null,
    
    // Playback state
    val isCurrentlyPlaying: Boolean = false,
    val isInLibrary: Boolean = false,
    val playbackPosition: Long = 0L // in milliseconds
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } 
            ?: extractSongFromFilename(filename)
    
    val displayTrackNumber: String
        get() = trackNumber ?: "?"
    
    val duration: Long
        get() = audioFile?.duration?.takeIf { it > 0 } ?: parseDuration(durationSeconds) ?: 0L
    
    val formattedDuration: String
        get() {
            val totalSeconds = duration
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
    
    val displayPosition: String
        get() = trackNumber?.let { "Track $it" } ?: ""
    
    companion object {
        /**
         * Extract song name from filename with improved logic
         */
        fun extractSongFromFilename(filename: String): String {
            val nameWithoutExtension = filename.substringBeforeLast('.')
            
            // Handle various filename patterns
            return when {
                // Pattern: "01-Track Name.flac" or "01 Track Name.flac"
                nameWithoutExtension.matches(Regex("^\\d+[-\\s](.+)")) -> {
                    nameWithoutExtension.replace(Regex("^\\d+[-\\s]"), "")
                        .replace("-", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                }
                
                // Pattern: "gd77-05-08d1t01.flac" - Archive.org pattern
                nameWithoutExtension.contains(Regex("d\\d+t\\d+")) -> {
                    val parts = nameWithoutExtension.split('t', 'd')
                    if (parts.size > 1) {
                        "Track ${parts.last()}"
                    } else {
                        nameWithoutExtension.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                            .trim()
                            .split(" ")
                            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    }
                }
                
                // Generic cleanup for other patterns
                else -> {
                    nameWithoutExtension.replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        .takeIf { it.isNotBlank() } ?: "Unknown Track"
                }
            }
        }
        
        /**
         * Parse duration from various formats (MM:SS or seconds as string)
         */
        fun parseDuration(durationString: String?): Long? {
            if (durationString.isNullOrBlank()) return null
            
            return try {
                // Try MM:SS format first
                if (durationString.contains(":")) {
                    val parts = durationString.split(":")
                    if (parts.size == 2) {
                        val minutes = parts[0].toIntOrNull() ?: 0
                        val seconds = parts[1].toIntOrNull() ?: 0
                        return (minutes * 60 + seconds).toLong()
                    }
                }
                
                // Fallback to seconds as string
                durationString.toDoubleOrNull()?.toLong()
            } catch (e: Exception) {
                null
            }
        }
    }
}