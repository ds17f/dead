package com.deadly.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ConcertSet(
    val setNumber: Int, // 1, 2, 3, etc.
    val setName: String, // "Set I", "Set II", "Encore", etc.
    val songs: List<String> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val totalDuration: Long = 0L // in seconds
) {
    val displayName: String
        get() = when (setNumber) {
            1 -> "Set I"
            2 -> "Set II"
            3 -> "Set III"
            else -> if (setName.lowercase().contains("encore")) "Encore" else setName
        }
    
    val songCount: Int
        get() = songs.size
    
    val trackCount: Int
        get() = tracks.size
        
    val formattedDuration: String
        get() {
            val hours = totalDuration / 3600
            val minutes = (totalDuration % 3600) / 60
            return if (hours > 0) {
                String.format("%d:%02d", hours, minutes)
            } else {
                String.format("%d min", minutes)
            }
        }
}