package com.deadly.core.backup.model

import kotlinx.serialization.Serializable

/**
 * Cross-app migration data format.
 * Used to export library/history from the old app for import into the new monorepo app.
 * Shows are matched by date + venue since showIds differ between apps.
 */
@Serializable
data class MigrationData(
    val version: Int = 1,
    val format: String = "deadly-migration",
    val createdAt: Long,
    val appVersion: String,
    val library: List<MigrationLibraryShow>,
    val recentPlays: List<MigrationRecentShow>,
    val lastPlayed: MigrationLastPlayed? = null
)

@Serializable
data class MigrationLibraryShow(
    val date: String,
    val venue: String? = null,
    val location: String? = null,
    val addedAt: Long,
    val preferredRecordingId: String? = null
)

@Serializable
data class MigrationRecentShow(
    val date: String,
    val venue: String? = null,
    val location: String? = null,
    val lastPlayedAt: Long,
    val firstPlayedAt: Long,
    val playCount: Int
)

@Serializable
data class MigrationLastPlayed(
    val showDate: String,
    val showVenue: String? = null,
    val recordingId: String,
    val trackIndex: Int,
    val positionMs: Long,
    val trackTitle: String,
    val trackFilename: String
)
