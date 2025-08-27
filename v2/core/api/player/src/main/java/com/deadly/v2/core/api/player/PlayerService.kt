package com.deadly.v2.core.api.player

import com.deadly.v2.core.model.CurrentTrackInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * V2 Player Service Interface
 * 
 * Business logic interface for V2 Player functionality.
 * Provides reactive state streams and playback control commands.
 * 
 * Implementation delegates to V2 MediaControllerRepository for state synchronization.
 */
interface PlayerService {
    
    /**
     * Whether audio is currently playing
     */
    val isPlaying: StateFlow<Boolean>
    
    /**
     * Current playback position in milliseconds
     */
    val currentPosition: StateFlow<Long>
    
    /**
     * Total track duration in milliseconds
     */
    val duration: StateFlow<Long>
    
    /**
     * Computed progress (0.0 to 1.0) for progress bars
     */
    val progress: StateFlow<Float>
    
    /**
     * Comprehensive current track information
     * Contains all metadata, playback state, and navigation data
     */
    val currentTrackInfo: StateFlow<CurrentTrackInfo?>
    
    /**
     * Whether next track is available
     */
    val hasNext: StateFlow<Boolean>
    
    /**
     * Whether previous track is available
     */
    val hasPrevious: StateFlow<Boolean>
    
    /**
     * Toggle between play and pause states
     */
    suspend fun togglePlayPause()
    
    /**
     * Skip to next track
     */
    suspend fun seekToNext()
    
    /**
     * Skip to previous track
     */
    suspend fun seekToPrevious()
    
    /**
     * Seek to specific position in current track
     */
    suspend fun seekToPosition(positionMs: Long)
    
    /**
     * Format duration milliseconds to MM:SS string
     */
    fun formatDuration(durationMs: Long): String
    
    /**
     * Format position milliseconds to MM:SS string
     */
    fun formatPosition(positionMs: Long): String
    
    /**
     * Get debug information about current MediaMetadata for inspection
     */
    suspend fun getDebugMetadata(): Map<String, String?>
    
    /**
     * Share currently playing track with current playback position
     */
    suspend fun shareCurrentTrack()
    
    /**
     * Share current show and recording
     */
    suspend fun shareCurrentShow()
}