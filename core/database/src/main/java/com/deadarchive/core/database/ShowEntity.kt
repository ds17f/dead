package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.ConcertSet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "concerts_new",
    indices = [
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["location"]),
        Index(value = ["year"]),
        Index(value = ["songNames"])
    ]
)
data class ShowEntity(
    @PrimaryKey
    val showId: String, // Unique identifier: "${date}_${venue_normalized}"
    
    // Show identity
    val date: String, // YYYY-MM-DD format
    val venue: String?,
    val location: String?, // City, State format
    val year: String?,
    
    // Show information shared across recordings
    val setlistRaw: String?,
    val setsJson: String? = null,
    val songNames: String? = null, // Denormalized comma-separated song names for search
    
    // UI state
    val isInLibrary: Boolean = false,
    
    // Cache management
    val cachedTimestamp: Long = System.currentTimeMillis()
) {
    fun toShow(recordings: List<com.deadarchive.core.model.Recording> = emptyList()): Show {
        val sets = setsJson?.let { json ->
            try {
                Json.decodeFromString<List<ConcertSet>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return Show(
            date = date,
            venue = venue,
            location = location,
            year = year,
            setlistRaw = setlistRaw,
            sets = sets,
            recordings = recordings,
            isInLibrary = isInLibrary
        )
    }
    
    companion object {
        fun fromShow(show: Show): ShowEntity {
            val setsJson = if (show.sets.isNotEmpty()) {
                Json.encodeToString(show.sets)
            } else null
            
            // Extract song names from sets for denormalized search
            val songNames = show.sets.flatMap { it.songs }.joinToString(", ")
            
            return ShowEntity(
                showId = show.showId,
                date = show.date,
                venue = show.venue,
                location = show.location,
                year = show.year,
                setlistRaw = show.setlistRaw,
                setsJson = setsJson,
                songNames = songNames.takeIf { it.isNotEmpty() },
                isInLibrary = show.isInLibrary
            )
        }
        
        // Helper to generate show ID from date and venue
        fun generateShowId(date: String, venue: String?): String {
            return "${date}_${normalizeVenue(venue)}"
        }
        
        /**
         * Normalize venue name to eliminate duplicates caused by inconsistent venue names.
         * This must match the logic in ShowRepository.normalizeVenue()
         */
        private fun normalizeVenue(venue: String?): String {
            if (venue.isNullOrBlank()) return "Unknown"
            
            return venue
                // Remove punctuation that causes issues
                .replace("'", "")      // Veterans' -> Veterans
                .replace("'", "")      // Smart quote
                .replace(".", "")      // U.C.S.B. -> UCSB
                .replace("\"", "")     // Remove quotes
                .replace("(", "_")     // Convert parens to underscores
                .replace(")", "_")
                
                // Normalize separators
                .replace(" - ", "_")   // Common separator
                .replace(" – ", "_")   // Em dash
                .replace(", ", "_")    // Comma separator
                .replace(" & ", "_and_")
                .replace("&", "_and_")
                
                // Standardize common word variations
                .replace("Theatre", "Theater", ignoreCase = true)
                .replace("Center", "Center", ignoreCase = true)  // Keep consistent
                .replace("Coliseum", "Coliseum", ignoreCase = true)
                
                // University abbreviations (most common cases)
                .replace(" University", "_U", ignoreCase = true)
                .replace(" College", "_C", ignoreCase = true)
                .replace(" State", "_St", ignoreCase = true)
                .replace("Memorial", "Mem", ignoreCase = true)
                .replace("Auditorium", "Aud", ignoreCase = true)
                .replace("Stadium", "Stad", ignoreCase = true)
                
                // Remove common filler words
                .replace(" The ", "_", ignoreCase = true)
                .replace("The ", "", ignoreCase = true)
                .replace(" of ", "_", ignoreCase = true)
                .replace(" at ", "_", ignoreCase = true)
                
                // Clean up and normalize
                .replace(Regex("\\s+"), "_")     // Any whitespace to underscore
                .replace(Regex("_+"), "_")       // Multiple underscores to single
                .trim('_')                       // Remove leading/trailing underscores
                .lowercase()                     // Consistent case
        }
    }
}