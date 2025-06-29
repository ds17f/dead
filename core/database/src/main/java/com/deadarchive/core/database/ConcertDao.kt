package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConcertDao {
    @Query("SELECT * FROM concerts WHERE isFavorite = 1")
    fun getFavoriteConcerts(): Flow<List<ConcertEntity>>
    
    @Query("SELECT * FROM concerts ORDER BY date DESC")
    fun getAllConcerts(): Flow<List<ConcertEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcert(concert: ConcertEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcerts(concerts: List<ConcertEntity>)
    
    @Query("SELECT * FROM concerts WHERE id = :id")
    suspend fun getConcertById(id: String): ConcertEntity?
    
    // ===== PRECISE SEARCH METHODS =====
    
    /**
     * Search by exact date: 1977-05-08
     */
    @Query("SELECT * FROM concerts WHERE date = :date ORDER BY date DESC")
    suspend fun searchConcertsByExactDate(date: String): List<ConcertEntity>
    
    /**
     * Search by year and month: 1977-05
     */
    @Query("SELECT * FROM concerts WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    suspend fun searchConcertsByYearMonth(yearMonth: String): List<ConcertEntity>
    
    /**
     * Search by year: 1977
     */
    @Query("SELECT * FROM concerts WHERE date LIKE :year || '%' ORDER BY date DESC")
    suspend fun searchConcertsByYear(year: String): List<ConcertEntity>
    
    /**
     * Search by year range: 1970-1979
     */
    @Query("SELECT * FROM concerts WHERE CAST(SUBSTR(date, 1, 4) AS INTEGER) BETWEEN :startYear AND :endYear ORDER BY date DESC")
    suspend fun searchConcertsByYearRange(startYear: Int, endYear: Int): List<ConcertEntity>
    
    /**
     * Search by venue name
     */
    @Query("SELECT * FROM concerts WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    suspend fun searchConcertsByVenue(venue: String): List<ConcertEntity>
    
    /**
     * Search by location (city/state)
     */
    @Query("SELECT * FROM concerts WHERE location LIKE '%' || :location || '%' ORDER BY date DESC")
    suspend fun searchConcertsByLocation(location: String): List<ConcertEntity>
    
    /**
     * General text search across multiple fields
     */
    @Query("""
        SELECT * FROM concerts WHERE 
        title LIKE '%' || :query || '%' OR 
        venue LIKE '%' || :query || '%' OR 
        location LIKE '%' || :query || '%' OR
        description LIKE '%' || :query || '%' OR
        source LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchConcertsGeneral(query: String): List<ConcertEntity>
    
    // ===== LEGACY METHODS (kept for backward compatibility) =====
    
    @Query("""
        SELECT * FROM concerts WHERE 
        title LIKE '%' || :query || '%' OR 
        venue LIKE '%' || :query || '%' OR 
        location LIKE '%' || :query || '%' OR
        date LIKE '%' || :query || '%' OR
        year LIKE '%' || :query || '%' OR
        description LIKE '%' || :query || '%' OR
        source LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchConcerts(query: String): List<ConcertEntity>
    
    @Query("SELECT * FROM concerts WHERE date LIKE :datePattern || '%' ORDER BY date DESC")
    suspend fun searchConcertsByDate(datePattern: String): List<ConcertEntity>
    
    @Query("SELECT * FROM concerts ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentConcerts(limit: Int = 50): List<ConcertEntity>
    
    @Query("DELETE FROM concerts WHERE id NOT IN (SELECT id FROM concerts WHERE isFavorite = 1) AND cachedTimestamp < :cutoffTimestamp")
    suspend fun cleanupOldCachedConcerts(cutoffTimestamp: Long)
    
    @Query("SELECT COUNT(*) FROM concerts WHERE id = :id")
    suspend fun concertExists(id: String): Int
}