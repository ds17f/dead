package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = :status")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE concertIdentifier = :concertId")
    fun getDownloadsForConcert(concertId: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED'")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloads(downloads: List<DownloadEntity>)
    
    @Update
    suspend fun updateDownload(download: DownloadEntity)
    
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)
    
    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedDownloads()
    
    @Query("DELETE FROM downloads WHERE status = 'FAILED'")
    suspend fun deleteFailedDownloads()
    
    @Query("UPDATE downloads SET status = 'CANCELLED' WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED')")
    suspend fun cancelAllActiveDownloads()
    
    @Query("UPDATE downloads SET progress = :progress, bytesDownloaded = :bytesDownloaded WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, progress: Float, bytesDownloaded: Long)
    
    @Query("UPDATE downloads SET status = :status, completedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String, timestamp: Long? = null)
}