package com.deadly.v2.core.api.miniplayer

import com.deadly.v2.core.model.CurrentTrackInfo
import com.deadly.v2.core.model.PlaybackStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * V2 MiniPlayer Service Interface
 * 
 * Business logic interface for MiniPlayer functionality.
 * Provides reactive state streams and playback control commands.
 * 
 * Implementation delegates to V2 MediaControllerRepository for state synchronization.
 */
interface MiniPlayerService {
    
    /**
     * Whether audio is currently playing
     */
    val isPlaying: StateFlow<Boolean>
    
    /**
     * Unified playback position state with computed progress
     * Contains currentPosition, duration, and computed progress as cohesive unit
     */
    val playbackStatus: StateFlow<PlaybackStatus>
    
    /**
     * Rich track metadata for MiniPlayer display
     * Contains showId and recordingId for navigation needs
     */
    val currentTrackInfo: StateFlow<CurrentTrackInfo?>
    
    /**
     * Toggle between play and pause states
     */
    suspend fun togglePlayPause()
    
    /**
     * Initialize service resources
     */
    suspend fun initialize()
    
    /**
     * Clean up service resources
     */
    suspend fun cleanup()
}