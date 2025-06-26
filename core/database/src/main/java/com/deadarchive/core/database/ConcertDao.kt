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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcert(concert: ConcertEntity)
    
    @Query("SELECT * FROM concerts WHERE id = :id")
    suspend fun getConcertById(id: String): ConcertEntity?
}