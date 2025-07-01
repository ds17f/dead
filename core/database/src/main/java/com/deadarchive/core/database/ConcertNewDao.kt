package com.deadarchive.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcertNewDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM concerts_new WHERE concertId = :concertId")
    suspend fun getConcertById(concertId: String): ConcertNewEntity?
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC")
    suspend fun getAllConcerts(): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC")
    fun getAllConcertsFlow(): Flow<List<ConcertNewEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcert(concert: ConcertNewEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcerts(concerts: List<ConcertNewEntity>)
    
    @Delete
    suspend fun deleteConcert(concert: ConcertNewEntity)
    
    @Query("DELETE FROM concerts_new WHERE concertId = :concertId")
    suspend fun deleteConcertById(concertId: String)
    
    // Date-based searches
    @Query("SELECT * FROM concerts_new WHERE date = :date ORDER BY venue ASC")
    suspend fun getConcertsByExactDate(date: String): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new WHERE date LIKE :yearMonth || '%' ORDER BY date ASC, venue ASC")
    suspend fun getConcertsByYearMonth(yearMonth: String): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new WHERE year = :year ORDER BY date ASC, venue ASC")
    suspend fun getConcertsByYear(year: String): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new WHERE year BETWEEN :startYear AND :endYear ORDER BY date ASC, venue ASC")
    suspend fun getConcertsByYearRange(startYear: String, endYear: String): List<ConcertNewEntity>
    
    // Venue and location searches
    @Query("SELECT * FROM concerts_new WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    suspend fun getConcertsByVenue(venue: String): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new WHERE location LIKE '%' || :location || '%' ORDER BY date DESC")
    suspend fun getConcertsByLocation(location: String): List<ConcertNewEntity>
    
    // General search
    @Query("""
        SELECT * FROM concerts_new 
        WHERE venue LIKE '%' || :query || '%' 
           OR location LIKE '%' || :query || '%'
           OR setlistRaw LIKE '%' || :query || '%'
           OR date LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchConcerts(query: String): List<ConcertNewEntity>
    
    // Favorites
    @Query("SELECT * FROM concerts_new WHERE isFavorite = 1 ORDER BY date DESC")
    suspend fun getFavoriteConcerts(): List<ConcertNewEntity>
    
    @Query("SELECT * FROM concerts_new WHERE isFavorite = 1 ORDER BY date DESC")
    fun getFavoriteConcertsFlow(): Flow<List<ConcertNewEntity>>
    
    @Query("UPDATE concerts_new SET isFavorite = :isFavorite WHERE concertId = :concertId")
    suspend fun updateFavoriteStatus(concertId: String, isFavorite: Boolean)
    
    // Statistics and utility
    @Query("SELECT COUNT(*) FROM concerts_new")
    suspend fun getConcertCount(): Int
    
    @Query("SELECT * FROM concerts_new ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentConcerts(limit: Int): List<ConcertNewEntity>
    
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
        LEFT JOIN recordings r ON c.concertId = r.concertId
        GROUP BY c.concertId
        ORDER BY c.date DESC
    """)
    suspend fun getConcertsWithRecordingCounts(): List<ConcertWithRecordingCount>
    
    // Cache management
    @Query("DELETE FROM concerts_new WHERE cachedTimestamp < :cutoffTime")
    suspend fun cleanupOldCachedConcerts(cutoffTime: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM concerts_new WHERE concertId = :concertId)")
    suspend fun concertExists(concertId: String): Boolean
    
    // Date range queries
    @Query("SELECT MIN(date) FROM concerts_new")
    suspend fun getEarliestConcertDate(): String?
    
    @Query("SELECT MAX(date) FROM concerts_new")
    suspend fun getLatestConcertDate(): String?
}

// Data classes for query results
data class ConcertWithRecordingCount(
    @Embedded val concert: ConcertNewEntity,
    val recordingCount: Int
)