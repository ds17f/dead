package com.deadly.feature.playlist.service

import android.util.Log
import com.deadly.core.model.CurrentTrackInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MiniPlayerV2ServiceStub - Stub implementation for V2 mini-player development
 * 
 * Provides realistic mock data and behavior for mini-player development.
 * Follows the same patterns as PlayerV2ServiceStub for consistency.
 */
@Singleton
class MiniPlayerV2ServiceStub @Inject constructor() : MiniPlayerV2Service {
    
    companion object {
        private const val TAG = "MiniPlayerV2ServiceStub"
        private const val MOCK_TRACK_DURATION = 8L * 60 * 1000 // 8 minutes in milliseconds
    }
    
    // State flows for mini-player data
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: Flow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(MOCK_TRACK_DURATION)
    override val duration: Flow<Long> = _duration.asStateFlow()
    
    private val _currentRecordingId = MutableStateFlow("gd1977-05-08") // Cornell '77 
    override val currentRecordingId: Flow<String?> = _currentRecordingId.asStateFlow()
    
    // Mock track info for realistic mini-player display
    private val _currentTrackInfo = MutableStateFlow(
        CurrentTrackInfo(
            trackUrl = "https://archive.org/download/gd1977-05-08/gd77-05-08d1t01.mp3",
            recordingId = "gd1977-05-08",
            showId = "gd1977-05-08",
            showDate = "1977-05-08",
            venue = "Barton Hall",
            location = "Cornell University, Ithaca, NY",
            songTitle = "Scarlet Begonias",
            trackNumber = 1,
            filename = "gd77-05-08d1t01.mp3",
            isPlaying = false,
            position = 0L,
            duration = MOCK_TRACK_DURATION
        )
    )
    override val currentTrackInfo: Flow<CurrentTrackInfo?> = _currentTrackInfo.asStateFlow()
    
    // Computed progress based on position and duration
    override val progress: Flow<Float> = combine(
        _currentPosition,
        _duration
    ) { position, duration ->
        if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    private var isInitialized = false
    
    override suspend fun initialize() {
        if (isInitialized) return
        
        Log.d(TAG, "Initializing MiniPlayerV2 stub service")
        isInitialized = true
        
        // Start mock playback position updates
        startPositionUpdates()
    }
    
    override suspend fun togglePlayPause() {
        val currentlyPlaying = _isPlaying.value
        val newState = !currentlyPlaying
        
        Log.d(TAG, "togglePlayPause: $currentlyPlaying -> $newState")
        _isPlaying.value = newState
        
        if (newState) {
            Log.d(TAG, "Mock playback started for: ${_currentTrackInfo.value?.displayTitle}")
        } else {
            Log.d(TAG, "Mock playback paused")
        }
    }
    
    override suspend fun expandToPlayer(recordingId: String?) {
        Log.d(TAG, "expandToPlayer called with recordingId: $recordingId")
        // In real implementation, this would trigger navigation to PlayerV2
    }
    
    override suspend fun cleanup() {
        Log.d(TAG, "Cleaning up MiniPlayerV2 stub service")
        _isPlaying.value = false
        isInitialized = false
    }
    
    /**
     * Simulate playback position updates when playing
     */
    private suspend fun startPositionUpdates() {
        // This would be replaced by real MediaController position updates
        // For now, just simulate position advancement when playing
        while (isInitialized) {
            delay(1000) // Update every second
            
            if (_isPlaying.value) {
                val currentPos = _currentPosition.value
                val duration = _duration.value
                
                if (currentPos < duration) {
                    _currentPosition.value = currentPos + 1000 // Advance 1 second
                } else {
                    // Simulate track ending
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    Log.d(TAG, "Mock track finished, resetting position")
                }
            }
        }
    }
    
    /**
     * Update mock track info for testing different scenarios
     */
    fun setMockTrack(
        title: String,
        date: String,
        venue: String,
        location: String,
        recordingId: String = "gd1977-05-08"
    ) {
        _currentTrackInfo.value = CurrentTrackInfo(
            trackUrl = "https://archive.org/download/$recordingId/${recordingId}d1t01.mp3",
            recordingId = recordingId,
            showId = recordingId,
            showDate = date,
            venue = venue,
            location = location,
            songTitle = title,
            trackNumber = 1,
            filename = "${recordingId}d1t01.mp3",
            isPlaying = _isPlaying.value,
            position = _currentPosition.value,
            duration = _duration.value
        )
        _currentRecordingId.value = recordingId
        _currentPosition.value = 0L
        
        Log.d(TAG, "Mock track updated: $title - $date")
    }
}