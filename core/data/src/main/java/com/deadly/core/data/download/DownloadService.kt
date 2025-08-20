package com.deadly.core.data.download

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.deadly.core.data.repository.DownloadRepository
import com.deadly.core.data.service.RecordingSelectionService
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import com.deadly.core.model.Track
import com.deadly.core.model.DownloadStatus
import com.deadly.core.design.component.DownloadState
import com.deadly.core.design.component.ShowDownloadState
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
    private val showRepository: com.deadly.core.data.api.repository.ShowRepository,
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
                        val pausedTracks = recordingDownloads.count { it.status == DownloadStatus.PAUSED }
                        val cancelledTracks = recordingDownloads.count { it.status == DownloadStatus.CANCELLED }
                        val downloadingTracks = recordingDownloads.count { it.status == DownloadStatus.DOWNLOADING }
                        val queuedTracks = recordingDownloads.count { it.status == DownloadStatus.QUEUED }
                        val hasActiveDownloads = recordingDownloads.any { 
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED 
                        }
                        
                        // Debug logging for state calculation
                        if (recordingDownloads.isNotEmpty()) {
                            Log.d(TAG, "STATE DEBUG: Recording $recordingId - Total: $totalTracks, Completed: $completedTracks, " +
                                "Downloading: $downloadingTracks, Queued: $queuedTracks, Paused: $pausedTracks, " +
                                "Failed: $failedTracks, Cancelled: $cancelledTracks, HasActive: $hasActiveDownloads")
                        }
                        
                        val state = when {
                            completedTracks == totalTracks -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> Downloaded (all completed)")
                                ShowDownloadState.Downloaded
                            }
                            failedTracks > 0 && !hasActiveDownloads && pausedTracks == 0 && cancelledTracks == 0 -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> Failed (has failures, no active/paused/cancelled)")
                                val firstError = recordingDownloads.find { it.status == DownloadStatus.FAILED }?.errorMessage
                                ShowDownloadState.Failed(firstError ?: "Download failed")
                            }
                            pausedTracks > 0 && !hasActiveDownloads -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> Paused (has paused, no active)")
                                // Calculate progress for paused downloads
                                val avgProgress = recordingDownloads.map { it.progress }.average().toFloat()
                                ShowDownloadState.Paused(
                                    progress = avgProgress,
                                    completedTracks = completedTracks,
                                    totalTracks = totalTracks
                                )
                            }
                            cancelledTracks > 0 && !hasActiveDownloads && pausedTracks == 0 -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> Cancelled (has cancelled, no active/paused)")
                                // Calculate progress for cancelled downloads
                                val avgProgress = recordingDownloads.map { it.progress }.average().toFloat()
                                ShowDownloadState.Cancelled(
                                    progress = avgProgress,
                                    completedTracks = completedTracks,
                                    totalTracks = totalTracks
                                )
                            }
                            hasActiveDownloads || completedTracks > 0 -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> Downloading (has active or completed)")
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
                            else -> {
                                Log.d(TAG, "STATE DEBUG: Recording $recordingId -> NotDownloaded (default)")
                                ShowDownloadState.NotDownloaded
                            }
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
     * Pause all downloads for a recording
     */
    suspend fun pauseRecordingDownloads(recordingId: String) {
        Log.d(TAG, "=== PAUSE DEBUG START: Pausing downloads for recording $recordingId ===")
        
        try {
            // Get all downloads and log total count
            val allDownloads = downloadRepository.getAllDownloads().first()
            Log.d(TAG, "PAUSE DEBUG: Total downloads in system: ${allDownloads.size}")
            
            // Log all downloads for this recording (regardless of status)
            val allRecordingDownloads = allDownloads.filter { it.recordingId == recordingId }
            Log.d(TAG, "PAUSE DEBUG: Total downloads for recording $recordingId: ${allRecordingDownloads.size}")
            allRecordingDownloads.forEach { download ->
                Log.d(TAG, "PAUSE DEBUG: Recording download: ${download.trackFilename} - Status: ${download.status} - Progress: ${download.progress}")
            }
            
            // Filter both downloading and queued downloads (anything "active")
            val recordingDownloads = allRecordingDownloads.filter { 
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED 
            }
            Log.d(TAG, "PAUSE DEBUG: Active tracks to pause (downloading + queued): ${recordingDownloads.size}")
            
            if (recordingDownloads.isEmpty()) {
                Log.w(TAG, "PAUSE DEBUG: No active downloads (DOWNLOADING/QUEUED) found to pause for recording $recordingId")
                // Check if there are any other statuses
                allRecordingDownloads.groupBy { it.status }.forEach { (status, downloads) ->
                    Log.d(TAG, "PAUSE DEBUG: Found ${downloads.size} downloads with status: $status")
                }
                return
            }
            
            // Pause each download and verify the result
            recordingDownloads.forEach { download ->
                Log.d(TAG, "PAUSE DEBUG: Attempting to pause download ID: ${download.id}, Track: ${download.trackFilename}")
                downloadRepository.pauseDownload(download.id)
                Log.d(TAG, "PAUSE DEBUG: Called pauseDownload for: ${download.trackFilename}")
            }
            
            // Small delay to let pause operations settle
            kotlinx.coroutines.delay(100)
            
            // Verify the pause operations worked
            val updatedDownloads = downloadRepository.getAllDownloads().first()
            val updatedRecordingDownloads = updatedDownloads.filter { it.recordingId == recordingId }
            Log.d(TAG, "PAUSE DEBUG: After pause operations, download statuses:")
            updatedRecordingDownloads.forEach { download ->
                Log.d(TAG, "PAUSE DEBUG: Post-pause: ${download.trackFilename} - Status: ${download.status}")
            }
            
            val nowPaused = updatedRecordingDownloads.count { it.status == DownloadStatus.PAUSED }
            val stillDownloading = updatedRecordingDownloads.count { it.status == DownloadStatus.DOWNLOADING }
            Log.d(TAG, "PAUSE DEBUG: Results - Paused: $nowPaused, Still downloading: $stillDownloading")
            
            Log.d(TAG, "=== PAUSE DEBUG END: Downloads paused successfully for recording $recordingId ===")
        } catch (e: Exception) {
            Log.e(TAG, "PAUSE DEBUG ERROR: Error pausing downloads for recording $recordingId", e)
            throw e
        }
    }
    
    /**
     * Resume all downloads for a recording
     */
    suspend fun resumeRecordingDownloads(recordingId: String) {
        Log.d(TAG, "Resuming downloads for recording $recordingId")
        
        try {
            // Get all downloads for this recording and resume them
            val downloads = downloadRepository.getAllDownloads().first()
            val recordingDownloads = downloads.filter { it.recordingId == recordingId && it.status == DownloadStatus.PAUSED }
            
            Log.d(TAG, "Found ${recordingDownloads.size} paused downloads to resume for recording $recordingId")
            
            recordingDownloads.forEach { download ->
                downloadRepository.resumeDownload(download.id)
                Log.d(TAG, "Resumed download: ${download.trackFilename}")
            }
            
            Log.d(TAG, "Downloads resumed successfully for recording $recordingId")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming downloads for recording $recordingId", e)
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
        Log.d(TAG, "getShowDownloadState: show=${show.showId}, bestRecording=${bestRecording?.identifier}")
        return if (bestRecording != null) {
            val state = getRecordingDownloadState(bestRecording)
            Log.d(TAG, "getShowDownloadState: final state for show ${show.showId} = $state")
            state
        } else {
            Log.w(TAG, "getShowDownloadState: No best recording found for show ${show.showId}")
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
            is ShowDownloadState.Paused -> DownloadState.Downloading(showState.progress) // Map to Downloading for backward compatibility
            is ShowDownloadState.Cancelled -> DownloadState.Error("Download cancelled")
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
                // Pause in-progress download
                Log.d(TAG, "Pausing in-progress download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        val bestRecording = recordingSelectionService.getBestRecording(show)
                        if (bestRecording != null) {
                            pauseRecordingDownloads(bestRecording.identifier)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pausing download for show ${show.showId}", e)
                        onError("Failed to pause download: ${e.message}")
                    }
                }
            }
            is ShowDownloadState.Paused -> {
                // Resume paused download
                Log.d(TAG, "Resuming paused download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        val bestRecording = recordingSelectionService.getBestRecording(show)
                        if (bestRecording != null) {
                            resumeRecordingDownloads(bestRecording.identifier)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming download for show ${show.showId}", e)
                        onError("Failed to resume download: ${e.message}")
                    }
                }
            }
            is ShowDownloadState.Cancelled -> {
                // Restart cancelled download
                Log.d(TAG, "Restarting cancelled download for show ${show.showId}")
                coroutineScope.launch {
                    try {
                        downloadShow(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restarting download for show ${show.showId}", e)
                        onError("Failed to restart download: ${e.message}")
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
     * Handle long-press actions from the unified DownloadButton component
     */
    fun handleDownloadAction(
        show: Show,
        action: com.deadly.core.design.component.DownloadAction,
        coroutineScope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "Handling download action $action for show ${show.showId}")
        
        when (action) {
            com.deadly.core.design.component.DownloadAction.RETRY -> {
                coroutineScope.launch {
                    try {
                        downloadShow(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error retrying download for show ${show.showId}", e)
                        onError("Failed to retry download: ${e.message}")
                    }
                }
            }
            com.deadly.core.design.component.DownloadAction.REMOVE -> {
                coroutineScope.launch {
                    try {
                        clearShowDownloads(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing download for show ${show.showId}", e)
                        onError("Failed to remove download: ${e.message}")
                    }
                }
            }
            com.deadly.core.design.component.DownloadAction.PAUSE -> {
                coroutineScope.launch {
                    try {
                        val bestRecording = recordingSelectionService.getBestRecording(show)
                        if (bestRecording != null) {
                            pauseRecordingDownloads(bestRecording.identifier)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error pausing download for show ${show.showId}", e)
                        onError("Failed to pause download: ${e.message}")
                    }
                }
            }
            com.deadly.core.design.component.DownloadAction.RESUME -> {
                coroutineScope.launch {
                    try {
                        val bestRecording = recordingSelectionService.getBestRecording(show)
                        if (bestRecording != null) {
                            resumeRecordingDownloads(bestRecording.identifier)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming download for show ${show.showId}", e)
                        onError("Failed to resume download: ${e.message}")
                    }
                }
            }
            com.deadly.core.design.component.DownloadAction.CANCEL -> {
                coroutineScope.launch {
                    try {
                        cancelShowDownloads(show)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error canceling download for show ${show.showId}", e)
                        onError("Failed to cancel download: ${e.message}")
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
    val downloadState: com.deadly.core.model.DownloadState,
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