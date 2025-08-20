package com.deadly.core.download.service

import android.util.Log
import com.deadly.core.download.api.DownloadV2Service
import com.deadly.core.download.api.DownloadStatus
import com.deadly.core.download.api.DownloadProgress
import com.deadly.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced stub implementation of DownloadV2Service with stateful download tracking.
 * 
 * This stub provides realistic behavior for UI development by:
 * - Maintaining in-memory state of downloaded shows
 * - Providing immediate feedback for download toggle actions
 * - Logging all method calls clearly for debugging
 * - Supporting reactive UI updates via StateFlow
 * 
 * This allows full UI development and testing without real download functionality.
 */
@Singleton
class DownloadV2ServiceStub @Inject constructor() : DownloadV2Service {
    
    companion object {
        private const val TAG = "DownloadV2ServiceStub"
    }
    
    // In-memory state for stub download tracking
    private val downloadedShows = MutableStateFlow<Set<String>>(emptySet())
    
    override suspend fun downloadShow(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: downloadShow(showId='${show.showId}') called")
        
        return if (downloadedShows.value.contains(show.showId)) {
            Log.d(TAG, "STUB: show already downloaded")
            Result.success(Unit) // Already downloaded, still success
        } else {
            downloadedShows.value = downloadedShows.value + show.showId
            Log.d(TAG, "STUB: added show to downloaded set, now has ${downloadedShows.value.size} shows")
            Result.success(Unit)
        }
    }
    
    override suspend fun cancelShowDownloads(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: cancelShowDownloads(showId='${show.showId}') called")
        
        downloadedShows.value = downloadedShows.value - show.showId
        Log.d(TAG, "STUB: removed show from downloaded set, now has ${downloadedShows.value.size} shows")
        return Result.success(Unit)
    }
    
    override fun getDownloadStatus(show: Show): Flow<DownloadStatus> {
        Log.d(TAG, "STUB: getDownloadStatus(showId='${show.showId}') called")
        
        return downloadedShows.map { downloadedSet ->
            val status = if (downloadedSet.contains(show.showId)) {
                DownloadStatus.COMPLETED
            } else {
                DownloadStatus.NOT_DOWNLOADED
            }
            Log.d(TAG, "STUB: status for ${show.showId} = $status")
            status
        }
    }
    
    override fun getDownloadProgress(show: Show): Flow<DownloadProgress> {
        Log.d(TAG, "STUB: getDownloadProgress(showId='${show.showId}') called")
        
        return downloadedShows.map { downloadedSet ->
            if (downloadedSet.contains(show.showId)) {
                DownloadProgress(
                    progress = 1.0f, // Complete
                    bytesDownloaded = 150_000_000L, // ~150MB simulated
                    totalBytes = 150_000_000L,
                    status = DownloadStatus.COMPLETED
                )
            } else {
                DownloadProgress(
                    progress = 0.0f,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    status = DownloadStatus.NOT_DOWNLOADED
                )
            }
        }
    }
    
    override fun hasDownloads(show: Show): Flow<Boolean> {
        Log.d(TAG, "STUB: hasDownloads(showId='${show.showId}') called")
        
        return downloadedShows.map { downloadedSet ->
            val hasDownload = downloadedSet.contains(show.showId)
            Log.d(TAG, "STUB: hasDownloads for ${show.showId} = $hasDownload")
            hasDownload
        }
    }
}