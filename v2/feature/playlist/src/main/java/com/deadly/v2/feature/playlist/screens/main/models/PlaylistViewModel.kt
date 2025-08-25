package com.deadly.v2.feature.playlist.screens.main.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.core.api.playlist.PlaylistService
import com.deadly.v2.core.model.*
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.media.exception.FormatNotAvailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

/**
 * PlaylistViewModel - Clean ViewModel for Playlist UI
 * 
 * Coordinates between UI components and the PlaylistService.
 * Maintains UI state for all playlist components.
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistService: PlaylistService,
    private val mediaControllerRepository: MediaControllerRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "PlaylistViewModel"
    }
    
    
    // Internal state for show and tracks data
    private val _baseUiState = MutableStateFlow(PlaylistUiState())
    private val _rawTrackData = MutableStateFlow<List<PlaylistTrackViewModel>>(emptyList())
    
    // Reactive UI state that combines base state with MediaController state
    val uiState: StateFlow<PlaylistUiState> = combine(
        _baseUiState,
        _rawTrackData,
        playlistService.isPlaying,
        playlistService.currentPlayingTrackInfo
    ) { baseState, rawTracks, isPlaying, currentTrackInfo ->
        
        // Update track data with current playing state
        val updatedTracks = rawTracks.map { track ->
            val isCurrentTrack = isTrackCurrentlyPlaying(track, currentTrackInfo)
            track.copy(
                isCurrentTrack = isCurrentTrack,
                isPlaying = isCurrentTrack && isPlaying
            )
        }
        
        baseState.copy(
            trackData = updatedTracks,
            isPlaying = isPlaying
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistUiState()
    )
    
    // Track current track loading job for cancellation
    private var trackLoadingJob: Job? = null
    
    /**
     * Determine if a playlist track matches the currently playing track from MediaController
     */
    private fun isTrackCurrentlyPlaying(track: PlaylistTrackViewModel, currentTrackInfo: CurrentTrackInfo?): Boolean {
        if (currentTrackInfo == null) return false
        
        // Match based on track title and current recording ID
        val currentRecordingId = _baseUiState.value.showData?.currentRecordingId
        if (currentRecordingId != currentTrackInfo.recordingId) return false
        
        // Match track title (exact match or contains match)
        return track.title.equals(currentTrackInfo.songTitle, ignoreCase = true) ||
               currentTrackInfo.songTitle.contains(track.title, ignoreCase = true)
    }
    
    /**
     * Load show data from the service
     */
    fun loadShow(showId: String?) {
        Log.d(TAG, "Loading show: $showId")
        viewModelScope.launch {
            try {
                _baseUiState.value = _baseUiState.value.copy(isLoading = true, error = null)
                
                // Load show in service (DB data)
                playlistService.loadShow(showId)
                
                // Show DB data immediately
                val showData = playlistService.getCurrentShowInfo()
                _baseUiState.value = _baseUiState.value.copy(
                    isLoading = false,
                    showData = showData,
                    currentTrackIndex = -1
                )
                
                Log.d(TAG, "Show loaded successfully: ${showData?.displayDate} - showing DB data immediately")
                
                // Start track loading in background
                loadTrackListAsync()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading show", e)
                _baseUiState.value = _baseUiState.value.copy(
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
                playlistService.playTrack(trackIndex)
                
                _baseUiState.value = _baseUiState.value.copy(
                    currentTrackIndex = trackIndex
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
        val currentState = _baseUiState.value
        if (currentState.currentTrackIndex >= 0) {
            // Playback state is now managed by MediaController - no need to update locally
            Log.d(TAG, "Toggle play/pause - MediaController will handle state")
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
                // TODO: Cancel distant prefetches in future iteration
                
                // Navigate in service (updates show instantly)
                playlistService.navigateToPreviousShow()
                
                // Show DB data immediately - no loading state blocks navigation
                val showData = playlistService.getCurrentShowInfo()
                _baseUiState.value = _baseUiState.value.copy(
                    showData = showData,
                    currentTrackIndex = -1,
                    isTrackListLoading = false // Reset track loading state
                )
                _rawTrackData.value = emptyList()
                
                Log.d(TAG, "Navigated to previous show: ${showData?.displayDate} - showing DB data immediately")
                
                // Start track loading with smart prefetch promotion
                loadTrackListAsync()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to previous show", e)
            }
        }
    }
    
    /**
     * Navigate to next show
     */
    fun navigateToNextShow() {
        viewModelScope.launch {
            try {
                // TODO: Cancel distant prefetches in future iteration
                
                // Navigate in service (updates show instantly)
                playlistService.navigateToNextShow()
                
                // Show DB data immediately - no loading state blocks navigation
                val showData = playlistService.getCurrentShowInfo()
                _baseUiState.value = _baseUiState.value.copy(
                    showData = showData,
                    currentTrackIndex = -1,
                    isTrackListLoading = false // Reset track loading state
                )
                _rawTrackData.value = emptyList()
                
                Log.d(TAG, "Navigated to next show: ${showData?.displayDate} - showing DB data immediately")
                
                // Start track loading with smart prefetch promotion
                loadTrackListAsync()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to next show", e)
            }
        }
    }
    
    /**
     * Add to library
     */
    fun addToLibrary() {
        viewModelScope.launch {
            try {
                playlistService.addToLibrary()
                _baseUiState.value = _baseUiState.value.copy(isInLibrary = true)
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
                playlistService.downloadShow()
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
                _baseUiState.value = _baseUiState.value.copy(isInLibrary = false)
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
                _baseUiState.value = _baseUiState.value.copy(isInLibrary = false)
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
                playlistService.shareShow()
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
        _baseUiState.value = _baseUiState.value.copy(showReviewDetails = true)
        loadReviews()
    }
    
    /**
     * Hide reviews (closes review details modal)
     */
    fun hideReviewDetails() {
        Log.d(TAG, "Hide reviews requested")
        _baseUiState.value = _baseUiState.value.copy(
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
                _baseUiState.value = _baseUiState.value.copy(
                    reviewsLoading = true,
                    reviewsError = null
                )
                
                // Get reviews from service
                val reviews = playlistService.getCurrentReviews()
                val ratingDistribution = playlistService.getRatingDistribution()
                
                _baseUiState.value = _baseUiState.value.copy(
                    reviewsLoading = false,
                    reviews = reviews,
                    ratingDistribution = ratingDistribution
                )
                
                Log.d(TAG, "Reviews loaded: ${reviews.size} reviews")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reviews", e)
                _baseUiState.value = _baseUiState.value.copy(
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
                playlistService.loadSetlist()
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
        _baseUiState.value = _baseUiState.value.copy(showMenu = true)
    }
    
    /**
     * Hide menu
     */
    fun hideMenu() {
        Log.d(TAG, "Hide menu requested")
        _baseUiState.value = _baseUiState.value.copy(showMenu = false)
    }
    
    /**
     * Choose recording (opens recording selection modal)
     */
    fun chooseRecording() {
        Log.d(TAG, "Choose recording requested")
        hideMenu()
        showRecordingSelection()
    }
    
    /**
     * Show recording selection modal
     */
    fun showRecordingSelection() {
        Log.d(TAG, "Show recording selection requested")
        viewModelScope.launch {
            try {
                // Set loading state
                _baseUiState.value = _baseUiState.value.copy(
                    recordingSelection = _baseUiState.value.recordingSelection.copy(
                        isVisible = true,
                        isLoading = true,
                        errorMessage = null
                    )
                )
                
                // Load recording options from service
                val showTitle = _baseUiState.value.showData?.displayDate ?: "Unknown Show"
                val recordingOptions = playlistService.getRecordingOptions()
                
                _baseUiState.value = _baseUiState.value.copy(
                    recordingSelection = _baseUiState.value.recordingSelection.copy(
                        showTitle = showTitle,
                        currentRecording = recordingOptions.currentRecording,
                        alternativeRecordings = recordingOptions.alternativeRecordings,
                        hasRecommended = recordingOptions.hasRecommended,
                        isLoading = false
                    )
                )
                
                Log.d(TAG, "Recording selection loaded: ${recordingOptions.alternativeRecordings.size} alternatives")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recording options", e)
                _baseUiState.value = _baseUiState.value.copy(
                    recordingSelection = _baseUiState.value.recordingSelection.copy(
                        isLoading = false,
                        errorMessage = "Failed to load recordings: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Hide recording selection modal
     */
    fun hideRecordingSelection() {
        Log.d(TAG, "Hide recording selection requested")
        _baseUiState.value = _baseUiState.value.copy(
            recordingSelection = RecordingSelectionState()
        )
    }
    
    /**
     * Select a recording
     */
    fun selectRecording(recordingId: String) {
        Log.d(TAG, "Recording selected: $recordingId")
        viewModelScope.launch {
            try {
                playlistService.selectRecording(recordingId)
                
                // Update selection state
                val currentSelection = _baseUiState.value.recordingSelection
                val updatedCurrent = currentSelection.currentRecording?.copy(isSelected = false)
                val updatedAlternatives = currentSelection.alternativeRecordings.map { option ->
                    option.copy(isSelected = option.identifier == recordingId)
                }
                
                _baseUiState.value = _baseUiState.value.copy(
                    recordingSelection = currentSelection.copy(
                        currentRecording = updatedCurrent,
                        alternativeRecordings = updatedAlternatives
                    )
                )
                
                Log.d(TAG, "Recording selection updated")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting recording", e)
            }
        }
    }
    
    /**
     * Set recording as default
     */
    fun setRecordingAsDefault(recordingId: String) {
        Log.d(TAG, "Set recording as default: $recordingId")
        viewModelScope.launch {
            try {
                playlistService.setRecordingAsDefault(recordingId)
                Log.d(TAG, "Recording set as default successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting recording as default", e)
            }
        }
    }
    
    /**
     * Reset to recommended recording
     */
    fun resetToRecommended() {
        Log.d(TAG, "Reset to recommended recording requested")
        viewModelScope.launch {
            try {
                playlistService.resetToRecommended()
                Log.d(TAG, "Reset to recommended successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting to recommended", e)
            }
        }
    }
    
    /**
     * Toggle playback (for main play/pause button) - V2 Media System
     */
    fun togglePlayback() {
        val currentState = _baseUiState.value
        Log.d(TAG, "V2 Toggle playback - currently playing: ${currentState.isPlaying}")

        viewModelScope.launch {
            try {
                // Get show data to determine recording ID
                val showData = currentState.showData
                if (showData == null) {
                    Log.w(TAG, "No show data - cannot start playback")
                    return@launch
                }

                // also need the current recording
                val currentRecording = showData.currentRecordingId
                if (currentRecording == null) {
                    Log.w(TAG, "No currentRecording - cannot start playback")
                    return@launch
                }

                // Get the format that was selected during playlist building
                val selectedFormat = playlistService.getCurrentSelectedFormat()
                
                if (selectedFormat == null) {
                    Log.w(TAG, "No format selected - cannot start playback")
                    return@launch
                }
                
                val recordingId = currentRecording
                
                Log.d(TAG, "V2 Media: Play All for $recordingId ($selectedFormat)")
                
                // Get show context from UI state
                val showContext = _baseUiState.value.showData
                if (showContext == null) {
                    Log.e(TAG, "Cannot start playback: No show data available in UI state")
                    // TODO: Show user-friendly error message instead of silent failure
                    return@launch
                }
                
                val showId = showContext.showId
                if (showId.isBlank()) {
                    Log.e(TAG, "Cannot start playback: showId is blank from service")
                    // TODO: Show user-friendly error message instead of silent failure  
                    return@launch
                }
                
                val showDate = showContext.date
                val venue = showContext?.venue
                val location = showContext?.location
                
                // Use MediaControllerRepository for Play All logic
                mediaControllerRepository.playAll(
                    recordingId = recordingId, 
                    format = selectedFormat,
                    showId = showId,
                    showDate = showDate,
                    venue = venue,
                    location = location
                )
                
                // UI state will be updated via MediaController state observation
                
            } catch (e: FormatNotAvailableException) {
                Log.e(TAG, "Format playback failed: ${e.message}")
                Log.e(TAG, "Available formats: ${e.availableFormats}")
                
                // Show user error
                _baseUiState.value = _baseUiState.value.copy(
                    error = "Playback format '${e.requestedFormat}' not available"
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in V2 togglePlayback", e)
            }
        }
    }
    
    /**
     * Play track - V2 Media System
     */
    fun playTrack(track: PlaylistTrackViewModel) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "V2 Playing track: ${track.title} (index: ${track.number - 1})")
                
                // Get show data to determine recording ID
                val currentShowData = _baseUiState.value.showData
                if (currentShowData == null) {
                    Log.w(TAG, "No show data - cannot play track")
                    return@launch
                }

                // also need the current recording
                val currentRecording = currentShowData.currentRecordingId
                if (currentRecording == null) {
                    Log.w(TAG, "No currentRecording - cannot start playback")
                    return@launch
                }

                // Get the format that was selected during playlist building
                val selectedFormat = playlistService.getCurrentSelectedFormat()
                
                if (selectedFormat == null) {
                    Log.w(TAG, "No format selected - cannot play track")
                    return@launch
                }
                
                val recordingId = currentRecording
                val trackIndex = track.number - 1 // Convert to 0-based
                
                Log.d(TAG, "V2 Media: Play track $trackIndex of $recordingId ($selectedFormat)")
                
                // Get show context from UI state
                val showContext = _baseUiState.value.showData
                if (showContext == null) {
                    Log.e(TAG, "Cannot start playback: No show data available in UI state")
                    // TODO: Show user-friendly error message instead of silent failure
                    return@launch
                }
                
                val showId = showContext.showId
                if (showId.isBlank()) {
                    Log.e(TAG, "Cannot start playback: showId is blank from service")
                    // TODO: Show user-friendly error message instead of silent failure  
                    return@launch
                }
                
                val showDate = showContext.date
                val venue = showContext?.venue
                val location = showContext?.location
                
                // Use MediaControllerRepository for track playback
                mediaControllerRepository.playTrack(
                    trackIndex = trackIndex, 
                    recordingId = recordingId, 
                    format = selectedFormat,
                    showId = showId,
                    showDate = showDate,
                    venue = venue,
                    location = location
                )
                
                // UI state will be updated via MediaController state observation
                
            } catch (e: FormatNotAvailableException) {
                Log.e(TAG, "Format playback failed for track: ${e.message}")
                Log.e(TAG, "Available formats: ${e.availableFormats}")
                
                // Show user error
                _baseUiState.value = _baseUiState.value.copy(
                    error = "Track format '${e.requestedFormat}' not available"
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in V2 playTrack", e)
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
        val updatedTracks = _rawTrackData.value.map { existingTrack ->
            if (existingTrack.number == track.number) {
                existingTrack.copy(downloadProgress = 0.1f) // Start downloading
            } else {
                existingTrack
            }
        }
        _rawTrackData.value = updatedTracks
    }
    
    /**
     * Load track list asynchronously with smart prefetch promotion
     * Shows loading spinner over track section only
     */
    private fun loadTrackListAsync() {
        // Cancel any previous track loading
        trackLoadingJob?.cancel()
        
        // Load tracks directly - internal prefetch is transparent
        loadTracksFromService()
    }
    
    /**
     * Load tracks from service (either fresh or from cache)
     */
    private fun loadTracksFromService() {
        // Start track loading with spinner
        _baseUiState.value = _baseUiState.value.copy(
            isTrackListLoading = true
        )
        _rawTrackData.value = emptyList() // Clear previous tracks while loading
        
        trackLoadingJob = viewModelScope.launch {
            try {
                Log.d(TAG, "Loading track list asynchronously...")
                val trackData = playlistService.getTrackList()
                
                _baseUiState.value = _baseUiState.value.copy(
                    isTrackListLoading = false
                )
                _rawTrackData.value = trackData
                
                Log.d(TAG, "Track list loaded: ${trackData.size} tracks")
                
                // Start background prefetching after current tracks load
                startAdjacentPrefetch()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading track list", e)
                _baseUiState.value = _baseUiState.value.copy(
                    isTrackListLoading = false
                )
                _rawTrackData.value = emptyList()
            }
        }
    }
    
    /**
     * Cancel prefetches that are no longer adjacent after navigation
     * (Internal prefetching handles this automatically)
     */
    private suspend fun cancelDistantPrefetches(direction: String) {
        // Prefetching is now handled internally by the service
        // No explicit cancel calls needed in ViewModel
        Log.d(TAG, "Prefetch cleanup handled internally after $direction navigation")
    }
    
    /**
     * Start prefetching adjacent shows in background
     * (Internal prefetching handles this automatically)
     */
    private fun startAdjacentPrefetch() {
        // Prefetching is now handled internally by the service
        // No explicit prefetch calls needed in ViewModel
        Log.d(TAG, "Prefetch started internally for adjacent shows")
    }
}