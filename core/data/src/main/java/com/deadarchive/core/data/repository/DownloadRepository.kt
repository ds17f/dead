package com.deadarchive.core.data.repository

import android.content.Context
import android.os.Environment
import com.deadarchive.core.data.mapper.DataMappers.createDownloadState
import com.deadarchive.core.data.mapper.DataMappers.toDownloadEntity
import com.deadarchive.core.data.mapper.DataMappers.toDownloadState
import com.deadarchive.core.data.mapper.DataMappers.toDownloadStates
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadRepository {
    /**
     * Get all downloads with real-time updates
     */
    fun getAllDownloads(): Flow<List<DownloadState>>
    
    /**
     * Get downloads filtered by status
     */
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadState>>
    
    /**
     * Get all active downloads (queued, downloading, paused)
     */
    fun getActiveDownloads(): Flow<List<DownloadState>>
    
    /**
     * Get completed downloads
     */
    fun getCompletedDownloads(): Flow<List<DownloadState>>
    
    /**
     * Get downloads for a specific recording
     */
    fun getDownloadsForRecording(recordingId: String): Flow<List<DownloadState>>
    
    /**
     * Get download by ID
     */
    suspend fun getDownloadById(id: String): DownloadState?
    
    /**
     * Start downloading a recording track
     */
    suspend fun startDownload(recording: Recording, trackFilename: String): String
    
    /**
     * Start downloading all tracks for a recording
     */
    suspend fun startRecordingDownload(recording: Recording): List<String>
    
    /**
     * Convenience method to download a recording (wrapper for startRecordingDownload)
     */
    suspend fun downloadRecording(recording: Recording): List<String>
    
    /**
     * Update download progress
     */
    suspend fun updateDownloadProgress(id: String, progress: Float, bytesDownloaded: Long)
    
    /**
     * Update download status
     */
    suspend fun updateDownloadStatus(id: String, status: DownloadStatus, errorMessage: String? = null)
    
    /**
     * Update download local file path
     */
    suspend fun updateDownloadLocalPath(id: String, localPath: String)
    
    /**
     * Pause a download
     */
    suspend fun pauseDownload(id: String)
    
    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(id: String)
    
    /**
     * Cancel a download
     */
    suspend fun cancelDownload(id: String)
    
    /**
     * Delete a download record
     */
    suspend fun deleteDownload(id: String)
    
    /**
     * Cancel all active downloads
     */
    suspend fun cancelAllActiveDownloads()
    
    /**
     * Cancel all downloads for a specific recording
     */
    suspend fun cancelRecordingDownloads(recordingId: String)
    
    /**
     * Delete all completed downloads
     */
    suspend fun deleteCompletedDownloads()
    
    /**
     * Delete all failed downloads
     */
    suspend fun deleteFailedDownloads()
    
    /**
     * Check if a track is downloaded
     */
    suspend fun isTrackDownloaded(recordingId: String, trackFilename: String): Boolean
    
    /**
     * Get local file path for a downloaded track
     */
    suspend fun getLocalFilePath(recordingId: String, trackFilename: String): String?
    
    /**
     * Enhanced download queue management
     */
    suspend fun getDownloadQueue(): List<DownloadState>
    suspend fun reorderQueue(downloadIds: List<String>)
    suspend fun clearQueue()
    
    /**
     * Download priority management
     */
    suspend fun setDownloadPriority(downloadId: String, priority: Int)
    suspend fun getNextQueuedDownload(): DownloadState?
    
    /**
     * File system integration
     */
    suspend fun getDownloadDirectory(): File
    suspend fun cleanupIncompleteDownloads()
    suspend fun validateStorageSpace(requiredBytes: Long): Boolean
    suspend fun getUsedStorageSpace(): Long
    suspend fun getAvailableStorageSpace(): Long
    
    /**
     * Retry and recovery operations
     */
    suspend fun retryFailedDownload(downloadId: String): String
    suspend fun retryAllFailedDownloads(): List<String>
    suspend fun autoRetryDownloads(maxRetries: Int = 3)
    
    /**
     * Batch download operations
     */
    suspend fun startMultipleDownloads(downloads: List<Pair<Recording, String>>): List<String>
    suspend fun pauseAllDownloads()
    suspend fun resumeAllDownloads()
    
    /**
     * Download analytics and monitoring
     */
    suspend fun getDownloadStats(): DownloadStats
    suspend fun getDownloadHistory(): List<DownloadState>
    
    /**
     * Export/Import downloaded content
     */
    suspend fun exportDownloadList(): List<DownloadState>
    suspend fun getDownloadedTracks(): List<Pair<Recording, String>>
    
    /**
     * Queue management operations
     */
    fun startDownloadQueueProcessing()
    fun stopDownloadQueueProcessing()
    suspend fun isQueueProcessingActive(): Boolean
    
    /**
     * Additional download management methods
     */
    suspend fun retryDownload(downloadId: String)
    suspend fun clearCompletedDownloads()
    
    /**
     * Soft delete methods for Spotify-style behavior
     */
    suspend fun markDownloadForDeletion(downloadId: String)
    suspend fun markRecordingForDeletion(recordingId: String)
    suspend fun restoreDeletedDownload(downloadId: String)
    suspend fun restoreDeletedRecording(recordingId: String)
    suspend fun getDownloadsMarkedForDeletion(): List<DownloadState>
    suspend fun cleanupDeletedDownloads(olderThanTimestamp: Long)
    suspend fun getStorageThreshold(): Long
}

