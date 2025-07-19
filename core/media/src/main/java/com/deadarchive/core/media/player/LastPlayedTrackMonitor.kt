package com.deadarchive.core.media.player

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors current playback and continuously saves the last played track.
 * 
 * This runs in the background and saves current track + position every few seconds,
 * so we always have the latest state for restoration.
 */
@Singleton
class LastPlayedTrackMonitor @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val lastPlayedTrackService: LastPlayedTrackService
) {
    
    companion object {
        private const val TAG = "LastPlayedTrackMonitor"
        private const val SAVE_INTERVAL_MS = 5000L // Save every 5 seconds
    }
    
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    
    /**
     * Start monitoring and saving current playback state
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring, skipping start")
            return
        }
        
        Log.d(TAG, "Starting last played track monitoring")
        isMonitoring = true
        
        monitorScope.launch {
            while (isActive && isMonitoring) {
                try {
                    saveCurrentStateIfPlaying()
                    delay(SAVE_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                    delay(SAVE_INTERVAL_MS) // Still wait before retrying
                }
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping last played track monitoring")
        isMonitoring = false
    }
    
    /**
     * Save current state if there's something playing
     */
    private suspend fun saveCurrentStateIfPlaying() {
        try {
            // Check if MediaController is connected
            if (!mediaControllerRepository.isConnected.value) {
                return
            }
            
            // Get current track info
            val currentTrackInfo = mediaControllerRepository.currentTrackInfo.value
            if (currentTrackInfo == null) {
                return
            }
            
            // Get current position
            val currentPosition = mediaControllerRepository.currentPosition.value
            
            // Get current recording ID
            val currentRecordingId = mediaControllerRepository.currentRecordingIdFlow.value
            if (currentRecordingId == null) {
                return
            }
            
            // Save the current state
            lastPlayedTrackService.saveCurrentTrack(
                recordingId = currentRecordingId,
                trackIndex = currentTrackInfo.trackNumber ?: 0,
                positionMs = currentPosition,
                trackTitle = currentTrackInfo.songTitle,
                trackFilename = currentTrackInfo.filename
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current state", e)
        }
    }
}