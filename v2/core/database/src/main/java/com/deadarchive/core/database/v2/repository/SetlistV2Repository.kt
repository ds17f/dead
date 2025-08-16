package com.deadarchive.v2.core.database.repository

import com.deadarchive.v2.core.database.dao.SetlistSongV2Dao
import com.deadarchive.v2.core.database.dao.SetlistV2Dao
import com.deadarchive.v2.core.database.dao.SongV2Dao
import com.deadarchive.v2.core.database.entities.SongV2Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetlistV2Repository @Inject constructor(
    private val songDao: SongV2Dao,
    private val setlistDao: SetlistV2Dao,
    private val setlistSongDao: SetlistSongV2Dao
) {
    
    /**
     * Get complete setlist for a show with all song details
     */
    suspend fun getCompleteSetlistForShow(showId: String) = withContext(Dispatchers.IO) {
        setlistSongDao.getCompleteSetlistForShow(showId)
    }
    
    /**
     * Find all performances of a specific song
     */
    suspend fun getPerformancesForSong(songKey: String) = withContext(Dispatchers.IO) {
        setlistSongDao.getPerformancesForSongKey(songKey)
    }
    
    /**
     * Get all songs in the catalog
     */
    suspend fun getAllSongs(): List<SongV2Entity> = withContext(Dispatchers.IO) {
        songDao.getAllSongs()
    }
    
    /**
     * Search songs by name
     */
    suspend fun searchSongs(query: String): List<SongV2Entity> = withContext(Dispatchers.IO) {
        songDao.searchSongs(query)
    }
    
    /**
     * Get song by exact name
     */
    suspend fun getSongByName(songName: String): SongV2Entity? = withContext(Dispatchers.IO) {
        songDao.getSongByName(songName)
    }
    
    /**
     * Get segue chains (songs that segue into next)
     */
    suspend fun getSegueChains() = withContext(Dispatchers.IO) {
        setlistSongDao.getSegueChains()
    }
    
    /**
     * Get basic statistics
     */
    suspend fun getSetlistStatistics() = withContext(Dispatchers.IO) {
        val songCount = songDao.getSongCount()
        val setlistCount = setlistDao.getSetlistCount()
        val performanceCount = setlistSongDao.getSetlistSongCount()
        
        Triple(songCount, setlistCount, performanceCount)
    }
}