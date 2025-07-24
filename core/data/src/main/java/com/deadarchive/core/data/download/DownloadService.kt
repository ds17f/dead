package com.deadarchive.core.data.download

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.deadarchive.core.data.repository.DownloadRepository
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
 */
@Singleton
class DownloadService @Inject constructor(
    private val downloadRepository: DownloadRepository
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
            // Get the best recording for this show
            val bestRecording = show.bestRecording
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
            val bestRecording = show.bestRecording
            if (bestRecording != null) {
                downloadRepository.cancelDownload(bestRecording.identifier)
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
        val bestRecording = show.bestRecording
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
     * Check if a track is downloaded
     */
    suspend fun isTrackDownloaded(track: Track): Boolean {
        val isDownloaded = track.audioFile?.downloadUrl?.let { url ->
            _trackDownloadStates.value[url] ?: false
        } ?: false
        
        Log.d(TAG, "Track '${track.displayTitle}' downloaded: $isDownloaded")
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
     * Confirm removal of download (soft delete)
     */
    suspend fun confirmRemoveDownload() {
        val show = _showConfirmationDialog.value
        if (show != null) {
            Log.d(TAG, "Confirming removal of download for show ${show.showId}")
            try {
                cancelShowDownloads(show)
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
}