/**
 * Download statistics data class
 */
data class DownloadStats(
    val totalDownloads: Int,
    val completedDownloads: Int,
    val failedDownloads: Int,
    val totalBytesDownloaded: Long,
    val averageDownloadSpeed: Double,
    val mostDownloadedRecording: String?
)

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val recordingDao: com.deadarchive.core.database.RecordingDao,
    private val libraryRepository: LibraryRepository,
    private val showRepository: ShowRepository,
    private val audioFormatFilterService: com.deadarchive.core.data.service.AudioFormatFilterService,
    private val downloadQueueManager: com.deadarchive.core.data.download.DownloadQueueManager,
    @ApplicationContext private val context: Context
) : DownloadRepository {
    
    // Download directory management
    private val downloadDir by lazy {
        // Use app-specific external files directory (no permissions required)
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val downloadDir = if (externalDir != null) {
            File(externalDir, "DeadArchive")
        } else {
            // Fallback to internal files directory
            File(context.filesDir, "downloads")
        }
        
        downloadDir.apply {
            if (!exists()) {
                val created = mkdirs()
                android.util.Log.d("DownloadRepository", "📁 Download directory created: $created at $absolutePath")
                android.util.Log.d("DownloadRepository", "📁 Directory permissions - canRead: ${canRead()}, canWrite: ${canWrite()}")
            } else {
                android.util.Log.d("DownloadRepository", "📁 Download directory exists at $absolutePath")
                android.util.Log.d("DownloadRepository", "📁 Directory permissions - canRead: ${canRead()}, canWrite: ${canWrite()}")
            }
        }
    }

    override fun getAllDownloads(): Flow<List<DownloadState>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.toDownloadStates()
        }
    }

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadState>> {
        return downloadDao.getDownloadsByStatus(status.name).map { entities ->
            entities.map { it.toDownloadState() }
        }
    }

    override fun getActiveDownloads(): Flow<List<DownloadState>> {
        return downloadDao.getActiveDownloads().map { entities ->
            entities.map { it.toDownloadState() }
        }
    }

    override fun getCompletedDownloads(): Flow<List<DownloadState>> {
        return downloadDao.getCompletedDownloads().map { entities ->
            entities.map { it.toDownloadState() }
        }
    }

    override fun getDownloadsForRecording(recordingId: String): Flow<List<DownloadState>> {
        return downloadDao.getDownloadsForConcert(recordingId).map { entities ->
            entities.map { it.toDownloadState() }
        }
    }

    override suspend fun getDownloadById(id: String): DownloadState? {
        return downloadDao.getDownloadById(id)?.toDownloadState()
    }

    override suspend fun startDownload(recording: Recording, trackFilename: String): String {
        // Get the proper streaming URL instead of constructing direct Archive.org URL
        val streamingUrl = showRepository.getStreamingUrl(recording.identifier, trackFilename)
            ?: "https://archive.org/download/${recording.identifier}/$trackFilename" // Fallback to direct URL
        
        return startDownloadWithUrl(recording, trackFilename, streamingUrl)
    }
    
    /**
     * Start download with a specific URL (used internally)
     */
    private suspend fun startDownloadWithUrl(recording: Recording, trackFilename: String, url: String): String {
        val downloadId = "${recording.identifier}_$trackFilename"
        
        // Check if download already exists
        val existingDownload = downloadDao.getDownloadById(downloadId)
        if (existingDownload != null) {
            // If it's failed or cancelled, reset it to queued
            if (existingDownload.status in listOf("FAILED", "CANCELLED")) {
                val updatedDownload = existingDownload.copy(
                    status = DownloadStatus.QUEUED.name,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    errorMessage = null,
                    startedTimestamp = System.currentTimeMillis(),
                    completedTimestamp = null
                )
                downloadDao.updateDownload(updatedDownload)
            }
            return downloadId
        }

        android.util.Log.d("DownloadRepository", "📱 Creating download for ${recording.identifier}/$trackFilename with URL: $url")
        
        // Create new download entry with resolved streaming URL
        val downloadEntity = createDownloadState(
            recordingId = recording.identifier,
            trackFilename = trackFilename,
            url = url,
            status = DownloadStatus.QUEUED
        )

        downloadDao.insertDownload(downloadEntity)
        
        // Trigger immediate queue processing when new download is added
        downloadQueueManager.triggerImmediateProcessing()
        
        return downloadId
    }

    override suspend fun startRecordingDownload(recording: Recording): List<String> {
        val downloadIds = mutableListOf<String>()
        
        // Get track streaming URLs to determine available tracks
        val trackUrls = showRepository.getTrackStreamingUrls(recording.identifier)
        
        // Apply format filtering to get only preferred formats (MP3 by default)
        val preferredFormats = com.deadarchive.core.model.AppConstants.PREFERRED_AUDIO_FORMATS
        val filteredTracks = audioFormatFilterService.filterTracksByPreferredFormat(
            tracks = trackUrls.map { (audioFile, url) ->
                com.deadarchive.core.model.Track(
                    filename = audioFile.filename,
                    title = extractTitleFromFilename(audioFile.filename),
                    trackNumber = extractTrackNumberFromFilename(audioFile.filename),
                    audioFile = audioFile,
                    streamingUrl = url
                )
            },
            formatPreferences = preferredFormats
        )
        
        android.util.Log.d("DownloadRepository", "📱 Filtered from ${trackUrls.size} tracks to ${filteredTracks.size} preferred format tracks for ${recording.identifier}")
        
        for (track in filteredTracks) {
            val downloadId = startDownloadWithUrl(recording, track.filename, track.streamingUrl ?: "")
            downloadIds.add(downloadId)
        }
        
        // Trigger immediate queue processing when downloads are added
        if (downloadIds.isNotEmpty()) {
            downloadQueueManager.triggerImmediateProcessing()
        }
        
        return downloadIds
    }
    
    override suspend fun downloadRecording(recording: Recording): List<String> {
        return startRecordingDownload(recording)
    }

    override suspend fun updateDownloadProgress(id: String, progress: Float, bytesDownloaded: Long) {
        downloadDao.updateDownloadProgress(id, progress, bytesDownloaded)
    }

    override suspend fun updateDownloadStatus(id: String, status: DownloadStatus, errorMessage: String?) {
        val timestamp = if (status == DownloadStatus.COMPLETED) {
            System.currentTimeMillis()
        } else null
        
        downloadDao.updateDownloadStatus(id, status.name, timestamp)
        
        // Update error message if provided
        if (errorMessage != null) {
            val existingDownload = downloadDao.getDownloadById(id)
            existingDownload?.let { download ->
                val updatedDownload = download.copy(errorMessage = errorMessage)
                downloadDao.updateDownload(updatedDownload)
            }
        }
        
        // Check if recording is complete when a track finishes downloading
        if (status == DownloadStatus.COMPLETED) {
            val download = downloadDao.getDownloadById(id)
            download?.let { 
                checkAndUpdateRecordingDownloadStatus(it.recordingId)
            }
        }
    }
    
    /**
     * Check if all tracks of a recording are downloaded and update recording status
     */
    private suspend fun checkAndUpdateRecordingDownloadStatus(recordingId: String) {
        try {
            // Get all downloads for this recording
            val recordingDownloads = downloadDao.getAllDownloadsSync()
                .filter { it.recordingId == recordingId }
            
            if (recordingDownloads.isNotEmpty()) {
                // Check if all downloads are completed
                val allCompleted = recordingDownloads.all { it.status == DownloadStatus.COMPLETED.name }
                
                if (allCompleted) {
                    // Update recording as downloaded
                    recordingDao.updateDownloadedStatus(recordingId, true)
                    android.util.Log.d("DownloadRepository", "✅ Recording $recordingId marked as downloaded (${recordingDownloads.size} tracks)")
                    
                    // Auto-add completed recording's show to library
                    try {
                        val recordingEntity = recordingDao.getRecordingById(recordingId)
                        if (recordingEntity != null) {
                            val show = showRepository.getShowById(recordingEntity.concertId)
                            if (show != null) {
                                val wasAdded = libraryRepository.addShowToLibrary(show)
                                if (wasAdded) {
                                    android.util.Log.d("DownloadRepository", "📚 Show ${show.showId} automatically added to library after download completion")
                                } else {
                                    android.util.Log.d("DownloadRepository", "📚 Show ${show.showId} already in library")
                                }
                            } else {
                                android.util.Log.w("DownloadRepository", "⚠️ Could not find show for downloaded recording $recordingId")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DownloadRepository", "❌ Failed to auto-add downloaded show to library", e)
                    }
                } else {
                    // Update recording as not downloaded (in case it was previously marked as downloaded)
                    recordingDao.updateDownloadedStatus(recordingId, false)
                    android.util.Log.d("DownloadRepository", "📋 Recording $recordingId status updated - ${recordingDownloads.count { it.status == DownloadStatus.COMPLETED.name }}/${recordingDownloads.size} tracks completed")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "❌ Failed to update recording download status for $recordingId", e)
        }
    }

    override suspend fun updateDownloadLocalPath(id: String, localPath: String) {
        downloadDao.updateDownloadLocalPath(id, localPath)
    }

    override suspend fun pauseDownload(id: String) {
        updateDownloadStatus(id, DownloadStatus.PAUSED)
    }

    override suspend fun resumeDownload(id: String) {
        updateDownloadStatus(id, DownloadStatus.QUEUED)
    }

    override suspend fun cancelDownload(id: String) {
        updateDownloadStatus(id, DownloadStatus.CANCELLED)
    }

    override suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownloadById(id)
    }

    override suspend fun cancelAllActiveDownloads() {
        downloadDao.cancelAllActiveDownloads()
    }

    override suspend fun cancelRecordingDownloads(recordingId: String) {
        // Get all active downloads for this recording
        val activeDownloads = downloadDao.getAllDownloadsSync()
            .filter { it.recordingId == recordingId && it.status in listOf("QUEUED", "DOWNLOADING", "PAUSED") }
        
        // Cancel each download
        activeDownloads.forEach { download ->
            updateDownloadStatus(download.id, DownloadStatus.CANCELLED)
        }
        
        android.util.Log.d("DownloadRepository", "🛑 Cancelled ${activeDownloads.size} downloads for recording $recordingId")
    }

    override suspend fun deleteCompletedDownloads() {
        downloadDao.deleteCompletedDownloads()
    }

    override suspend fun deleteFailedDownloads() {
        downloadDao.deleteFailedDownloads()
    }

    override suspend fun isTrackDownloaded(concertId: String, trackFilename: String): Boolean {
        val downloadId = "${concertId}_$trackFilename"
        val download = downloadDao.getDownloadById(downloadId)
        return download?.status == DownloadStatus.COMPLETED.name
    }

    override suspend fun getLocalFilePath(concertId: String, trackFilename: String): String? {
        val downloadId = "${concertId}_$trackFilename"
        val download = downloadDao.getDownloadById(downloadId)
        return if (download?.status == DownloadStatus.COMPLETED.name) {
            download.localPath
        } else null
    }

    // ============ Enhanced Queue Management ============

    override suspend fun getDownloadQueue(): List<DownloadState> {
        return downloadDao.getDownloadsByStatusList(DownloadStatus.QUEUED.name).toDownloadStates()
    }

    override suspend fun reorderQueue(downloadIds: List<String>) {
        // Update priority based on order in list
        downloadIds.forEachIndexed { index, downloadId ->
            setDownloadPriority(downloadId, downloadIds.size - index)
        }
    }

    override suspend fun clearQueue() {
        downloadDao.deleteDownloadsByStatus(DownloadStatus.QUEUED.name)
    }

    // ============ Priority Management ============

    override suspend fun setDownloadPriority(downloadId: String, priority: Int) {
        downloadDao.updateDownloadPriority(downloadId, priority)
    }

    override suspend fun getNextQueuedDownload(): DownloadState? {
        return downloadDao.getNextQueuedDownload()?.toDownloadState()
    }

    // ============ File System Integration ============

    override suspend fun getDownloadDirectory(): File {
        return downloadDir
    }

    override suspend fun cleanupIncompleteDownloads() {
        val incompleteDownloads = downloadDao.getIncompleteDownloads()
        for (download in incompleteDownloads) {
            download.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            downloadDao.deleteDownloadById(download.id)
        }
    }

    override suspend fun validateStorageSpace(requiredBytes: Long): Boolean {
        return getAvailableStorageSpace() >= requiredBytes
    }

    override suspend fun getUsedStorageSpace(): Long {
        return downloadDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    override suspend fun getAvailableStorageSpace(): Long {
        return downloadDir.usableSpace
    }

    // ============ Retry and Recovery Operations ============

    override suspend fun retryFailedDownload(downloadId: String): String {
        val download = downloadDao.getDownloadById(downloadId)
        if (download?.status == DownloadStatus.FAILED.name) {
            val updatedDownload = download.copy(
                status = DownloadStatus.QUEUED.name,
                progress = 0f,
                bytesDownloaded = 0L,
                errorMessage = null,
                startedTimestamp = System.currentTimeMillis()
            )
            downloadDao.updateDownload(updatedDownload)
        }
        return downloadId
    }

    override suspend fun retryAllFailedDownloads(): List<String> {
        val failedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.FAILED.name)
        val retriedIds = mutableListOf<String>()
        
        for (download in failedDownloads) {
            retryFailedDownload(download.id)
            retriedIds.add(download.id)
        }
        
        // Note: triggerImmediateProcessing() is already called in retryFailedDownload()
        return retriedIds
    }

    override suspend fun autoRetryDownloads(maxRetries: Int) {
        val failedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.FAILED.name)
        var hasRetries = false
        
        for (download in failedDownloads) {
            val retryCount = download.retryCount ?: 0
            if (retryCount < maxRetries) {
                val updatedDownload = download.copy(
                    status = DownloadStatus.QUEUED.name,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    errorMessage = null,
                    retryCount = retryCount + 1,
                    startedTimestamp = System.currentTimeMillis()
                )
                downloadDao.updateDownload(updatedDownload)
                hasRetries = true
            }
        }
    }

    // ============ Batch Operations ============

    override suspend fun startMultipleDownloads(downloads: List<Pair<Recording, String>>): List<String> {
        val downloadIds = mutableListOf<String>()
        
        for ((recording, trackFilename) in downloads) {
            val downloadId = startDownload(recording, trackFilename)
            downloadIds.add(downloadId)
        }
        
        return downloadIds
    }

    override suspend fun pauseAllDownloads() {
        val activeDownloads = downloadDao.getActiveDownloadsList()
        for (download in activeDownloads) {
            if (download.status in listOf(DownloadStatus.DOWNLOADING.name, DownloadStatus.QUEUED.name)) {
                updateDownloadStatus(download.id, DownloadStatus.PAUSED)
            }
        }
    }

    override suspend fun resumeAllDownloads() {
        val pausedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.PAUSED.name)
        for (download in pausedDownloads) {
            updateDownloadStatus(download.id, DownloadStatus.QUEUED)
        }
    }

    // ============ Analytics and Monitoring ============

    override suspend fun getDownloadStats(): DownloadStats {
        val totalDownloads = downloadDao.getTotalDownloadCount()
        val completedDownloads = downloadDao.getDownloadCountByStatus(DownloadStatus.COMPLETED.name)
        val failedDownloads = downloadDao.getDownloadCountByStatus(DownloadStatus.FAILED.name)
        val totalBytesDownloaded = downloadDao.getTotalBytesDownloaded()
        val averageDownloadSpeed = calculateAverageDownloadSpeed()
        val mostDownloadedRecording = downloadDao.getMostDownloadedRecording()

        return DownloadStats(
            totalDownloads = totalDownloads,
            completedDownloads = completedDownloads,
            failedDownloads = failedDownloads,
            totalBytesDownloaded = totalBytesDownloaded,
            averageDownloadSpeed = averageDownloadSpeed,
            mostDownloadedRecording = mostDownloadedRecording
        )
    }

    override suspend fun getDownloadHistory(): List<DownloadState> {
        return downloadDao.getDownloadHistory().toDownloadStates()
    }

    // ============ Export/Import Operations ============

    override suspend fun exportDownloadList(): List<DownloadState> {
        return downloadDao.getAllDownloadsSync().toDownloadStates()
    }

    override suspend fun getDownloadedTracks(): List<Pair<Recording, String>> {
        val completedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.COMPLETED.name)
        val downloadedTracks = mutableListOf<Pair<Recording, String>>()
        
        for (download in completedDownloads) {
            val recording = showRepository.getRecordingById(download.recordingId)
            if (recording != null) {
                downloadedTracks.add(Pair(recording, download.trackFilename))
            }
        }
        
        return downloadedTracks
    }

    // ============ Private Helper Methods ============

    private suspend fun calculateAverageDownloadSpeed(): Double {
        val completedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.COMPLETED.name)
        if (completedDownloads.isEmpty()) return 0.0
        
        val totalTime = completedDownloads.mapNotNull { download ->
            val startTime = download.startedTimestamp
            val endTime = download.completedTimestamp
            if (endTime != null && startTime != null) {
                (endTime - startTime) / 1000.0 // Convert to seconds
            } else null
        }.sum()
        
        val totalBytes = completedDownloads.sumOf { it.totalBytes }
        
        return if (totalTime > 0) totalBytes / totalTime else 0.0
    }

    // ============ Queue Management Operations ============
    // Note: Queue processing is managed by DownloadQueueManager directly
    // These methods are placeholders for interface compatibility

    override fun startDownloadQueueProcessing() {
        // Queue processing should be managed by DownloadQueueManager
        // This is called from external components that have access to DownloadQueueManager
    }

    override fun stopDownloadQueueProcessing() {
        // Queue processing should be managed by DownloadQueueManager
        // This is called from external components that have access to DownloadQueueManager
    }

    override suspend fun isQueueProcessingActive(): Boolean {
        // Queue processing status should be checked via DownloadQueueManager
        // This is a placeholder that returns false
        return false
    }
    
    override suspend fun retryDownload(downloadId: String) {
        try {
            // Reset download status to queued for retry
            downloadDao.updateDownloadStatus(downloadId, DownloadStatus.QUEUED.name)
            downloadDao.updateDownloadProgress(downloadId, 0f, 0L) // Reset progress
            android.util.Log.d("DownloadRepository", "🔄 Download retry queued: $downloadId")
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to retry download: $downloadId", e)
            throw e
        }
    }
    
    override suspend fun clearCompletedDownloads() {
        try {
            downloadDao.deleteDownloadsByStatus(DownloadStatus.COMPLETED.name)
            android.util.Log.d("DownloadRepository", "🗑️ Completed downloads cleared")
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to clear completed downloads", e)
            throw e
        }
    }
    
    // ============ Helper Methods for Format Filtering ============
    
    /**
     * Extract title from filename for track grouping
     */
    private fun extractTitleFromFilename(filename: String): String? {
        return filename.substringBeforeLast(".")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }
    
    /**
     * Extract track number from filename for sorting
     */
    private fun extractTrackNumberFromFilename(filename: String): String? {
        // Look for patterns like "t01", "d1t01", "track01", etc.
        val trackPattern = Regex("(?:t|track)(\\d+)", RegexOption.IGNORE_CASE)
        val match = trackPattern.find(filename)
        return match?.groupValues?.get(1)
    }
    
    // ============ Soft Delete Implementation ============
    
    override suspend fun markDownloadForDeletion(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null) {
            val updatedDownload = download.copy(
                isMarkedForDeletion = true,
                deletionTimestamp = System.currentTimeMillis()
            )
            downloadDao.updateDownload(updatedDownload)
            android.util.Log.d("DownloadRepository", "🗑️ Download marked for deletion: $downloadId")
        }
    }
    
    override suspend fun markRecordingForDeletion(recordingId: String) {
        // Mark all downloads for this recording for deletion
        val recordingDownloads = downloadDao.getAllDownloadsSync()
            .filter { it.recordingId == recordingId }
            
        recordingDownloads.forEach { download ->
            markDownloadForDeletion(download.id)
        }
        
        // Mark recording itself for deletion
        val recordingEntity = recordingDao.getRecordingById(recordingId)
        if (recordingEntity != null) {
            val updatedEntity = recordingEntity.copy(
                isMarkedForDeletion = true,
                deletionTimestamp = System.currentTimeMillis()
            )
            recordingDao.updateRecording(updatedEntity)
            android.util.Log.d("DownloadRepository", "🗑️ Recording marked for deletion: $recordingId")
        }
    }
    
    override suspend fun restoreDeletedDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download?.isMarkedForDeletion == true) {
            val updatedDownload = download.copy(
                isMarkedForDeletion = false,
                deletionTimestamp = null,
                lastAccessTimestamp = System.currentTimeMillis()
            )
            downloadDao.updateDownload(updatedDownload)
            android.util.Log.d("DownloadRepository", "♻️ Download restored from deletion: $downloadId")
        }
    }
    
    override suspend fun restoreDeletedRecording(recordingId: String) {
        // Restore all downloads for this recording
        val recordingDownloads = downloadDao.getAllDownloadsSync()
            .filter { it.recordingId == recordingId && it.isMarkedForDeletion }
            
        recordingDownloads.forEach { download ->
            restoreDeletedDownload(download.id)
        }
        
        // Restore recording itself
        val recordingEntity = recordingDao.getRecordingById(recordingId)
        if (recordingEntity?.isMarkedForDeletion == true) {
            val updatedEntity = recordingEntity.copy(
                isMarkedForDeletion = false,
                deletionTimestamp = null
            )
            recordingDao.updateRecording(updatedEntity)
            android.util.Log.d("DownloadRepository", "♻️ Recording restored from deletion: $recordingId")
        }
    }
    
    override suspend fun getDownloadsMarkedForDeletion(): List<DownloadState> {
        return downloadDao.getAllDownloadsSync()
            .filter { it.isMarkedForDeletion }
            .map { it.toDownloadState() }
    }
    
    override suspend fun cleanupDeletedDownloads(olderThanTimestamp: Long) {
        val downloadsToCleanup = downloadDao.getAllDownloadsSync()
            .filter { download ->
                val deletionTimestamp = download.deletionTimestamp
                download.isMarkedForDeletion && 
                deletionTimestamp != null && 
                deletionTimestamp < olderThanTimestamp
            }
        
        for (download in downloadsToCleanup) {
            // Delete actual file if it exists
            download.localPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d("DownloadRepository", "🗑️ File deletion: $path - success: $deleted")
                }
            }
            
            // Remove from database
            downloadDao.deleteDownloadById(download.id)
        }
        
        android.util.Log.d("DownloadRepository", "🧹 Cleaned up ${downloadsToCleanup.size} downloads from storage")
    }
    
    override suspend fun getStorageThreshold(): Long {
        // Return available storage space threshold (e.g., 500MB)
        // This would typically come from settings/preferences
        return 500L * 1024L * 1024L // 500MB
    }
    
}