package com.deadly.v2.core.playback.service

import com.deadly.v2.core.api.playback.PlaybackStateService
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.media.state.MediaControllerStateUtil
import com.deadly.v2.core.model.CurrentTrackInfo
import com.deadly.v2.core.model.PlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of centralized PlaybackStateService
 * 
 * Delegates to MediaControllerRepository for all core MediaController operations
 * and provides unified access to playback state and commands across all V2 services.
 * 
 * This eliminates duplication by centralizing all common MediaController interactions
 * that were previously scattered across PlayerService, MiniPlayerService, etc.
 */
@Singleton
class PlaybackStateServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val mediaControllerStateUtil: MediaControllerStateUtil
) : PlaybackStateService {
    
    companion object {
        private const val TAG = "PlaybackStateServiceImpl"
    }
    
    private val serviceScope: CoroutineScope = GlobalScope
    
    // === Core Playback State ===
    
    override val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    
    override val playbackStatus: StateFlow<PlaybackStatus> = mediaControllerRepository.playbackStatus
    
    override val currentTrackInfo: StateFlow<CurrentTrackInfo?> = 
        mediaControllerStateUtil.createCurrentTrackInfoStateFlow(serviceScope)
    
    override val hasNext: StateFlow<Boolean> = combine(
        mediaControllerRepository.currentTrackIndex,
        mediaControllerRepository.mediaItemCount,
        mediaControllerRepository.currentTrack
    ) { currentIndex, queueSize, currentTrack ->
        if (currentTrack == null || queueSize == 0) {
            false // No queue loaded
        } else {
            currentIndex < (queueSize - 1) // Has next if not at last track
        }
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )
    
    override val hasPrevious: StateFlow<Boolean> = combine(
        mediaControllerRepository.currentTrackIndex,
        mediaControllerRepository.currentTrack
    ) { currentIndex, currentTrack ->
        if (currentTrack == null) {
            false // No queue loaded
        } else {
            currentIndex > 0 // Has previous if not at index 0
        }
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )
    
    // === Core Playback Commands ===
    
    override suspend fun play() {
        mediaControllerRepository.play()
    }
    
    override suspend fun pause() {
        mediaControllerRepository.pause()
    }
    
    override suspend fun togglePlayPause() {
        mediaControllerRepository.togglePlayPause()
    }
    
    override suspend fun seekToNext() {
        mediaControllerRepository.seekToNext()
    }
    
    override suspend fun seekToPrevious() {
        mediaControllerRepository.seekToPrevious()
    }
    
    override suspend fun seekToPosition(positionMs: Long) {
        mediaControllerRepository.seekToPosition(positionMs)
    }
    
    // === Utility Functions ===
    
    override fun formatDuration(durationMs: Long): String {
        return formatTime(durationMs)
    }
    
    override fun formatPosition(positionMs: Long): String {
        return formatTime(positionMs)
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    // === Sharing Functions ===
    
    override suspend fun shareCurrentTrack() {
        // Extract current track metadata
        val currentMetadata = mediaControllerRepository.currentTrack.value
        if (currentMetadata == null) {
            android.util.Log.w(TAG, "No current track to share")
            return
        }
        
        // Get current position for time-based sharing
        val currentPositionSeconds = playbackStatus.value.currentPosition / 1000
        
        // TODO: Implement actual sharing logic - need ShareService dependency
        android.util.Log.d(TAG, "Sharing track: ${currentMetadata.title} at position ${currentPositionSeconds}s")
    }
    
    override suspend fun shareCurrentShow() {
        // Extract current show/recording info
        val trackInfo = currentTrackInfo.value
        if (trackInfo == null) {
            android.util.Log.w(TAG, "No current show to share")
            return
        }
        
        // TODO: Implement actual sharing logic - need ShareService dependency
        android.util.Log.d(TAG, "Sharing show: ${trackInfo.showDate} - ${trackInfo.venue}")
    }
    
    // === Debug Functions ===
    
    override suspend fun getDebugMetadata(): Map<String, String?> {
        val metadata = mediaControllerRepository.currentTrack.value ?: return emptyMap()
        
        return mapOf(
            "title" to metadata.title?.toString(),
            "artist" to metadata.artist?.toString(),
            "album" to metadata.albumTitle?.toString(),
            "duration" to metadata.extras?.getString("duration"),
            "format" to metadata.extras?.getString("format"),
            "filename" to metadata.extras?.getString("filename"),
            "mediaId" to metadata.extras?.getString("mediaId")
        )
    }
}