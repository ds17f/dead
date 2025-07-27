package com.deadarchive.core.download.service

import android.util.Log
import com.deadarchive.core.download.api.DownloadV2Service
import com.deadarchive.core.download.api.DownloadStatus
import com.deadarchive.core.download.api.DownloadProgress
import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal logging-only stub implementation of DownloadV2Service.
 * 
 * This stub provides the simplest possible implementation that:
 * - Logs all method calls clearly
 * - Returns safe default values
 * - Establishes the architecture and integration patterns
 * 
 * This stub is designed to be shared across all UI components (Library, Browse, Playlist)
 * to validate the architecture before implementing complex download functionality.
 */
@Singleton
class DownloadV2ServiceStub @Inject constructor() : DownloadV2Service {
    
    companion object {
        private const val TAG = "DownloadV2ServiceStub"
    }
    
    override suspend fun downloadShow(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: downloadShow(showId='${show.showId}') called")
        // Just log and return success
        return Result.success(Unit)
    }
    
    override suspend fun cancelShowDownloads(show: Show): Result<Unit> {
        Log.d(TAG, "STUB: cancelShowDownloads(showId='${show.showId}') called")
        // Just log and return success
        return Result.success(Unit)
    }
    
    override fun getDownloadStatus(show: Show): Flow<DownloadStatus> {
        Log.d(TAG, "STUB: getDownloadStatus(showId='${show.showId}') called")
        // Always return not downloaded
        return flowOf(DownloadStatus.NOT_DOWNLOADED)
    }
    
    override fun getDownloadProgress(show: Show): Flow<DownloadProgress> {
        Log.d(TAG, "STUB: getDownloadProgress(showId='${show.showId}') called")
        // Return zero progress
        return flowOf(
            DownloadProgress(
                progress = 0.0f,
                bytesDownloaded = 0L,
                totalBytes = 0L,
                status = DownloadStatus.NOT_DOWNLOADED
            )
        )
    }
    
    override fun hasDownloads(show: Show): Flow<Boolean> {
        Log.d(TAG, "STUB: hasDownloads(showId='${show.showId}') called")
        // Always return false
        return flowOf(false)
    }
}