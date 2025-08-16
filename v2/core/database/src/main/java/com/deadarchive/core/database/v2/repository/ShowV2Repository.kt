package com.deadarchive.v2.core.database.repository

import com.deadarchive.v2.core.database.dao.ShowV2Dao
import com.deadarchive.v2.core.database.dao.VenueV2Dao
import com.deadarchive.v2.core.database.dao.DataVersionDao
import com.deadarchive.v2.core.database.entities.ShowV2Entity
import com.deadarchive.v2.core.database.entities.VenueV2Entity
import com.deadarchive.v2.core.database.entities.DataVersionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for V2 show data access
 * Simple interface for verification queries only
 */
@Singleton
class ShowV2Repository @Inject constructor(
    private val showDao: ShowV2Dao,
    private val venueDao: VenueV2Dao,
    private val dataVersionDao: DataVersionDao
) {
    
    // Show queries
    suspend fun getAllShows(): List<ShowV2Entity> = showDao.getAllShows()
    
    suspend fun getShowById(showId: String): ShowV2Entity? = showDao.getShowById(showId)
    
    suspend fun getShowsByYear(year: Int): List<ShowV2Entity> = showDao.getShowsByYear(year)
    
    suspend fun getShowCount(): Int = showDao.getShowCount()
    
    // Cornell '77 verification
    suspend fun getCornell77(): List<ShowV2Entity> = showDao.getCornell77()
    
    // Recent shows for verification
    suspend fun getRecentShows(limit: Int = 20): List<ShowV2Entity> = showDao.getRecentShows(limit)
    
    // Venue queries
    suspend fun getAllVenues(): List<VenueV2Entity> = venueDao.getAllVenues()
    
    suspend fun getVenueCount(): Int = venueDao.getVenueCount()
    
    // Data version info
    suspend fun getCurrentDataVersion(): DataVersionEntity? = dataVersionDao.getCurrentDataVersion()
    
    suspend fun getCurrentVersion(): String? = dataVersionDao.getCurrentVersion()
    
    suspend fun hasDataVersion(): Boolean = dataVersionDao.hasDataVersion()
    
    // Flow-based queries for reactive UI (if needed later)
    fun getAllShowsFlow(): Flow<List<ShowV2Entity>> = showDao.getAllShowsFlow()
    
    fun getAllVenuesFlow(): Flow<List<VenueV2Entity>> = venueDao.getAllVenuesFlow()
}