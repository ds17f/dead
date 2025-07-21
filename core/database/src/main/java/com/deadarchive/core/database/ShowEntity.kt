package com.deadarchive.core.database

import com.deadarchive.core.model.util.VenueUtil

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
    
    // Library state - timestamp when added to library, null if not in library
    val addedToLibraryAt: Long? = null,
    
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
            isInLibrary = addedToLibraryAt != null
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
                addedToLibraryAt = if (show.isInLibrary) System.currentTimeMillis() else null
            )
        }
        
        // Helper to generate show ID from date and venue
        fun generateShowId(date: String, venue: String?): String {
            return "${date}_${VenueUtil.normalizeVenue(venue)}"
        }
    }
}