package com.deadarchive.core.database

import androidx.room.*
import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Dao
abstract class ShowWithRecordingsDao {
    
    @get:Ignore
    internal abstract val showDao: ShowDao
    
    @get:Ignore
    internal abstract val recordingDao: RecordingDao
    
    // Get full show with all recordings
    suspend fun getShowWithRecordings(showId: String): Show? {
        val showEntity = showDao.getShowById(showId) ?: return null
        val recordingEntities = recordingDao.getRecordingsByConcertId(showId)
        val recordings = recordingEntities.map { it.toRecording() }
        return showEntity.toShow(recordings)
    }
    
    // Get all shows with their recordings
    suspend fun getAllShowsWithRecordings(): List<Show> {
        val showEntities = showDao.getAllShows()
        return showEntities.map { showEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(showEntity.showId)
            val recordings = recordingEntities.map { it.toRecording() }
            showEntity.toShow(recordings)
        }
    }
    
    // Flow version for UI
    fun getAllShowsWithRecordingsFlow(): Flow<List<Show>> {
        return showDao.getAllShowsFlow().combine(
            // This is a simplified approach - in practice you might want to optimize this
            showDao.getAllShowsFlow()
        ) { shows, _ ->
            shows.map { showEntity ->
                val recordingEntities = recordingDao.getRecordingsByConcertId(showEntity.showId)
                val recordings = recordingEntities.map { it.toRecording() }
                showEntity.toShow(recordings)
            }
        }
    }
    
    // Search shows with recordings
    suspend fun searchShowsWithRecordings(query: String): List<Show> {
        val showEntities = showDao.searchShows(query)
        return showEntities.map { showEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(showEntity.showId)
            val recordings = recordingEntities.map { it.toRecording() }
            showEntity.toShow(recordings)
        }
    }
    
    // Get shows by date with recordings
    suspend fun getShowsByDateWithRecordings(date: String): List<Show> {
        val showEntities = showDao.getShowsByExactDate(date)
        return showEntities.map { showEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(showEntity.showId)
            val recordings = recordingEntities.map { it.toRecording() }
            showEntity.toShow(recordings)
        }
    }
    
    // Get favorite shows with recordings
    suspend fun getFavoriteShowsWithRecordings(): List<Show> {
        val showEntities = showDao.getFavoriteShows()
        return showEntities.map { showEntity ->
            val recordingEntities = recordingDao.getRecordingsByConcertId(showEntity.showId)
            val recordings = recordingEntities.map { it.toRecording() }
            showEntity.toShow(recordings)
        }
    }
    
    // Insert show with recordings (transaction)
    @Transaction
    open suspend fun insertShowWithRecordings(show: Show) {
        // Insert the show first
        val showEntity = ShowEntity.fromShow(show)
        showDao.insertShow(showEntity)
        
        // Then insert all recordings
        val recordingEntities = show.recordings.map { recording ->
            RecordingEntity.fromRecording(recording, show.showId)
        }
        recordingDao.insertRecordings(recordingEntities)
    }
    
    // Insert multiple shows with recordings (transaction)
    @Transaction
    open suspend fun insertShowsWithRecordings(shows: List<Show>) {
        shows.forEach { show ->
            insertShowWithRecordings(show)
        }
    }
    
    // Delete show with all recordings (transaction)
    @Transaction
    open suspend fun deleteShowWithRecordings(showId: String) {
        // Recordings will be deleted automatically due to foreign key cascade
        showDao.deleteShowById(showId)
    }
    
    // Statistics
    suspend fun getShowStatistics(): ShowStatistics {
        val showCount = showDao.getShowCount()
        val recordingCount = recordingDao.getRecordingCount()
        val uniqueSourceCount = recordingDao.getUniqueSourceCount()
        val uniqueTaperCount = recordingDao.getUniqueTaperCount()
        
        return ShowStatistics(
            concertCount = showCount,
            recordingCount = recordingCount,
            uniqueSourceCount = uniqueSourceCount,
            uniqueTaperCount = uniqueTaperCount
        )
    }
}

data class ShowStatistics(
    val concertCount: Int,
    val recordingCount: Int,
    val uniqueSourceCount: Int,
    val uniqueTaperCount: Int
)