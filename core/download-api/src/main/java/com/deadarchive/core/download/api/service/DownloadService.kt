package com.deadarchive.core.download.api.service

import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import kotlinx.coroutines.CoroutineScope

/**
 * API interface for high-level download service operations.
 * Provides coordinated download functionality with state management and error handling.
 */
interface DownloadService {
    
    /**
     * Handle download button click with smart state-based logic
     */
    fun handleDownloadButtonClick(
        show: Show,
        coroutineScope: CoroutineScope,
        onError: (String) -> Unit
    )
    
    /**
     * Start downloading a recording with progress tracking
     */
    suspend fun startRecordingDownload(
        recording: Recording,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    
    /**
     * Start downloading the best recording of a show
     */
    suspend fun startShowDownload(
        show: Show,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    
    /**
     * Check if a show has any downloads
     */
    suspend fun hasDownloads(show: Show): Boolean
    
    /**
     * Get download progress for a show (0.0 to 1.0)
     */
    suspend fun getDownloadProgress(show: Show): Float
    
    /**
     * Get download information for a show
     */
    suspend fun getDownloadInfo(show: Show): String
    
    /**
     * Cancel all active downloads for a show
     */
    suspend fun cancelActiveDownloads(show: Show)
    
    /**
     * Remove all downloaded content for a show
     */
    suspend fun removeDownloadedContent(show: Show)
    
    /**
     * Pause all downloads for a show
     */
    suspend fun pauseShowDownloads(show: Show)
    
    /**
     * Resume all paused downloads for a show
     */
    suspend fun resumeShowDownloads(show: Show)
}