package com.deadarchive.core.media.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Media3 queue as the single source of truth for playback queue state.
 * Provides a clean interface for queue operations and eliminates complex reactive state management.
 */
@Singleton
class QueueManager @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val localFileResolver: LocalFileResolver
) {
    
    companion object {
        private const val TAG = "QueueManager"
    }
    
    private val _currentRecording = MutableStateFlow<Recording?>(null)
    val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()
    
    /**
     * Load an entire show into the queue and optionally start playback from the specified track.
     */
    suspend fun loadShow(recording: Recording, startTrackIndex: Int = 0, startPositionMs: Long = 0, autoPlay: Boolean = true) {
        Log.d(TAG, "Loading show: ${recording.title} with ${recording.tracks.size} tracks, starting at index $startTrackIndex, position ${startPositionMs}ms")
        
        _currentRecording.value = recording
        
        // Create MediaItems for all tracks in the recording
        val mediaItems = recording.tracks.mapNotNull { track ->
            val audioFile = track.audioFile
            val downloadUrl = audioFile?.downloadUrl
            if (downloadUrl != null) {
                createMediaItem(track, downloadUrl, recording)
            } else {
                Log.w(TAG, "Skipping track ${track.displayTitle} - no download URL")
                null
            }
        }
        
        if (mediaItems.isEmpty()) {
            Log.e(TAG, "No playable tracks found in recording ${recording.identifier}")
            return
        }
        
        Log.d(TAG, "Created ${mediaItems.size} MediaItems for queue")
        
        // Get the MediaController and set the queue
        val controller = mediaControllerRepository.getMediaController()
        if (controller != null) {
            if (autoPlay) {
                Log.d(TAG, "Setting MediaItems in controller and starting playback")
            } else {
                Log.d(TAG, "Setting MediaItems in controller without auto-playing")
            }
            controller.setMediaItems(mediaItems, startTrackIndex, startPositionMs)
            controller.prepare()  // Always prepare the media to get into STATE_READY
            
            // Update only PlaybackStateSync with Recording data for enriched metadata (skip PlaybackCommandProcessor sync)
            val queueUrls = recording.tracks.mapNotNull { it.audioFile?.downloadUrl }
            val queueMetadata = recording.tracks.map { track ->
                Pair(track.audioFile?.downloadUrl ?: "", track.displayTitle)
            }.filter { it.first.isNotEmpty() }
            
            // Only update PlaybackStateSync, not PlaybackCommandProcessor (which would overwrite MediaItems)
            mediaControllerRepository.updatePlaybackStateSyncOnly(
                queueUrls = queueUrls,
                currentIndex = startTrackIndex,
                queueMetadata = queueMetadata,
                recording = recording
            )
            
            Log.d(TAG, "Updated PlaybackStateSync with Recording data: ${recording.identifier}")
            
            if (autoPlay) {
                controller.play()
            } else {
                // For UI restoration, sync position immediately so progress bar shows correct position
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200) // Small delay to let MediaController settle
                    mediaControllerRepository.updatePosition()
                    Log.d(TAG, "Synced position for UI restoration: ${controller.currentPosition}ms")
                }
            }
        } else {
            Log.e(TAG, "MediaController not available - cannot load show")
        }
    }
    
    /**
     * Play a specific track. If the track is already in the queue, seek to it.
     * If not, load the entire show and jump to the track.
     */
    suspend fun playTrack(track: Track, recording: Recording) {
        Log.d(TAG, "Playing track: ${track.displayTitle} from ${recording.title}")
        
        val controller = mediaControllerRepository.getMediaController()
        if (controller == null) {
            Log.e(TAG, "MediaController not available - cannot play track")
            return
        }
        
        // Check if the track is already in the current queue
        val trackInQueue = findTrackInQueue(track, controller)
        
        if (trackInQueue != null) {
            Log.d(TAG, "Track found in queue at index ${trackInQueue.index}, seeking to it")
            controller.seekTo(trackInQueue.index, 0)
            controller.play()
        } else {
            // Track not in queue - load entire show and jump to track
            val trackIndex = recording.tracks.indexOf(track)
            if (trackIndex >= 0) {
                Log.d(TAG, "Track not in queue, loading entire show and jumping to track index $trackIndex")
                loadShow(recording, trackIndex, 0, true)
            } else {
                Log.e(TAG, "Track ${track.displayTitle} not found in recording ${recording.identifier}")
            }
        }
    }
    
    /**
     * Get the current queue state from Media3 as a Flow
     */
    fun getCurrentQueue(): Flow<List<QueueItem>> {
        return mediaControllerRepository.currentTrack.map { currentTrack ->
            val controller = mediaControllerRepository.getMediaController()
            if (controller != null) {
                val items = mutableListOf<QueueItem>()
                for (i in 0 until controller.mediaItemCount) {
                    val mediaItem = controller.getMediaItemAt(i)
                    items.add(QueueItem(
                        index = i,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        mediaId = mediaItem.mediaId ?: "",
                        isCurrent = i == controller.currentMediaItemIndex
                    ))
                }
                items
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * Skip to the next track in the queue
     */
    fun skipToNext() {
        val controller = mediaControllerRepository.getMediaController()
        if (controller != null && controller.hasNextMediaItem()) {
            Log.d(TAG, "Skipping to next track")
            controller.seekToNextMediaItem()
        } else {
            Log.d(TAG, "No next track available")
        }
    }
    
    /**
     * Skip to the previous track in the queue
     */
    fun skipToPrevious() {
        val controller = mediaControllerRepository.getMediaController()
        if (controller != null && controller.hasPreviousMediaItem()) {
            Log.d(TAG, "Skipping to previous track")
            controller.seekToPreviousMediaItem()
        } else {
            Log.d(TAG, "No previous track available")
        }
    }
    
    /**
     * Skip to a specific track index in the queue
     */
    fun skipToIndex(index: Int) {
        val controller = mediaControllerRepository.getMediaController()
        if (controller != null && index >= 0 && index < controller.mediaItemCount) {
            Log.d(TAG, "Skipping to track index $index")
            controller.seekTo(index, 0)
        } else {
            Log.e(TAG, "Invalid track index: $index")
        }
    }
    
    /**
     * Clear the queue
     */
    fun clearQueue() {
        val controller = mediaControllerRepository.getMediaController()
        if (controller != null) {
            Log.d(TAG, "Clearing queue")
            controller.clearMediaItems()
            _currentRecording.value = null
        }
    }
    
    /**
     * Create a MediaItem for a track with proper metadata
     */
    private suspend fun createMediaItem(track: Track, downloadUrl: String, recording: Recording): MediaItem {
        // Resolve local file URL if available
        val resolvedUrl = localFileResolver.resolveLocalFile(downloadUrl) ?: downloadUrl
        
        val metadata = MediaMetadata.Builder()
            .setTitle(track.displayTitle)
            .setArtist("${recording.concertVenue ?: "Unknown Venue"}, ${recording.concertLocation ?: "Unknown Location"}")
            .setAlbumTitle("${recording.concertDate} - ${recording.concertVenue ?: "Unknown Venue"}")
            .setDisplayTitle(track.displayTitle)
            .build()
        
        return MediaItem.Builder()
            .setUri(android.net.Uri.parse(resolvedUrl))
            .setMediaId("${recording.identifier}_${track.filename}") // Use stable recordingId_filename
            .setMediaMetadata(metadata)
            .build()
    }
    
    /**
     * Find a track in the current queue
     */
    private fun findTrackInQueue(track: Track, controller: MediaController): QueueItem? {
        val downloadUrl = track.audioFile?.downloadUrl ?: return null
        
        for (i in 0 until controller.mediaItemCount) {
            val mediaItem = controller.getMediaItemAt(i)
            if (mediaItem.mediaId == downloadUrl) {
                return QueueItem(
                    index = i,
                    title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                    mediaId = mediaItem.mediaId ?: "",
                    isCurrent = i == controller.currentMediaItemIndex
                )
            }
        }
        return null
    }
}

/**
 * Represents an item in the playback queue
 */
data class QueueItem(
    val index: Int,
    val title: String,
    val artist: String,
    val mediaId: String,
    val isCurrent: Boolean
)