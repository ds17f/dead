package com.deadarchive.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM recordings WHERE identifier = :recordingId")
    suspend fun getRecordingById(recordingId: String): RecordingEntity?
    
    @Query("SELECT * FROM recordings WHERE concertId = :concertId ORDER BY source ASC, identifier ASC")
    suspend fun getRecordingsByConcertId(concertId: String): List<RecordingEntity>
    
    @Query("SELECT * FROM recordings WHERE concertId = :concertId ORDER BY source ASC, identifier ASC")
    fun getRecordingsByConcertIdFlow(concertId: String): Flow<List<RecordingEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordings(recordings: List<RecordingEntity>)
    
    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)
    
    @Query("DELETE FROM recordings WHERE identifier = :recordingId")
    suspend fun deleteRecordingById(recordingId: String)
    
    // Search operations
    @Query("""
        SELECT * FROM recordings 
        WHERE source LIKE '%' || :source || '%' 
        ORDER BY concertDate DESC, identifier ASC
    """)
    suspend fun getRecordingsBySource(source: String): List<RecordingEntity>
    
    @Query("""
        SELECT * FROM recordings 
        WHERE taper LIKE '%' || :taper || '%' 
        ORDER BY concertDate DESC, identifier ASC
    """)
    suspend fun getRecordingsByTaper(taper: String): List<RecordingEntity>
    
    @Query("""
        SELECT * FROM recordings 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR taper LIKE '%' || :query || '%'
           OR uploader LIKE '%' || :query || '%'
        ORDER BY concertDate DESC, identifier ASC
    """)
    suspend fun searchRecordings(query: String): List<RecordingEntity>
    
    // Library
    @Query("SELECT * FROM recordings WHERE isInLibrary = 1 ORDER BY concertDate DESC, identifier ASC")
    fun getLibraryRecordings(): Flow<List<RecordingEntity>>
    
    @Query("SELECT * FROM recordings WHERE isInLibrary = 1 ORDER BY concertDate DESC, identifier ASC")
    fun getLibraryRecordingsFlow(): Flow<List<RecordingEntity>>
    
    @Query("SELECT * FROM recordings ORDER BY concertDate DESC, identifier ASC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>
    
    @Query("UPDATE recordings SET isInLibrary = :isInLibrary WHERE identifier = :recordingId")
    suspend fun updateLibraryStatus(recordingId: String, isInLibrary: Boolean)
    
    @Query("UPDATE recordings SET isDownloaded = :isDownloaded WHERE identifier = :recordingId")
    suspend fun updateDownloadedStatus(recordingId: String, isDownloaded: Boolean)
    
    // Statistics
    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int
    
    @Query("SELECT COUNT(DISTINCT source) FROM recordings WHERE source IS NOT NULL")
    suspend fun getUniqueSourceCount(): Int
    
    @Query("SELECT COUNT(DISTINCT taper) FROM recordings WHERE taper IS NOT NULL")
    suspend fun getUniqueTaperCount(): Int
    
    @Query("SELECT source, COUNT(*) as count FROM recordings WHERE source IS NOT NULL GROUP BY source ORDER BY count DESC")
    suspend fun getSourceCounts(): List<SourceCount>
    
    // Cache management
    @Query("DELETE FROM recordings WHERE cachedTimestamp < :cutoffTime")
    suspend fun cleanupOldCachedRecordings(cutoffTime: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM recordings WHERE identifier = :recordingId)")
    suspend fun recordingExists(recordingId: String): Boolean
}

// Data classes for query results
data class SourceCount(
    val source: String,
    val count: Int
)