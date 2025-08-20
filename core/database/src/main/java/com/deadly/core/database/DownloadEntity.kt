package com.deadly.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadly.core.model.DownloadState
import com.deadly.core.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val recordingId: String,
    val trackFilename: String,
    val status: String,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val startedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null,
    val priority: Int = 0,
    val retryCount: Int = 0,
    
    // Soft delete fields
    val isMarkedForDeletion: Boolean = false,
    val deletionTimestamp: Long? = null,
    val lastAccessTimestamp: Long = System.currentTimeMillis()
) {
    fun toDownloadState(): DownloadState {
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
            retryCount = retryCount,
            isMarkedForDeletion = isMarkedForDeletion,
            deletionTimestamp = deletionTimestamp,
            lastAccessTimestamp = lastAccessTimestamp
        )
    }
    
    companion object {
        fun fromDownloadState(downloadState: DownloadState): DownloadEntity {
            return DownloadEntity(
                id = downloadState.id,
                recordingId = downloadState.recordingId,
                trackFilename = downloadState.trackFilename,
                status = downloadState.status.name,
                progress = downloadState.progress,
                bytesDownloaded = downloadState.bytesDownloaded,
                totalBytes = downloadState.totalBytes,
                localPath = downloadState.localPath,
                errorMessage = downloadState.errorMessage,
                startedTimestamp = downloadState.startedTimestamp,
                completedTimestamp = downloadState.completedTimestamp,
                priority = downloadState.priority,
                retryCount = downloadState.retryCount,
                isMarkedForDeletion = downloadState.isMarkedForDeletion,
                deletionTimestamp = downloadState.deletionTimestamp,
                lastAccessTimestamp = downloadState.lastAccessTimestamp
            )
        }
    }
}