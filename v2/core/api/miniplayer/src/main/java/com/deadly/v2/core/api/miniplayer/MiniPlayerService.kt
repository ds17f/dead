package com.deadly.v2.core.api.miniplayer

import com.deadly.v2.core.model.CurrentTrackInfo
import kotlinx.coroutines.flow.Flow

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
    val isPlaying: Flow<Boolean>
    
    /**
     * Current playback position in milliseconds
     */
    val currentPosition: Flow<Long>
    
    /**
     * Total track duration in milliseconds
     */
    val duration: Flow<Long>
    
    /**
     * Computed progress (0.0 to 1.0) for progress bars
     */
    val progress: Flow<Float>
    
    /**
     * Rich track metadata for MiniPlayer display
     */
    val currentTrackInfo: Flow<CurrentTrackInfo?>
    
    /**
     * Show ID for playlist navigation
     */
    val currentShowId: Flow<String?>
    
    /**
     * Recording ID for playback restoration
     */
    val currentRecordingId: Flow<String?>
    
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