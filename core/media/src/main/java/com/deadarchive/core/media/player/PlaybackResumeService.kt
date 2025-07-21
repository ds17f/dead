package com.deadarchive.core.media.player

import android.util.Log
import com.deadarchive.core.data.repository.PlaybackHistoryRepository
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.database.PlaybackHistoryEntity
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for resuming playback from the last interrupted track.
 * 
 * When the app is force-stopped or crashes, this service can restore
 * the user's playback state by finding the last incomplete track from
 * playback history and resuming from the saved position.
 * 
 * Key features:
 * - Only resumes tracks that were meaningfully played (>30 seconds)
 * - Doesn't resume tracks that were completed (>90% played)
 * - Restores both track position and queue context
 * - Respects user preferences for resume behavior
 */
@Singleton
class PlaybackResumeService @Inject constructor(
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val showRepository: ShowRepository,
    private val queueManager: QueueManager,
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    companion object {
        private const val TAG = "PlaybackResumeService"
        private const val MIN_RESUME_POSITION_MS = 30_000L // 30 seconds
        private const val COMPLETION_THRESHOLD = 0.9f // 90%
    }
    
    /**
     * Attempt to resume the last incomplete track if conditions are met
     */
    suspend fun attemptResumeLastTrack() {
        try {
            Log.d(TAG, "Checking for last incomplete track to resume")
            
            val lastTrack = playbackHistoryRepository.getLastIncompleteTrack()
            if (lastTrack == null) {
                Log.d(TAG, "No incomplete track found to resume")
                return
            }
            
            if (shouldResumeTrack(lastTrack)) {
                Log.d(TAG, "Attempting to resume track: ${lastTrack.trackTitle}")
                restorePlayback(lastTrack)
            } else {
                Log.d(TAG, "Track doesn't meet resume criteria: ${lastTrack.trackTitle}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume last track", e)
        }
    }
    
    /**
     * Determine if a track should be resumed based on playback criteria
     */
    private fun shouldResumeTrack(track: PlaybackHistoryEntity): Boolean {
        // Must have meaningful position
        if (track.finalPosition < MIN_RESUME_POSITION_MS) {
            Log.d(TAG, "Track position too short for resume: ${track.finalPosition}ms")
            return false
        }
        
        // Must not be completed
        if (track.wasCompleted) {
            Log.d(TAG, "Track was completed, not resuming")
            return false
        }
        
        // Check completion percentage if track duration is known
        val trackDuration = track.trackDuration
        if (trackDuration != null && trackDuration > 0) {
            val completionPercent = track.finalPosition.toFloat() / trackDuration.toFloat()
            if (completionPercent >= COMPLETION_THRESHOLD) {
                Log.d(TAG, "Track was nearly completed (${(completionPercent * 100).toInt()}%), not resuming")
                return false
            }
        }
        
        // Must be recent (within last 24 hours)
        val age = System.currentTimeMillis() - track.playbackTimestamp
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        if (age > maxAge) {
            Log.d(TAG, "Track too old to resume: ${age / (60 * 60 * 1000)}h ago")
            return false
        }
        
        return true
    }
    
    /**
     * Restore playback state for the given track
     */
    private suspend fun restorePlayback(track: PlaybackHistoryEntity) {
        try {
            // Wait for MediaController to be ready
            waitForMediaControllerConnection()
            
            // Get the recording and find the track
            val recording = showRepository.getRecordingById(track.recordingId)
            if (recording == null) {
                Log.e(TAG, "Could not find recording: ${track.recordingId}")
                return
            }
            
            Log.d(TAG, "Found recording: ${recording.identifier}")
            
            // Find the track within the recording
            val trackIndex = recording.tracks.indexOfFirst { audioFile ->
                audioFile.audioFile?.downloadUrl == track.trackUrl ||
                audioFile.filename == track.trackFilename
            }
            
            if (trackIndex < 0) {
                Log.e(TAG, "Could not find track in recording: ${track.trackFilename}")
                return
            }
            
            Log.d(TAG, "Found track at index $trackIndex: ${track.trackTitle}")
            
            // Load the show into the queue starting at the track (without auto-playing)
            queueManager.loadShow(recording, trackIndex, autoPlay = false)
            
            // Wait a moment for the queue to load
            delay(1500)
            
            // Seek to the saved position
            Log.d(TAG, "Seeking to position: ${track.finalPosition}ms")
            mediaControllerRepository.seekTo(track.finalPosition)
            
            // Optionally start playing immediately (could be user preference)
            // mediaControllerRepository.play()
            
            Log.d(TAG, "Successfully resumed playback of ${track.trackTitle} at ${track.finalPosition}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore playback for track: ${track.trackTitle}", e)
        }
    }
    
    /**
     * Wait for MediaController to be connected before attempting playback operations
     */
    private suspend fun waitForMediaControllerConnection() {
        var attempts = 0
        val maxAttempts = 10
        
        while (!mediaControllerRepository.isConnected.value && attempts < maxAttempts) {
            Log.d(TAG, "Waiting for MediaController connection... attempt ${attempts + 1}")
            delay(500)
            attempts++
        }
        
        if (!mediaControllerRepository.isConnected.value) {
            throw IllegalStateException("MediaController not connected after waiting")
        }
        
        Log.d(TAG, "MediaController is connected, proceeding with resume")
    }
    
    /**
     * Get information about the last incomplete track without resuming it
     */
    suspend fun getLastIncompleteTrackInfo(): ResumeInfo? {
        val lastTrack = playbackHistoryRepository.getLastIncompleteTrack()
        if (lastTrack != null && shouldResumeTrack(lastTrack)) {
            val recording = showRepository.getRecordingById(lastTrack.recordingId)
            return ResumeInfo(
                trackTitle = lastTrack.trackTitle,
                showDate = recording?.concertDate ?: "Unknown",
                venue = recording?.concertVenue ?: "Unknown Venue",
                position = lastTrack.finalPosition,
                duration = lastTrack.trackDuration,
                playbackTime = lastTrack.playbackTimestamp
            )
        }
        return null
    }
    
    /**
     * Information about a track that can be resumed
     */
    data class ResumeInfo(
        val trackTitle: String,
        val showDate: String,
        val venue: String,
        val position: Long,
        val duration: Long?,
        val playbackTime: Long
    )
}