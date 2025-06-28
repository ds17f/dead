package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val concertIdentifier: String,
    val trackFilename: String,
    val status: String,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val startedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null
) {
    fun toDownloadState(): DownloadState {
        return DownloadState(
            concertIdentifier = concertIdentifier,
            trackFilename = trackFilename,
            status = DownloadStatus.valueOf(status),
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            localPath = localPath,
            errorMessage = errorMessage,
            startedTimestamp = startedTimestamp,
            completedTimestamp = completedTimestamp
        )
    }
    
    companion object {
        fun fromDownloadState(downloadState: DownloadState): DownloadEntity {
            return DownloadEntity(
                id = downloadState.id,
                concertIdentifier = downloadState.concertIdentifier,
                trackFilename = downloadState.trackFilename,
                status = downloadState.status.name,
                progress = downloadState.progress,
                bytesDownloaded = downloadState.bytesDownloaded,
                totalBytes = downloadState.totalBytes,
                localPath = downloadState.localPath,
                errorMessage = downloadState.errorMessage,
                startedTimestamp = downloadState.startedTimestamp,
                completedTimestamp = downloadState.completedTimestamp
            )
        }
    }
}