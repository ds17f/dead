package com.deadarchive.core.data.repository

import com.deadarchive.core.data.mapper.DataMappers.createDownloadState
import com.deadarchive.core.data.mapper.DataMappers.toDownloadEntity
import com.deadarchive.core.data.mapper.DataMappers.toDownloadState
import com.deadarchive.core.data.mapper.DataMappers.toDownloadStates
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
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
     * Get downloads for a specific concert
     */
    fun getDownloadsForConcert(concertId: String): Flow<List<DownloadState>>
    
    /**
     * Get download by ID
     */
    suspend fun getDownloadById(id: String): DownloadState?
    
    /**
     * Start downloading a concert track
     */
    suspend fun startDownload(concert: Concert, trackFilename: String): String
    
    /**
     * Start downloading all tracks for a concert
     */
    suspend fun startConcertDownload(concert: Concert): List<String>
    
    /**
     * Update download progress
     */
    suspend fun updateDownloadProgress(id: String, progress: Float, bytesDownloaded: Long)
    
    /**
     * Update download status
     */
    suspend fun updateDownloadStatus(id: String, status: DownloadStatus, errorMessage: String? = null)
    
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
    suspend fun isTrackDownloaded(concertId: String, trackFilename: String): Boolean
    
    /**
     * Get local file path for a downloaded track
     */
    suspend fun getLocalFilePath(concertId: String, trackFilename: String): String?
    
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
    suspend fun startMultipleDownloads(downloads: List<Pair<Concert, String>>): List<String>
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
    suspend fun getDownloadedTracks(): List<Pair<Concert, String>>
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
    val mostDownloadedConcert: String?
)

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val concertRepository: ConcertRepository
) : DownloadRepository {
    
    // Download directory management
    private val downloadDir by lazy {
        File("/storage/emulated/0/Android/data/com.deadarchive.app/files/Downloads").apply {
            if (!exists()) mkdirs()
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

    override fun getDownloadsForConcert(concertId: String): Flow<List<DownloadState>> {
        return downloadDao.getDownloadsForConcert(concertId).map { entities ->
            entities.map { it.toDownloadState() }
        }
    }

    override suspend fun getDownloadById(id: String): DownloadState? {
        return downloadDao.getDownloadById(id)?.toDownloadState()
    }

    override suspend fun startDownload(concert: Concert, trackFilename: String): String {
        val downloadId = "${concert.identifier}_$trackFilename"
        
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

        // Create new download entry
        val downloadState = DownloadState(
            concertIdentifier = concert.identifier,
            trackFilename = trackFilename,
            status = DownloadStatus.QUEUED
        )

        val downloadEntity = DownloadEntity.fromDownloadState(downloadState)
        downloadDao.insertDownload(downloadEntity)
        
        return downloadId
    }

    override suspend fun startConcertDownload(concert: Concert): List<String> {
        val downloadIds = mutableListOf<String>()
        
        // Get track streaming URLs to determine available tracks
        val trackUrls = concertRepository.getTrackStreamingUrls(concert.identifier)
        
        for ((audioFile, _) in trackUrls) {
            val downloadId = startDownload(concert, audioFile.filename)
            downloadIds.add(downloadId)
        }
        
        return downloadIds
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
        
        return retriedIds
    }

    override suspend fun autoRetryDownloads(maxRetries: Int) {
        val failedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.FAILED.name)
        
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
            }
        }
    }

    // ============ Batch Operations ============

    override suspend fun startMultipleDownloads(downloads: List<Pair<Concert, String>>): List<String> {
        val downloadIds = mutableListOf<String>()
        
        for ((concert, trackFilename) in downloads) {
            val downloadId = startDownload(concert, trackFilename)
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
        val mostDownloadedConcert = downloadDao.getMostDownloadedConcert()

        return DownloadStats(
            totalDownloads = totalDownloads,
            completedDownloads = completedDownloads,
            failedDownloads = failedDownloads,
            totalBytesDownloaded = totalBytesDownloaded,
            averageDownloadSpeed = averageDownloadSpeed,
            mostDownloadedConcert = mostDownloadedConcert
        )
    }

    override suspend fun getDownloadHistory(): List<DownloadState> {
        return downloadDao.getDownloadHistory().toDownloadStates()
    }

    // ============ Export/Import Operations ============

    override suspend fun exportDownloadList(): List<DownloadState> {
        return downloadDao.getAllDownloadsSync().toDownloadStates()
    }

    override suspend fun getDownloadedTracks(): List<Pair<Concert, String>> {
        val completedDownloads = downloadDao.getDownloadsByStatusList(DownloadStatus.COMPLETED.name)
        val downloadedTracks = mutableListOf<Pair<Concert, String>>()
        
        for (download in completedDownloads) {
            val concert = concertRepository.getConcertById(download.concertIdentifier)
            if (concert != null) {
                downloadedTracks.add(Pair(concert, download.trackFilename))
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
}