package com.deadarchive.core.data.mapper

import com.deadarchive.core.database.RecordingEntity
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.*
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse

/**
 * Data mapping extensions for converting between domain models and entities
 */
object DataMappers {
    
    // Recording conversion methods (updated for new Recording model)
    fun ArchiveSearchResponse.ArchiveDoc.toRecording(): Recording {
        return Recording(
            identifier = identifier,
            title = title,
            source = source,
            taper = taper,
            transferer = transferer,
            lineage = lineage,
            description = description,
            uploader = uploader,
            addedDate = addedDate,
            publicDate = publicDate,
            concertDate = date ?: "",
            concertVenue = venue,
            concertLocation = coverage,
            isInLibrary = false,
            isDownloaded = false
        )
    }
    
    // Download state mapping
    fun DownloadEntity.toDownloadState(): DownloadState {
        return DownloadState(
            recordingId = recordingId,
            trackFilename = trackFilename,
            status = DownloadStatus.valueOf(status),
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            localPath = localPath,
            errorMessage = errorMessage,
            startedTimestamp = startedTimestamp,
            completedTimestamp = completedTimestamp,
            priority = priority,
            retryCount = retryCount
        )
    }
    
    fun List<DownloadEntity>.toDownloadStates(): List<DownloadState> {
        return map { it.toDownloadState() }
    }
    
    fun createDownloadState(
        recordingId: String,
        trackFilename: String,
        url: String,
        status: DownloadStatus = DownloadStatus.QUEUED
    ): DownloadEntity {
        val downloadId = "${recordingId}_$trackFilename"
        return DownloadEntity(
            id = downloadId,
            recordingId = recordingId,
            trackFilename = trackFilename,
            status = status.name,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            localPath = null,
            errorMessage = null,
            startedTimestamp = System.currentTimeMillis(),
            completedTimestamp = null,
            priority = 0,
            retryCount = 0
        )
    }
    
    fun DownloadState.toDownloadEntity(): DownloadEntity {
        return DownloadEntity(
            id = id,
            recordingId = recordingId,
            trackFilename = trackFilename,
            status = status.name,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            localPath = localPath,
            errorMessage = errorMessage,
            startedTimestamp = startedTimestamp,
            completedTimestamp = completedTimestamp,
            priority = priority,
            retryCount = retryCount
        )
    }
    
}