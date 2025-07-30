package com.deadarchive.feature.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.PlayerV2State
import com.deadarchive.feature.player.service.PlayerV2Service
import com.deadarchive.feature.player.service.TrackDisplayInfo
import com.deadarchive.feature.player.service.ProgressDisplayInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PlayerV2ViewModel - UI State coordinator following V2 architecture
 * 
 * This ViewModel follows the V2 pattern of:
 * 1. UI drives the requirements (discovered through component building)
 * 2. ViewModel coordinates between UI and services
 * 3. Service provides domain logic and data access
 * 4. Reactive state flows for UI updates
 * 
 * The ViewModel transforms service data into UI-specific state
 * and handles UI interaction commands.
 */
@HiltViewModel
class PlayerV2ViewModel @Inject constructor(
    private val playerV2Service: PlayerV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlayerV2ViewModel"
    }
    
    // UI State - transformed from service state for UI consumption
    private val _uiState = MutableStateFlow(PlayerV2UiState())
    val uiState: StateFlow<PlayerV2UiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "PlayerV2ViewModel initialized")
        observeServiceState()
        loadInitialState()
    }
    
    /**
     * Load recording for playback
     * Called when navigating to player with recordingId
     */
    fun loadRecording(recordingId: String?) {
        if (recordingId == null) {
            Log.d(TAG, "Loading default player state")
            return
        }
        
        Log.d(TAG, "Loading recording: $recordingId")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                playerV2Service.loadRecording(recordingId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recording: $recordingId", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load recording: ${e.message}"
                )
            }
        }
    }
    
    // UI Command Handlers (discovered through building UI components)
    
    fun onPlayPauseClicked() {
        Log.d(TAG, "Play/Pause clicked")
        viewModelScope.launch {
            try {
                playerV2Service.togglePlayPause()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle play/pause", e)
            }
        }
    }
    
    fun onPreviousClicked() {
        Log.d(TAG, "Previous clicked")
        viewModelScope.launch {
            try {
                playerV2Service.skipToPrevious()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to skip to previous", e)
            }
        }
    }
    
    fun onNextClicked() {
        Log.d(TAG, "Next clicked")
        viewModelScope.launch {
            try {
                playerV2Service.skipToNext()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to skip to next", e)
            }
        }
    }
    
    fun onSeek(position: Float) {
        Log.d(TAG, "Seeking to position: $position")
        viewModelScope.launch {
            try {
                playerV2Service.seekToPosition(position)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seek to position: $position", e)
            }
        }
    }
    
    // Private Methods
    
    private fun observeServiceState() {
        viewModelScope.launch {
            playerV2Service.playerState
                .catch { exception ->
                    Log.e(TAG, "Error observing player state", exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Playback error: ${exception.message}"
                    )
                }
                .collect { serviceState ->
                    updateUiStateFromService(serviceState)
                }
        }
    }
    
    private suspend fun updateUiStateFromService(serviceState: PlayerV2State) {
        Log.d(TAG, "Updating UI state from service")
        
        // Transform service state into UI state
        val trackInfo = playerV2Service.getCurrentTrackInfo()
        val progressInfo = playerV2Service.getProgressInfo()
        
        _uiState.value = _uiState.value.copy(
            isLoading = serviceState.isLoading,
            isPlaying = serviceState.isPlaying,
            trackInfo = trackInfo,
            progressInfo = progressInfo,
            canPlay = playerV2Service.isReady(),
            error = null // Clear any previous errors
        )
    }
    
    private fun loadInitialState() {
        Log.d(TAG, "Loading initial state")
        viewModelScope.launch {
            // Load some default mock data for initial display
            if (!playerV2Service.isReady()) {
                playerV2Service.loadRecording("default-mock")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "PlayerV2ViewModel cleared")
        viewModelScope.launch {
            playerV2Service.cleanup()
        }
    }
}

/**
 * UI State for PlayerV2Screen
 * Discovered and refined through building UI components
 */
data class PlayerV2UiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val trackInfo: TrackDisplayInfo? = null,
    val progressInfo: ProgressDisplayInfo? = null,
    val canPlay: Boolean = false,
    val error: String? = null
)