package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DownloadState(
    val concertIdentifier: String,
    val trackFilename: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String? = null,
    val errorMessage: String? = null,
    val startedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null
) {
    val id: String
        get() = "${concertIdentifier}_${trackFilename}"
    
    val progressPercentage: Int
        get() = (progress * 100).toInt()
    
    val isCompleted: Boolean
        get() = status == DownloadStatus.COMPLETED
    
    val isFailed: Boolean
        get() = status == DownloadStatus.FAILED
    
    val isInProgress: Boolean
        get() = status == DownloadStatus.DOWNLOADING
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING, 
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}