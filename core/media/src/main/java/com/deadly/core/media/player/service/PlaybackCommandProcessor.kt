package com.deadly.core.media.player.service

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.deadly.core.media.player.LocalFileResolver
import com.deadly.core.media.player.QueueStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for command handling and queue operations.
 * Processes all playback commands and manages queue synchronization with MediaController.
 */
@UnstableApi
@Singleton
class PlaybackCommandProcessor @Inject constructor(
    private val localFileResolver: LocalFileResolver
) {
    
    companion object {
        private const val TAG = "PlaybackCommandProcessor"
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var mediaController: MediaController? = null
    private var queueStateManager: QueueStateManager? = null
    
    // Queue context for proper MediaController synchronization
    private var currentQueueUrls: List<String> = emptyList()
    private var currentQueueIndex: Int = 0
    
    /**
     * Set the MediaController for command processing
     */
    fun setMediaController(controller: MediaController) {
        this.mediaController = controller
        Log.d(TAG, "MediaController set for command processing")
    }
    
    /**
     * Set the QueueStateManager for queue operations
     */
    fun setQueueStateManager(queueStateManager: QueueStateManager) {
        this.queueStateManager = queueStateManager
        Log.d(TAG, "QueueStateManager set for command processing")
    }
    
    /**
     * Update queue context for proper MediaController synchronization
     */
    fun updateQueueContext(queueUrls: List<String>, currentIndex: Int = 0) {
        Log.d(TAG, "updateQueueContext: ${queueUrls.size} tracks, index: $currentIndex")
        this.currentQueueUrls = queueUrls
        this.currentQueueIndex = currentIndex
        
        // Sync queue to MediaController
        syncQueueToMediaController()
        
        // QueueStateManager observes queue state changes automatically
    }
    
    /**
     * Update queue context with metadata
     */
    fun updateQueueContext(
        queueUrls: List<String>, 
        currentIndex: Int = 0,
        queueMetadata: List<Pair<String, String>> = emptyList()
    ) {
        Log.d(TAG, "updateQueueContext with metadata: ${queueUrls.size} tracks, ${queueMetadata.size} metadata entries")
        this.currentQueueUrls = queueUrls
        this.currentQueueIndex = currentIndex
        
        // Sync queue to MediaController with metadata
        syncQueueToMediaController(queueMetadata)
        
        // QueueStateManager observes queue state changes automatically
    }
    
    /**
     * Synchronize current queue context to MediaController
     */
    private fun syncQueueToMediaController(metadata: List<Pair<String, String>> = emptyList()) {
        val controller = mediaController
        if (controller == null) {
            Log.w(TAG, "Cannot sync queue - MediaController not available")
            return
        }
        
        if (currentQueueUrls.isEmpty()) {
            Log.d(TAG, "No queue to sync - empty queue")
            return
        }
        
        Log.d(TAG, "=== SYNCING QUEUE TO MEDIACONTROLLER ===")
        Log.d(TAG, "Queue size: ${currentQueueUrls.size}")
        Log.d(TAG, "Current index: $currentQueueIndex")
        Log.d(TAG, "Metadata entries: ${metadata.size}")
        
        try {
            // Create MediaItems with enriched metadata
            val mediaItems = currentQueueUrls.mapIndexed { index, url ->
                val title = if (metadata.isNotEmpty() && index < metadata.size) {
                    metadata[index].first
                } else {
                    extractSongTitleFromUrl(url)
                }
                val artist = if (metadata.isNotEmpty() && index < metadata.size) {
                    metadata[index].second
                } else {
                    "Grateful Dead"
                }
                
                createEnrichedMediaItem(url, title, artist)
            }
            
            // Set the entire playlist
            controller.setMediaItems(mediaItems, currentQueueIndex, 0L)
            
            Log.d(TAG, "Queue synchronized successfully")
            Log.d(TAG, "MediaController queue size: ${controller.mediaItemCount}")
            Log.d(TAG, "MediaController current index: ${controller.currentMediaItemIndex}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing queue to MediaController", e)
        }
    }
    
    /**
     * Play a single track from Archive.org with queue context.
     * Now supports offline playback by checking for local files first.
     */
    fun playTrack(url: String, title: String, artist: String? = null, currentRecordingId: String? = null) {
        Log.d(TAG, "=== PLAY TRACK COMMAND ===")
        Log.d(TAG, "Original URL: $url")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Artist: $artist")
        
        val controller = mediaController
        if (controller == null) {
            Log.e(TAG, "=== COMMAND FAILED: MediaController not connected ===")
            return
        }
        
        // Check for local file first (offline playback support)
        coroutineScope.launch {
            val finalUrl = resolvePlaybackUrl(url, currentRecordingId) ?: url
            playTrackWithResolvedUrl(finalUrl, url, title, artist, controller)
        }
    }
    
    /**
     * Resolve the best URL for playback (local file if available, otherwise streaming URL)
     */
    private suspend fun resolvePlaybackUrl(originalUrl: String, currentRecordingId: String?): String? {
        return try {
            val localFileUri = localFileResolver.resolveLocalFile(originalUrl, currentRecordingId)
            if (localFileUri != null) {
                Log.i(TAG, "🎵 OFFLINE PLAYBACK: Using local file for $originalUrl")
                localFileUri
            } else {
                Log.d(TAG, "📡 STREAMING PLAYBACK: No local file found for $originalUrl")
                null // Will use original URL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local file for $originalUrl", e)
            null
        }
    }
    
    /**
     * Play track with resolved URL (either local or streaming)
     */
    private fun playTrackWithResolvedUrl(
        resolvedUrl: String,
        originalUrl: String, 
        title: String, 
        artist: String?,
        controller: MediaController
    ) {
        Log.d(TAG, "=== PLAYING TRACK WITH RESOLVED URL ===")
        Log.d(TAG, "Resolved URL: $resolvedUrl")
        Log.d(TAG, "Original URL: $originalUrl")
        
        try {
            // Create enriched MediaItem
            val mediaItem = createEnrichedMediaItem(resolvedUrl, title, artist ?: "Grateful Dead")
            
            // Set single item and play
            controller.setMediaItem(mediaItem, true)
            
            Log.d(TAG, "MediaItem set successfully")
            Log.d(TAG, "Controller will begin playback automatically")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track with resolved URL", e)
        }
    }
    
    /**
     * Play a playlist starting at specific index
     */
    fun playPlaylist(urls: List<String>, startIndex: Int = 0) {
        Log.d(TAG, "=== PLAY PLAYLIST COMMAND ===")
        Log.d(TAG, "Playlist size: ${urls.size}")
        Log.d(TAG, "Start index: $startIndex")
        
        val controller = mediaController
        if (controller == null) {
            Log.e(TAG, "=== COMMAND FAILED: MediaController not connected ===")
            return
        }
        
        if (urls.isEmpty()) {
            Log.w(TAG, "Empty playlist - nothing to play")
            return
        }
        
        try {
            // Create MediaItems for entire playlist
            val mediaItems = urls.map { url ->
                val title = extractSongTitleFromUrl(url)
                createEnrichedMediaItem(url, title, "Grateful Dead")
            }
            
            // Update queue context
            updateQueueContext(urls, startIndex)
            
            // Set playlist and start playing
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
            
            Log.d(TAG, "Playlist set and playback started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing playlist", e)
        }
    }
    
    /**
     * Resume or start playback
     */
    fun play() {
        Log.d(TAG, "=== PLAY COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Controller state before play: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
            controller.play()
            Log.d(TAG, "Controller state after play: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
        } else {
            Log.w(TAG, "=== PLAY COMMAND FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        Log.d(TAG, "=== PAUSE COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            Log.d(TAG, "Controller state before pause: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
            controller.pause()
            Log.d(TAG, "Controller state after pause: isPlaying=${controller.isPlaying}, playbackState=${controller.playbackState}")
        } else {
            Log.w(TAG, "=== PAUSE COMMAND FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        Log.d(TAG, "stop() called")
        mediaController?.stop() ?: Log.w(TAG, "MediaController not connected")
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Long) {
        Log.d(TAG, "seekTo: $position")
        mediaController?.seekTo(position) ?: Log.w(TAG, "MediaController not connected")
    }
    
    /**
     * Skip to next track (queue-aware)
     */
    fun skipToNext() {
        Log.d(TAG, "=== SKIP TO NEXT COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            if (controller.hasNextMediaItem()) {
                Log.d(TAG, "MediaController has next item - calling seekToNextMediaItem()")
                controller.seekToNextMediaItem()
            } else {
                Log.d(TAG, "No next track available in MediaController")
            }
        } else {
            Log.w(TAG, "=== SKIP TO NEXT FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Skip to previous track (queue-aware)
     */
    fun skipToPrevious() {
        Log.d(TAG, "=== SKIP TO PREVIOUS COMMAND ===")
        val controller = mediaController
        if (controller != null) {
            if (controller.hasPreviousMediaItem()) {
                Log.d(TAG, "MediaController has previous item - calling seekToPreviousMediaItem()")
                controller.seekToPreviousMediaItem()
            } else {
                Log.d(TAG, "No previous track available in MediaController")
            }
        } else {
            Log.w(TAG, "=== SKIP TO PREVIOUS FAILED: MediaController not connected ===")
        }
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: Int) {
        Log.d(TAG, "setRepeatMode: $repeatMode")
        mediaController?.repeatMode = repeatMode
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleMode: Boolean) {
        Log.d(TAG, "setShuffleMode: $shuffleMode")
        mediaController?.shuffleModeEnabled = shuffleMode
    }
    
    /**
     * Create enriched MediaItem with proper metadata and URI handling
     */
    private fun createEnrichedMediaItem(url: String, title: String, artist: String): MediaItem {
        return try {
            // Create metadata
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumArtist("Grateful Dead")
                .build()
            
            // Build MediaItem with URI and metadata
            MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMediaMetadata(metadata)
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error creating enriched MediaItem for $url", e)
            // Fallback to basic MediaItem
            MediaItem.fromUri(Uri.parse(url))
        }
    }
    
    /**
     * Extract song title from Archive.org URL
     */
    private fun extractSongTitleFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val filename = uri.lastPathSegment ?: "Unknown Track"
            val baseName = filename.substringBeforeLast(".")
            // Remove track number prefix if present
            baseName.replace(Regex("^\\d+\\s*[-.]\\s*"), "").ifEmpty { baseName }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from URL: $url", e)
            "Unknown Track"
        }
    }
}