package com.deadarchive.core.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deadarchive.core.database.v2.entities.SongV2Entity

@Dao
interface SongV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongV2Entity>): List<Long>
    
    @Query("SELECT * FROM songs_v2 WHERE song_key = :songKey LIMIT 1")
    suspend fun getSongByKey(songKey: String): SongV2Entity?
    
    @Query("SELECT * FROM songs_v2 WHERE song_name = :songName LIMIT 1")
    suspend fun getSongByName(songName: String): SongV2Entity?
    
    @Query("SELECT * FROM songs_v2 WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongV2Entity?
    
    @Query("SELECT * FROM songs_v2 ORDER BY song_name")
    suspend fun getAllSongs(): List<SongV2Entity>
    
    @Query("SELECT COUNT(*) FROM songs_v2")
    suspend fun getSongCount(): Int
    
    @Query("SELECT * FROM songs_v2 WHERE song_name LIKE '%' || :searchQuery || '%' ORDER BY song_name")
    suspend fun searchSongs(searchQuery: String): List<SongV2Entity>
    
    /**
     * Get or create a song by name with automatic key generation.
     * This is the primary method for ensuring songs exist during import.
     */
    @Transaction
    suspend fun getOrCreateSong(songName: String, songUrl: String? = null): SongV2Entity {
        val songKey = normalizeSongKey(songName)
        
        // Try to find existing song by key first
        getSongByKey(songKey)?.let { return it }
        
        // Create new song
        val newSong = SongV2Entity(
            songName = songName.trim(),
            songKey = songKey,
            songUrl = songUrl
        )
        
        val insertId = insertSong(newSong)
        return if (insertId > 0) {
            newSong.copy(id = insertId)
        } else {
            // Handle race condition - another thread may have inserted
            getSongByKey(songKey) ?: throw IllegalStateException("Failed to create or retrieve song: $songName")
        }
    }
    
    @Query("DELETE FROM songs_v2")
    suspend fun deleteAllSongs()
}

/**
 * Normalize song name to a consistent key format.
 * Examples:
 * - "Dark Star" -> "dark-star"
 * - "Scarlet Begonias" -> "scarlet-begonias"  
 * - "Saint Stephen" -> "saint-stephen"
 * - "Uncle John's Band" -> "uncle-johns-band"
 */
private fun normalizeSongKey(songName: String): String {
    return songName
        .lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
        .replace(Regex("\\s+"), "-") // Replace spaces with hyphens
        .trim('-') // Remove leading/trailing hyphens
}