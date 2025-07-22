package com.deadarchive.core.media.player

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event-driven monitor that saves track state immediately when tracks change.
 * 
 * Replaces the old polling-based system with Media3 event listeners to eliminate
 * race conditions and ensure accurate track index/position saving.
 */
@Singleton
class LastPlayedTrackMonitor @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository,
    private val lastPlayedTrackService: LastPlayedTrackService
) {
    
    companion object {
        private const val TAG = "LastPlayedTrackMonitor"
        private const val DEBOUNCE_DELAY_MS = 2000L // Wait 2s after track change before saving
        private const val POSITION_SAVE_INTERVAL_MS = 10000L // Save position every 10s during playback
    }
    
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isMonitoring = false
    private var debounceJob: Job? = null
    private var positionSaveJob: Job? = null
    
    /**
     * Start monitoring track changes and position updates
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Already monitoring, skipping start")
            return
        }
        
        Log.d(TAG, "Starting event-driven track monitoring")
        isMonitoring = true
        
        // Start listening to track changes
        startTrackChangeMonitoring()
        
        // Start periodic position saving during playback
        startPositionSaveMonitoring()
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        Log.d(TAG, "Stopping track monitoring")
        isMonitoring = false
        debounceJob?.cancel()
        positionSaveJob?.cancel()
    }
    
    /**
     * Monitor track changes using currentTrackUrl flow (only changes when track actually changes)
     */
    private fun startTrackChangeMonitoring() {
        monitorScope.launch {
            // Listen to changes in currentTrackUrl (which only updates on actual track changes)
            mediaControllerRepository.currentTrackUrl
                .filterNotNull()
                .collect { trackUrl ->
                    if (!isMonitoring) return@collect
                    
                    Log.d(TAG, "Track URL change detected: ${trackUrl.substringAfterLast("/")}")
                    
                    // Cancel previous debounce and start new one
                    debounceJob?.cancel()
                    debounceJob = monitorScope.launch {
                        delay(DEBOUNCE_DELAY_MS)
                        
                        if (isMonitoring) {
                            Log.d(TAG, "Debounce complete, saving track state")
                            saveCurrentStateWithValidation()
                        }
                    }
                }
        }
    }
    
    /**
     * Monitor playback state changes and manage periodic position saving
     */
    private fun startPositionSaveMonitoring() {
        monitorScope.launch {
            // Only listen to isPlaying changes (stable flow)
            mediaControllerRepository.isPlaying.collect { isPlaying ->
                if (!isMonitoring) return@collect
                
                // Cancel previous position job
                positionSaveJob?.cancel()
                
                if (isPlaying) {
                    Log.d(TAG, "Playback started - beginning periodic position saves")
                    positionSaveJob = monitorScope.launch {
                        while (isMonitoring && mediaControllerRepository.isPlaying.value) {
                            delay(POSITION_SAVE_INTERVAL_MS)
                            if (isMonitoring && mediaControllerRepository.isPlaying.value) {
                                Log.d(TAG, "Periodic position save during playback")
                                saveCurrentStateWithValidation()
                            }
                        }
                        Log.d(TAG, "Position save loop ended")
                    }
                } else {
                    Log.d(TAG, "Playback stopped, canceling position monitoring")
                }
            }
        }
    }
    
    /**
     * Save current state with validation to ensure accuracy
     */
    private suspend fun saveCurrentStateWithValidation() {
        try {
            // Check if MediaController is connected
            if (!mediaControllerRepository.isConnected.value) {
                Log.w(TAG, "MediaController not connected, skipping save")
                return
            }
            
            // Get current track info from repository
            val currentTrackInfo = mediaControllerRepository.currentTrackInfo.value
            if (currentTrackInfo == null) {
                Log.w(TAG, "No current track info, skipping save")
                return
            }
            
            // Get current recording ID
            val currentRecordingId = mediaControllerRepository.currentRecordingIdFlow.value
            if (currentRecordingId == null) {
                Log.w(TAG, "No current recording ID, skipping save")
                return
            }
            
            // Get validated state directly from Media3 controller
            val controller = mediaControllerRepository.getMediaController()
            if (controller == null) {
                Log.w(TAG, "MediaController is null, skipping save")
                return
            }
            
            val trackIndex = controller.currentMediaItemIndex
            val currentMediaItem = controller.currentMediaItem
            
            // Get position directly from controller AND from repository to compare
            val controllerPosition = controller.currentPosition
            val repositoryPosition = mediaControllerRepository.currentPosition.value
            
            Log.d(TAG, "Position comparison - Controller: ${controllerPosition}ms, Repository: ${repositoryPosition}ms")
            
            // Use controller position as it's more direct and real-time
            val currentPosition = controllerPosition
            
            // Validate that we have consistent state
            if (currentMediaItem == null) {
                Log.w(TAG, "Current MediaItem is null, skipping save")
                return
            }
            
            // Extract track info from MediaItem for consistency
            val trackTitle = currentMediaItem.mediaMetadata.title?.toString() 
                ?: currentTrackInfo.songTitle
            val trackFilename = currentMediaItem.mediaId 
                ?: currentTrackInfo.filename
            
            // Additional validation: ensure trackIndex is reasonable
            if (trackIndex < 0 || trackIndex >= controller.mediaItemCount) {
                Log.w(TAG, "Invalid track index: $trackIndex (total: ${controller.mediaItemCount}), skipping save")
                return
            }
            
            Log.d(TAG, "Saving validated state: track=$trackIndex, title='$trackTitle', position=${currentPosition}ms")
            
            lastPlayedTrackService.saveCurrentTrack(
                recordingId = currentRecordingId,
                trackIndex = trackIndex,
                positionMs = currentPosition,
                trackTitle = trackTitle,
                trackFilename = trackFilename
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current state", e)
        }
    }
}