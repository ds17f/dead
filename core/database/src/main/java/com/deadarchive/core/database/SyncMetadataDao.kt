package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for managing sync metadata
 */
@Dao
interface SyncMetadataDao {
    
    @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
    suspend fun getSyncMetadata(): SyncMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSyncMetadata(metadata: SyncMetadataEntity)
    
    @Query("UPDATE sync_metadata SET lastDeltaSync = :timestamp WHERE id = 1")
    suspend fun updateLastDeltaSync(timestamp: Long)
    
    @Query("UPDATE sync_metadata SET totalConcerts = :count WHERE id = 1")
    suspend fun updateTotalConcerts(count: Int)
    
    @Query("DELETE FROM sync_metadata WHERE id = 1")
    suspend fun clearSyncMetadata()
}