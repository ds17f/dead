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
        Index(value = ["year"])
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
            
            return ShowEntity(
                showId = show.showId,
                date = show.date,
                venue = show.venue,
                location = show.location,
                year = show.year,
                setlistRaw = show.setlistRaw,
                setsJson = setsJson,
                isInLibrary = show.isInLibrary
            )
        }
        
        // Helper to generate show ID from date and venue
        fun generateShowId(date: String, venue: String?): String {
            return "${date}_${venue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and") ?: "Unknown"}"
        }
    }
}