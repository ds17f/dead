package com.deadly.v2.core.api.playback

import com.deadly.v2.core.model.CurrentTrackInfo
import com.deadly.v2.core.model.PlaybackStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized Playback State Service
 * 
 * Single source of truth for all MediaController state and commands shared across V2 features.
 * Eliminates duplication between PlayerService, MiniPlayerService, and PlaylistService by
 * providing unified access to core playback functionality.
 * 
 * This service provides all essential MediaController operations that playback-related 
 * features need, following DRY principles and ensuring perfect state synchronization.
 */
interface PlaybackStateService {
    
    // === Core Playback State ===
    
    /**
     * Whether audio is currently playing
     * Used by: PlayerService, MiniPlayerService, PlaylistService
     */
    val isPlaying: StateFlow<Boolean>
    
    /**
     * Unified playback position state with computed progress
     * Contains currentPosition, duration, and computed progress as cohesive unit
     * Used by: PlayerService, MiniPlayerService
     */
    val playbackStatus: StateFlow<PlaybackStatus>
    
    /**
     * Comprehensive current track information
     * Contains all metadata, playback state, and navigation data
     * Used by: PlayerService, MiniPlayerService, PlaylistService
     */
    val currentTrackInfo: StateFlow<CurrentTrackInfo?>
    
    /**
     * Whether next track is available in queue
     * Used by: PlayerService (for UI button state)
     */
    val hasNext: StateFlow<Boolean>
    
    /**
     * Whether previous track is available in queue  
     * Used by: PlayerService (for UI button state)
     */
    val hasPrevious: StateFlow<Boolean>
    
    // === Core Playback Commands ===
    
    /**
     * Explicitly start playback (always play)
     * Used by: PlaylistService.resume()
     */
    suspend fun play()
    
    /**
     * Explicitly pause playback (always pause)
     * Used by: PlaylistService.pause()
     */
    suspend fun pause()
    
    /**
     * Context-aware play/pause toggle based on current state
     * Used by: PlayerService, MiniPlayerService
     */
    suspend fun togglePlayPause()
    
    /**
     * Skip to next track in queue
     * Used by: PlayerService
     */
    suspend fun seekToNext()
    
    /**
     * Skip to previous track in queue
     * Used by: PlayerService
     */
    suspend fun seekToPrevious()
    
    /**
     * Seek to specific position in current track
     * Used by: PlayerService
     */
    suspend fun seekToPosition(positionMs: Long)
    
    // === Utility Functions ===
    
    /**
     * Format duration milliseconds to MM:SS string
     * Used by: PlayerService
     */
    fun formatDuration(durationMs: Long): String
    
    /**
     * Format position milliseconds to MM:SS string
     * Used by: PlayerService
     */
    fun formatPosition(positionMs: Long): String
    
    // === Sharing Functions ===
    
    /**
     * Share currently playing track with current playback position
     * Used by: PlayerService
     */
    suspend fun shareCurrentTrack()
    
    /**
     * Share current show and recording
     * Used by: PlayerService
     */
    suspend fun shareCurrentShow()
    
    // === Debug Functions ===
    
    /**
     * Get debug information about current MediaMetadata for inspection
     * Used by: PlayerService
     */
    suspend fun getDebugMetadata(): Map<String, String?>
}