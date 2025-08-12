package com.deadarchive.feature.playlist.service

import com.deadarchive.core.model.CurrentTrackInfo
import kotlinx.coroutines.flow.Flow

/**
 * MiniPlayerV2Service - Service interface for V2 mini-player operations
 * 
 * Clean V2 service abstraction following PlayerV2Service patterns.
 * Provides state and playback control for the global mini-player.
 */
interface MiniPlayerV2Service {
    
    /**
     * Current playback state flow
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
     * Current track information with enriched metadata
     */
    val currentTrackInfo: Flow<CurrentTrackInfo?>
    
    /**
     * Current recording ID for visual theming
     */
    val currentRecordingId: Flow<String?>
    
    /**
     * Playback progress as a float between 0.0 and 1.0
     */
    val progress: Flow<Float>
    
    /**
     * Toggle play/pause state
     */
    suspend fun togglePlayPause()
    
    /**
     * Navigate to expanded player view
     * @param recordingId Optional recording ID for navigation context
     */
    suspend fun expandToPlayer(recordingId: String?)
    
    /**
     * Initialize service and start state monitoring
     */
    suspend fun initialize()
    
    /**
     * Clean up resources when service is no longer needed
     */
    suspend fun cleanup()
}