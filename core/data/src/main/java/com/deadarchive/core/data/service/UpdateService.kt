package com.deadarchive.core.data.service

import com.deadarchive.core.model.AppUpdate
import com.deadarchive.core.model.UpdateDownloadState
import com.deadarchive.core.model.UpdateStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Service interface for app update functionality.
 * 
 * Handles checking for updates from GitHub releases, downloading APK files,
 * and managing update preferences like skipped versions.
 */
interface UpdateService {
    
    /**
     * Check for available app updates from GitHub releases.
     * 
     * @return UpdateStatus containing update information or error details
     */
    suspend fun checkForUpdates(): Result<UpdateStatus>
    
    /**
     * Download an app update APK file.
     * 
     * @param update The update to download
     * @return Result containing the downloaded APK file or error
     */
    suspend fun downloadUpdate(update: AppUpdate): Result<File>
    
    /**
     * Get real-time download progress for an update.
     * 
     * @param update The update being downloaded
     * @return Flow of download state updates
     */
    fun getDownloadProgress(update: AppUpdate): Flow<UpdateDownloadState>
    
    /**
     * Install a downloaded APK file using PackageInstaller.
     * 
     * @param apkFile The APK file to install
     * @return Result indicating success or failure
     */
    suspend fun installUpdate(apkFile: File): Result<Unit>
    
    /**
     * Mark a specific version as skipped by the user.
     * 
     * @param version Version string to skip (e.g., "1.2.3")
     */
    suspend fun skipUpdate(version: String)
    
    /**
     * Get the set of versions that have been skipped by the user.
     * 
     * @return Flow of skipped version strings
     */
    fun getSkippedVersions(): Flow<Set<String>>
    
    /**
     * Check if a specific version has been skipped.
     * 
     * @param version Version string to check
     * @return True if the version has been skipped
     */
    suspend fun isVersionSkipped(version: String): Boolean
    
    /**
     * Clear all skipped versions (for settings reset).
     */
    suspend fun clearSkippedVersions()
    
    /**
     * Get the timestamp of the last update check.
     * 
     * @return Flow of the last check timestamp (0L if never checked)
     */
    fun getLastUpdateCheck(): Flow<Long>
    
    /**
     * Check if automatic update checking is enabled.
     * 
     * @return Flow of automatic update check preference
     */
    fun isAutoUpdateCheckEnabled(): Flow<Boolean>
    
    /**
     * Enable or disable automatic update checking on startup.
     * 
     * @param enabled True to enable automatic checking
     */
    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean)
}