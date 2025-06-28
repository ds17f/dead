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
    
    @Query("SELECT * FROM concerts WHERE title LIKE '%' || :query || '%' OR venue LIKE '%' || :query || '%' OR date LIKE '%' || :query || '%' ORDER BY date DESC")
    suspend fun searchConcerts(query: String): List<ConcertEntity>
    
    @Query("SELECT * FROM concerts ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentConcerts(limit: Int = 50): List<ConcertEntity>
    
    @Query("DELETE FROM concerts WHERE id NOT IN (SELECT id FROM concerts WHERE isFavorite = 1) AND cachedTimestamp < :cutoffTimestamp")
    suspend fun cleanupOldCachedConcerts(cutoffTimestamp: Long)
    
    @Query("SELECT COUNT(*) FROM concerts WHERE id = :id")
    suspend fun concertExists(id: String): Int
}