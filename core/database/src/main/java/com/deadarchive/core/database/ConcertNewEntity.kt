package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadarchive.core.model.ConcertNew
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
data class ConcertNewEntity(
    @PrimaryKey
    val concertId: String, // Unique identifier: "${date}_${venue_normalized}"
    
    // Concert identity
    val date: String, // YYYY-MM-DD format
    val venue: String?,
    val location: String?, // City, State format
    val year: String?,
    
    // Concert information shared across recordings
    val setlistRaw: String?,
    val setsJson: String? = null,
    
    // UI state
    val isFavorite: Boolean = false,
    
    // Cache management
    val cachedTimestamp: Long = System.currentTimeMillis()
) {
    fun toConcertNew(recordings: List<com.deadarchive.core.model.Recording> = emptyList()): ConcertNew {
        val sets = setsJson?.let { json ->
            try {
                Json.decodeFromString<List<ConcertSet>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return ConcertNew(
            date = date,
            venue = venue,
            location = location,
            year = year,
            setlistRaw = setlistRaw,
            sets = sets,
            recordings = recordings,
            isFavorite = isFavorite
        )
    }
    
    companion object {
        fun fromConcertNew(concert: ConcertNew): ConcertNewEntity {
            val setsJson = if (concert.sets.isNotEmpty()) {
                Json.encodeToString(concert.sets)
            } else null
            
            return ConcertNewEntity(
                concertId = concert.concertId,
                date = concert.date,
                venue = concert.venue,
                location = concert.location,
                year = concert.year,
                setlistRaw = concert.setlistRaw,
                setsJson = setsJson,
                isFavorite = concert.isFavorite
            )
        }
        
        // Helper to generate concert ID from date and venue
        fun generateConcertId(date: String, venue: String?): String {
            return "${date}_${venue?.replace(" ", "_")?.replace(",", "") ?: "Unknown"}"
        }
    }
}