package com.deadarchive.feature.player.service

import android.util.Log
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerDownloadServiceImpl @Inject constructor(
    private val downloadRepository: DownloadRepository
) : PlayerDownloadService {
    
    companion object {
        private const val TAG = "PlayerDownloadService"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _downloadStates = MutableStateFlow<Map<String, ShowDownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, ShowDownloadState>> = _downloadStates.asStateFlow()
    
    private val _trackDownloadStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val trackDownloadStates: StateFlow<Map<String, Boolean>> = _trackDownloadStates.asStateFlow()
    
    override fun startDownloadStateMonitoring() {
        Log.d(TAG, "startDownloadStateMonitoring: Starting download state monitoring")
        
        serviceScope.launch {
            try {
                // Monitor download progress for all downloads
                downloadRepository.getAllDownloads().collect { downloads ->
                    val states = mutableMapOf<String, ShowDownloadState>()
                    val trackStates = mutableMapOf<String, Boolean>()
                    
                    downloads.forEach { download ->
                        val state = when (download.status) {
                            DownloadStatus.QUEUED -> ShowDownloadState.NotDownloaded
                            DownloadStatus.DOWNLOADING -> ShowDownloadState.Downloading(download.progress)
                            DownloadStatus.PAUSED -> ShowDownloadState.Downloading(download.progress)
                            DownloadStatus.COMPLETED -> ShowDownloadState.Downloaded
                            DownloadStatus.FAILED -> ShowDownloadState.Failed(download.errorMessage ?: "Download failed")
                            DownloadStatus.CANCELLED -> ShowDownloadState.NotDownloaded
                        }
                        
                        states[download.recordingId] = state
                        trackStates[download.trackFilename] = download.status == DownloadStatus.COMPLETED
                    }
                    
                    _downloadStates.value = states
                    _trackDownloadStates.value = trackStates
                }
            } catch (e: Exception) {
                Log.e(TAG, "startDownloadStateMonitoring: Error monitoring downloads", e)
            }
        }
    }
    
    override suspend fun downloadRecording(recording: Recording) {
        Log.d(TAG, "downloadRecording: Starting download for recording ${recording.identifier}")
        
        try {
            downloadRepository.downloadRecording(recording)
            Log.d(TAG, "downloadRecording: Download initiated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "downloadRecording: Error starting download", e)
            throw e
        }
    }
    
    override suspend fun cancelRecordingDownloads(recording: Recording) {
        Log.d(TAG, "cancelRecordingDownloads: Canceling downloads for recording ${recording.identifier}")
        
        try {
            downloadRepository.cancelDownload(recording.identifier)
            Log.d(TAG, "cancelRecordingDownloads: Downloads canceled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "cancelRecordingDownloads: Error canceling downloads", e)
            throw e
        }
    }
    
    override fun getRecordingDownloadState(recording: Recording): ShowDownloadState {
        val state = _downloadStates.value[recording.identifier] ?: ShowDownloadState.NotDownloaded
        Log.d(TAG, "getRecordingDownloadState: Recording ${recording.identifier} state: $state")
        return state
    }
    
    override suspend fun isTrackDownloaded(track: Track): Boolean {
        val isDownloaded = track.audioFile?.downloadUrl?.let { url ->
            _trackDownloadStates.value[url] ?: false
        } ?: false
        
        Log.d(TAG, "isTrackDownloaded: Track '${track.displayTitle}' downloaded: $isDownloaded")
        return isDownloaded
    }
    
    override fun showRemoveDownloadConfirmation(recording: Recording) {
        Log.d(TAG, "showRemoveDownloadConfirmation: Showing confirmation for recording ${recording.identifier}")
        // This would typically trigger a UI dialog - implementation depends on UI architecture
        // For now, we'll just log the intent
    }
}