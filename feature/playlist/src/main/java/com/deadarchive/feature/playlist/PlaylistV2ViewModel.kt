package com.deadarchive.feature.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.feature.playlist.model.PlaylistShowViewModel
import com.deadarchive.feature.playlist.model.PlaylistTrackViewModel
import com.deadarchive.feature.playlist.service.PlaylistV2Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PlaylistV2ViewModel - Clean ViewModel for PlaylistV2 UI
 * 
 * Coordinates between UI components and the PlaylistV2Service.
 * Maintains UI state for all playlist components.
 */
@HiltViewModel
class PlaylistV2ViewModel @Inject constructor(
    private val playlistV2Service: PlaylistV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlaylistV2ViewModel"
    }
    
    
    private val _uiState = MutableStateFlow(PlaylistV2UiState())
    val uiState: StateFlow<PlaylistV2UiState> = _uiState.asStateFlow()
    
    /**
     * Load show data from the service
     */
    fun loadShow(showId: String?) {
        Log.d(TAG, "Loading show: $showId")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Load data from service
                playlistV2Service.loadShow(showId)
                
                // Get ViewModel data from service
                val showData = playlistV2Service.getCurrentShowInfo()
                val trackData = playlistV2Service.getTrackList()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showData = showData,
                    trackData = trackData,
                    currentTrackIndex = -1,
                    isPlaying = false
                )
                
                Log.d(TAG, "Show loaded successfully: ${showData?.displayDate}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading show", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load show: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Play a specific track
     */
    fun playTrack(trackIndex: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Playing track $trackIndex")
                playlistV2Service.playTrack(trackIndex)
                
                _uiState.value = _uiState.value.copy(
                    currentTrackIndex = trackIndex,
                    isPlaying = true
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing track", e)
            }
        }
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        val currentState = _uiState.value
        if (currentState.currentTrackIndex >= 0) {
            _uiState.value = currentState.copy(isPlaying = !currentState.isPlaying)
        } else {
            // Start playing first track
            playTrack(0)
        }
    }
    
    /**
     * Navigate to previous show
     */
    fun navigateToPreviousShow() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isNavigationLoading = true)
                playlistV2Service.navigateToPreviousShow()
                
                // Refresh UI with new show data
                val showData = playlistV2Service.getCurrentShowInfo()
                val trackData = playlistV2Service.getTrackList()
                
                _uiState.value = _uiState.value.copy(
                    isNavigationLoading = false,
                    showData = showData,
                    trackData = trackData,
                    currentTrackIndex = -1,
                    isPlaying = false
                )
                
                Log.d(TAG, "Navigated to previous show: ${showData?.displayDate}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to previous show", e)
                _uiState.value = _uiState.value.copy(isNavigationLoading = false)
            }
        }
    }
    
    /**
     * Navigate to next show
     */
    fun navigateToNextShow() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isNavigationLoading = true)
                playlistV2Service.navigateToNextShow()
                
                // Refresh UI with new show data
                val showData = playlistV2Service.getCurrentShowInfo()
                val trackData = playlistV2Service.getTrackList()
                
                _uiState.value = _uiState.value.copy(
                    isNavigationLoading = false,
                    showData = showData,
                    trackData = trackData,
                    currentTrackIndex = -1,
                    isPlaying = false
                )
                
                Log.d(TAG, "Navigated to next show: ${showData?.displayDate}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to next show", e)
                _uiState.value = _uiState.value.copy(isNavigationLoading = false)
            }
        }
    }
    
    /**
     * Add to library
     */
    fun addToLibrary() {
        viewModelScope.launch {
            try {
                playlistV2Service.addToLibrary()
                _uiState.value = _uiState.value.copy(isInLibrary = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to library", e)
            }
        }
    }
    
    /**
     * Download show
     */
    fun downloadShow() {
        viewModelScope.launch {
            try {
                playlistV2Service.downloadShow()
                // In real implementation, this would trigger download state updates
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading show", e)
            }
        }
    }
}

/**
 * UI State for PlaylistV2 components
 */
data class PlaylistV2UiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showData: PlaylistShowViewModel? = null,
    val trackData: List<PlaylistTrackViewModel> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isInLibrary: Boolean = false,
    val isNavigationLoading: Boolean = false
)