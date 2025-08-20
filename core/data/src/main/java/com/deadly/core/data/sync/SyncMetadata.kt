package com.deadly.core.data.sync

/**
 * Metadata about sync operations to track sync state and history
 */
data class SyncMetadata(
    val id: Int = 1, // Always use ID 1 as we only need one record
    val lastFullSync: Long?, // Timestamp of last complete catalog download
    val lastDeltaSync: Long?, // Timestamp of last incremental sync  
    val totalConcerts: Int, // Total number of concerts in local database
    val syncVersion: Int // Version number for future schema changes
)