package com.deadly.v2.core.miniplayer.service

import android.util.Log
import androidx.media3.common.MediaMetadata
import com.deadly.v2.core.api.miniplayer.MiniPlayerService
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.model.CurrentTrackInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val mediaControllerRepository: MediaControllerRepository
) : MiniPlayerService {
    
    companion object {
        private const val TAG = "MiniPlayerServiceImpl"
    }
    
    // Direct StateFlow delegation - perfect synchronization with MediaController
    override val isPlaying: Flow<Boolean> = mediaControllerRepository.isPlaying
    override val currentPosition: Flow<Long> = mediaControllerRepository.currentPosition
    override val duration: Flow<Long> = mediaControllerRepository.duration
    override val progress: Flow<Float> = mediaControllerRepository.progress
    override val currentShowId: Flow<String?> = mediaControllerRepository.currentShowId
    override val currentRecordingId: Flow<String?> = mediaControllerRepository.currentRecordingId
    
    /**
     * Convert MediaMetadata to rich CurrentTrackInfo for MiniPlayer display
     * Transforms raw Media3 metadata into structured V2 model
     */
    override val currentTrackInfo: Flow<CurrentTrackInfo?> =
        mediaControllerRepository.currentTrack.map { metadata ->
            metadata?.let { createCurrentTrackInfo(it) }
        }
    
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
    
    /**
     * Transform Media3 MediaMetadata into structured V2 CurrentTrackInfo model
     * Extracts rich metadata for MiniPlayer display
     */
    private fun createCurrentTrackInfo(metadata: MediaMetadata): CurrentTrackInfo {
        return CurrentTrackInfo(
            trackUrl = metadata.extras?.getString("trackUrl") ?: "",
            recordingId = metadata.extras?.getString("recordingId") ?: "",
            showId = metadata.extras?.getString("showId") ?: "",
            showDate = metadata.extras?.getString("showDate") ?: "",
            venue = metadata.extras?.getString("venue") ?: "",
            location = metadata.extras?.getString("location") ?: "",
            songTitle = metadata.title?.toString() ?: "Unknown Track",
            trackNumber = metadata.trackNumber ?: 0,
            filename = metadata.extras?.getString("filename") ?: "",
            isPlaying = false, // Will be combined with isPlaying flow in ViewModel
            position = 0L,     // Will be combined with currentPosition flow in ViewModel
            duration = 0L      // Will be combined with duration flow in ViewModel
        )
    }
}