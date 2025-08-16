package com.deadarchive.v2.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.SetlistSongV2Entity

@Dao
interface SetlistSongV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlistSong(setlistSong: SetlistSongV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlistSongs(setlistSongs: List<SetlistSongV2Entity>): List<Long>
    
    @Query("SELECT * FROM setlist_songs_v2 WHERE id = :id")
    suspend fun getSetlistSongById(id: Long): SetlistSongV2Entity?
    
    @Query("SELECT * FROM setlist_songs_v2 WHERE setlist_id = :setlistId ORDER BY position")
    suspend fun getSongsForSetlist(setlistId: Long): List<SetlistSongV2Entity>
    
    @Query("SELECT * FROM setlist_songs_v2 WHERE song_id = :songId ORDER BY setlist_id, position")
    suspend fun getPerformancesForSong(songId: Long): List<SetlistSongV2Entity>
    
    @Query("SELECT COUNT(*) FROM setlist_songs_v2")
    suspend fun getSetlistSongCount(): Int
    
    @Query("SELECT COUNT(*) FROM setlist_songs_v2 WHERE setlist_id = :setlistId")
    suspend fun getSongCountForSetlist(setlistId: Long): Int
    
    @Query("SELECT COUNT(*) FROM setlist_songs_v2 WHERE song_id = :songId")
    suspend fun getPerformanceCountForSong(songId: Long): Int
    
    @Query("DELETE FROM setlist_songs_v2 WHERE setlist_id = :setlistId")
    suspend fun deleteSongsForSetlist(setlistId: Long)
    
    @Query("DELETE FROM setlist_songs_v2")
    suspend fun deleteAllSetlistSongs()
    
    /**
     * Get songs for a setlist with song details joined.
     * Returns a comprehensive view for setlist display.
     */
    @Query("""
        SELECT ss.*, s.song_name, s.song_key, s.song_url
        FROM setlist_songs_v2 ss
        JOIN songs_v2 s ON ss.song_id = s.id
        WHERE ss.setlist_id = :setlistId
        ORDER BY ss.position
    """)
    suspend fun getSongsWithDetailsForSetlist(setlistId: Long): List<SetlistSongWithDetailsV2>
    
    /**
     * Find all performances of a specific song with show context.
     * Useful for "Find all Dark Star performances" queries.
     */
    @Query("""
        SELECT ss.*, s.song_name, s.song_key, s.song_url,
               sl.set_name, sl.set_order,
               sh.showId as show_id, v.name as venue, sh.city, sh.state, sh.date
        FROM setlist_songs_v2 ss
        JOIN songs_v2 s ON ss.song_id = s.id
        JOIN setlists_v2 sl ON ss.setlist_id = sl.id
        JOIN shows_v2 sh ON sl.show_id = sh.showId
        JOIN venues_v2 v ON sh.venueId = v.venueId
        WHERE s.song_key = :songKey
        ORDER BY sh.date, sl.set_order, ss.position
    """)
    suspend fun getPerformancesForSongKey(songKey: String): List<SongPerformanceWithContextV2>
    
    /**
     * Get complete setlist for a show with all song details.
     * Primary query for displaying complete show setlists.
     */
    @Query("""
        SELECT ss.*, s.song_name, s.song_key, s.song_url,
               sl.set_name, sl.set_order
        FROM setlist_songs_v2 ss
        JOIN songs_v2 s ON ss.song_id = s.id
        JOIN setlists_v2 sl ON ss.setlist_id = sl.id
        WHERE sl.show_id = :showId
        ORDER BY sl.set_order, ss.position
    """)
    suspend fun getCompleteSetlistForShow(showId: String): List<SetlistSongWithSetDetailsV2>
    
    /**
     * Find segue chains (songs that segue into next).
     * Critical for Dead show analysis.
     */
    @Query("""
        SELECT ss.*, s.song_name, s.song_key,
               next_s.song_name as next_song_name, next_s.song_key as next_song_key
        FROM setlist_songs_v2 ss
        JOIN songs_v2 s ON ss.song_id = s.id
        LEFT JOIN setlist_songs_v2 next_ss ON ss.setlist_id = next_ss.setlist_id 
                                           AND next_ss.position = ss.position + 1
        LEFT JOIN songs_v2 next_s ON next_ss.song_id = next_s.id
        WHERE ss.segue_into_next = 1
        ORDER BY ss.setlist_id, ss.position
    """)
    suspend fun getSegueChains(): List<SegueChainV2>
}

// Data classes for complex query results
data class SetlistSongWithDetailsV2(
    val id: Long,
    @ColumnInfo(name = "setlist_id") val setlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
    @ColumnInfo(name = "segue_into_next") val segueIntoNext: Boolean,
    @ColumnInfo(name = "song_name") val songName: String,
    @ColumnInfo(name = "song_key") val songKey: String,
    @ColumnInfo(name = "song_url") val songUrl: String?
)

data class SongPerformanceWithContextV2(
    val id: Long,
    @ColumnInfo(name = "setlist_id") val setlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
    @ColumnInfo(name = "segue_into_next") val segueIntoNext: Boolean,
    @ColumnInfo(name = "song_name") val songName: String,
    @ColumnInfo(name = "song_key") val songKey: String,
    @ColumnInfo(name = "song_url") val songUrl: String?,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "set_order") val setOrder: Int,
    @ColumnInfo(name = "show_id") val showId: String,
    val venue: String,
    val city: String?,
    val state: String?,
    val date: String
)

data class SetlistSongWithSetDetailsV2(
    val id: Long,
    @ColumnInfo(name = "setlist_id") val setlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
    @ColumnInfo(name = "segue_into_next") val segueIntoNext: Boolean,
    @ColumnInfo(name = "song_name") val songName: String,
    @ColumnInfo(name = "song_key") val songKey: String,
    @ColumnInfo(name = "song_url") val songUrl: String?,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "set_order") val setOrder: Int
)

data class SegueChainV2(
    val id: Long,
    @ColumnInfo(name = "setlist_id") val setlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
    @ColumnInfo(name = "segue_into_next") val segueIntoNext: Boolean,
    @ColumnInfo(name = "song_name") val songName: String,
    @ColumnInfo(name = "song_key") val songKey: String,
    @ColumnInfo(name = "next_song_name") val nextSongName: String?,
    @ColumnInfo(name = "next_song_key") val nextSongKey: String?
)