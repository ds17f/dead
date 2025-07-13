package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadarchive.core.model.RecordingRating
import com.deadarchive.core.model.ShowRating

/**
 * Database entity for storing pre-computed recording ratings.
 */
@Entity(
    tableName = "recording_ratings",
    indices = [
        Index(value = ["identifier"], unique = true),
        Index(value = ["sourceType"]),
        Index(value = ["rating"])
    ]
)
data class RecordingRatingEntity(
    @PrimaryKey
    val identifier: String,
    val rating: Float,                           // Weighted rating (for internal ranking)
    val rawRating: Float,                        // Simple average (for display)
    val reviewCount: Int,
    val sourceType: String,                      // SBD, AUD, MATRIX, etc.
    val confidence: Float,                       // 0.0 to 1.0
    val distributionJson: String? = null,        // JSON string of rating distribution
    val highRatings: Int = 0,                    // Count of 4-5★ reviews
    val lowRatings: Int = 0,                     // Count of 1-2★ reviews
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toRecordingRating(): RecordingRating {
        return RecordingRating(
            identifier = identifier,
            rating = rating,
            rawRating = rawRating,
            reviewCount = reviewCount,
            sourceType = sourceType,
            confidence = confidence,
            ratingDistribution = distributionJson?.let { json ->
                try {
                    val map = mutableMapOf<Int, Int>()
                    val jsonObj = org.json.JSONObject(json)
                    for (key in jsonObj.keys()) {
                        map[key.toInt()] = jsonObj.getInt(key)
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap(),
            highRatings = highRatings,
            lowRatings = lowRatings
        )
    }
    
    companion object {
        fun fromRecordingRating(rating: RecordingRating): RecordingRatingEntity {
            val distributionJson = if (rating.ratingDistribution.isNotEmpty()) {
                val jsonObj = org.json.JSONObject()
                for ((star, count) in rating.ratingDistribution) {
                    jsonObj.put(star.toString(), count)
                }
                jsonObj.toString()
            } else null
            
            return RecordingRatingEntity(
                identifier = rating.identifier,
                rating = rating.rating,
                rawRating = rating.rawRating,
                reviewCount = rating.reviewCount,
                sourceType = rating.sourceType,
                confidence = rating.confidence,
                distributionJson = distributionJson,
                highRatings = rating.highRatings,
                lowRatings = rating.lowRatings
            )
        }
    }
}

/**
 * Database entity for storing pre-computed show ratings.
 */
@Entity(
    tableName = "show_ratings",
    indices = [
        Index(value = ["showKey"], unique = true),
        Index(value = ["date"]),
        Index(value = ["rating"]),
        Index(value = ["confidence"])
    ]
)
data class ShowRatingEntity(
    @PrimaryKey
    val showKey: String, // Format: "YYYY-MM-DD_Venue_Name"
    val date: String,    // YYYY-MM-DD format
    val venue: String,
    val rating: Float,                           // Weighted rating (for internal ranking)
    val rawRating: Float,                        // Simple average (for display)
    val confidence: Float,                       // 0.0 to 1.0
    val bestRecordingId: String,
    val recordingCount: Int,
    val totalHighRatings: Int = 0,               // Total 4-5★ reviews across all recordings
    val totalLowRatings: Int = 0,                // Total 1-2★ reviews across all recordings
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toShowRating(): ShowRating {
        return ShowRating(
            showKey = showKey,
            date = date,
            venue = venue,
            rating = rating,
            rawRating = rawRating,
            confidence = confidence,
            bestRecordingId = bestRecordingId,
            recordingCount = recordingCount,
            totalHighRatings = totalHighRatings,
            totalLowRatings = totalLowRatings
        )
    }
    
    companion object {
        fun fromShowRating(rating: ShowRating): ShowRatingEntity {
            return ShowRatingEntity(
                showKey = rating.showKey,
                date = rating.date,
                venue = rating.venue,
                rating = rating.rating,
                rawRating = rating.rawRating,
                confidence = rating.confidence,
                bestRecordingId = rating.bestRecordingId,
                recordingCount = rating.recordingCount,
                totalHighRatings = rating.totalHighRatings,
                totalLowRatings = rating.totalLowRatings
            )
        }
    }
}