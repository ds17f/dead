package com.deadly.v2.core.api.miniplayer

import com.deadly.v2.core.model.CurrentTrackInfo
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
     * Rich track metadata for MiniPlayer display
     */
    val currentTrackInfo: StateFlow<CurrentTrackInfo?>
    
    /**
     * Show ID for playlist navigation
     */
    val currentShowId: StateFlow<String?>
    
    /**
     * Recording ID for playback restoration
     */
    val currentRecordingId: StateFlow<String?>
    
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