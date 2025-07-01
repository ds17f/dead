package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConcertNew(
    // Concert identity - unique combination of date + venue
    @SerialName("date")
    val date: String, // YYYY-MM-DD format
    
    @SerialName("venue")
    val venue: String? = null,
    
    @SerialName("coverage")
    val location: String? = null, // City, State format
    
    @SerialName("year")
    val year: String? = null,
    
    // Concert information shared across all recordings
    @SerialName("setlist")
    val setlistRaw: String? = null,
    
    val sets: List<ConcertSet> = emptyList(),
    
    // All recordings of this concert
    val recordings: List<Recording> = emptyList(),
    
    // UI state
    val isFavorite: Boolean = false
) {
    // Unique identifier combining date and venue
    val concertId: String
        get() = "${date}_${venue?.replace(" ", "_")?.replace(",", "") ?: "Unknown"}"
    
    val displayTitle: String
        get() = "${displayVenue} - $date"
    
    val displayLocation: String
        get() = location ?: "Unknown Location"
    
    val displayVenue: String
        get() = venue ?: "Unknown Venue"
    
    val displayDate: String
        get() = date
        
    // Recording statistics
    val recordingCount: Int
        get() = recordings.size
        
    val availableSources: List<String>
        get() = recordings.mapNotNull { it.source }.distinct().sorted()
        
    val bestRecording: Recording?
        get() = recordings.minByOrNull { recording ->
            // Priority: SBD > MATRIX > FM > AUD
            when (recording.source?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
        }
        
    val hasMultipleRecordings: Boolean
        get() = recordings.size > 1
}