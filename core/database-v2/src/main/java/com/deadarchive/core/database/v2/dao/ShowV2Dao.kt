package com.deadarchive.core.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.deadarchive.core.database.v2.entities.ShowV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowV2Dao {
    
    // Core operations for import
    @Insert
    suspend fun insert(show: ShowV2Entity)
    
    @Insert
    suspend fun insertAll(shows: List<ShowV2Entity>)
    
    // Basic queries for verification
    @Query("SELECT * FROM shows_v2 ORDER BY date DESC")
    suspend fun getAllShows(): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 ORDER BY date DESC")
    fun getAllShowsFlow(): Flow<List<ShowV2Entity>>
    
    @Query("SELECT * FROM shows_v2 WHERE showId = :showId")
    suspend fun getShowById(showId: String): ShowV2Entity?
    
    @Query("SELECT COUNT(*) FROM shows_v2")
    suspend fun getShowCount(): Int
    
    // Date-based queries
    @Query("SELECT * FROM shows_v2 WHERE year = :year ORDER BY date")
    suspend fun getShowsByYear(year: Int): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 WHERE yearMonth = :yearMonth ORDER BY date")
    suspend fun getShowsByYearMonth(yearMonth: String): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 WHERE date = :date ORDER BY showSequence")
    suspend fun getShowsByDate(date: String): List<ShowV2Entity>
    
    // Location queries
    @Query("SELECT * FROM shows_v2 WHERE venueId = :venueId ORDER BY date")
    suspend fun getShowsByVenue(venueId: String): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 WHERE city = :city ORDER BY date DESC")
    suspend fun getShowsByCity(city: String): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 WHERE state = :state ORDER BY date DESC")
    suspend fun getShowsByState(state: String): List<ShowV2Entity>
    
    // Search queries
    @Query("""
        SELECT * FROM shows_v2 
        WHERE songList LIKE '%' || :songName || '%' 
        ORDER BY date DESC
    """)
    suspend fun getShowsBySong(songName: String): List<ShowV2Entity>
    
    // Popular/featured queries
    @Query("SELECT * FROM shows_v2 WHERE averageRating IS NOT NULL ORDER BY averageRating DESC LIMIT :limit")
    suspend fun getTopRatedShows(limit: Int = 20): List<ShowV2Entity>
    
    @Query("SELECT * FROM shows_v2 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentShows(limit: Int = 20): List<ShowV2Entity>
    
    // Specific famous shows for verification
    @Query("SELECT * FROM shows_v2 WHERE date = '1977-05-08'")
    suspend fun getCornell77(): List<ShowV2Entity>
    
    // Management operations
    @Query("DELETE FROM shows_v2")
    suspend fun deleteAll()
}