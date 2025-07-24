package com.deadarchive.core.media.player

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.deadarchive.core.media.player.service.MediaServiceConnector
import com.deadarchive.core.media.player.service.PlaybackCommandProcessor
import com.deadarchive.core.media.player.service.PlaybackStateSync
import com.deadarchive.core.model.CurrentTrackInfo
import com.deadarchive.core.model.Recording
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refactored MediaController repository using service composition pattern.
 * 
 * This facade delegates specialized operations to focused services while maintaining
 * the exact same public interface for backward compatibility. All existing UI components
 * can continue using this repository without any changes.
 * 
 * Architecture:
 * - MediaServiceConnector: Connection lifecycle and service binding
 * - PlaybackStateSync: StateFlow synchronization and position updates  
 * - PlaybackCommandProcessor: Command handling and queue operations
 * - MediaControllerRepository: Facade coordinator using composition
 */
@UnstableApi
@Singleton
class MediaControllerRepositoryRefactored @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaServiceConnector: MediaServiceConnector,
    private val playbackStateSync: PlaybackStateSync,
    private val playbackCommandProcessor: PlaybackCommandProcessor
) {
    
    companion object {
        private const val TAG = "MediaControllerRepository"
    }
    
    // QueueStateManager reference for queue operations
    private var queueStateManager: QueueStateManager? = null
    
    // Initialize services when repository is created
    init {
        Log.d(TAG, "Initializing MediaControllerRepository with service composition")
        connectToService()
    }
    
    // === PUBLIC INTERFACE - StateFlow Properties (Identical to Original) ===
    
    val isPlaying: StateFlow<Boolean> = playbackStateSync.isPlaying
    val currentPosition: StateFlow<Long> = playbackStateSync.currentPosition
    val duration: StateFlow<Long> = playbackStateSync.duration
    val currentTrack: StateFlow<MediaItem?> = playbackStateSync.currentTrack
    val currentTrackUrl: StateFlow<String?> = playbackStateSync.currentTrackUrl
    val playbackState: StateFlow<Int> = playbackStateSync.playbackState
    val lastError: StateFlow<PlaybackException?> = playbackStateSync.lastError
    val isConnected: StateFlow<Boolean> = mediaServiceConnector.isConnected
    
    // Queue state flows
    val queueUrls: StateFlow<List<String>> = playbackStateSync.queueUrls
    val queueIndex: StateFlow<Int> = playbackStateSync.queueIndex
    val queueMetadata: StateFlow<List<Pair<String, String>>> = playbackStateSync.queueMetadata
    val hasNext: StateFlow<Boolean> = playbackStateSync.hasNext
    val hasPrevious: StateFlow<Boolean> = playbackStateSync.hasPrevious
    
    // Current track info
    val currentRecordingId: StateFlow<String?> = playbackStateSync.currentRecordingId
    val currentTrackInfo: StateFlow<CurrentTrackInfo?> = playbackStateSync.currentTrackInfo
    
    // === PUBLIC INTERFACE - Methods (Identical to Original) ===
    
    /**
     * Get current recording ID
     */
    fun getCurrentRecordingId(): String? = playbackStateSync.currentRecordingId.value
    
    /**
     * Update queue context for proper MediaController synchronization
     */
    fun updateQueueContext(queueUrls: List<String>, currentIndex: Int = 0) {
        playbackCommandProcessor.updateQueueContext(queueUrls, currentIndex)
        playbackStateSync.updateQueueState(queueUrls, currentIndex, emptyList())
    }
    
    /**
     * Update queue context with metadata
     */
    fun updateQueueContext(
        queueUrls: List<String>, 
        currentIndex: Int = 0,
        queueMetadata: List<Pair<String, String>> = emptyList()
    ) {
        playbackCommandProcessor.updateQueueContext(queueUrls, currentIndex, queueMetadata)
        playbackStateSync.updateQueueState(queueUrls, currentIndex, queueMetadata)
    }
    
    /**
     * Update queue context with Recording data for enriched metadata
     */
    fun updateQueueContext(
        queueUrls: List<String>, 
        currentIndex: Int = 0,
        queueMetadata: List<Pair<String, String>> = emptyList(),
        recording: Recording?
    ) {
        playbackCommandProcessor.updateQueueContext(queueUrls, currentIndex, queueMetadata)
        playbackStateSync.updateQueueState(queueUrls, currentIndex, queueMetadata, recording)
    }
    
    /**
     * Play a single track from Archive.org with queue context
     */
    fun playTrack(url: String, title: String, artist: String? = null) {
        val currentRecordingId = getCurrentRecordingId()
        playbackCommandProcessor.playTrack(url, title, artist, currentRecordingId)
    }
    
    /**
     * Play a playlist starting at specific index
     */
    fun playPlaylist(urls: List<String>, startIndex: Int = 0) {
        playbackCommandProcessor.playPlaylist(urls, startIndex)
    }
    
    /**
     * Resume or start playback
     */
    fun play() {
        playbackCommandProcessor.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        playbackCommandProcessor.pause()
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        playbackCommandProcessor.stop()
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Long) {
        playbackCommandProcessor.seekTo(position)
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        playbackCommandProcessor.skipToNext()
    }
    
    /**
     * Skip to previous track
     */
    fun skipToPrevious() {
        playbackCommandProcessor.skipToPrevious()
    }
    
    /**
     * Set repeat mode
     */
    fun setRepeatMode(repeatMode: Int) {
        playbackCommandProcessor.setRepeatMode(repeatMode)
    }
    
    /**
     * Set shuffle mode
     */
    fun setShuffleMode(shuffleMode: Boolean) {
        playbackCommandProcessor.setShuffleMode(shuffleMode)
    }
    
    /**
     * Manual position update (for UI responsiveness)
     */
    fun updatePosition() {
        // This is now handled automatically by PlaybackStateSync
        // Left as no-op for backward compatibility
    }
    
    /**
     * Get MediaController instance (for advanced use cases)
     */
    fun getMediaController(): MediaController? {
        return mediaServiceConnector.getMediaController()
    }
    
    /**
     * Set QueueStateManager for queue operations
     */
    fun setQueueStateManager(queueStateManager: QueueStateManager) {
        this.queueStateManager = queueStateManager
        playbackCommandProcessor.setQueueStateManager(queueStateManager)
    }
    
    /**
     * Release resources and disconnect from service
     */
    fun release() {
        Log.d(TAG, "Releasing MediaControllerRepository resources")
        playbackStateSync.release()
        mediaServiceConnector.disconnect()
    }
    
    // === PRIVATE METHODS - Service Coordination ===
    
    /**
     * Connect to MediaSessionService through MediaServiceConnector
     */
    private fun connectToService() {
        Log.d(TAG, "Connecting to MediaSession service")
        
        mediaServiceConnector.connectToService(
            onSuccess = { controller ->
                onServiceConnected(controller)
            },
            onFailure = { error ->
                onServiceConnectionFailed(error)
            }
        )
    }
    
    /**
     * Called when service connection succeeds
     */
    private fun onServiceConnected(controller: MediaController) {
        Log.d(TAG, "Service connected successfully - setting up services")
        
        // Configure all services with the connected MediaController
        playbackStateSync.setupMediaControllerListener(controller)
        playbackCommandProcessor.setMediaController(controller)
        
        // Set QueueStateManager if available
        queueStateManager?.let { qsm ->
            playbackCommandProcessor.setQueueStateManager(qsm)
        }
        
        Log.d(TAG, "All services configured - MediaControllerRepository ready")
    }
    
    /**
     * Called when service connection fails
     */
    private fun onServiceConnectionFailed(error: Exception) {
        Log.e(TAG, "Service connection failed", error)
        // Error state is handled by MediaServiceConnector's isConnected flow
    }
}