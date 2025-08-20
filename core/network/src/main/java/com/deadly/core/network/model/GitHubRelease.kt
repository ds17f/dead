package com.deadly.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub API response model for release information.
 * Maps to GitHub's /repos/{owner}/{repo}/releases/latest endpoint.
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("body")
    val body: String,
    
    @SerialName("published_at")
    val publishedAt: String,
    
    @SerialName("prerelease")
    val prerelease: Boolean = false,
    
    @SerialName("draft")
    val draft: Boolean = false,
    
    @SerialName("assets")
    val assets: List<GitHubAsset>
)

/**
 * GitHub API response model for release assets (downloadable files).
 */
@Serializable
data class GitHubAsset(
    @SerialName("name")
    val name: String,
    
    @SerialName("size")
    val size: Long,
    
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    
    @SerialName("content_type")
    val contentType: String,
    
    @SerialName("download_count")
    val downloadCount: Int = 0
) {
    /**
     * True if this asset is an Android APK file.
     */
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true) || 
                contentType == "application/vnd.android.package-archive"
    
    /**
     * Formatted file size for display.
     */
    val formattedSize: String
        get() = when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
}