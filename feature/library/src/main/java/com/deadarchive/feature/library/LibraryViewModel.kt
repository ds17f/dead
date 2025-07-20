package com.deadarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.LibraryItemType
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.deadarchive.core.model.DownloadStatus
import javax.inject.Inject

enum class LibrarySortOption(val displayName: String) {
    DATE_ASCENDING("Date ↑"),
    DATE_DESCENDING("Date ↓"), 
    ADDED_ASCENDING("Added ↑"),
    ADDED_DESCENDING("Added ↓")
}

enum class DecadeFilter(val displayName: String, val decade: String?) {
    ALL("All", null),
    SEVENTIES("70s", "197"),
    EIGHTIES("80s", "198"), 
    NINETIES("90s", "199")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val showRepository: ShowRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    // Download state tracking
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    // Confirmation dialog state
    private val _showConfirmationDialog = MutableStateFlow<Show?>(null)
    val showConfirmationDialog: StateFlow<Show?> = _showConfirmationDialog.asStateFlow()
    
    // Sort option state
    private val _sortOption = MutableStateFlow(LibrarySortOption.DATE_DESCENDING)
    val sortOption: StateFlow<LibrarySortOption> = _sortOption.asStateFlow()
    
    // Decade filter state
    private val _decadeFilter = MutableStateFlow(DecadeFilter.ALL)
    val decadeFilter: StateFlow<DecadeFilter> = _decadeFilter.asStateFlow()
    
    init {
        loadLibraryItems()
        // Start monitoring download states
        startDownloadStateMonitoring()
    }
    
    private fun loadLibraryItems() {
        viewModelScope.launch {
            try {
                _uiState.value = LibraryUiState.Loading
                
                // Use combine to properly handle flows with exception safety, filtering and sorting
                combine(
                    libraryRepository.getAllLibraryItems(),
                    showRepository.getLibraryShows(),
                    _sortOption,
                    _decadeFilter
                ) { libraryItems, libraryShows, sortOption, decadeFilter ->
                    println("DEBUG LibraryViewModel: Found ${libraryItems.size} library items")
                    libraryItems.forEach { item ->
                        println("DEBUG LibraryViewModel: Library item - showId: ${item.showId}, type: ${item.type}")
                    }
                    println("DEBUG LibraryViewModel: Found ${libraryShows.size} library shows")
                    
                    // Find matching shows with recordings and prepare for sorting
                    val matchedLibraryShowsWithTimestamp = libraryItems.map { libraryItem ->
                        val matchingShow = libraryShows.find { show -> show.showId == libraryItem.showId }
                        
                        val show = if (matchingShow != null) {
                            // Use the actual show data with recordings
                            println("DEBUG LibraryViewModel: Found show ${libraryItem.showId} with ${matchingShow.recordings.size} recordings")
                            if (libraryItem.showId.contains("1995-07-09")) {
                                println("DEBUG LibraryViewModel: 1995-07-09 details - showId: ${matchingShow.showId}, recordings: ${matchingShow.recordings.size}")
                                matchingShow.recordings.take(3).forEach { recording ->
                                    println("DEBUG LibraryViewModel: 1995-07-09 recording: ${recording.identifier}")
                                }
                            }
                            matchingShow.copy(isInLibrary = true)
                        } else {
                            // This should never happen now - shows should always have ShowEntity records
                            println("ERROR LibraryViewModel: Show ${libraryItem.showId} not found in getLibraryShows() - this indicates a bug!")
                            if (libraryItem.showId.contains("1995-07-09")) {
                                println("ERROR LibraryViewModel: 1995-07-09 is missing from getLibraryShows()!")
                                println("ERROR LibraryViewModel: Available library shows: ${libraryShows.map { it.showId }.filter { it.contains("1995") }}")
                            }
                            val parts = libraryItem.showId.split("_")
                            val date = parts.getOrNull(0) ?: "Unknown Date"
                            val venue = parts.drop(1).joinToString(" ").replace("_", " ") 
                            
                            Show(
                                date = date,
                                venue = venue,
                                location = null,
                                year = date.take(4),
                                recordings = emptyList(),
                                isInLibrary = true
                            )
                        }
                        
                        // Pair show with its library item for sorting
                        Pair(show, libraryItem)
                    }
                    
                    // Apply decade filtering first
                    val filteredShows = if (decadeFilter.decade != null) {
                        matchedLibraryShowsWithTimestamp.filter { (show, _) ->
                            show.date.startsWith(decadeFilter.decade)
                        }
                    } else {
                        matchedLibraryShowsWithTimestamp
                    }
                    
                    // Apply sorting based on sort option
                    val matchedLibraryShows = when (sortOption) {
                        LibrarySortOption.DATE_ASCENDING -> {
                            filteredShows
                                .sortedBy { it.first.date }
                                .map { it.first }
                        }
                        LibrarySortOption.DATE_DESCENDING -> {
                            filteredShows
                                .sortedByDescending { it.first.date }
                                .map { it.first }
                        }
                        LibrarySortOption.ADDED_ASCENDING -> {
                            filteredShows
                                .sortedBy { it.second.addedTimestamp }
                                .map { it.first }
                        }
                        LibrarySortOption.ADDED_DESCENDING -> {
                            filteredShows
                                .sortedByDescending { it.second.addedTimestamp }
                                .map { it.first }
                        }
                    }
                    println("DEBUG LibraryViewModel: Created ${matchedLibraryShows.size} shows for display")
                    LibraryUiState.Success(libraryItems, matchedLibraryShows)
                }
                .catch { exception ->
                    println("ERROR LibraryViewModel: ${exception.message}")
                    _uiState.value = LibraryUiState.Error(
                        exception.message ?: "Failed to load library items"
                    )
                }
                .collect { uiState ->
                    _uiState.value = uiState
                }
            } catch (e: Exception) {
                println("EXCEPTION LibraryViewModel: ${e.message}")
                _uiState.value = LibraryUiState.Error(
                    e.message ?: "Failed to load library items"
                )
            }
        }
    }
    
