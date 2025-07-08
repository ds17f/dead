package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Show(
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
    val isInLibrary: Boolean = false,
    
    // Rating information (optional)
    val rating: Float? = null,
    val ratingConfidence: Float? = null
) {
    // Unique identifier combining date and venue
    val showId: String
        get() {
            // Extract just the date part (YYYY-MM-DD) from potentially timestamped dates
            val dateOnly = if (date.contains("T")) {
                date.substringBefore("T")
            } else {
                date
            }
            return "${dateOnly}_${venue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and") ?: "Unknown"}"
        }
    
    val displayTitle: String
        get() = "${displayVenue} - $date"
    
    val displayLocation: String
        get() = location ?: "Unknown Location"
    
    val displayVenue: String
        get() = venue ?: "Unknown Venue"
    
    val displayDate: String
        get() = if (date.contains("T")) {
            android.util.Log.w("Show", "WARNING: Show still has ISO timestamp date: '$date' for venue: '$venue'")
            date.substringBefore("T")
        } else {
            date
        }
        
    // Recording statistics
    val recordingCount: Int
        get() = recordings.size
        
    val availableSources: List<String>
        get() = recordings.mapNotNull { it.cleanSource }.distinct().sorted()
        
    val bestRecording: Recording?
        get() = recordings.minByOrNull { recording ->
            // Priority: SBD > MATRIX > FM > AUD
            when (recording.cleanSource?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
        }
        
    val hasMultipleRecordings: Boolean
        get() = recordings.size > 1
    
    // Rating properties
    val hasRating: Boolean
        get() = rating != null
    
    val isHighlyRated: Boolean
        get() = rating != null && rating >= 4.0f
    
    val isReliablyRated: Boolean
        get() = rating != null && ratingConfidence != null && ratingConfidence >= 0.7f
    
    val stars: Int?
        get() = rating?.let { kotlin.math.round(it).toInt().coerceIn(1, 5) }
}