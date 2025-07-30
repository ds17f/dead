package com.deadarchive.feature.player.service

import android.util.Log
import com.deadarchive.core.model.PlayerV2State
import com.deadarchive.core.model.PlayerV2Track
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * PlayerV2ServiceImpl - Stub implementation for UI-first development
 * 
 * This implementation provides mock data and basic state management
 * to enable UI development and testing. Following V2 architecture,
 * this stub lets us build and test the UI before integrating with
 * the full V1 service ecosystem.
 * 
 * Next Steps:
 * 1. Connect to real V1 services (MediaControllerRepository, etc.)
 * 2. Replace mock data with real Archive.org content
 * 3. Implement proper playback state management
 */
@Singleton
class PlayerV2ServiceImpl @Inject constructor() : PlayerV2Service {
    
    companion object {
        private const val TAG = "PlayerV2Service"
    }
    
    // Mock state for initial development
    private val _playerState = MutableStateFlow(createMockPlayerState())
    override val playerState: StateFlow<PlayerV2State> = _playerState.asStateFlow()
    
    private var isPlaying = false
    private var currentPosition = 0.3f // Mock 30% progress
    private var currentRecordingInfo: MockRecordingInfo? = null
    
    override suspend fun loadRecording(recordingId: String) {
        Log.d(TAG, "Loading recording: $recordingId")
        
        // Create mock data based on recordingId
        currentRecordingInfo = createMockRecordingInfo(recordingId)
        val mockState = createMockPlayerState(recordingId)
        _playerState.value = mockState
        
        Log.d(TAG, "Recording loaded: ${currentRecordingInfo?.displayName}")
    }
    
    override suspend fun getCurrentTrackInfo(): TrackDisplayInfo? {
        val recording = currentRecordingInfo ?: return null
        
        return TrackDisplayInfo(
            trackTitle = recording.currentTrackTitle,
            recordingName = recording.displayName,
            showDate = recording.showDate,
            venue = recording.venue
        )
    }
    
    override suspend fun togglePlayPause() {
        Log.d(TAG, "Toggle play/pause - currently playing: $isPlaying")
        isPlaying = !isPlaying
        
        // Update the state with new playing status using proper domain model method
        _playerState.value = _playerState.value.updatePlaybackState(
            if (isPlaying) com.deadarchive.core.model.PlaybackState.PLAYING 
            else com.deadarchive.core.model.PlaybackState.PAUSED
        )
        
        Log.d(TAG, "Playback state updated - now playing: $isPlaying")
    }
    
    override suspend fun skipToPrevious() {
        Log.d(TAG, "Skip to previous track")
        currentRecordingInfo?.let { recording ->
            currentRecordingInfo = recording.copy(
                currentTrackTitle = "Previous Track"
            )
        }
        updateState()
    }
    
    override suspend fun skipToNext() {
        Log.d(TAG, "Skip to next track")
        currentRecordingInfo?.let { recording ->
            currentRecordingInfo = recording.copy(
                currentTrackTitle = "Next Track"
            )
        }
        updateState()
    }
    
    override suspend fun seekToPosition(position: Float) {
        Log.d(TAG, "Seeking to position: $position")
        currentPosition = position.coerceIn(0f, 1f)
        updateState()
    }
    
    override suspend fun getProgressInfo(): ProgressDisplayInfo? {
        return ProgressDisplayInfo(
            currentTime = formatTime((currentPosition * 495).toInt()), // Mock total duration
            totalTime = formatTime(495), // 8:15 in seconds
            progress = currentPosition
        )
    }
    
    override fun isReady(): Boolean {
        return currentRecordingInfo != null
    }
    
    override suspend fun cleanup() {
        Log.d(TAG, "Cleaning up PlayerV2Service")
        // Reset state
        _playerState.value = PlayerV2State()
        isPlaying = false
        currentPosition = 0f
        currentRecordingInfo = null
    }
    
    // Helper methods for mock data creation
    
    private fun createMockPlayerState(recordingId: String? = null): PlayerV2State {
        val initialState = PlayerV2State(
            isLoading = false
        )
        return initialState.updatePlaybackState(
            if (isPlaying) com.deadarchive.core.model.PlaybackState.PLAYING 
            else com.deadarchive.core.model.PlaybackState.PAUSED
        )
    }
    
    private fun createMockRecordingInfo(recordingId: String): MockRecordingInfo {
        return MockRecordingInfo(
            id = recordingId,
            displayName = "Cornell 5/8/77",
            showDate = "May 8, 1977",
            venue = "Barton Hall, Cornell University",
            currentTrackTitle = "Scarlet Begonias"
        )
    }
    
    private fun updateState() {
        _playerState.value = _playerState.value.updatePlaybackState(
            if (isPlaying) com.deadarchive.core.model.PlaybackState.PLAYING 
            else com.deadarchive.core.model.PlaybackState.PAUSED
        )
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}

/**
 * Simple data class for mock recording information
 */
private data class MockRecordingInfo(
    val id: String,
    val displayName: String,
    val showDate: String,
    val venue: String,
    val currentTrackTitle: String
)