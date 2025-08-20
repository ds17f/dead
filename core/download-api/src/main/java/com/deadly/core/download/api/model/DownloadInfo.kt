package com.deadly.core.download.api.model

import com.deadly.core.design.component.ShowDownloadState

/**
 * Information about downloads for a specific show
 */
data class DownloadInfo(
    val hasDownloads: Boolean,
    val downloadInfo: String,
    val downloadState: ShowDownloadState
)

/**
 * Progress information for an ongoing download
 */
data class DownloadProgress(
    val downloadId: String,
    val progress: Float, // 0.0 to 1.0
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isCompleted: Boolean,
    val isFailed: Boolean,
    val isPaused: Boolean
)

/**
 * Enriched download information with metadata
 */
data class EnrichedDownload(
    val downloadId: String,
    val showTitle: String,
    val trackTitle: String,
    val progress: DownloadProgress,
    val showId: String,
    val recordingId: String
)