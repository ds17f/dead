package com.deadarchive.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM concerts_new WHERE showId = :showId")
    suspend fun getShowById(showId: String): ShowEntity?
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC")
    suspend fun getAllShows(): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC")
    fun getAllShowsFlow(): Flow<List<ShowEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: ShowEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<ShowEntity>)
    
    @Delete
    suspend fun deleteShow(show: ShowEntity)
    
    @Query("DELETE FROM concerts_new WHERE showId = :showId")
    suspend fun deleteShowById(showId: String)
    
    // Date-based searches
    @Query("SELECT * FROM concerts_new WHERE date = :date ORDER BY venue ASC")
    suspend fun getShowsByExactDate(date: String): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new WHERE date LIKE :yearMonth || '%' ORDER BY date ASC, venue ASC")
    suspend fun getShowsByYearMonth(yearMonth: String): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new WHERE year = :year ORDER BY date ASC, venue ASC")
    suspend fun getShowsByYear(year: String): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new WHERE year BETWEEN :startYear AND :endYear ORDER BY date ASC, venue ASC")
    suspend fun getShowsByYearRange(startYear: String, endYear: String): List<ShowEntity>
    
    // Venue and location searches
    @Query("SELECT * FROM concerts_new WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    suspend fun getShowsByVenue(venue: String): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new WHERE location LIKE '%' || :location || '%' ORDER BY date DESC")
    suspend fun getShowsByLocation(location: String): List<ShowEntity>
    
    // General search
    @Query("""
        SELECT * FROM concerts_new 
        WHERE venue LIKE '%' || :query || '%' 
           OR location LIKE '%' || :query || '%'
           OR setlistRaw LIKE '%' || :query || '%'
           OR date LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchShows(query: String): List<ShowEntity>
    
    // Library
    @Query("SELECT * FROM concerts_new WHERE isInLibrary = 1 ORDER BY date DESC")
    suspend fun getLibraryShows(): List<ShowEntity>
    
    @Query("SELECT * FROM concerts_new WHERE isInLibrary = 1 ORDER BY date DESC")
    fun getLibraryShowsFlow(): Flow<List<ShowEntity>>
    
    @Query("UPDATE concerts_new SET isInLibrary = :isInLibrary WHERE showId = :showId")
    suspend fun updateLibraryStatus(showId: String, isInLibrary: Boolean)
    
    // Statistics and utility
    @Query("SELECT COUNT(*) FROM concerts_new")
    suspend fun getShowCount(): Int
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentShows(limit: Int): List<ShowEntity>
    
    @Query("SELECT DISTINCT venue FROM concerts_new WHERE venue IS NOT NULL ORDER BY venue ASC")
    suspend fun getAllVenues(): List<String>
    
    @Query("SELECT DISTINCT location FROM concerts_new WHERE location IS NOT NULL ORDER BY location ASC")
    suspend fun getAllLocations(): List<String>
    
    @Query("SELECT DISTINCT year FROM concerts_new WHERE year IS NOT NULL ORDER BY year DESC")
    suspend fun getAllYears(): List<String>
    
    // Concert with recordings count
    @Query("""
        SELECT c.*, COUNT(r.identifier) as recordingCount
        FROM concerts_new c
        LEFT JOIN recordings r ON c.showId = r.concertId
        GROUP BY c.showId
        ORDER BY c.date DESC
    """)
    suspend fun getShowsWithRecordingCounts(): List<ShowWithRecordingCount>
    
    // Cache management
    @Query("DELETE FROM concerts_new WHERE cachedTimestamp < :cutoffTime")
    suspend fun cleanupOldCachedShows(cutoffTime: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM concerts_new WHERE showId = :showId)")
    suspend fun showExists(showId: String): Boolean
    
    // Date range queries
    @Query("SELECT MIN(date) FROM concerts_new")
    suspend fun getEarliestShowDate(): String?
    
    @Query("SELECT MAX(date) FROM concerts_new")
    suspend fun getLatestShowDate(): String?
}

// Data classes for query results
data class ShowWithRecordingCount(
    @Embedded val show: ShowEntity,
    val recordingCount: Int
)