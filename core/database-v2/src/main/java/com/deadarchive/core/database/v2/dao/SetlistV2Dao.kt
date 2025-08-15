package com.deadarchive.core.database.v2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deadarchive.core.database.v2.entities.SetlistV2Entity

@Dao
interface SetlistV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: SetlistV2Entity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlists(setlists: List<SetlistV2Entity>): List<Long>
    
    @Query("SELECT * FROM setlists_v2 WHERE id = :setlistId")
    suspend fun getSetlistById(setlistId: Long): SetlistV2Entity?
    
    @Query("SELECT * FROM setlists_v2 WHERE show_id = :showId ORDER BY set_order")
    suspend fun getSetlistsByShowId(showId: String): List<SetlistV2Entity>
    
    @Query("SELECT * FROM setlists_v2 WHERE show_id = :showId AND set_name = :setName LIMIT 1")
    suspend fun getSetlistByShowAndSetName(showId: String, setName: String): SetlistV2Entity?
    
    @Query("SELECT COUNT(*) FROM setlists_v2")
    suspend fun getSetlistCount(): Int
    
    @Query("SELECT COUNT(*) FROM setlists_v2 WHERE show_id = :showId")
    suspend fun getSetlistCountForShow(showId: String): Int
    
    @Query("DELETE FROM setlists_v2 WHERE show_id = :showId")
    suspend fun deleteSetlistsForShow(showId: String)
    
    @Query("DELETE FROM setlists_v2")
    suspend fun deleteAllSetlists()
    
    /**
     * Get or create a setlist for a show and set name.
     * Used during import to ensure setlists exist before adding songs.
     */
    @Transaction
    suspend fun getOrCreateSetlist(showId: String, setName: String, setOrder: Int): SetlistV2Entity {
        // Try to find existing setlist
        getSetlistByShowAndSetName(showId, setName)?.let { return it }
        
        // Create new setlist
        val newSetlist = SetlistV2Entity(
            showId = showId,
            setName = setName,
            setOrder = setOrder
        )
        
        val insertId = insertSetlist(newSetlist)
        return newSetlist.copy(id = insertId)
    }
}