package com.deadarchive.feature.library.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.feature.library.LibraryUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for download management in the Library feature.
 * Handles show downloads, recording downloads, download cancellation, and state monitoring.
 * Follows the same pattern as BrowseDownloadService for consistency.
 */
@Singleton
class LibraryDownloadService @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    
    companion object {
        private const val TAG = "LibraryDownloadService"
    }
    
    // Download state tracking
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    // Confirmation dialog state
    private val _showConfirmationDialog = MutableStateFlow<Show?>(null)
    val showConfirmationDialog: StateFlow<Show?> = _showConfirmationDialog.asStateFlow()
    
    /**
     * Start downloading a recording
     */
    fun downloadRecording(
        recording: Recording,
        coroutineScope: CoroutineScope,
        onStateChange: (LibraryUiState) -> Unit,
        currentState: LibraryUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting download for recording ${recording.identifier}")
                downloadRepository.downloadRecording(recording)
                
                // Update the UI state locally to show the recording's show is now in library
                if (currentState is LibraryUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        // Find the show that contains this recording
                        if (existingShow.recordings.any { it.identifier == recording.identifier }) {
                            existingShow.copy(isInLibrary = true)
                        } else {
                            existingShow
                        }
                    }
                    onStateChange(LibraryUiState.Success(currentState.libraryItems, updatedShows))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download for recording ${recording.identifier}: ${e.message}")
                // Could add error handling/snackbar here
            }
        }
    }
    
    /**
     * Start downloading the best recording of a show
     */
    fun downloadShow(
        show: Show,
        coroutineScope: CoroutineScope,
        onStateChange: (LibraryUiState) -> Unit,
        currentState: LibraryUiState
    ) {
        coroutineScope.launch {
            try {
                // Get the best recording for this show
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    Log.d(TAG, "Downloading best recording for show ${show.showId}: ${bestRecording.identifier}")
                    
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
                    if (currentState is LibraryUiState.Success) {
                        val updatedShows = currentState.shows.map { existingShow ->
                            if (existingShow.showId == show.showId) {
                                existingShow.copy(isInLibrary = true)
                            } else {
                                existingShow
                            }
                        }
                        onStateChange(LibraryUiState.Success(currentState.libraryItems, updatedShows))
                    }
                } else {
                    Log.w(TAG, "No best recording available for show ${show.showId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download for show ${show.showId}: ${e.message}")
                
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
    fun cancelShowDownloads(show: Show, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            try {
                val bestRecording = show.bestRecording
                if (bestRecording != null) {
                    Log.d(TAG, "Cancelling downloads for show ${show.showId}, recording: ${bestRecording.identifier}")
                    downloadRepository.cancelRecordingDownloads(bestRecording.identifier)
                } else {
                    Log.w(TAG, "No best recording found for show ${show.showId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel downloads for show ${show.showId}: ${e.message}")
            }
        }
    }
    
    /**
     * Get the current download state for a recording
     */
    fun getDownloadState(@Suppress("UNUSED_PARAMETER") recording: Recording): DownloadState {
        return try {
            // This is a simplified version for individual recording downloads
            // For now, return Available state as most UI uses show-level downloads
            DownloadState.Available
        } catch (e: Exception) {
            DownloadState.Error("Failed to get download state")
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
        Log.d(TAG, "Showing remove download confirmation for show ${show.showId}")
        _showConfirmationDialog.value = show
    }
    
    /**
     * Hide confirmation dialog
     */
    fun hideConfirmationDialog() {
        Log.d(TAG, "Hiding confirmation dialog")
        _showConfirmationDialog.value = null
    }
    
    /**
     * Confirm removal of download (soft delete)
     */
    fun confirmRemoveDownload(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val show = _showConfirmationDialog.value
            if (show != null) {
                try {
                    val bestRecording = show.bestRecording
                    if (bestRecording != null) {
                        Log.d(TAG, "Confirming removal - marking recording ${bestRecording.identifier} for soft deletion")
                        // Soft delete the recording
                        downloadRepository.markRecordingForDeletion(bestRecording.identifier)
                        Log.d(TAG, "🗑️ Recording ${bestRecording.identifier} marked for soft deletion")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark recording for deletion: ${e.message}")
                } finally {
                    _showConfirmationDialog.value = null
                }
            }
        }
    }
    
    /**
     * Start the download monitoring coroutine - called from ViewModel with its scope
     */
    fun startDownloadStateMonitoring(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
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
}