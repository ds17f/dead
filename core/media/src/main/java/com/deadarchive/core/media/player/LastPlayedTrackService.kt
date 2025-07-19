package com.deadarchive.core.media.player

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.deadarchive.core.data.repository.ShowRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple service for persisting and restoring the last played track.
 * 
 * Works like Spotify - just remembers the last track and position,
 * and restores it when the app starts so the mini player appears.
 */
@Singleton
class LastPlayedTrackService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val showRepository: ShowRepository,
    private val queueManager: QueueManager,
    private val mediaControllerRepository: MediaControllerRepository
) {
    
    companion object {
        private const val TAG = "LastPlayedTrackService"
        private const val PREFS_NAME = "last_played_track"
        private const val KEY_RECORDING_ID = "recording_id"
        private const val KEY_TRACK_INDEX = "track_index"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_TRACK_TITLE = "track_title"
        private const val KEY_TRACK_FILENAME = "track_filename"
        private const val KEY_LAST_SAVED = "last_saved"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save the current track and position
     */
    fun saveCurrentTrack(
        recordingId: String,
        trackIndex: Int,
        positionMs: Long,
        trackTitle: String,
        trackFilename: String
    ) {
        Log.d(TAG, "Saving last played track: $trackTitle at ${positionMs}ms")
        
        prefs.edit()
            .putString(KEY_RECORDING_ID, recordingId)
            .putInt(KEY_TRACK_INDEX, trackIndex)
            .putLong(KEY_POSITION_MS, positionMs)
            .putString(KEY_TRACK_TITLE, trackTitle)
            .putString(KEY_TRACK_FILENAME, trackFilename)
            .putLong(KEY_LAST_SAVED, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Get the last played track info
     */
    fun getLastPlayedTrack(): LastPlayedTrack? {
        val recordingId = prefs.getString(KEY_RECORDING_ID, null)
        val trackIndex = prefs.getInt(KEY_TRACK_INDEX, -1)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val trackTitle = prefs.getString(KEY_TRACK_TITLE, null)
        val trackFilename = prefs.getString(KEY_TRACK_FILENAME, null)
        val lastSaved = prefs.getLong(KEY_LAST_SAVED, 0L)
        
        return if (recordingId != null && trackIndex >= 0 && trackTitle != null && trackFilename != null) {
            LastPlayedTrack(
                recordingId = recordingId,
                trackIndex = trackIndex,
                positionMs = positionMs,
                trackTitle = trackTitle,
                trackFilename = trackFilename,
                lastSavedTime = lastSaved
            )
        } else {
            null
        }
    }
    
    /**
     * Restore the last played track to the MediaController
     * This will make the mini player appear immediately
     */
    suspend fun restoreLastPlayedTrack() {
        try {
            val lastTrack = getLastPlayedTrack()
            if (lastTrack == null) {
                Log.d(TAG, "No last played track to restore")
                return
            }
            
            Log.d(TAG, "Restoring last played track: ${lastTrack.trackTitle} at ${lastTrack.positionMs}ms")
            
            // Wait for MediaController to be ready
            waitForMediaControllerConnection()
            
            // Get the recording and load it
            val recording = showRepository.getRecordingById(lastTrack.recordingId)
            if (recording == null) {
                Log.e(TAG, "Could not find recording: ${lastTrack.recordingId}")
                return
            }
            
            // Load the show into the queue starting at the saved track
            queueManager.loadShow(recording, lastTrack.trackIndex)
            
            // Wait a moment for the queue to load
            delay(1000)
            
            // Seek to the saved position
            mediaControllerRepository.seekTo(lastTrack.positionMs)
            
            // Don't auto-play - just restore the state so mini player appears
            Log.d(TAG, "Successfully restored last played track: ${lastTrack.trackTitle}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore last played track", e)
        }
    }
    
    /**
     * Clear the saved last played track
     */
    fun clearLastPlayedTrack() {
        Log.d(TAG, "Clearing last played track")
        prefs.edit().clear().apply()
    }
    
    /**
     * Wait for MediaController to be connected
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
        
        Log.d(TAG, "MediaController is connected, proceeding with restore")
    }
    
    /**
     * Data class for last played track info
     */
    data class LastPlayedTrack(
        val recordingId: String,
        val trackIndex: Int,
        val positionMs: Long,
        val trackTitle: String,
        val trackFilename: String,
        val lastSavedTime: Long
    )
}