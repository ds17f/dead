package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    
    @Query("SELECT * FROM favorites ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    
    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY addedTimestamp DESC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>
    
    @Query("SELECT * FROM favorites WHERE concertIdentifier = :concertId ORDER BY addedTimestamp DESC")
    fun getFavoritesForConcert(concertId: String): Flow<List<FavoriteEntity>>
    
    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getFavoriteById(id: String): FavoriteEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE concertIdentifier = :concertId AND type = 'CONCERT')")
    suspend fun isConcertFavorite(concertId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE concertIdentifier = :concertId AND trackFilename = :trackFilename AND type = 'TRACK')")
    suspend fun isTrackFavorite(concertId: String, trackFilename: String): Boolean
    
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
    
    @Query("SELECT COUNT(*) FROM favorites WHERE type = :type")
    suspend fun getFavoriteCountByType(type: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)
    
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)
    
    @Query("DELETE FROM favorites WHERE concertIdentifier = :concertId")
    suspend fun deleteFavoritesForConcert(concertId: String)
    
    @Query("DELETE FROM favorites WHERE type = :type")
    suspend fun deleteFavoritesByType(type: String)
    
    @Query("UPDATE favorites SET notes = :notes WHERE id = :id")
    suspend fun updateFavoriteNotes(id: String, notes: String?)
}