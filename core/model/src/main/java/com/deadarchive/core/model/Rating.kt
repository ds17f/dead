package com.deadarchive.core.model

/**
 * Data class representing a rating for a specific recording.
 */
data class RecordingRating(
    val identifier: String,
    val rating: Float,
    val reviewCount: Int,
    val sourceType: String, // SBD, AUD, MATRIX, etc.
    val confidence: Float   // 0.0 to 1.0 based on review count and source quality
) {
    /**
     * Get a user-friendly description of the source type.
     */
    val sourceDescription: String
        get() = when (sourceType.uppercase()) {
            "SBD" -> "Soundboard"
            "AUD" -> "Audience"
            "MATRIX" -> "Matrix Mix"
            "FM" -> "FM Broadcast"
            "REMASTER" -> "Remastered"
            else -> sourceType
        }
    
    /**
     * Check if this rating is considered reliable (high confidence).
     */
    val isReliable: Boolean
        get() = confidence >= 0.7f && reviewCount >= 3
    
    /**
     * Get star rating as integer (1-5 stars).
     */
    val stars: Int
        get() = kotlin.math.round(rating).toInt().coerceIn(1, 5)
}

/**
 * Data class representing an aggregated rating for an entire show.
 */
data class ShowRating(
    val showKey: String,    // Unique identifier for the show
    val date: String,       // YYYY-MM-DD format
    val venue: String,
    val rating: Float,
    val confidence: Float,  // 0.0 to 1.0 based on total reviews and source quality
    val bestRecordingId: String,
    val recordingCount: Int
) {
    /**
     * Get a formatted display date.
     */
    val displayDate: String
        get() = try {
            val parts = date.split("-")
            "${parts[1]}/${parts[2]}/${parts[0]}"
        } catch (e: Exception) {
            date
        }
    
    /**
     * Check if this show rating is considered reliable.
     */
    val isReliable: Boolean
        get() = confidence >= 0.7f
    
    /**
     * Get star rating as integer (1-5 stars).
     */
    val stars: Int
        get() = kotlin.math.round(rating).toInt().coerceIn(1, 5)
    
    /**
     * Get confidence level as a user-friendly string.
     */
    val confidenceLevel: String
        get() = when {
            confidence >= 0.9f -> "Very High"
            confidence >= 0.7f -> "High"
            confidence >= 0.5f -> "Medium"
            confidence >= 0.3f -> "Low"
            else -> "Very Low"
        }
}

/**
 * Extension for Show model to include rating information.
 */
data class RatedShow(
    val show: Show,
    val rating: ShowRating?
) {
    /**
     * Get the overall rating for this show, or null if not rated.
     */
    val overallRating: Float?
        get() = rating?.rating
    
    /**
     * Check if this show has a reliable rating.
     */
    val hasReliableRating: Boolean
        get() = rating?.isReliable == true
    
    /**
     * Get star rating as integer, or null if not rated.
     */
    val stars: Int?
        get() = rating?.stars
}

/**
 * Extension for Recording model to include rating information.
 */
data class RatedRecording(
    val recording: Recording,
    val rating: RecordingRating?
) {
    /**
     * Get the rating for this recording, or null if not rated.
     */
    val recordingRating: Float?
        get() = rating?.rating
    
    /**
     * Check if this recording has a reliable rating.
     */
    val hasReliableRating: Boolean
        get() = rating?.isReliable == true
    
    /**
     * Get star rating as integer, or null if not rated.
     */
    val stars: Int?
        get() = rating?.stars
}