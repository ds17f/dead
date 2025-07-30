package com.deadarchive.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Domain model representing a Track within Player context.
 * Combines core track data with player-specific metadata and state.
 * 
 * Key Design Principles:
 * - Composition over inheritance (contains Track, doesn't extend it)
 * - Rich domain model with computed properties
 * - Single source of truth for player track state
 * - Immutable data structure with functional updates
 */
data class PlayerV2Track(
    val track: Track,                                      // Core track data
    val playbackPosition: Duration = Duration.ZERO,       // Current playback position
    val isCurrentlyPlaying: Boolean = false,              // Active playback state
    val isLoading: Boolean = false,                       // Track loading/buffering state
    val playbackQuality: AudioQuality = AudioQuality.AUTO, // Audio quality preference
    val queuePosition: Int = 0,                           // Position in queue
    val addedToQueueAt: Long = System.currentTimeMillis(), // When added to queue
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED // Download state
) {
    // Delegate Track properties for convenient access
    val trackId: String get() = track.filename
    val filename: String get() = track.filename
    val displayTitle: String get() = track.displayTitle
    val duration: Duration get() = track.duration.milliseconds
    val audioFile: AudioFile? get() = track.audioFile
    
    // Player-specific computed properties
    val remainingTime: Duration 
        get() = if (duration > Duration.ZERO) duration - playbackPosition else Duration.ZERO
    
    val progressPercentage: Float 
        get() = if (duration > Duration.ZERO) {
            (playbackPosition / duration).toFloat().coerceIn(0f, 1f)
        } else 0f
    
    val isCompleted: Boolean 
        get() = duration > Duration.ZERO && progressPercentage >= 0.95f
    
    val hasAudioFile: Boolean 
        get() = audioFile != null
    
    val isDownloaded: Boolean 
        get() = downloadStatus == DownloadStatus.COMPLETED
    
    val canPlay: Boolean 
        get() = hasAudioFile && !isLoading
    
    val displayStatus: String
        get() = when {
            isCurrentlyPlaying -> "Playing"
            isLoading -> "Loading..."
            isCompleted -> "Completed"
            playbackPosition > Duration.ZERO -> "Paused"
            else -> "Ready"
        }
    
    // Queue management properties
    val isFirstInQueue: Boolean 
        get() = queuePosition == 0
    
    val canMoveUp: Boolean 
        get() = queuePosition > 0
    
    val canMoveDown: Boolean 
        get() = true // Would need queue context for real implementation
    
    // Functional update methods
    fun updatePlaybackPosition(position: Duration): PlayerV2Track = 
        copy(playbackPosition = position)
    
    fun markAsPlaying(): PlayerV2Track = 
        copy(isCurrentlyPlaying = true, isLoading = false)
    
    fun markAsNotPlaying(): PlayerV2Track = 
        copy(isCurrentlyPlaying = false)
    
    fun markAsLoading(): PlayerV2Track = 
        copy(isLoading = true)
    
    fun updateQueuePosition(position: Int): PlayerV2Track = 
        copy(queuePosition = position)
    
    fun updateDownloadStatus(status: DownloadStatus): PlayerV2Track = 
        copy(downloadStatus = status)
    
    companion object {
        /**
         * Create PlayerV2Track from core Track with current timestamp
         */
        fun fromTrack(track: Track, queuePosition: Int = 0): PlayerV2Track = PlayerV2Track(
            track = track,
            queuePosition = queuePosition,
            addedToQueueAt = System.currentTimeMillis()
        )
        
        /**
         * Create PlayerV2Track with specific player state
         */
        fun create(
            track: Track,
            playbackPosition: Duration = Duration.ZERO,
            isCurrentlyPlaying: Boolean = false,
            queuePosition: Int = 0,
            downloadStatus: DownloadStatus = DownloadStatus.QUEUED
        ): PlayerV2Track = PlayerV2Track(
            track = track,
            playbackPosition = playbackPosition,
            isCurrentlyPlaying = isCurrentlyPlaying,
            queuePosition = queuePosition,
            downloadStatus = downloadStatus
        )
        
        /**
         * Default comparator for queue ordering
         */
        val QUEUE_ORDER_COMPARATOR = compareBy<PlayerV2Track> { it.queuePosition }
            .thenBy { it.addedToQueueAt }
    }
}

/**
 * Audio quality preferences for playback
 */
enum class AudioQuality {
    AUTO,       // Let system decide based on connection
    LOW,        // Prefer smaller file sizes
    MEDIUM,     // Balanced quality/size
    HIGH,       // Prefer highest quality
    LOSSLESS    // Lossless formats only
}