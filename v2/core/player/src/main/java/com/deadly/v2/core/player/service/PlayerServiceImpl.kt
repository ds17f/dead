package com.deadly.v2.core.player.service

import android.util.Log
import androidx.media3.common.MediaMetadata
import com.deadly.v2.core.api.player.PlayerService
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.media.state.MediaControllerStateUtil
import com.deadly.v2.core.media.service.MetadataHydratorService
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.Show
import com.deadly.v2.core.model.Recording
import com.deadly.v2.core.model.Track
import com.deadly.v2.core.model.CurrentTrackInfo
import com.deadly.v2.core.model.PlaybackStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2 Player Service Implementation with Metadata Hydration
 * 
 * Real implementation of PlayerService that delegates to V2 MediaControllerRepository.
 * Provides perfect synchronization with Media3 playback state through direct StateFlow delegation.
 * Uses MetadataHydratorService for on-demand metadata enrichment from database.
 * 
 * Key Innovation: Hydrates metadata on-demand when accessed, ensuring fresh show/venue info.
 */
@Singleton
class PlayerServiceImpl @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val mediaControllerStateUtil: MediaControllerStateUtil,
    private val metadataHydratorService: MetadataHydratorService,
    private val showRepository: ShowRepository,
    private val shareService: ShareService
) : PlayerService {
    
    companion object {
        private const val TAG = "PlayerServiceImpl"
        private const val PREVIOUS_TRACK_THRESHOLD_MS = 3000L // 3 seconds
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Direct delegation to MediaControllerRepository state flows
    override val isPlaying: StateFlow<Boolean> = mediaControllerRepository.isPlaying
    
    override val playbackStatus: StateFlow<PlaybackStatus> = mediaControllerRepository.playbackStatus
    
    // DUPLICATION ELIMINATION: Central CurrentTrackInfo using shared utility
    // Instead of 6+ individual StateFlows extracting metadata pieces,
    // create one comprehensive CurrentTrackInfo and expose it directly
    override val currentTrackInfo: StateFlow<CurrentTrackInfo?> = mediaControllerStateUtil.createCurrentTrackInfoStateFlow(serviceScope)
    
    /**
     * Format show date from YYYY-MM-DD to readable format
     */
    private fun formatShowDate(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                
                "${monthNames[month - 1]} $day, $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
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
            val wasPlaying = mediaControllerRepository.isPlaying.value
            mediaControllerRepository.seekToNext()
            
            // Auto-play new track if we were paused
            if (!wasPlaying) {
                Log.d(TAG, "Was paused - starting playback of new track")
                mediaControllerRepository.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to next", e)
        }
    }
    
    override suspend fun seekToPrevious() {
        Log.d(TAG, "Smart previous: checking current position")
        try {
            val currentPositionMs = mediaControllerRepository.currentPosition.value
            val wasPlaying = mediaControllerRepository.isPlaying.value
            
            if (currentPositionMs > PREVIOUS_TRACK_THRESHOLD_MS) {
                // Restart current track (seek to beginning)
                Log.d(TAG, "Position ${currentPositionMs}ms > ${PREVIOUS_TRACK_THRESHOLD_MS}ms, restarting track")
                mediaControllerRepository.seekToPosition(0L)
                
                // If paused, stay paused after restart (just reset position)
                // If playing, continue playing after restart
                if (wasPlaying) {
                    Log.d(TAG, "Was playing - continuing playback after restart")
                } else {
                    Log.d(TAG, "Was paused - staying paused after restart")
                }
            } else {
                // Go to previous track
                Log.d(TAG, "Position ${currentPositionMs}ms <= ${PREVIOUS_TRACK_THRESHOLD_MS}ms, seeking to previous")
                mediaControllerRepository.seekToPrevious()
                
                // Auto-play new track if we were paused
                if (!wasPlaying) {
                    Log.d(TAG, "Was paused - starting playback of previous track")
                    mediaControllerRepository.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in smart previous", e)
        }
    }
    
    override suspend fun seekToPosition(positionMs: Long) {
        Log.d(TAG, "Seek to position: ${positionMs}ms")
        try {
            mediaControllerRepository.seekToPosition(positionMs)
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
    
    override suspend fun getDebugMetadata(): Map<String, String?> {
        val currentMetadata = mediaControllerRepository.currentTrack.value
        val currentMediaItem = mediaControllerRepository.currentMediaItem.value
        
        return if (currentMediaItem != null && currentMetadata != null) {
            mapOf(
                // === MEDIA ITEM FIELDS ===
                "🆔 MediaItem.mediaId" to (currentMediaItem.mediaId ?: "null"),
                "🔗 MediaItem.playbackProperties.uri" to currentMediaItem.localConfiguration?.uri?.toString(),
                "🏷️ MediaItem.playbackProperties.mimeType" to currentMediaItem.localConfiguration?.mimeType,
                "📋 MediaItem.playbackProperties.tag" to currentMediaItem.localConfiguration?.tag?.toString(),
                
                // === MEDIA METADATA FIELDS ===
                "title" to currentMetadata.title?.toString(),
                "artist" to currentMetadata.artist?.toString(),
                "albumTitle" to currentMetadata.albumTitle?.toString(),
                "albumArtist" to currentMetadata.albumArtist?.toString(),
                "genre" to currentMetadata.genre?.toString(),
                "trackNumber" to currentMetadata.trackNumber?.toString(),
                "totalTrackCount" to currentMetadata.totalTrackCount?.toString(),
                "recordingYear" to currentMetadata.recordingYear?.toString(),
                "releaseYear" to currentMetadata.releaseYear?.toString(),
                "writer" to currentMetadata.writer?.toString(),
                "composer" to currentMetadata.composer?.toString(),
                "conductor" to currentMetadata.conductor?.toString(),
                "discNumber" to currentMetadata.discNumber?.toString(),
                "totalDiscCount" to currentMetadata.totalDiscCount?.toString(),
                "artworkUri" to currentMetadata.artworkUri?.toString(),
                
                // === CUSTOM EXTRAS ===
                "trackUrl" to currentMetadata.extras?.getString("trackUrl"),
                "recordingId" to currentMetadata.extras?.getString("recordingId"),
                "showId" to currentMetadata.extras?.getString("showId"),
                "showDate" to currentMetadata.extras?.getString("showDate"),
                "venue" to currentMetadata.extras?.getString("venue"),
                "location" to currentMetadata.extras?.getString("location"),
                "filename" to currentMetadata.extras?.getString("filename"),
                "format" to currentMetadata.extras?.getString("format"),
                "isHydrated" to currentMetadata.extras?.getBoolean("isHydrated", false)?.toString(),
                "hydratedAt" to currentMetadata.extras?.getString("hydratedAt"),
                
                // === EXTRAS INSPECTION ===
                "extrasKeys" to currentMetadata.extras?.keySet()?.joinToString(", ") { "[$it]" }
            )
        } else {
            mapOf(
                "status" to "No current MediaItem/MediaMetadata available",
                "hasMediaItem" to (currentMediaItem != null).toString(),
                "hasMediaMetadata" to (currentMetadata != null).toString()
            )
        }
    }
    
    override suspend fun shareCurrentTrack() {
        Log.d(TAG, "Sharing current track")
        try {
            val currentMetadata = mediaControllerRepository.currentTrack.value
            if (currentMetadata == null) {
                Log.w(TAG, "No current track metadata available for sharing")
                return
            }
            
            val showId = currentMetadata.extras?.getString("showId")
            val recordingId = currentMetadata.extras?.getString("recordingId")
            
            if (showId.isNullOrBlank() || recordingId.isNullOrBlank()) {
                Log.w(TAG, "Missing showId or recordingId in metadata for sharing")
                return
            }
            
            // Get show and recording data from repository
            val show = showRepository.getShowById(showId)
            if (show == null) {
                Log.w(TAG, "Show not found for sharing: $showId")
                return
            }
            
            val recording = showRepository.getRecordingById(recordingId)
            if (recording == null) {
                Log.w(TAG, "Recording not found for sharing: $recordingId")
                return
            }
            
            val trackTitle = currentMetadata.title?.toString() ?: "Unknown Track"
            val trackNumber = currentMetadata.trackNumber?.let { if (it > 0) it else null }
            val duration = formatDuration(playbackStatus.value.duration)
            val track = Track(
                name = currentMetadata.extras?.getString("filename") ?: trackTitle,
                title = trackTitle,
                trackNumber = trackNumber,
                duration = duration,
                format = currentMetadata.extras?.getString("format") ?: "mp3"
            )
            
            // Get current position in seconds for time-based sharing
            val currentPositionSeconds = playbackStatus.value.currentPosition / 1000
            
            shareService.shareTrack(show, recording, track, currentPositionSeconds)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing current track", e)
        }
    }
    
    override suspend fun shareCurrentShow() {
        Log.d(TAG, "Sharing current show")
        try {
            val currentMetadata = mediaControllerRepository.currentTrack.value
            if (currentMetadata == null) {
                Log.w(TAG, "No current track metadata available for sharing")
                return
            }
            
            val showId = currentMetadata.extras?.getString("showId")
            val recordingId = currentMetadata.extras?.getString("recordingId")
            
            if (showId.isNullOrBlank() || recordingId.isNullOrBlank()) {
                Log.w(TAG, "Missing showId or recordingId in metadata for sharing")
                return
            }
            
            // Get show and recording data from repository
            val show = showRepository.getShowById(showId)
            if (show == null) {
                Log.w(TAG, "Show not found for sharing: $showId")
                return
            }
            
            val recording = showRepository.getRecordingById(recordingId)
            if (recording == null) {
                Log.w(TAG, "Recording not found for sharing: $recordingId")
                return
            }
            
            shareService.shareShow(show, recording)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing current show", e)
        }
    }
    
}