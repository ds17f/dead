package com.deadly.core.download.api.repository

import com.deadly.core.design.component.DownloadState
import com.deadly.core.design.component.ShowDownloadState
import com.deadly.core.model.Show
import com.deadly.core.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * API interface for download operations.
 * Defines the contract for managing downloads without exposing implementation details.
 */
interface DownloadRepository {
    
    /**
     * Download states for all shows, keyed by recordingId
     */
    val downloadStates: StateFlow<Map<String, ShowDownloadState>>
    
    /**
     * Track download states for individual files, keyed by downloadId
     */
    val trackDownloadStates: StateFlow<Map<String, Boolean>>
    
    /**
     * Show confirmation dialog state
     */
    val showConfirmationDialog: StateFlow<Show?>
    
    /**
     * Start downloading a recording
     */
    suspend fun downloadRecording(recording: Recording)
    
    /**
     * Start downloading the best recording of a show
     */
    suspend fun downloadShow(show: Show)
    
    /**
     * Get the current download state for a recording
     */
    fun getDownloadState(recording: Recording): DownloadState
    
    /**
     * Get the current download state for a show
     */
    fun getShowDownloadState(show: Show): ShowDownloadState
    
    /**
     * Cancel all downloads for a show
     */
    suspend fun cancelShowDownloads(show: Show)
    
    /**
     * Clear/remove all downloads for a show
     */
    suspend fun clearShowDownloads(show: Show)
    
    /**
     * Cancel an individual download
     */
    suspend fun cancelDownload(downloadId: String)
    
    /**
     * Retry a failed download
     */
    suspend fun retryDownload(downloadId: String)
    
    /**
     * Force start a queued download
     */
    suspend fun forceDownload(downloadId: String)
    
    /**
     * Remove a download completely from the system
     */
    suspend fun removeDownload(downloadId: String)
    
    /**
     * Pause a download
     */
    suspend fun pauseDownload(downloadId: String)
    
    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(downloadId: String)
    
    /**
     * Get all downloads as a flow
     */
    fun getAllDownloads(): Flow<List<Any>> // TODO: Define proper download model
    
    /**
     * Get enriched downloads with show and track metadata
     */
    suspend fun getEnrichedDownloads(): List<Any> // TODO: Define proper enriched download model
    
    /**
     * Show confirmation dialog for removing download
     */
    fun showRemoveDownloadConfirmation(show: Show)
    
    /**
     * Hide confirmation dialog
     */
    fun hideConfirmationDialog()
    
    /**
     * Confirm removal of download
     */
    suspend fun confirmRemoveDownload()
}