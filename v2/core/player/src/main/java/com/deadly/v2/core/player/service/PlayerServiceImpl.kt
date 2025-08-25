package com.deadly.v2.core.player.service

import android.util.Log
import androidx.media3.common.MediaMetadata
import com.deadly.v2.core.api.player.PlayerService
import com.deadly.v2.core.media.repository.MediaControllerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2 Player Service Implementation
 * 
 * Real implementation of PlayerService that delegates to V2 MediaControllerRepository.
 * Provides perfect synchronization with Media3 playback state through direct StateFlow delegation.
 * 
 * Follows the same architectural patterns as MiniPlayerServiceImpl.
 */
@Singleton
class PlayerServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) : PlayerService {
    
    companion object {
        private const val TAG = "PlayerServiceImpl"
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Direct delegation to MediaControllerRepository state flows
    override val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    
    override val currentPosition: StateFlow<Long> = mediaControllerRepository.currentPosition
    
    override val duration: StateFlow<Long> = mediaControllerRepository.duration
    
    override val progress: StateFlow<Float> = mediaControllerRepository.progress
    
    // Extract track title from MediaMetadata
    override val currentTrackTitle: StateFlow<String?> = mediaControllerRepository.currentTrack.map { metadata ->
        metadata?.title?.toString() ?: "Unknown Track"
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = "Unknown Track"
    )
    
    // Extract album info from MediaMetadata  
    override val currentAlbum: StateFlow<String?> = mediaControllerRepository.currentTrack.map { metadata ->
        metadata?.albumTitle?.toString() ?: "Unknown Album" 
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = "Unknown Album"
    )
    
    // TODO: Implement proper hasNext/hasPrevious based on queue state
    // For now, always show enabled (consistent with mock)
    override val hasNext: StateFlow<Boolean> = combine(
        mediaControllerRepository.currentTrack
    ) { track ->
        track != null // Has next if we have a current track
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )
    
    override val hasPrevious: StateFlow<Boolean> = combine(
        mediaControllerRepository.currentTrack  
    ) { track ->
        track != null // Has previous if we have a current track
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )
    
    override suspend fun togglePlayPause() {
        Log.d(TAG, "Toggle play/pause")
        try {
            mediaControllerRepository.togglePlayPause()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
        }
    }
    
    override suspend fun seekToNext() {
        Log.d(TAG, "Seek to next track")
        try {
            // MediaControllerRepository doesn't have seekToNext yet
            // TODO: Add seekToNext method to MediaControllerRepository
            Log.w(TAG, "seekToNext not yet implemented in MediaControllerRepository")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to next", e)
        }
    }
    
    override suspend fun seekToPrevious() {
        Log.d(TAG, "Seek to previous track")
        try {
            // MediaControllerRepository doesn't have seekToPrevious yet
            // TODO: Add seekToPrevious method to MediaControllerRepository  
            Log.w(TAG, "seekToPrevious not yet implemented in MediaControllerRepository")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to previous", e)
        }
    }
    
    override suspend fun seekToPosition(positionMs: Long) {
        Log.d(TAG, "Seek to position: ${positionMs}ms")
        try {
            // MediaControllerRepository doesn't have seekToPosition yet
            // TODO: Add seekToPosition method to MediaControllerRepository
            Log.w(TAG, "seekToPosition not yet implemented in MediaControllerRepository")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position", e)
        }
    }
    
    override fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0:00"
        
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
    override fun formatPosition(positionMs: Long): String {
        return formatDuration(positionMs)
    }
}