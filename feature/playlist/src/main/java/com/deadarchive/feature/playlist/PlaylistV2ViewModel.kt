package com.deadarchive.feature.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.design.component.LibraryAction
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
 * Review data for V2 architecture
 */
data class Review(
    val username: String,
    val rating: Int,
    val stars: Double,
    val reviewText: String,
    val reviewDate: String
)

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
    
    /**
     * Handle library actions from LibraryButton
     */
    fun handleLibraryAction(action: LibraryAction) {
        when (action) {
            LibraryAction.ADD_TO_LIBRARY -> addToLibrary()
            LibraryAction.REMOVE_FROM_LIBRARY -> removeFromLibrary()
            LibraryAction.REMOVE_WITH_DOWNLOADS -> removeFromLibraryWithDownloads()
        }
    }
    
    /**
     * Remove from library
     */
    private fun removeFromLibrary() {
        viewModelScope.launch {
            try {
                // In real implementation, would call service method
                _uiState.value = _uiState.value.copy(isInLibrary = false)
                Log.d(TAG, "Removed from library")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from library", e)
            }
        }
    }
    
    /**
     * Remove from library with downloads
     */
    private fun removeFromLibraryWithDownloads() {
        viewModelScope.launch {
            try {
                // In real implementation, would call service method to remove downloads too
                _uiState.value = _uiState.value.copy(isInLibrary = false)
                Log.d(TAG, "Removed from library with downloads")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from library with downloads", e)
            }
        }
    }
    
    /**
     * Share show
     */
    fun shareShow() {
        viewModelScope.launch {
            try {
                playlistV2Service.shareShow()
                Log.d(TAG, "Shared show")
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing show", e)
            }
        }
    }
    
    /**
     * Show reviews (opens review details modal)
     */
    fun showReviews() {
        Log.d(TAG, "Show reviews requested")
        _uiState.value = _uiState.value.copy(showReviewDetails = true)
        loadReviews()
    }
    
    /**
     * Hide reviews (closes review details modal)
     */
    fun hideReviewDetails() {
        Log.d(TAG, "Hide reviews requested")
        _uiState.value = _uiState.value.copy(
            showReviewDetails = false,
            reviews = emptyList(),
            ratingDistribution = emptyMap(),
            reviewsError = null
        )
    }
    
    /**
     * Load reviews from service
     */
    private fun loadReviews() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    reviewsLoading = true,
                    reviewsError = null
                )
                
                // Get reviews from service
                val reviews = playlistV2Service.getCurrentReviews()
                val ratingDistribution = playlistV2Service.getRatingDistribution()
                
                _uiState.value = _uiState.value.copy(
                    reviewsLoading = false,
                    reviews = reviews,
                    ratingDistribution = ratingDistribution
                )
                
                Log.d(TAG, "Reviews loaded: ${reviews.size} reviews")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews", e)
                _uiState.value = _uiState.value.copy(
                    reviewsLoading = false,
                    reviewsError = "Failed to load reviews: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Show setlist
     */
    fun showSetlist() {
        Log.d(TAG, "Show setlist requested")
        // In real implementation, would open setlist bottom sheet
        viewModelScope.launch {
            try {
                playlistV2Service.loadSetlist()
                Log.d(TAG, "Setlist loaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading setlist", e)
            }
        }
    }
    
    /**
     * Show menu (for dropdown actions)
     */
    fun showMenu() {
        Log.d(TAG, "Show menu requested")
        // In real implementation, would show dropdown menu state
        // This would typically update a showMenu boolean in UiState
    }
    
    /**
     * Toggle playback (for main play/pause button)
     */
    fun togglePlayback() {
        val currentState = _uiState.value
        Log.d(TAG, "Toggle playback - currently playing: ${currentState.isPlaying}")
        
        viewModelScope.launch {
            try {
                if (currentState.isPlaying) {
                    playlistV2Service.pause()
                    _uiState.value = currentState.copy(isPlaying = false)
                } else {
                    if (currentState.currentTrackIndex >= 0) {
                        // Resume current track
                        playlistV2Service.resume()
                    } else {
                        // Start playing first track
                        playlistV2Service.playTrack(0)
                        _uiState.value = currentState.copy(currentTrackIndex = 0)
                    }
                    _uiState.value = _uiState.value.copy(isPlaying = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling playback", e)
            }
        }
    }
    
    /**
     * Play track
     */
    fun playTrack(track: PlaylistTrackViewModel) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Playing track: ${track.title}")
                playlistV2Service.playTrack(track.number - 1) // Convert to 0-based index
                
                // Update UI state to reflect current track
                val updatedTracks = _uiState.value.trackData.map { existingTrack ->
                    existingTrack.copy(
                        isCurrentTrack = existingTrack.number == track.number,
                        isPlaying = existingTrack.number == track.number
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    trackData = updatedTracks,
                    currentTrackIndex = track.number - 1,
                    isPlaying = true
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing track", e)
            }
        }
    }
    
    /**
     * Download individual track
     */
    fun downloadTrack(track: PlaylistTrackViewModel) {
        Log.d(TAG, "Download track requested: ${track.title}")
        // In real implementation, would start track download
        // For now, just update the track to show downloading state
        val updatedTracks = _uiState.value.trackData.map { existingTrack ->
            if (existingTrack.number == track.number) {
                existingTrack.copy(downloadProgress = 0.1f) // Start downloading
            } else {
                existingTrack
            }
        }
        _uiState.value = _uiState.value.copy(trackData = updatedTracks)
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
    val isNavigationLoading: Boolean = false,
    // Review details modal state
    val showReviewDetails: Boolean = false,
    val reviewsLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val ratingDistribution: Map<Int, Int> = emptyMap(),
    val reviewsError: String? = null
)