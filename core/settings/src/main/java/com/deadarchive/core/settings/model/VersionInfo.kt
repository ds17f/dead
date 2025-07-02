package com.deadarchive.core.settings.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contains version and build information for display in settings
 */
data class VersionInfo(
    val versionName: String,
    val versionCode: Int,
    val buildType: String,
    val buildTime: Long,
    val gitCommitHash: String? = null
) {
    /**
     * Get formatted version string for display
     */
    fun getFormattedVersion(): String {
        return if (buildType == "debug" && !gitCommitHash.isNullOrEmpty()) {
            "$versionName-$buildType ($gitCommitHash)"
        } else {
            versionName
        }
    }

    /**
     * Get formatted build date for display
     */
    fun getFormattedBuildDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return formatter.format(Date(buildTime))
    }

    /**
     * Check if this is a debug build
     */
    fun isDebugBuild(): Boolean = buildType == "debug"
}