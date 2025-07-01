package com.deadarchive.core.database

import androidx.room.*
import com.deadarchive.core.model.ConcertNew
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Dao
abstract class ConcertWithRecordingsDao {
    
    @get:Ignore
    internal abstract val concertDao: ConcertNewDao
    
    @get:Ignore
    internal abstract val recordingDao: RecordingDao
    
    // Get full concert with all recordings
    suspend fun getConcertWithRecordings(concertId: String): ConcertNew? {
        val concertEntity = concertDao.getConcertById(concertId) ?: return null
        val recordingEntities = recordingDao.getRecordingsByConcertId(concertId)
        val recordings = recordingEntities.map { it.toRecording() }
        return concertEntity.toConcertNew(recordings)
    }
    
    // Get all concerts with their recordings
    suspend fun getAllConcertsWithRecordings(): List<ConcertNew> {
        val concertEntities = concertDao.getAllConcerts()
        return concertEntities.map { concertEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(concertEntity.concertId)
            val recordings = recordingEntities.map { it.toRecording() }
            concertEntity.toConcertNew(recordings)
        }
    }
    
    // Flow version for UI
    fun getAllConcertsWithRecordingsFlow(): Flow<List<ConcertNew>> {
        return concertDao.getAllConcertsFlow().combine(
            // This is a simplified approach - in practice you might want to optimize this
            concertDao.getAllConcertsFlow()
        ) { concerts, _ ->
            concerts.map { concertEntity ->
                val recordingEntities = recordingDao.getRecordingsByConcertId(concertEntity.concertId)
                val recordings = recordingEntities.map { it.toRecording() }
                concertEntity.toConcertNew(recordings)
            }
        }
    }
    
    // Search concerts with recordings
    suspend fun searchConcertsWithRecordings(query: String): List<ConcertNew> {
        val concertEntities = concertDao.searchConcerts(query)
        return concertEntities.map { concertEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(concertEntity.concertId)
            val recordings = recordingEntities.map { it.toRecording() }
            concertEntity.toConcertNew(recordings)
        }
    }
    
    // Get concerts by date with recordings
    suspend fun getConcertsByDateWithRecordings(date: String): List<ConcertNew> {
        val concertEntities = concertDao.getConcertsByExactDate(date)
        return concertEntities.map { concertEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(concertEntity.concertId)
            val recordings = recordingEntities.map { it.toRecording() }
            concertEntity.toConcertNew(recordings)
        }
    }
    
    // Get favorite concerts with recordings
    suspend fun getFavoriteConcertsWithRecordings(): List<ConcertNew> {
        val concertEntities = concertDao.getFavoriteConcerts()
        return concertEntities.map { concertEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(concertEntity.concertId)
            val recordings = recordingEntities.map { it.toRecording() }
            concertEntity.toConcertNew(recordings)
        }
    }
    
    // Insert concert with recordings (transaction)
    @Transaction
    open suspend fun insertConcertWithRecordings(concert: ConcertNew) {
        // Insert the concert first
        val concertEntity = ConcertNewEntity.fromConcertNew(concert)
        concertDao.insertConcert(concertEntity)
        
        // Then insert all recordings
        val recordingEntities = concert.recordings.map { recording ->
            RecordingEntity.fromRecording(recording, concert.concertId)
        }
        recordingDao.insertRecordings(recordingEntities)
    }
    
    // Insert multiple concerts with recordings (transaction)
    @Transaction
    open suspend fun insertConcertsWithRecordings(concerts: List<ConcertNew>) {
        concerts.forEach { concert ->
            insertConcertWithRecordings(concert)
        }
    }
    
    // Delete concert with all recordings (transaction)
    @Transaction
    open suspend fun deleteConcertWithRecordings(concertId: String) {
        // Recordings will be deleted automatically due to foreign key cascade
        concertDao.deleteConcertById(concertId)
    }
    
    // Statistics
    suspend fun getConcertStatistics(): ConcertStatistics {
        val concertCount = concertDao.getConcertCount()
        val recordingCount = recordingDao.getRecordingCount()
        val uniqueSourceCount = recordingDao.getUniqueSourceCount()
        val uniqueTaperCount = recordingDao.getUniqueTaperCount()
        
        return ConcertStatistics(
            concertCount = concertCount,
            recordingCount = recordingCount,
            uniqueSourceCount = uniqueSourceCount,
            uniqueTaperCount = uniqueTaperCount
        )
    }
}

data class ConcertStatistics(
    val concertCount: Int,
    val recordingCount: Int,
    val uniqueSourceCount: Int,
    val uniqueTaperCount: Int
)