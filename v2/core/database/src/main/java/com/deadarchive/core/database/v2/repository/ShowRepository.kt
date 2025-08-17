package com.deadarchive.v2.core.database.repository

import com.deadarchive.v2.core.database.dao.ShowDao
import com.deadarchive.v2.core.database.dao.RecordingDao
import com.deadarchive.v2.core.database.dao.DataVersionDao
import com.deadarchive.v2.core.database.entities.ShowEntity
import com.deadarchive.v2.core.database.entities.RecordingEntity
import com.deadarchive.v2.core.database.entities.DataVersionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified repository for show data access
 */
@Singleton
class ShowRepository @Inject constructor(
    private val showDao: ShowDao,
    private val recordingDao: RecordingDao,
    private val dataVersionDao: DataVersionDao
) {
    
    // Show queries
    suspend fun getAllShows(): List<ShowEntity> = showDao.getAllShows()
    
    fun getAllShowsFlow(): Flow<List<ShowEntity>> = showDao.getAllShowsFlow()
    
    suspend fun getShowById(showId: String): ShowEntity? = showDao.getShowById(showId)
    
    suspend fun getShowsByYear(year: Int): List<ShowEntity> = showDao.getShowsByYear(year)
    
    suspend fun getShowsByYearMonth(yearMonth: String): List<ShowEntity> = showDao.getShowsByYearMonth(yearMonth)
    
    suspend fun getShowsByVenue(venueName: String): List<ShowEntity> = showDao.getShowsByVenue(venueName)
    
    suspend fun getShowsByCity(city: String): List<ShowEntity> = showDao.getShowsByCity(city)
    
    suspend fun getShowsByState(state: String): List<ShowEntity> = showDao.getShowsByState(state)
    
    suspend fun getShowsBySong(songName: String): List<ShowEntity> = showDao.getShowsBySong(songName)
    
    suspend fun getTopRatedShows(limit: Int = 20): List<ShowEntity> = showDao.getTopRatedShows(limit)
    
    suspend fun getRecentShows(limit: Int = 20): List<ShowEntity> = showDao.getRecentShows(limit)
    
    suspend fun getShowCount(): Int = showDao.getShowCount()
    
    // Recording queries
    suspend fun getRecordingsForShow(showId: String): List<RecordingEntity> = recordingDao.getRecordingsForShow(showId)
    
    suspend fun getBestRecordingForShow(showId: String): RecordingEntity? = recordingDao.getBestRecordingForShow(showId)
    
    suspend fun getRecordingById(identifier: String): RecordingEntity? = recordingDao.getRecordingById(identifier)
    
    suspend fun getTopRatedRecordings(minRating: Double = 2.0, minReviews: Int = 5, limit: Int = 50): List<RecordingEntity> = 
        recordingDao.getTopRatedRecordings(minRating, minReviews, limit)
    
    // Data version queries
    suspend fun getDataVersion(): DataVersionEntity? = dataVersionDao.getDataVersion()
}