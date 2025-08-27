package com.deadly.v2.core.miniplayer.service

import android.util.Log
import com.deadly.v2.core.api.miniplayer.MiniPlayerService
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.media.state.MediaControllerStateUtil
import com.deadly.v2.core.model.CurrentTrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2 MiniPlayer Service Implementation
 * 
 * Real implementation of MiniPlayerService that delegates to V2 MediaControllerRepository.
 * Provides perfect synchronization with Media3 playback state through direct StateFlow delegation.
 * 
 * No stub/mock - this is the production implementation following V2 architecture patterns.
 */
@Singleton
class MiniPlayerServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val mediaControllerStateUtil: MediaControllerStateUtil
) : MiniPlayerService {
    
    companion object {
        private const val TAG = "MiniPlayerServiceImpl"
    }
    
    // Service scope for StateFlow conversion
    private val serviceScope: CoroutineScope = kotlinx.coroutines.GlobalScope
    
    // Direct StateFlow delegation - perfect synchronization with MediaController
    override val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    override val currentPosition: StateFlow<Long> = mediaControllerRepository.currentPosition
    override val duration: StateFlow<Long> = mediaControllerRepository.duration
    override val progress: StateFlow<Float> = mediaControllerRepository.progress
    
    /**
     * Convert MediaMetadata to rich CurrentTrackInfo for MiniPlayer display
     * 
     * DUPLICATION ELIMINATION: Now using shared MediaControllerStateUtil
     * instead of duplicating metadata transformation logic.
     * 
     * Note: MiniPlayer needs incomplete CurrentTrackInfo (ViewModel combines with other flows)
     * so we use the utility's createCurrentTrackInfo() method with minimal state.
     */
    override val currentTrackInfo: StateFlow<CurrentTrackInfo?> =
        mediaControllerRepository.currentTrack.map { metadata ->
            metadata?.let { 
                // Use shared utility but with minimal state for MiniPlayer pattern
                mediaControllerStateUtil.createCurrentTrackInfo(
                    metadata = it,
                    recordingId = null,  // MiniPlayer extracts from metadata.extras
                    showId = null,       // MiniPlayer extracts from metadata.extras  
                    isPlaying = false,   // ViewModel combines with isPlaying flow
                    position = 0L,       // ViewModel combines with currentPosition flow
                    duration = 0L        // ViewModel combines with duration flow
                )
            }
        }.stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )
    
    /**
     * Delegate playback commands to MediaControllerRepository
     * Ensures all playback control flows through single source of truth
     */
    override suspend fun togglePlayPause() {
        Log.d(TAG, "MiniPlayer togglePlayPause requested")
        mediaControllerRepository.togglePlayPause()
    }
    
    /**
     * Initialize service resources
     * MediaControllerRepository handles its own initialization
     */
    override suspend fun initialize() {
        Log.d(TAG, "MiniPlayer service initialized")
        // MediaControllerRepository handles connection lifecycle
    }
    
    /**
     * Clean up service resources
     * No cleanup needed - MediaControllerRepository manages its own lifecycle
     */
    override suspend fun cleanup() {
        Log.d(TAG, "MiniPlayer service cleanup")
        // MediaControllerRepository manages its own resources
    }
    
}