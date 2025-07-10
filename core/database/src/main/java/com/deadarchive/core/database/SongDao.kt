package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for song operations.
 */
@Dao
interface SongDao {
    
    // Basic CRUD operations
    
    @Query("SELECT * FROM songs WHERE songId = :songId")
    suspend fun getSong(songId: String): SongEntity?
    
    @Query("SELECT * FROM songs WHERE songId IN (:songIds)")
    suspend fun getSongs(songIds: List<String>): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE name = :name")
    suspend fun getSongByName(name: String): SongEntity?
    
    @Query("SELECT * FROM songs WHERE canonicalName = :canonicalName")
    suspend fun getSongByCanonicalName(canonicalName: String): SongEntity?
    
    @Query("SELECT * FROM songs ORDER BY name")
    suspend fun getAllSongs(): List<SongEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)
    
    @Query("DELETE FROM songs WHERE songId = :songId")
    suspend fun deleteSong(songId: String)
    
    @Query("DELETE FROM songs WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldSongs(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
    
    // Search operations
    
    @Query("""
        SELECT * FROM songs 
        WHERE name LIKE '%' || :query || '%' 
           OR canonicalName LIKE '%' || :query || '%'
           OR aliasesJson LIKE '%' || :query || '%'
           OR variantsJson LIKE '%' || :query || '%'
           OR originalArtist LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN name = :query THEN 1
                 WHEN canonicalName = :query THEN 2  
                 WHEN name LIKE :query || '%' THEN 3
                 WHEN canonicalName LIKE :query || '%' THEN 4
                 ELSE 5 
            END, name
    """)
    suspend fun searchSongs(query: String): List<SongEntity>
    
    @Query("""
        SELECT * FROM songs 
        WHERE name LIKE :query || '%' 
           OR canonicalName LIKE :query || '%'
        ORDER BY name
        LIMIT :limit
    """)
    suspend fun searchSongsStartingWith(query: String, limit: Int = 20): List<SongEntity>
    
    // Category-based queries
    
    @Query("SELECT * FROM songs WHERE isOriginal = 1 ORDER BY name")
    suspend fun getOriginalSongs(): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE isCover = 1 ORDER BY name")
    suspend fun getCoverSongs(): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE category = :category ORDER BY name")
    suspend fun getSongsByCategory(category: String): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE originalArtist = :artist ORDER BY name")
    suspend fun getSongsByOriginalArtist(artist: String): List<SongEntity>
    
    // Performance-based queries
    
    @Query("SELECT * FROM songs WHERE timesPlayed >= :minTimes ORDER BY timesPlayed DESC, name")
    suspend fun getSongsPlayedAtLeast(minTimes: Int): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE timesPlayed IS NOT NULL ORDER BY timesPlayed DESC LIMIT :limit")
    suspend fun getMostPlayedSongs(limit: Int = 50): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE timesPlayed IS NOT NULL ORDER BY timesPlayed ASC LIMIT :limit")
    suspend fun getLeastPlayedSongs(limit: Int = 50): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE timesPlayed = 1 ORDER BY name")
    suspend fun getSongsPlayedOnce(): List<SongEntity>
    
    // Date-based queries
    
    @Query("SELECT * FROM songs WHERE firstPerformed = :date ORDER BY name")
    suspend fun getSongsFirstPerformedOn(date: String): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE lastPerformed = :date ORDER BY name")
    suspend fun getSongsLastPerformedOn(date: String): List<SongEntity>
    
    @Query("SELECT * FROM songs WHERE firstPerformed LIKE :year || '%' ORDER BY firstPerformed, name")
    suspend fun getSongsDebutedInYear(year: String): List<SongEntity>
    
    @Query("""
        SELECT * FROM songs 
        WHERE firstPerformed BETWEEN :startDate AND :endDate 
        ORDER BY firstPerformed, name
    """)
    suspend fun getSongsDebutedInPeriod(startDate: String, endDate: String): List<SongEntity>
    
    // Reactive queries with Flow
    
    @Query("SELECT * FROM songs ORDER BY name")
    fun getAllSongsFlow(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE isOriginal = 1 ORDER BY name")
    fun getOriginalSongsFlow(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE isCover = 1 ORDER BY name")
    fun getCoverSongsFlow(): Flow<List<SongEntity>>
    
    @Query("SELECT * FROM songs WHERE timesPlayed IS NOT NULL ORDER BY timesPlayed DESC LIMIT :limit")
    fun getMostPlayedSongsFlow(limit: Int = 50): Flow<List<SongEntity>>
    
    // Bulk operations
    
    @Transaction
    suspend fun replaceAllSongs(songs: List<SongEntity>) {
        clearSongs()
        insertSongs(songs)
    }
    
    @Query("DELETE FROM songs")
    suspend fun clearSongs()
    
    // Statistics queries
    
    @Query("SELECT COUNT(*) FROM songs WHERE isOriginal = 1")
    suspend fun getOriginalSongCount(): Int
    
    @Query("SELECT COUNT(*) FROM songs WHERE isCover = 1")
    suspend fun getCoverSongCount(): Int
    
    @Query("SELECT COUNT(*) FROM songs WHERE category = :category")
    suspend fun getSongCountByCategory(category: String): Int
    
    @Query("SELECT AVG(timesPlayed) FROM songs WHERE timesPlayed IS NOT NULL")
    suspend fun getAverageTimesPlayed(): Float?
    
    @Query("SELECT MAX(timesPlayed) FROM songs")
    suspend fun getMaxTimesPlayed(): Int?
    
    @Query("SELECT MIN(timesPlayed) FROM songs WHERE timesPlayed > 0")
    suspend fun getMinTimesPlayed(): Int?
    
    @Query("""
        SELECT category, COUNT(*) as count, AVG(timesPlayed) as avgPlayed 
        FROM songs 
        WHERE category IS NOT NULL AND timesPlayed IS NOT NULL
        GROUP BY category
        ORDER BY count DESC
    """)
    suspend fun getSongStatsByCategory(): List<SongCategoryStats>
    
    @Query("""
        SELECT originalArtist, COUNT(*) as count
        FROM songs 
        WHERE originalArtist IS NOT NULL
        GROUP BY originalArtist
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getTopOriginalArtists(limit: Int = 20): List<SongArtistStats>
}

/**
 * Data class for song statistics by category.
 */
data class SongCategoryStats(
    val category: String,
    val count: Int,
    val avgPlayed: Float
)

/**
 * Data class for song statistics by original artist.
 */
data class SongArtistStats(
    val originalArtist: String,
    val count: Int
)