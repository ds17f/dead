package com.deadarchive.core.download.api

import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow

/**
 * Clean API interface for Download V2 operations.
 * Defines the contract that both stub and real implementations must follow.
 * 
 * This interface is designed to be shared across all UI components (Library, Browse, Playlist)
 * that need download functionality, providing a consistent API for download operations.
 */
interface DownloadV2Service {
    
    /**
     * Start downloading a show (best recording)
     * @param show The show to download
     * @return Result indicating success or failure with error details
     */
    suspend fun downloadShow(show: Show): Result<Unit>
    
    /**
     * Cancel all active downloads for a show
     * @param show The show to cancel downloads for
     * @return Result indicating success or failure with error details
     */
    suspend fun cancelShowDownloads(show: Show): Result<Unit>
    
    /**
     * Get the current download status for a show (reactive)
     * @param show The show to check
     * @return Flow of DownloadStatus
     */
    fun getDownloadStatus(show: Show): Flow<DownloadStatus>
    
    /**
     * Get the current download progress for a show (reactive)
     * @param show The show to check
     * @return Flow of DownloadProgress
     */
    fun getDownloadProgress(show: Show): Flow<DownloadProgress>
    
    /**
     * Check if a show has any downloads (reactive)
     * @param show The show to check
     * @return Flow of boolean indicating if show has downloads
     */
    fun hasDownloads(show: Show): Flow<Boolean>
}

/**
 * Enum representing the various states a download can be in
 */
enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING, 
    COMPLETED,
    FAILED
}

/**
 * Data class containing download progress information
 */
data class DownloadProgress(
    val progress: Float,        // 0.0 to 1.0
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: DownloadStatus
)