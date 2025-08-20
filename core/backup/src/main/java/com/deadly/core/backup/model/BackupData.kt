package com.deadly.core.backup.model

import com.deadly.core.settings.api.model.AppSettings
import kotlinx.serialization.Serializable

/**
 * Main backup data container that holds user's personal library and settings
 * This only backs up the user's library (which shows they've added) and their preferences,
 * not the entire database catalog.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String,
    val settings: AppSettings,
    val libraryShows: List<BackupLibraryShow>
)

/**
 * Library show entry for backup/restore
 * Only contains the essential data needed to restore a user's library
 */
@Serializable
data class BackupLibraryShow(
    val showId: String,
    val date: String,
    val venue: String?,
    val location: String?,
    val addedAt: Long,
    val preferredRecordingId: String? = null // User's preferred recording for this show
)