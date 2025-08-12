package com.deadarchive.feature.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.model.CurrentTrackInfo
import com.deadarchive.feature.playlist.model.MiniPlayerV2UiState
import com.deadarchive.feature.playlist.service.MiniPlayerV2Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MiniPlayerV2ViewModel - V2 ViewModel for global mini-player
 * 
 * Follows PlayerV2ViewModel patterns with clean V2 service integration.
 * Coordinates between MiniPlayerV2Service and UI layer.
 */
@HiltViewModel
class MiniPlayerV2ViewModel @Inject constructor(
    private val miniPlayerV2Service: MiniPlayerV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "MiniPlayerV2ViewModel"
    }
    
    private val _uiState = MutableStateFlow(MiniPlayerV2UiState())
    val uiState: StateFlow<MiniPlayerV2UiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "MiniPlayerV2ViewModel initialized")
        initializeService()
        observeServiceState()
    }
    
    /**
     * Initialize the V2 service
     */
    private fun initializeService() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing MiniPlayerV2Service")
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                miniPlayerV2Service.initialize()
                
                Log.d(TAG, "MiniPlayerV2Service initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MiniPlayerV2Service", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initialize mini-player: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Observe state changes from V2 service and update UI state
     */
    private fun observeServiceState() {
        viewModelScope.launch {
            combine(
                miniPlayerV2Service.isPlaying,
                miniPlayerV2Service.currentPosition,
                miniPlayerV2Service.duration,
                miniPlayerV2Service.progress,
                miniPlayerV2Service.currentTrackInfo,
                miniPlayerV2Service.currentRecordingId
            ) { values ->
                val isPlaying = values[0] as Boolean
                val position = values[1] as Long
                val duration = values[2] as Long
                val progress = values[3] as Float
                val trackInfo = values[4] as CurrentTrackInfo?
                val recordingId = values[5] as String?
                
                Log.d(TAG, "Service state updated: playing=$isPlaying, trackInfo=${trackInfo?.displayTitle}")
                
                MiniPlayerV2UiState(
                    isLoading = false,
                    isPlaying = isPlaying,
                    currentPosition = position,
                    duration = duration,
                    progress = progress,
                    trackInfo = trackInfo,
                    recordingId = recordingId,
                    error = null,
                    isVisible = trackInfo != null
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
    
    /**
     * Handle play/pause button clicks
     */
    fun onPlayPauseClicked() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "onPlayPauseClicked: current state isPlaying=${_uiState.value.isPlaying}")
                miniPlayerV2Service.togglePlayPause()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle play/pause", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to toggle playback: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Handle mini-player tap to expand to full player
     */
    fun onTapToExpand(recordingId: String?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "onTapToExpand: recordingId=$recordingId")
                miniPlayerV2Service.expandToPlayer(recordingId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to expand to player", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to open player: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MiniPlayerV2ViewModel cleared, cleaning up service")
        
        viewModelScope.launch {
            try {
                miniPlayerV2Service.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "Error during service cleanup", e)
            }
        }
    }
}