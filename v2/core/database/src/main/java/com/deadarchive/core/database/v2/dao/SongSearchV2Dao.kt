package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.SongSearchV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongSearchV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongSearchV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongSearchV2Entity)
    
    @Query("DELETE FROM song_search_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM song_search_v2 WHERE songName LIKE '%' || :query || '%' OR songKey LIKE '%' || :query || '%' ORDER BY rating DESC, date DESC")
    suspend fun searchSongs(query: String): List<SongSearchV2Entity>
    
    @Query("SELECT * FROM song_search_v2 WHERE songKey = :songKey ORDER BY rating DESC, date DESC")
    suspend fun getSongPerformances(songKey: String): List<SongSearchV2Entity>
    
    @Query("SELECT * FROM song_search_v2 WHERE showId = :showId ORDER BY setName, position")
    suspend fun getSongsForShow(showId: String): List<SongSearchV2Entity>
    
    @Query("SELECT * FROM song_search_v2 GROUP BY songKey ORDER BY songName")
    suspend fun getAllUniqueSongs(): List<SongSearchV2Entity>
    
    @Query("SELECT * FROM song_search_v2 WHERE date BETWEEN :startDate AND :endDate ORDER BY rating DESC")
    suspend fun getSongsInDateRange(startDate: String, endDate: String): List<SongSearchV2Entity>
    
    @Query("SELECT * FROM song_search_v2 WHERE venue LIKE '%' || :venue || '%' ORDER BY rating DESC, date DESC")
    suspend fun getSongsByVenue(venue: String): List<SongSearchV2Entity>
    
    @Query("SELECT COUNT(*) FROM song_search_v2")
    suspend fun getCount(): Int
}