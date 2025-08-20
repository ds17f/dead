package com.deadly.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a complete setlist for a show.
 */
@Serializable
data class Setlist(
    @SerialName("show_id")
    val showId: String,
    
    @SerialName("date")
    val date: String, // YYYY-MM-DD format
    
    @SerialName("venue_id")
    val venueId: String? = null,
    
    @SerialName("venue_line")
    val venueLine: String? = null,
    
    @SerialName("source")
    val source: String, // "cmu" or "gdsets"
    
    @SerialName("sets")
    val sets: Map<String, List<String>> = emptyMap(), // set1, set2, encore, etc.
    
    @SerialName("songs")
    val songs: List<SetlistSong> = emptyList(), // Songs with IDs
    
    @SerialName("raw_content")
    val rawContent: String? = null,
    
    @SerialName("cmu_raw_content")
    val cmuRawContent: String? = null,
    
    @SerialName("cmu_venue_line")
    val cmuVenueLine: String? = null
) {
    /**
     * Get display date in user-friendly format.
     */
    val displayDate: String
        get() = try {
            val parts = date.split("-")
            "${parts[1]}/${parts[2]}/${parts[0]}"
        } catch (e: Exception) {
            date
        }
    
    /**
     * Get the primary venue information.
     */
    val displayVenue: String
        get() = venueLine ?: cmuVenueLine ?: "Unknown Venue"
    
    /**
     * Check if this setlist has song data.
     */
    val hasSongs: Boolean
        get() = songs.isNotEmpty()
    
    /**
     * Get all unique song IDs in this setlist.
     */
    val songIds: Set<String>
        get() = songs.mapNotNull { it.songId }.toSet()
    
    /**
     * Get total number of songs in the setlist.
     */
    val totalSongs: Int
        get() = songs.size
    
    /**
     * Check if this is a GDSets setlist (higher quality).
     */
    val isGDSets: Boolean
        get() = source == "gdsets"
    
    /**
     * Get songs grouped by set.
     */
    val songsBySet: Map<String, List<SetlistSong>>
        get() = songs.groupBy { it.setName ?: "unknown" }
    
    /**
     * Get formatted set information.
     */
    val setInfo: String
        get() {
            val setCounts = songsBySet.entries
                .filter { it.key != "unknown" }
                .sortedBy { it.key }
                .joinToString(", ") { "${it.key}: ${it.value.size}" }
            return if (setCounts.isNotEmpty()) setCounts else "No set information"
        }
}

/**
 * Data class representing a song within a setlist.
 */
@Serializable
data class SetlistSong(
    @SerialName("song_name")
    val songName: String,
    
    @SerialName("song_id")
    val songId: String? = null,
    
    @SerialName("set_name")
    val setName: String? = null, // set1, set2, encore, etc.
    
    @SerialName("position")
    val position: Int? = null,
    
    @SerialName("is_segue")
    val isSegue: Boolean = false,
    
    @SerialName("segue_type")
    val segueType: String? = null // ">" or "->"
) {
    /**
     * Get display name with segue information.
     */
    val displayName: String
        get() = when {
            isSegue && segueType != null -> "$songName $segueType"
            else -> songName
        }
    
    /**
     * Check if this song was successfully matched to the songs database.
     */
    val isMatched: Boolean
        get() = songId != null
}