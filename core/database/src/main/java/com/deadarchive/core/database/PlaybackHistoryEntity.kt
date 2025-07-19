package com.deadarchive.core.database

import androidx.room.*
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Database entity representing a single playback event in the user's listening history.
 * Tracks when tracks are played, for how long, and provides the foundation for
 * listening analytics and recommendations.
 * 
 * Follows the existing database patterns with proper foreign key relationships,
 * indexing for efficient queries, and companion object conversion methods.
 */
@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordingId"]),
        Index(value = ["playbackTimestamp"]),
        Index(value = ["trackFilename"]),
        Index(value = ["sessionId"]),
        Index(value = ["wasCompleted"]),
        Index(value = ["playbackSource"])
    ]
)
@Serializable
data class PlaybackHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    /** Foreign key reference to the recording that was played */
    val recordingId: String,
    
    /** Filename of the specific track that was played */
    val trackFilename: String,
    
    /** URL of the track that was played (for debugging and validation) */
    val trackUrl: String,
    
    /** Display title of the track at time of playback */
    val trackTitle: String,
    
    /** Track number within the recording, if available */
    val trackNumber: Int? = null,
    
    /** Unix timestamp (milliseconds) when playback started */
    val playbackTimestamp: Long,
    
    /** Unix timestamp (milliseconds) when track finished, null if incomplete */
    val completionTimestamp: Long? = null,
    
    /** Total duration the track was actively playing (milliseconds) */
    val playbackDuration: Long? = null,
    
    /** Position in track when playback ended/paused (milliseconds) */
    val finalPosition: Long = 0L,
    
    /** Total length of the track (milliseconds) */
    val trackDuration: Long? = null,
    
    /** Whether the track was played to completion (>90% played) */
    val wasCompleted: Boolean = false,
    
    /** Source of playback: LOCAL, STREAM, DOWNLOAD, etc. */
    val playbackSource: String = "UNKNOWN",
    
    /** Session ID to group related playback events */
    val sessionId: String? = null,
    
    /** Reason for track transition: USER_ACTION, QUEUE_END, AUTO_ADVANCE, etc. */
    val transitionReason: String? = null,
    
    /** Additional metadata about the playback context */
    val playbackContext: String? = null,
    
    /** Version of the tracking system that recorded this event */
    val trackingVersion: Int = 1
) {
    companion object {
        /**
         * Calculate completion percentage for this playback event
         */
        fun PlaybackHistoryEntity.completionPercentage(): Float {
            return if (trackDuration != null && trackDuration > 0) {
                (finalPosition.toFloat() / trackDuration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        
        /**
         * Check if this playback event represents a meaningful listen
         * (played for at least 30 seconds or 25% of track duration)
         */
        fun PlaybackHistoryEntity.isMeaningfulListen(): Boolean {
            val minDuration = 30_000L // 30 seconds
            val actualDuration = playbackDuration ?: 0L
            
            return actualDuration >= minDuration || 
                   (trackDuration != null && actualDuration >= trackDuration * 0.25f)
        }
        
        /**
         * Get human-readable playback source name
         */
        fun PlaybackHistoryEntity.getPlaybackSourceName(): String {
            return when (playbackSource) {
                "LOCAL" -> "Local File"
                "STREAM" -> "Stream"
                "DOWNLOAD" -> "Downloaded"
                "CACHE" -> "Cached"
                else -> "Unknown"
            }
        }
    }
}