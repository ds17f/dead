package com.deadly.core.model

/**
 * Domain model representing an available app update.
 * Contains all information needed to present and install an update.
 */
data class AppUpdate(
    val version: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
    val assetName: String
) {
    /**
     * Check if this update is newer than the current version.
     * Uses semantic version comparison (e.g., "1.2.3" vs "1.2.2").
     */
    fun isNewerThan(currentVersion: String): Boolean {
        return compareVersions(version, currentVersion) > 0
    }
    
    /**
     * Get formatted size if available from asset name or other metadata.
     */
    val displayName: String
        get() = "Dead Archive v$version"
    
    /**
     * Get short release notes for display in dialogs (first 200 characters).
     */
    val shortReleaseNotes: String
        get() = if (releaseNotes.length > 200) {
            releaseNotes.take(197) + "..."
        } else {
            releaseNotes
        }
    
    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLength) {
            val part1 = parts1.getOrElse(i) { 0 }
            val part2 = parts2.getOrElse(i) { 0 }
            
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }
        
        return 0
    }
}

/**
 * Represents the current update status of the app.
 */
data class UpdateStatus(
    val isUpdateAvailable: Boolean,
    val update: AppUpdate?,
    val isSkipped: Boolean = false,
    val lastChecked: Long = 0L,
    val error: String? = null
) {
    /**
     * True if an update is available and hasn't been skipped by the user.
     */
    val shouldShowUpdate: Boolean
        get() = isUpdateAvailable && !isSkipped && update != null
    
    /**
     * True if we successfully checked for updates (regardless of result).
     */
    val isSuccessfulCheck: Boolean
        get() = error == null
    
    companion object {
        /**
         * Create UpdateStatus for when no update is available.
         */
        fun noUpdateAvailable(lastChecked: Long = System.currentTimeMillis()): UpdateStatus {
            return UpdateStatus(
                isUpdateAvailable = false,
                update = null,
                isSkipped = false,
                lastChecked = lastChecked
            )
        }
        
        /**
         * Create UpdateStatus for when an update check failed.
         */
        fun checkFailed(error: String, lastChecked: Long = System.currentTimeMillis()): UpdateStatus {
            return UpdateStatus(
                isUpdateAvailable = false,
                update = null,
                isSkipped = false,
                lastChecked = lastChecked,
                error = error
            )
        }
        
        /**
         * Create UpdateStatus for when an update is available.
         */
        fun updateAvailable(
            update: AppUpdate, 
            isSkipped: Boolean = false,
            lastChecked: Long = System.currentTimeMillis()
        ): UpdateStatus {
            return UpdateStatus(
                isUpdateAvailable = true,
                update = update,
                isSkipped = isSkipped,
                lastChecked = lastChecked
            )
        }
    }
}

/**
 * Represents the download state of an update.
 */
data class UpdateDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedFile: String? = null,
    val error: String? = null
) {
    /**
     * True if download completed successfully.
     */
    val isDownloadComplete: Boolean
        get() = !isDownloading && downloadedFile != null && error == null
    
    /**
     * True if download failed.
     */
    val isDownloadFailed: Boolean
        get() = !isDownloading && error != null
    
    /**
     * Formatted progress percentage for display.
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()
    
    /**
     * Formatted download progress text.
     */
    val progressText: String
        get() = when {
            isDownloadComplete -> "Download complete"
            isDownloadFailed -> "Download failed"
            isDownloading -> "${progressPercentage}%"
            else -> "Ready to download"
        }
}