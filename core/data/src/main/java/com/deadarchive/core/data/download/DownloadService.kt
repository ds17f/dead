package com.deadarchive.core.data.download

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.service.RecordingSelectionService
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared download service that manages all download operations across the app.
 * Replaces individual feature download services with a single source of truth.
 * Provides real-time download state synchronization across all screens.
 * 
 * DATA MODEL CLARIFICATION:
 * - We download individual RECORDINGS (specific taper's version of a show)
 * - UI shows SHOWS (concerts) with download buttons
 * - Clicking "download show" downloads the BEST RECORDING for that show  
 * - Download state is tracked per RECORDING but displayed per SHOW
 * - Unchecking "removes" the RECORDING downloads but UI shows it as "show undownloaded"
 */
@Singleton
class DownloadService @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val showRepository: com.deadarchive.core.data.api.repository.ShowRepository,
    private val recordingSelectionService: RecordingSelectionService
) {
    
    companion object {
        private const val TAG = "DownloadService"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Shared download state tracking
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    // Track-level download states
    private val _trackDownloadStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val trackDownloadStates: StateFlow<Map<String, Boolean>> = _trackDownloadStates.asStateFlow()
    
    // Confirmation dialog state (for shows that need removal confirmation)
    private val _showConfirmationDialog = MutableStateFlow<Show?>(null)
    val showConfirmationDialog: StateFlow<Show?> = _showConfirmationDialog.asStateFlow()
    
    init {
        // Start monitoring download states when service is created
        startDownloadStateMonitoring()
    }
    
    /**
     * Start monitoring download states for all downloads
     */
    private fun startDownloadStateMonitoring() {
        Log.d(TAG, "Starting download state monitoring")
        
        serviceScope.launch {
            try {
                // Monitor download progress for all downloads
                downloadRepository.getAllDownloads().collect { downloads ->
                    val states = mutableMapOf<String, ShowDownloadState>()
                    val trackStates = mutableMapOf<String, Boolean>()
                    
                    // Group downloads by recording ID to aggregate progress
                    val downloadsByRecording = downloads.groupBy { it.recordingId }
                    
                    downloadsByRecording.forEach { (recordingId, recordingDownloads) ->
                        val completedTracks = recordingDownloads.count { it.status == DownloadStatus.COMPLETED }
                        val totalTracks = recordingDownloads.size
                        val failedTracks = recordingDownloads.count { it.status == DownloadStatus.FAILED }
                        val hasActiveDownloads = recordingDownloads.any { 
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED 
                        }
                        
                        val state = when {
                            completedTracks == totalTracks -> ShowDownloadState.Downloaded
                            failedTracks > 0 && !hasActiveDownloads -> {
                                val firstError = recordingDownloads.find { it.status == DownloadStatus.FAILED }?.errorMessage
                                ShowDownloadState.Failed(firstError ?: "Download failed")
                            }
                            hasActiveDownloads || completedTracks > 0 -> {
                                // Calculate average progress of active downloads
                                val activeDownloads = recordingDownloads.filter { 
                                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED 
                                }
                                val avgProgress = if (activeDownloads.isNotEmpty()) {
                                    activeDownloads.map { if (it.status == DownloadStatus.QUEUED) 0f else it.progress }.average().toFloat()
                                } else 0f
                                
                                ShowDownloadState.Downloading(
                                    progress = avgProgress,
                                    completedTracks = completedTracks,
                                    totalTracks = totalTracks
                                )
                            }
                            else -> ShowDownloadState.NotDownloaded
                        }
                        
                        states[recordingId] = state
                    }
                    
                    // Update track states
                    downloads.forEach { download ->
                        trackStates[download.trackFilename] = download.status == DownloadStatus.COMPLETED
                    }
                    
                    _downloadStates.value = states
                    _trackDownloadStates.value = trackStates
                    
                    Log.d(TAG, "Updated download states: ${states.size} recordings, ${trackStates.size} tracks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring downloads", e)
            }
        }
    }
    
    /**
     * Start downloading a recording
     */
    suspend fun downloadRecording(recording: Recording) {
        Log.d(TAG, "Starting download for recording ${recording.identifier}")
        
        try {
            downloadRepository.downloadRecording(recording)
            Log.d(TAG, "Download initiated successfully for recording ${recording.identifier}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting download for recording ${recording.identifier}", e)
            throw e
        }
    }
    
    /**
     * Start downloading the best recording of a show
     */
    suspend fun downloadShow(show: Show) {
        Log.d(TAG, "Starting download for show ${show.showId}")
        
        try {
            // Get the best recording for this show using centralized service
            val bestRecording = recordingSelectionService.getBestRecording(show)
            if (bestRecording != null) {
                Log.d(TAG, "Downloading best recording for show ${show.showId}: ${bestRecording.identifier}")
                downloadRepository.downloadRecording(bestRecording)
                Log.d(TAG, "Download initiated successfully for show ${show.showId}")
            } else {
                Log.w(TAG, "No best recording available for show ${show.showId}")
                throw IllegalStateException("No recordings available for show ${show.showId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting download for show ${show.showId}", e)
            throw e
        }
    }
    
    /**
     * Cancel downloads for a recording
     */
    suspend fun cancelRecordingDownloads(recording: Recording) {
        Log.d(TAG, "Canceling downloads for recording ${recording.identifier}")
        
        try {
            downloadRepository.cancelDownload(recording.identifier)
            Log.d(TAG, "Downloads canceled successfully for recording ${recording.identifier}")
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling downloads for recording ${recording.identifier}", e)
            throw e
        }
    }
    
    /**
     * Cancel all downloads for a show (best recording)
     */
    suspend fun cancelShowDownloads(show: Show) {
        Log.d(TAG, "Canceling downloads for show ${show.showId}")
        
        try {
            val bestRecording = recordingSelectionService.getBestRecording(show)
            if (bestRecording != null) {
                downloadRepository.cancelRecordingDownloads(bestRecording.identifier)
                Log.d(TAG, "Downloads canceled successfully for show ${show.showId}")
            } else {
                Log.w(TAG, "No best recording to cancel for show ${show.showId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling downloads for show ${show.showId}", e)
            throw e
        }
    }
    
    /**
     * Clear/remove all downloads for a show (completely delete from system)
     * This is what users expect when they "uncheck" a downloaded show
     */
    suspend fun clearShowDownloads(show: Show) {
        Log.d(TAG, "Clearing downloads for show ${show.showId}")
        
        try {
            val bestRecording = recordingSelectionService.getBestRecording(show)
            if (bestRecording != null) {
                // Get all downloads for this recording and delete them completely
                val downloads = downloadRepository.getAllDownloads().first()
                val recordingDownloads = downloads.filter { it.recordingId == bestRecording.identifier }
                
                Log.d(TAG, "Found ${recordingDownloads.size} downloads to clear for recording ${bestRecording.identifier}")
                
                // Delete each download completely from the system
                recordingDownloads.forEach { download ->
                    downloadRepository.deleteDownload(download.id)
                    Log.d(TAG, "Deleted download: ${download.trackFilename}")
                }
                
                Log.d(TAG, "Downloads cleared successfully for show ${show.showId}")
            } else {
                Log.w(TAG, "No best recording to clear for show ${show.showId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing downloads for show ${show.showId}", e)
            throw e
        }
    }
    
    /**
     * Get the current download state for a recording
     */
    fun getRecordingDownloadState(recording: Recording): ShowDownloadState {
        val state = _downloadStates.value[recording.identifier] ?: ShowDownloadState.NotDownloaded
        Log.d(TAG, "Recording ${recording.identifier} download state: $state")
        return state
    }
    
    /**
     * Get the current download state for a show (based on its best recording)
     */
    fun getShowDownloadState(show: Show): ShowDownloadState {
        val bestRecording = recordingSelectionService.getBestRecording(show)
        return if (bestRecording != null) {
            getRecordingDownloadState(bestRecording)
        } else {
            ShowDownloadState.NotDownloaded
        }
    }
    
    /**
     * Get download state for an individual recording by ID (for UI components)
     */
    fun getDownloadState(recording: Recording): DownloadState {
        val showState = getRecordingDownloadState(recording)
        return when (showState) {
            is ShowDownloadState.NotDownloaded -> DownloadState.Available
            is ShowDownloadState.Downloading -> DownloadState.Downloading(showState.progress)
            is ShowDownloadState.Downloaded -> DownloadState.Downloaded
            is ShowDownloadState.Failed -> DownloadState.Error(showState.errorMessage ?: "Download failed")
        }
    }
    
    /**
     * Smart handler for download button clicks that determines the appropriate action
     * based on current download state. This centralizes the business logic that was
     * previously scattered across UI components.
     */
    fun handleDownloadButtonClick(
        show: Show,
        coroutineScope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        val currentState = getShowDownloadState(show)
        Log.d(TAG, "Handling download button click for show ${show.showId}, current state: $currentState")
        
        when (currentState) {
            is ShowDownloadState.NotDownloaded -> {
                // Start download
                Log.d(TAG, "Starting download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        downloadShow(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting download for show ${show.showId}", e)
                        onError("Failed to start download: ${e.message}")
                    }
                }
            }
            is ShowDownloadState.Downloading -> {
                // Cancel in-progress download
                Log.d(TAG, "Canceling in-progress download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        cancelShowDownloads(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error canceling download for show ${show.showId}", e)
                        onError("Failed to cancel download: ${e.message}")
                    }
                }
            }
            is ShowDownloadState.Downloaded -> {
                // Show confirmation dialog for removal
                Log.d(TAG, "Showing removal confirmation for downloaded show ${show.showId}")
                showRemoveDownloadConfirmation(show)
            }
            is ShowDownloadState.Failed -> {
                // Retry failed download
                Log.d(TAG, "Retrying failed download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        downloadShow(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error retrying download for show ${show.showId}", e)
                        onError("Failed to retry download: ${e.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Check if a track is downloaded
     */
    suspend fun isTrackDownloaded(track: Track): Boolean {
        val isDownloaded = track.audioFile?.filename?.let { filename ->
            _trackDownloadStates.value[filename] ?: false
        } ?: false
        
        Log.d(TAG, "Track '${track.displayTitle}' (filename: ${track.audioFile?.filename}) downloaded: $isDownloaded")
        return isDownloaded
    }
    
    /**
     * Show confirmation dialog for removing download
     */
    fun showRemoveDownloadConfirmation(show: Show) {
        Log.d(TAG, "Showing confirmation dialog for show ${show.showId}")
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
     * Confirm removal of download (completely delete from system)
     */
    suspend fun confirmRemoveDownload() {
        val show = _showConfirmationDialog.value
        if (show != null) {
            Log.d(TAG, "Confirming removal of download for show ${show.showId}")
            try {
                clearShowDownloads(show)
                hideConfirmationDialog()
                Log.d(TAG, "Download removal confirmed for show ${show.showId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing download for show ${show.showId}", e)
                throw e
            }
        } else {
            Log.w(TAG, "No show selected for download removal")
        }
    }
    
    /**
     * Get all individual downloads as a Flow for the download queue UI
     */
    fun getAllDownloads() = downloadRepository.getAllDownloads()
    
    /**
     * Cancel an individual download by ID
     */
    suspend fun cancelDownload(downloadId: String) {
        Log.d(TAG, "Cancelling download: $downloadId")
        downloadRepository.cancelDownload(downloadId)
    }
    
    /**
     * Retry a failed download by ID
     */
    suspend fun retryDownload(downloadId: String) {
        Log.d(TAG, "Retrying download: $downloadId")
        downloadRepository.retryDownload(downloadId)
    }
    
    /**
     * Force start a queued download by setting high priority
     */
    suspend fun forceDownload(downloadId: String) {
        Log.d(TAG, "Force starting download: $downloadId")
        downloadRepository.setDownloadPriority(downloadId, Int.MAX_VALUE)
        downloadRepository.resumeDownload(downloadId)
    }
    
    /**
     * Remove a download completely from the system (for failed/canceled downloads)
     */
    suspend fun removeDownload(downloadId: String) {
        Log.d(TAG, "Removing download from system: $downloadId")
        try {
            downloadRepository.deleteDownload(downloadId)
            Log.d(TAG, "Download removed successfully: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing download: $downloadId", e)
            throw e
        }
    }
    
    /**
     * Pause a download
     */
    suspend fun pauseDownload(downloadId: String) {
        Log.d(TAG, "Pausing download: $downloadId")
        downloadRepository.pauseDownload(downloadId)
    }
    
    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(downloadId: String) {
        Log.d(TAG, "Resuming download: $downloadId")
        downloadRepository.resumeDownload(downloadId)
    }
    
    /**
     * Get all downloads enriched with show and track metadata
     */
    suspend fun getEnrichedDownloads(): List<EnrichedDownloadState> {
        return try {
            val downloads = downloadRepository.getAllDownloads().first()
            downloads.map { download ->
                val recording = showRepository.getRecordingById(download.recordingId)
                val track = recording?.tracks?.find { it.filename == download.trackFilename }
                val show = recording?.let { 
                    // Try to find show by date extracted from recording
                    val dateFromRecording = extractDateFromRecordingId(recording.identifier)
                    // For now, use the recording's concert info
                    Show(
                        date = recording.concertDate ?: dateFromRecording ?: "Unknown",
                        venue = recording.concertVenue ?: "Unknown Venue",
                        location = recording.concertLocation ?: "",
                        recordings = listOf(recording)
                    )
                }
                
                EnrichedDownloadState(
                    downloadState = download,
                    recording = recording,
                    track = track,
                    show = show
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enriching downloads", e)
            emptyList()
        }
    }
    
    private fun extractDateFromRecordingId(recordingId: String): String? {
        // Extract date from Archive.org identifiers like "gd1977-05-08.sbd.hicks.4982.sbeok.shnf"
        val dateRegex = Regex("""(\d{4}-\d{2}-\d{2})""")
        return dateRegex.find(recordingId)?.value
    }
}

/**
 * Download state enriched with recording and track metadata
 */
data class EnrichedDownloadState(
    val downloadState: com.deadarchive.core.model.DownloadState,
    val recording: Recording?,
    val track: Track?,
    val show: Show?
) {
    val displayShowName: String
        get() = show?.let { "${it.date} - ${it.venue}" } 
            ?: recording?.let { "${it.concertDate ?: "Unknown Date"} - ${it.concertVenue ?: "Unknown Venue"}" }
            ?: formatShowName(downloadState.recordingId)
    
    val displayTrackTitle: String
        get() = track?.displayTitle ?: Track.extractSongFromFilename(downloadState.trackFilename)
    
    val displayTrackNumber: String
        get() = track?.displayTrackNumber ?: extractTrackNumberFromFilename(downloadState.trackFilename)
    
    val downloadUrl: String
        get() = "https://archive.org/download/${downloadState.recordingId}/${downloadState.trackFilename}"
    
    private fun extractTrackNumberFromFilename(filename: String): String {
        val trackRegex = Regex("""t(\d+)""")
        return trackRegex.find(filename)?.groupValues?.get(1) ?: "?"
    }
    
    private fun formatShowName(recordingId: String): String {
        val dateRegex = Regex("""(\d{4}-\d{2}-\d{2})""")
        val dateMatch = dateRegex.find(recordingId)
        return if (dateMatch != null) {
            "Show: ${dateMatch.value}"
        } else {
            "Recording: ${recordingId.take(20)}..."
        }
    }
}