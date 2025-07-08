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
    val rating: Float,
    val reviewCount: Int,
    val sourceType: String, // SBD, AUD, MATRIX, etc.
    val confidence: Float,  // 0.0 to 1.0
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toRecordingRating(): RecordingRating {
        return RecordingRating(
            identifier = identifier,
            rating = rating,
            reviewCount = reviewCount,
            sourceType = sourceType,
            confidence = confidence
        )
    }
    
    companion object {
        fun fromRecordingRating(rating: RecordingRating): RecordingRatingEntity {
            return RecordingRatingEntity(
                identifier = rating.identifier,
                rating = rating.rating,
                reviewCount = rating.reviewCount,
                sourceType = rating.sourceType,
                confidence = rating.confidence
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
    val rating: Float,
    val confidence: Float,  // 0.0 to 1.0
    val bestRecordingId: String,
    val recordingCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toShowRating(): ShowRating {
        return ShowRating(
            showKey = showKey,
            date = date,
            venue = venue,
            rating = rating,
            confidence = confidence,
            bestRecordingId = bestRecordingId,
            recordingCount = recordingCount
        )
    }
    
    companion object {
        fun fromShowRating(rating: ShowRating): ShowRatingEntity {
            return ShowRatingEntity(
                showKey = rating.showKey,
                date = rating.date,
                venue = rating.venue,
                rating = rating.rating,
                confidence = rating.confidence,
                bestRecordingId = rating.bestRecordingId,
                recordingCount = rating.recordingCount
            )
        }
    }
}