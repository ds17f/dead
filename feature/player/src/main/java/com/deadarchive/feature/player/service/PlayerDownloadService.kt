package com.deadarchive.feature.player.service

import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Service responsible for download management and monitoring.
 * Handles recording downloads, track download states, and download monitoring.
 */
interface PlayerDownloadService {
    
    /**
     * Download states for recordings/shows
     */
    val downloadStates: StateFlow<Map<String, ShowDownloadState>>
    
    /**
     * Track-level download states
     */
    val trackDownloadStates: StateFlow<Map<String, Boolean>>
    
    /**
     * Start monitoring download states for the current context
     */
    fun startDownloadStateMonitoring()
    
    /**
     * Download the current recording
     * @param recording Recording to download
     */
    suspend fun downloadRecording(recording: Recording)
    
    /**
     * Cancel downloads for the current recording
     * @param recording Recording to cancel downloads for
     */
    suspend fun cancelRecordingDownloads(recording: Recording)
    
    /**
     * Get download state for the current recording
     * @param recording Recording to check download state for
     * @return Current download state
     */
    fun getRecordingDownloadState(recording: Recording): ShowDownloadState
    
    /**
     * Check if a specific track is downloaded
     * @param track Track to check
     * @return True if track is downloaded
     */
    suspend fun isTrackDownloaded(track: Track): Boolean
    
    /**
     * Show confirmation dialog for removing downloads
     * @param recording Recording to potentially remove downloads for
     */
    fun showRemoveDownloadConfirmation(recording: Recording)
}