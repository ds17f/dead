package com.deadly.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for tracking sync metadata and history
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val id: Int = 1, // Always use ID 1 as we only need one record
    val lastFullSync: Long?, // Timestamp of last complete catalog download
    val lastDeltaSync: Long?, // Timestamp of last incremental sync  
    val totalConcerts: Int, // Total number of concerts in local database
    val syncVersion: Int // Version number for future schema changes
)