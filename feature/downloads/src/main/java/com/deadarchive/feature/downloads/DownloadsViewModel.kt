package com.deadarchive.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.download.DownloadQueueManager
import com.deadarchive.core.model.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val downloadQueueManager: DownloadQueueManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()
    
    init {
        loadDownloads()
    }
    
    private fun loadDownloads() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            downloadRepository.getAllDownloads().collect { downloads ->
                val downloadUiModels = downloads.map { download ->
                    DownloadUiModel(
                        id = download.id,
                        recordingId = download.recordingId,
                        trackFilename = download.trackFilename,
                        status = download.status,
                        progress = download.progress,
                        bytesDownloaded = download.bytesDownloaded,
                        totalBytes = download.totalBytes,
                        errorMessage = download.errorMessage,
                        createdAt = download.createdAt,
                        updatedAt = download.updatedAt
                    )
                }
                
                val totalDownloads = downloadUiModels.size
                val completedDownloads = downloadUiModels.count { it.isCompleted }
                val activeDownloads = downloadUiModels.count { it.isActive }
                val queuedDownloads = downloadUiModels.count { it.isQueued }
                
                _uiState.value = DownloadsUiState(
                    isLoading = false,
                    downloads = downloadUiModels,
                    totalDownloads = totalDownloads,
                    completedDownloads = completedDownloads,
                    activeDownloads = activeDownloads,
                    queuedDownloads = queuedDownloads
                )
            }
        }
    }
    
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.cancelDownload(downloadId)
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
    
    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            try {
                downloadRepository.retryDownload(downloadId)
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
    
    fun clearCompleted() {
        viewModelScope.launch {
            try {
                downloadRepository.clearCompletedDownloads()
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
    
    fun pauseAll() {
        viewModelScope.launch {
            try {
                downloadRepository.pauseAllDownloads()
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
    
    fun resumeAll() {
        viewModelScope.launch {
            try {
                downloadRepository.resumeAllDownloads()
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
    
    fun triggerQueueProcessing() {
        viewModelScope.launch {
            try {
                downloadQueueManager.triggerImmediateProcessing()
            } catch (e: Exception) {
                // Handle error - could show snackbar
            }
        }
    }
}

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val downloads: List<DownloadUiModel> = emptyList(),
    val totalDownloads: Int = 0,
    val completedDownloads: Int = 0,
    val activeDownloads: Int = 0,
    val queuedDownloads: Int = 0,
    val error: String? = null
)

data class DownloadUiModel(
    val id: String,
    val recordingId: String,
    val trackFilename: String,
    val status: DownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isQueued: Boolean
        get() = status == DownloadStatus.QUEUED
    
    val isActive: Boolean
        get() = status == DownloadStatus.DOWNLOADING
    
    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED
    
    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED || 
                 status == DownloadStatus.CANCELLED
    
    val statusText: String
        get() = when (status) {
            DownloadStatus.QUEUED -> "Queued"
            DownloadStatus.DOWNLOADING -> if (progress >= 0f) "${(progress * 100).toInt()}%" else "Downloading..."
            DownloadStatus.COMPLETED -> "Completed"
            DownloadStatus.FAILED -> "Failed"
            DownloadStatus.CANCELLED -> "Cancelled"
            DownloadStatus.PAUSED -> "Paused"
        }
}