package com.deadarchive.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for setlist operations.
 */
@Dao
interface SetlistDao {
    
    // Basic CRUD operations
    
    @Query("SELECT * FROM setlists WHERE showId = :showId")
    suspend fun getSetlist(showId: String): SetlistEntity?
    
    @Query("SELECT * FROM setlists WHERE showId IN (:showIds)")
    suspend fun getSetlists(showIds: List<String>): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE date = :date ORDER BY showId")
    suspend fun getSetlistsByDate(date: String): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE date BETWEEN :startDate AND :endDate ORDER BY date, showId")
    suspend fun getSetlistsByDateRange(startDate: String, endDate: String): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE venueId = :venueId ORDER BY date DESC")
    suspend fun getSetlistsByVenue(venueId: String): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE source = :source ORDER BY date DESC")
    suspend fun getSetlistsBySource(source: String): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE hasSongs = 1 ORDER BY date DESC")
    suspend fun getSetlistsWithSongs(): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE totalSongs >= :minSongs ORDER BY totalSongs DESC, date DESC")
    suspend fun getSetlistsWithMinSongs(minSongs: Int): List<SetlistEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: SetlistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlists(setlists: List<SetlistEntity>)
    
    @Query("DELETE FROM setlists WHERE showId = :showId")
    suspend fun deleteSetlist(showId: String)
    
    @Query("DELETE FROM setlists WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldSetlists(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM setlists")
    suspend fun getSetlistCount(): Int
    
    @Query("SELECT COUNT(*) FROM setlists WHERE hasSongs = 1")
    suspend fun getSetlistsWithSongsCount(): Int
    
    // Search operations
    
    @Query("""
        SELECT * FROM setlists 
        WHERE date LIKE '%' || :query || '%'
           OR venueLine LIKE '%' || :query || '%' 
           OR cmuVenueLine LIKE '%' || :query || '%'
           OR rawContent LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    suspend fun searchSetlists(query: String): List<SetlistEntity>
    
    @Query("""
        SELECT * FROM setlists 
        WHERE date LIKE :year || '%'
        ORDER BY date DESC
    """)
    suspend fun getSetlistsByYear(year: String): List<SetlistEntity>
    
    @Query("""
        SELECT * FROM setlists 
        WHERE SUBSTR(date, 6, 5) = :monthDay 
        ORDER BY date DESC
    """)
    suspend fun getSetlistsByMonthDay(monthDay: String): List<SetlistEntity>
    
    // Quality-based queries
    
    @Query("SELECT * FROM setlists WHERE source = 'gdsets' ORDER BY date DESC")
    suspend fun getGDSetsSetlists(): List<SetlistEntity>
    
    @Query("SELECT * FROM setlists WHERE source = 'cmu' ORDER BY date DESC")
    suspend fun getCMUSetlists(): List<SetlistEntity>
    
    @Query("""
        SELECT * FROM setlists 
        WHERE hasSongs = 1 
        ORDER BY date DESC
    """)
    suspend fun getBestQualitySetlists(): List<SetlistEntity>
    
    // Reactive queries with Flow
    
    @Query("SELECT * FROM setlists WHERE hasSongs = 1 ORDER BY date DESC LIMIT :limit")
    fun getSetlistsWithSongsFlow(limit: Int = 100): Flow<List<SetlistEntity>>
    
    @Query("SELECT * FROM setlists WHERE date = :date ORDER BY showId")
    fun getSetlistsByDateFlow(date: String): Flow<List<SetlistEntity>>
    
    @Query("SELECT * FROM setlists WHERE venueId = :venueId ORDER BY date DESC")
    fun getSetlistsByVenueFlow(venueId: String): Flow<List<SetlistEntity>>
    
    // Bulk operations
    
    @Transaction
    suspend fun replaceAllSetlists(setlists: List<SetlistEntity>) {
        clearSetlists()
        insertSetlists(setlists)
    }
    
    @Query("DELETE FROM setlists")
    suspend fun clearSetlists()
    
    // Statistics queries
    
    @Query("SELECT COUNT(*) FROM setlists WHERE source = :source")
    suspend fun getSetlistCountBySource(source: String): Int
    
    @Query("SELECT AVG(totalSongs) FROM setlists WHERE hasSongs = 1")
    suspend fun getAverageSongCount(): Float?
    
    @Query("""
        SELECT source, COUNT(*) as count, AVG(totalSongs) as avgSongs 
        FROM setlists 
        WHERE hasSongs = 1
        GROUP BY source
    """)
    suspend fun getSetlistStatsBySource(): List<SetlistSourceStats>
    
    @Query("""
        SELECT date, COUNT(*) as showCount, AVG(totalSongs) as avgSongs
        FROM setlists 
        WHERE hasSongs = 1 AND date LIKE :year || '%'
        GROUP BY date
        ORDER BY date
    """)
    suspend fun getSetlistStatsByYear(year: String): List<SetlistDateStats>
}

/**
 * Data class for setlist statistics by source.
 */
data class SetlistSourceStats(
    val source: String,
    val count: Int,
    val avgSongs: Float
)

/**
 * Data class for setlist statistics by date.
 */
data class SetlistDateStats(
    val date: String,
    val showCount: Int,
    val avgSongs: Float
)