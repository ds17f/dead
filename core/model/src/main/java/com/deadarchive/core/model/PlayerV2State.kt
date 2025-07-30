package com.deadarchive.core.model

import kotlin.time.Duration

/**
 * Domain model representing the complete Player V2 state.
 * Combines recording, queue, and playback state into a single source of truth.
 * 
 * Key Design Principles:
 * - Rich domain model with computed properties
 * - Single source of truth for all player state
 * - Cross-feature integration (library, downloads, navigation)
 * - Immutable data structure with functional updates
 */
data class PlayerV2State(
    val currentRecording: PlayerV2Recording? = null,      // Currently loaded recording
    val queue: PlayerV2Queue = PlayerV2Queue.empty(),    // Queue state
    val playbackState: PlaybackState = PlaybackState.IDLE, // Current playback state
    val currentPosition: Duration = Duration.ZERO,        // Current playback position
    val isLoading: Boolean = false,                       // Loading state
    val error: String? = null,                           // Error message
    val navigationHistory: List<String> = emptyList(),   // Recording navigation history
    val settings: PlayerV2Settings = PlayerV2Settings()  // Player settings
) {
    // Current track information (primary source of truth)
    val currentTrack: PlayerV2Track?
        get() = currentRecording?.currentTrack
    
    val currentQueueItem: PlayerV2QueueItem?
        get() = queue.currentItem
    
    // Playback state computed properties
    val isPlaying: Boolean
        get() = playbackState == PlaybackState.PLAYING
    
    val isPaused: Boolean
        get() = playbackState == PlaybackState.PAUSED
    
    val isBuffering: Boolean
        get() = playbackState == PlaybackState.BUFFERING
    
    val canPlay: Boolean
        get() = currentTrack?.canPlay == true && !isLoading
    
    val hasContent: Boolean
        get() = currentRecording != null && currentRecording.playerTracks.isNotEmpty()
    
    val hasError: Boolean
        get() = error != null
    
    // Navigation state
    val hasNext: Boolean
        get() = queue.hasNext
    
    val hasPrevious: Boolean
        get() = queue.hasPrevious
    
    val canNavigateNext: Boolean
        get() = hasNext && queue.nextItem?.let { !it.audioUrl.isNullOrBlank() } == true
    
    val canNavigatePrevious: Boolean
        get() = hasPrevious && queue.previousItem?.let { !it.audioUrl.isNullOrBlank() } == true
    
    // Progress information
    val currentTrackProgress: Float
        get() = currentTrack?.progressPercentage ?: 0f
    
    val overallQueueProgress: Float
        get() = if (queue.size > 0) {
            val completedTracks = (0 until queue.currentIndex).count()
            val currentTrackProgress = currentTrackProgress
            (completedTracks + currentTrackProgress) / queue.size
        } else 0f
    
    val remainingTimeInQueue: Duration
        get() = queue.upcomingItems.fold(Duration.ZERO) { acc, item -> acc + item.duration } +
                (currentTrack?.remainingTime ?: Duration.ZERO)
    
    // Display properties
    val displayTitle: String
        get() = currentTrack?.displayTitle ?: "No track selected"
    
    val displayArtist: String
        get() = currentRecording?.displayTitle ?: "Unknown Artist"
    
    val displayAlbum: String
        get() = currentRecording?.concertDisplayDate ?: ""
    
    val displayStatus: String
        get() = when {
            hasError -> "Error"
            isLoading -> "Loading..."
            isPlaying -> "Playing"
            isPaused -> "Paused"
            hasContent -> "Ready"
            else -> "No content"
        }
    
    // Queue display information
    val queueDisplayTitle: String
        get() = queue.displayTitle
    
    val queuePosition: String
        get() = if (queue.size > 0) {
            "${queue.currentIndex + 1} of ${queue.size}"
        } else "No queue"
    
    // Cross-feature integration
    val isCurrentRecordingInLibrary: Boolean
        get() = currentRecording?.isInLibrary == true
    
    val currentRecordingDownloadStatus: DownloadStatus
        get() = currentRecording?.downloadStatus ?: DownloadStatus.QUEUED
    
    val hasSetlistData: Boolean
        get() = currentRecording?.hasSetlist == true
    
    // Navigation history
    val canNavigateBack: Boolean
        get() = navigationHistory.size > 1
    
    val previousRecordingId: String?
        get() = navigationHistory.getOrNull(navigationHistory.size - 2)
    
    // Functional update methods
    fun updateCurrentRecording(recording: PlayerV2Recording?): PlayerV2State {
        val newQueue = recording?.let { PlayerV2Queue.fromRecording(it) } ?: PlayerV2Queue.empty()
        val newHistory = recording?.let { rec ->
            if (navigationHistory.lastOrNull() != rec.recordingId) {
                navigationHistory + rec.recordingId
            } else navigationHistory
        } ?: navigationHistory
        
        return copy(
            currentRecording = recording,
            queue = newQueue,
            navigationHistory = newHistory,
            error = null
        )
    }
    
    fun updatePlaybackState(state: PlaybackState): PlayerV2State =
        copy(playbackState = state)
    
    fun updateCurrentPosition(position: Duration): PlayerV2State {
        val updatedRecording = currentRecording?.let { recording ->
            recording.updateTrackPlaybackPosition(recording.currentTrackIndex, position)
        }
        return copy(
            currentRecording = updatedRecording,
            currentPosition = position
        )
    }
    
    fun updateCurrentTrackIndex(index: Int): PlayerV2State {
        val updatedRecording = currentRecording?.updateCurrentTrackIndex(index)
        val updatedQueue = queue.updateCurrentIndex(index)
        return copy(
            currentRecording = updatedRecording,
            queue = updatedQueue
        )
    }
    
    fun markTrackAsPlaying(trackIndex: Int): PlayerV2State {
        val updatedRecording = currentRecording?.markTrackAsPlaying(trackIndex)
        return copy(
            currentRecording = updatedRecording,
            playbackState = PlaybackState.PLAYING
        )
    }
    
    fun setLoading(loading: Boolean): PlayerV2State =
        copy(isLoading = loading, error = if (loading) null else error)
    
    fun setError(errorMessage: String?): PlayerV2State =
        copy(error = errorMessage, isLoading = false)
    
    fun clearError(): PlayerV2State =
        copy(error = null)
    
    fun updateQueue(newQueue: PlayerV2Queue): PlayerV2State =
        copy(queue = newQueue)
    
    fun updateLibraryStatus(inLibrary: Boolean): PlayerV2State {
        val updatedRecording = currentRecording?.updateLibraryStatus(inLibrary)
        return copy(currentRecording = updatedRecording)
    }
    
    fun updateDownloadStatus(status: DownloadStatus): PlayerV2State {
        val updatedRecording = currentRecording?.updateDownloadStatus(status)
        return copy(currentRecording = updatedRecording)
    }
    
    fun updateSettings(newSettings: PlayerV2Settings): PlayerV2State =
        copy(settings = newSettings)
    
    companion object {
        /**
         * Create initial empty state
         */
        fun initial(): PlayerV2State = PlayerV2State()
        
        /**
         * Create state with recording loaded
         */
        fun withRecording(recording: PlayerV2Recording): PlayerV2State =
            initial().updateCurrentRecording(recording)
        
        /**
         * Create state with error
         */
        fun withError(errorMessage: String): PlayerV2State =
            initial().setError(errorMessage)
    }
}

/**
 * Playback state enumeration
 */
enum class PlaybackState {
    IDLE,        // Not playing, no content
    BUFFERING,   // Loading/buffering content
    PLAYING,     // Actively playing
    PAUSED,      // Paused but ready to play
    ENDED,       // Finished playing current item
    ERROR        // Playback error occurred
}

/**
 * Player V2 settings and preferences
 */
data class PlayerV2Settings(
    val audioFormatPreferences: List<String> = emptyList(), // Preferred audio formats
    val autoPlay: Boolean = true,                          // Auto-play next track
    val crossfade: Boolean = false,                        // Crossfade between tracks
    val gaplessPlayback: Boolean = true,                   // Gapless playback
    val replayGain: Boolean = false,                       // ReplayGain normalization
    val showSpectrum: Boolean = false,                     // Show audio spectrum
    val savePosition: Boolean = true,                      // Remember playback position
    val backgroundPlayback: Boolean = true                 // Continue in background
)