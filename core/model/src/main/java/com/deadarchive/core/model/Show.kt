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
    val rating: Float? = null,                        // Weighted rating (for internal ranking)
    val rawRating: Float? = null,                     // Simple average (for display)
    val ratingConfidence: Float? = null,
    val totalHighRatings: Int? = null,                // Total 4-5★ reviews across all recordings
    val totalLowRatings: Int? = null,                 // Total 1-2★ reviews across all recordings
    val bestRecordingId: String? = null              // Identifier of best recording
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
            return "${dateOnly}_${venue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and")?.replace("'", "") ?: "Unknown"}"
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
            // Multi-tier priority system for selecting the best recording:
            //
            // Tier 1: Rating Status (most important)
            //   - Rated recordings always preferred over unrated ones
            //   - Example: 2.5★ SBD beats unrated SBD
            //
            // Tier 2: Source Type (within same rating tier)  
            //   - SBD > MATRIX > FM > AUD > Unknown
            //   - Example: Rated SBD beats rated AUD
            //
            // Tier 3: Rating Value (tie-breaker)
            //   - Higher rating wins within same rating+source group
            //   - Example: 4.2★ SBD beats 3.1★ SBD
            
            val ratingPriority = if (recording.hasRawRating) 0 else 1 // Rated recordings first
            
            val sourcePriority = when (recording.cleanSource?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2  
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
            
            // Small rating bonus for tie-breaking (inverted since minByOrNull picks smallest)
            val ratingValue = recording.rawRating ?: 0f
            val ratingBonus = (5f - ratingValue) / 10f // 0.0-0.4 range
            
            // Combined score: rating status dominates, then source, then rating value
            ratingPriority * 10 + sourcePriority + ratingBonus
        }
        
    val hasMultipleRecordings: Boolean
        get() = recordings.size > 1
    
    // Rating properties
    val hasRating: Boolean
        get() = rawRating != null && rawRating > 0f
    
    val isHighlyRated: Boolean
        get() = rawRating != null && rawRating >= 4.0f
    
    val isReliablyRated: Boolean
        get() = rating != null && ratingConfidence != null && ratingConfidence >= 0.7f
    
    val stars: Int?
        get() = rawRating?.let { kotlin.math.round(it).toInt().coerceIn(1, 5) }
    
    // NEW: Enhanced rating properties
    val displayRating: String
        get() = rawRating?.let { "%.1f★".format(it) } ?: "Not Rated"
    
    val ratingContext: String
        get() = when {
            totalHighRatings == null || totalLowRatings == null -> ""
            totalHighRatings > 0 && totalLowRatings > 0 -> "Mixed reactions"
            totalHighRatings > totalLowRatings -> "Fan favorite"
            totalLowRatings > totalHighRatings -> "Polarizing"
            else -> ""
        }
    
    val hasRawRating: Boolean
        get() = rawRating != null && rawRating > 0
    
    val isPolarizing: Boolean
        get() = totalHighRatings != null && totalLowRatings != null &&
                totalHighRatings > 0 && totalLowRatings > 0 &&
                kotlin.math.abs(totalHighRatings - totalLowRatings) <= 5
    
    val ratingDescription: String
        get() = when {
            !hasRawRating -> "No ratings available"
            ratingContext.isNotEmpty() -> "$displayRating (${recordings.sumOf { it.reviewCount ?: 0 }} reviews) • $ratingContext"
            else -> "$displayRating (${recordings.sumOf { it.reviewCount ?: 0 }} reviews)"
        }
}