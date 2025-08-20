package com.deadly.core.media.api

import com.deadly.core.model.AudioFile
import com.deadly.core.model.Recording
import com.deadly.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for media playback operations
 * Abstracts the underlying media player implementation
 */
interface MediaPlayer {
    /**
     * Current playback state
     */
    val playbackState: StateFlow<PlaybackState>
    
    /**
     * Current position in the playing track in milliseconds
     */
    val currentPosition: StateFlow<Long>
    
    /**
     * Current track duration in milliseconds
     */
    val duration: StateFlow<Long>
    
    /**
     * Currently playing track
     */
    val currentTrack: StateFlow<Track?>
    
    /**
     * Currently playing recording
     */
    val currentRecording: StateFlow<Recording?>
    
    /**
     * Queue of tracks
     */
    val queue: StateFlow<List<Track>>
    
    /**
     * Play a track
     * @param track The track to play
     * @param recordingId The ID of the recording the track belongs to
     */
    suspend fun play(track: Track, recordingId: String)
    
    /**
     * Play a track at a specific position
     * @param track The track to play
     * @param recordingId The ID of the recording the track belongs to
     * @param position The position in milliseconds to start playback
     */
    suspend fun playFromPosition(track: Track, recordingId: String, position: Long)
    
    /**
     * Play all tracks from a recording
     * @param recording The recording to play
     * @param startTrackIndex The index of the track to start playing (default 0)
     */
    suspend fun playRecording(recording: Recording, startTrackIndex: Int = 0)
    
    /**
     * Pause playback
     */
    fun pause()
    
    /**
     * Resume playback
     */
    fun resume()
    
    /**
     * Stop playback
     */
    fun stop()
    
    /**
     * Skip to the next track in the queue
     */
    fun skipToNext()
    
    /**
     * Skip to the previous track in the queue
     */
    fun skipToPrevious()
    
    /**
     * Seek to a specific position in the current track
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)
    
    /**
     * Set the queue of tracks to play
     * @param tracks List of tracks to queue
     * @param recordingId The ID of the recording the tracks belong to
     * @param startIndex The index to start playing from
     */
    suspend fun setQueue(tracks: List<Track>, recordingId: String, startIndex: Int = 0)
    
    /**
     * Add tracks to the end of the current queue
     * @param tracks List of tracks to add
     * @param recordingId The ID of the recording the tracks belong to
     */
    suspend fun addToQueue(tracks: List<Track>, recordingId: String)
    
    /**
     * Clear the playback queue
     */
    fun clearQueue()
    
    /**
     * Set audio files for a track
     * @param trackId The track ID
     * @param audioFiles List of audio files available for the track
     */
    suspend fun setAudioFilesForTrack(trackId: String, audioFiles: List<AudioFile>)
    
    /**
     * Save playback state for later restoration
     */
    suspend fun savePlaybackState()
    
    /**
     * Restore previous playback state if available
     * @return True if state was restored, false otherwise
     */
    suspend fun restorePlaybackState(): Boolean
}

/**
 * Data class representing playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val currentIndex: Int = 0,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

/**
 * Repeat mode for playback
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL
}