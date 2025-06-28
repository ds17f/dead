package com.deadarchive.core.data.repository

import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val concertRepository: ConcertRepository
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadState>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadState() }
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
}