package com.deadly.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.core.data.service.LibraryService
import com.deadly.core.data.repository.DownloadRepository
import com.deadly.core.data.download.DownloadService
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import com.deadly.core.design.component.DownloadState
import com.deadly.core.design.component.ShowDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConcertListViewModel @Inject constructor(
    private val libraryService: LibraryService,
    private val downloadRepository: DownloadRepository,
    private val downloadService: DownloadService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ConcertListUiState>(ConcertListUiState.Loading)
    val uiState: StateFlow<ConcertListUiState> = _uiState.asStateFlow()
    
    // Download confirmation dialog state (passthrough to DownloadService)
    val showConfirmationDialog: StateFlow<Show?> = downloadService.showConfirmationDialog
    
    init {
        loadConcerts()
    }
    
    private fun loadConcerts() {
        viewModelScope.launch {
            try {
                _uiState.value = ConcertListUiState.Loading
                
                // TODO: Replace with actual repository call
                // val concerts = concertRepository.getAllConcertsWithRecordings()
                
                // Mock data for now
                val mockConcerts = createMockConcerts()
                
                _uiState.value = ConcertListUiState.Success(mockConcerts)
            } catch (e: Exception) {
                _uiState.value = ConcertListUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun retry() {
        loadConcerts()
    }
    
    fun toggleLibrary(show: Show) {
        viewModelScope.launch {
            try {
                // Add/remove the show to/from library
                libraryService.toggleLibrary(show)
                
                // Update the state locally
                val currentState = _uiState.value
                if (currentState is ConcertListUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        if (existingShow.showId == show.showId) {
                            existingShow.copy(isInLibrary = !existingShow.isInLibrary)
                        } else {
                            existingShow
                        }
                    }
                    _uiState.value = ConcertListUiState.Success(updatedShows)
                }
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    // Mock data for demonstration
    private fun createMockConcerts(): List<Show> {
        return listOf(
            Show(
                date = "1977-05-08",
                venue = "Cornell University",
                location = "Ithaca, NY",
                year = "1977",
                setlistRaw = "Set I: Minglewood Blues, Loser, El Paso, They Love Each Other...",
                recordings = listOf(
                    com.deadly.core.model.Recording(
                        identifier = "gd77-05-08.sbd",
                        title = "Grateful Dead Live at Cornell University on 1977-05-08",
                        source = "SBD",
                        taper = "Betty Cantor-Jackson",
                        concertDate = "1977-05-08",
                        concertVenue = "Cornell University",
                        tracks = emptyList() // Would be populated with actual tracks
                    ),
                    com.deadly.core.model.Recording(
                        identifier = "gd77-05-08.aud",
                        title = "Grateful Dead Live at Cornell University on 1977-05-08 (Audience)",
                        source = "AUD",
                        taper = "Unknown",
                        concertDate = "1977-05-08",
                        concertVenue = "Cornell University",
                        tracks = emptyList()
                    )
                ),
                isInLibrary = false
            ),
            Show(
                date = "1977-05-07",
                venue = "Boston Garden",
                location = "Boston, MA", 
                year = "1977",
                setlistRaw = null,
                recordings = listOf(
                    com.deadly.core.model.Recording(
                        identifier = "gd77-05-07.sbd",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07",
                        source = "SBD",
                        taper = "Betty Cantor-Jackson",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    ),
                    com.deadly.core.model.Recording(
                        identifier = "gd77-05-07.matrix",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07 (Matrix)",
                        source = "MATRIX",
                        taper = "Charlie Miller",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    ),
                    com.deadly.core.model.Recording(
                        identifier = "gd77-05-07.aud",
                        title = "Grateful Dead Live at Boston Garden on 1977-05-07 (Audience)",
                        source = "AUD",
                        taper = "Jerry Moore",
                        concertDate = "1977-05-07",
                        concertVenue = "Boston Garden",
                        tracks = emptyList()
                    )
                ),
                isInLibrary = true
            ),
            Show(
                date = "1972-05-03",
                venue = "Olympia Theatre",
                location = "Paris, France",
                year = "1972",
                setlistRaw = "Set I: Truckin', Sugaree, Jack Straw, Deal...",
                recordings = listOf(
                    com.deadly.core.model.Recording(
                        identifier = "gd72-05-03.sbd",
                        title = "Grateful Dead Live at Olympia Theatre on 1972-05-03",
                        source = "SBD",
                        taper = "Owsley Stanley",
                        concertDate = "1972-05-03",
                        concertVenue = "Olympia Theatre",
                        tracks = emptyList()
                    )
                ),
                isInLibrary = false
            )
        )
    }
    
    /**
     * Start downloading a recording
     */
    fun downloadRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                downloadRepository.downloadRecording(recording)
            } catch (e: Exception) {
                // Could add error handling/snackbar here
                println("Failed to start download for recording ${recording.identifier}: ${e.message}")
            }
        }
    }
    
    /**
     * Get the current download state for a recording
     */
    fun getDownloadState(recording: Recording): DownloadState {
        return downloadService.getDownloadState(recording)
    }
    
    /**
     * Start downloading the best recording of a show
     */
    fun downloadShow(show: Show) {
        viewModelScope.launch {
            try {
                // Get the best recording for this show
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    println("Downloading best recording for show ${show.showId}: ${bestRecording.identifier}")
                    downloadRepository.downloadRecording(bestRecording)
                } else {
                    println("No best recording available for show ${show.showId}")
                }
            } catch (e: Exception) {
                println("Failed to start download for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Handle download button click with smart state-based logic
     */
    fun handleDownloadButtonClick(show: Show) {
        android.util.Log.d("ConcertListViewModel", "handleDownloadButtonClick called for show: ${show.showId}")
        downloadService.handleDownloadButtonClick(
            show = show,
            coroutineScope = viewModelScope,
            onError = { errorMessage ->
                // Handle error appropriately for playlist context
                println("Download button error for show ${show.showId}: $errorMessage")
            }
        )
    }
    
    /**
     * Get the current download state for a show (based on its best recording)
     */
    fun getShowDownloadState(show: Show): ShowDownloadState {
        return downloadService.getShowDownloadState(show)
    }
    
    /**
     * Hide download confirmation dialog (passthrough to DownloadService)
     */
    fun hideConfirmationDialog() {
        downloadService.hideConfirmationDialog()
    }
    
    /**
     * Confirm removal of download (passthrough to DownloadService)
     */
    fun confirmRemoveDownload() {
        viewModelScope.launch {
            downloadService.confirmRemoveDownload()
        }
    }
    
    /**
     * Handle library button actions from unified LibraryButton component
     */
    fun handleLibraryAction(action: com.deadly.core.design.component.LibraryAction, show: Show) {
        viewModelScope.launch {
            try {
                when (action) {
                    com.deadly.core.design.component.LibraryAction.ADD_TO_LIBRARY -> {
                        libraryService.addToLibrary(show)
                    }
                    com.deadly.core.design.component.LibraryAction.REMOVE_FROM_LIBRARY -> {
                        libraryService.removeFromLibrary(show)
                    }
                    com.deadly.core.design.component.LibraryAction.REMOVE_WITH_DOWNLOADS -> {
                        libraryService.removeShowWithDownloadCleanup(show, alsoRemoveDownloads = true)
                    }
                }
                
                // Update the state locally for immediate UI feedback
                val currentState = _uiState.value
                if (currentState is ConcertListUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        if (existingShow.showId == show.showId) {
                            existingShow.copy(isInLibrary = when (action) {
                                com.deadly.core.design.component.LibraryAction.ADD_TO_LIBRARY -> true
                                com.deadly.core.design.component.LibraryAction.REMOVE_FROM_LIBRARY,
                                com.deadly.core.design.component.LibraryAction.REMOVE_WITH_DOWNLOADS -> false
                            })
                        } else {
                            existingShow
                        }
                    }
                    _uiState.value = ConcertListUiState.Success(updatedShows)
                }
            } catch (e: Exception) {
                // TODO: Handle error appropriately (could show snackbar)
                println("Library action error for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Get reactive library status for a specific show.
     * Returns a Flow that automatically updates when library status changes.
     */
    fun getLibraryStatusFlow(show: Show): kotlinx.coroutines.flow.Flow<Boolean> {
        return libraryService.isShowInLibraryFlow(show.showId)
    }
    
    /**
     * Get download information for library removal confirmation dialog
     */
    fun getLibraryRemovalInfo(show: Show): com.deadly.core.data.service.LibraryRemovalInfo {
        return try {
            var result = com.deadly.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Checking...",
                downloadState = com.deadly.core.design.component.ShowDownloadState.NotDownloaded
            )
            
            viewModelScope.launch {
                result = libraryService.getDownloadInfoForShow(show)
            }
            
            // Return default while async operation completes
            result
        } catch (e: Exception) {
            com.deadly.core.data.service.LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Error checking downloads",
                downloadState = com.deadly.core.design.component.ShowDownloadState.NotDownloaded
            )
        }
    }
}

sealed class ConcertListUiState {
    object Loading : ConcertListUiState()
    
    data class Success(
        val shows: List<Show>
    ) : ConcertListUiState()
    
    data class Error(
        val message: String
    ) : ConcertListUiState()
}