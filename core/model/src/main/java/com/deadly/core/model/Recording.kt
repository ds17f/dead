package com.deadly.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recording(
    @SerialName("identifier")
    val identifier: String, // Archive.org recording identifier
    
    @SerialName("title")
    val title: String,
    
    // Recording-specific metadata
    @SerialName("source")
    val source: String? = null, // SBD, AUD, FM, etc.
    
    @SerialName("taper")
    val taper: String? = null, // Person who recorded the show
    
    @SerialName("transferer")
    val transferer: String? = null, // Person who transferred the recording
    
    @SerialName("lineage")
    val lineage: String? = null, // Equipment chain used for recording
    
    @SerialName("description")
    val description: String? = null, // Recording-specific description
    
    // Archive.org upload metadata
    @SerialName("uploader")
    val uploader: String? = null,
    
    @SerialName("addeddate")
    val addedDate: String? = null,
    
    @SerialName("publicdate")
    val publicDate: String? = null,
    
    // Concert reference
    val concertDate: String, // YYYY-MM-DD format - links to Concert
    val concertVenue: String? = null, // For grouping recordings by concert
    
    @SerialName("coverage")
    val concertLocation: String? = null, // City, State format
    
    // Audio content  
    val tracks: List<Track> = emptyList(),
    val audioFiles: List<AudioFile> = emptyList(),
    
    // UI state
    val isDownloaded: Boolean = false,
    
    // Rating information (optional)  
    val rating: Float? = null,                        // Weighted rating (for internal ranking)
    val rawRating: Float? = null,                     // Simple average (for display)
    val ratingConfidence: Float? = null,
    val reviewCount: Int? = null,                     // Number of reviews
    val sourceType: String? = null,                   // SBD, AUD, MATRIX, etc.
    val ratingDistribution: Map<Int, Int>? = null,    // Distribution {1: 7, 2: 6, ...}
    val highRatings: Int? = null,                     // Count of 4-5★ reviews
    val lowRatings: Int? = null,                      // Count of 1-2★ reviews
    
    // Soft delete fields (recording-level)
    val isMarkedForDeletion: Boolean = false,
    val deletionTimestamp: Long? = null
) {
    val displayTitle: String
        get() = if (concertVenue?.isNotBlank() == true && concertDate.isNotBlank()) {
            "$concertDate - $concertVenue"
        } else if (concertDate.isNotBlank()) {
            concertDate
        } else {
            title
        }
        
    val displaySource: String
        get() = source ?: "Unknown Source"
        
    val displayTaper: String
        get() = taper ?: "Unknown Taper"
        
    val archiveUrl: String
        get() = "https://archive.org/details/$identifier"
        
    val recordingQuality: String
        get() = when (cleanSource?.uppercase()) {
            "SBD" -> "Soundboard"
            "AUD" -> "Audience" 
            "FM" -> "FM Broadcast"
            "MATRIX" -> "Matrix"
            else -> cleanSource ?: "Unknown"
        }
    
    val cleanSource: String?
        get() {
            val clean = source?.replace(Regex("<[^>]*>"), "")?.trim()
            return when {
                clean == null -> null
                clean.contains("Soundboard", ignoreCase = true) -> "SBD"
                clean.startsWith("SBD", ignoreCase = true) -> "SBD"  // Handles "SBD -> Cassette Master"
                clean.contains("Audience", ignoreCase = true) -> "AUD"
                clean.startsWith("AUD", ignoreCase = true) -> "AUD"  // Handles "AUD -> ..."
                clean.contains("Matrix", ignoreCase = true) -> "MATRIX"
                clean.startsWith("MATRIX", ignoreCase = true) -> "MATRIX"
                clean.contains("FM", ignoreCase = true) -> "FM"
                clean.startsWith("FM", ignoreCase = true) -> "FM"
                else -> clean
            }
        }
    
    // Rating properties
    val hasRating: Boolean
        get() = rawRating != null && rawRating > 0f
    
    val isHighlyRated: Boolean
        get() = rawRating != null && rawRating >= 4.0f
    
    // NEW: Enhanced rating properties
    val displayRating: String
        get() = rawRating?.let { "%.1f★".format(it) } ?: "Not Rated"
    
    val ratingContext: String
        get() = when {
            highRatings == null || lowRatings == null -> ""
            highRatings > 0 && lowRatings > 0 -> "Mixed reactions"
            highRatings > lowRatings -> "Generally loved"
            lowRatings > highRatings -> "Generally disliked"
            else -> ""
        }
    
    val hasRawRating: Boolean
        get() = rawRating != null && rawRating > 0
    
    val isPolarizing: Boolean
        get() = highRatings != null && lowRatings != null && 
                highRatings > 0 && lowRatings > 0 &&
                kotlin.math.abs(highRatings - lowRatings) <= 3
    
    val isReliablyRated: Boolean
        get() = rating != null && ratingConfidence != null && ratingConfidence >= 0.7f
    
    val stars: Int?
        get() = rawRating?.let { kotlin.math.round(it).toInt().coerceIn(1, 5) }
}