    fun removeFromLibrary(libraryItem: LibraryItem) {
        viewModelScope.launch {
            try {
                when (libraryItem.type) {
                    LibraryItemType.SHOW -> {
                        libraryRepository.removeShowFromLibrary(libraryItem.showId)
                    }
                }
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    fun removeShowFromLibrary(showId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.removeShowFromLibrary(showId)
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    fun clearLibrary() {
        viewModelScope.launch {
            try {
                libraryRepository.clearLibrary()
            } catch (e: Exception) {
                // TODO: Handle error appropriately
            }
        }
    }
    
    fun retry() {
        loadLibraryItems()
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
        return try {
            // For now, return Available state as a placeholder
            // In Task 5, we'll implement proper download progress tracking
            DownloadState.Available
        } catch (e: Exception) {
            DownloadState.Error("Failed to get download state")
        }
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
                    
                    // Provide immediate UI feedback by setting download state to "queued"
                    val currentDownloadStates = _downloadStates.value.toMutableMap()
                    currentDownloadStates[bestRecording.identifier] = ShowDownloadState.Downloading(
                        progress = -1f, // -1 indicates "queued/starting"
                        bytesDownloaded = 0L,
                        completedTracks = 0,
                        totalTracks = 1 // Placeholder until actual track count is known
                    )
                    _downloadStates.value = currentDownloadStates
                    
                    // Start the actual download
                    downloadRepository.downloadRecording(bestRecording)
                    
                    // Update the UI state locally to show the show is now in library (following Browse pattern)
                    val currentState = _uiState.value
                    if (currentState is LibraryUiState.Success) {
                        val updatedShows = currentState.shows.map { existingShow ->
                            if (existingShow.showId == show.showId) {
                                existingShow.copy(isInLibrary = true)
                            } else {
                                existingShow
                            }
                        }
                        _uiState.value = LibraryUiState.Success(currentState.libraryItems, updatedShows)
                    }
                } else {
                    println("No best recording available for show ${show.showId}")
                }
            } catch (e: Exception) {
                println("Failed to start download for show ${show.showId}: ${e.message}")
                
                // On error, revert the optimistic UI state
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    val currentDownloadStates = _downloadStates.value.toMutableMap()
                    currentDownloadStates[bestRecording.identifier] = ShowDownloadState.Failed("Failed to start download")
                    _downloadStates.value = currentDownloadStates
                }
            }
        }
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    fun cancelShowDownloads(show: Show) {
        viewModelScope.launch {
            try {
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    downloadRepository.cancelRecordingDownloads(bestRecording.identifier)
                } else {
                    println("No best recording found for show ${show.showId}")
                }
            } catch (e: Exception) {
                println("Failed to cancel downloads for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Start monitoring download states for all recordings
     */
    private fun startDownloadStateMonitoring() {
        viewModelScope.launch {
            // Monitor all downloads and update states
            downloadRepository.getAllDownloads().collect { downloads ->
                val stateMap = mutableMapOf<String, ShowDownloadState>()
                
                // Group downloads by recording ID
                val downloadsByRecording = downloads.groupBy { it.recordingId }
                
                downloadsByRecording.forEach { (recordingId, recordingDownloads) ->
                    val showDownloadState = when {
                        // If any download is marked for deletion, treat as not downloaded
                        recordingDownloads.any { it.isMarkedForDeletion } -> {
                            ShowDownloadState.NotDownloaded
                        }
                        // Handle failed downloads separately (show as failed)
                        recordingDownloads.any { it.status == DownloadStatus.FAILED } -> {
                            val failedTrack = recordingDownloads.first { it.status == DownloadStatus.FAILED }
                            ShowDownloadState.Failed(failedTrack.errorMessage)
                        }
                        // Filter out cancelled and failed downloads for status determination
                        else -> recordingDownloads.filter { it.status !in listOf(DownloadStatus.CANCELLED, DownloadStatus.FAILED) }.let { activeDownloads ->
                            when {
                                activeDownloads.all { it.status == DownloadStatus.COMPLETED } && activeDownloads.isNotEmpty() -> {
                                    ShowDownloadState.Downloaded
                                }
                                activeDownloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED } -> {
                                    // Calculate track-based progress (Spotify-style immediate feedback)
                                    val totalTracks = activeDownloads.size
                                    val completedTracks = activeDownloads.count { it.status == DownloadStatus.COMPLETED }
                                    
                                    // Get byte progress from actively downloading track if any
                                    val downloadingTrack = activeDownloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                                    val byteProgress = downloadingTrack?.progress ?: -1f
                                    val bytesDownloaded = downloadingTrack?.bytesDownloaded ?: 0L
                                    
                                    ShowDownloadState.Downloading(
                                        progress = byteProgress,
                                        bytesDownloaded = bytesDownloaded,
                                        completedTracks = completedTracks,
                                        totalTracks = totalTracks
                                    )
                                }
                                else -> {
                                    ShowDownloadState.NotDownloaded
                                }
                            }
                        }
                    }
                    
                    stateMap[recordingId] = showDownloadState
                }
                
                _downloadStates.value = stateMap
            }
        }
    }
    
    /**
     * Get the current download state for a show (based on its best recording)
     */
    fun getShowDownloadState(show: Show): ShowDownloadState {
        return try {
            val bestRecording = show.bestRecording
            if (bestRecording != null) {
                // Get the current state from our monitored state map
                _downloadStates.value[bestRecording.identifier] ?: ShowDownloadState.NotDownloaded
            } else {
                ShowDownloadState.NotDownloaded
            }
        } catch (e: Exception) {
            ShowDownloadState.Failed("Failed to get download state")
        }
    }
    
    /**
     * Show confirmation dialog for removing download
     */
    fun showRemoveDownloadConfirmation(show: Show) {
        _showConfirmationDialog.value = show
    }
    
    /**
     * Hide confirmation dialog
     */
    fun hideConfirmationDialog() {
        _showConfirmationDialog.value = null
    }
    
    /**
     * Confirm removal of download (soft delete)
     */
    fun confirmRemoveDownload() {
        viewModelScope.launch {
            val show = _showConfirmationDialog.value
            if (show != null) {
                try {
                    val bestRecording = show.bestRecording
                    if (bestRecording != null) {
                        // Soft delete the recording
                        downloadRepository.markRecordingForDeletion(bestRecording.identifier)
                        println("🗑️ Recording ${bestRecording.identifier} marked for soft deletion")
                    }
                } catch (e: Exception) {
                    println("Failed to mark recording for deletion: ${e.message}")
                } finally {
                    _showConfirmationDialog.value = null
                }
            }
        }
    }
    
    /**
     * Set the sort option
     */
    fun setSortOption(option: LibrarySortOption) {
        _sortOption.value = option
    }
    
    /**
     * Set the decade filter
     */
    fun setDecadeFilter(filter: DecadeFilter) {
        _decadeFilter.value = filter
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val libraryItems: List<LibraryItem>,
        val shows: List<Show>
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}