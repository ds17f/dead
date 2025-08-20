package com.deadly.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    
    @Query("SELECT * FROM library_items ORDER BY addedTimestamp DESC")
    fun getAllLibraryItems(): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library_items WHERE type = :type ORDER BY addedTimestamp DESC")
    fun getLibraryItemsByType(type: String): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library_items WHERE showId = :showId ORDER BY addedTimestamp DESC")
    fun getLibraryItemsForShow(showId: String): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getLibraryItemById(id: String): LibraryEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM library_items WHERE id = :id)")
    suspend fun isInLibrary(id: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM library_items WHERE showId = :showId AND type = 'SHOW')")
    suspend fun isShowInLibrary(showId: String): Boolean
    
    @Query("SELECT COUNT(*) FROM library_items")
    suspend fun getLibraryItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM library_items WHERE type = :type")
    suspend fun getLibraryItemCountByType(type: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(libraryItem: LibraryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItems(libraryItems: List<LibraryEntity>)
    
    @Delete
    suspend fun deleteLibraryItem(libraryItem: LibraryEntity)
    
    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteLibraryItemById(id: String)
    
    @Query("DELETE FROM library_items WHERE showId = :showId")
    suspend fun deleteLibraryItemsForShow(showId: String)
    
    @Query("DELETE FROM library_items WHERE type = :type")
    suspend fun deleteLibraryItemsByType(type: String)
    
    @Query("UPDATE library_items SET notes = :notes WHERE id = :id")
    suspend fun updateLibraryItemNotes(id: String, notes: String?)
    
    @Query("DELETE FROM library_items WHERE id IN (:libraryItemIds)")
    suspend fun deleteLibraryItemsByIds(libraryItemIds: List<String>)
    
    @Query("SELECT * FROM library_items ORDER BY addedTimestamp DESC")
    suspend fun getAllLibraryItemsSync(): List<LibraryEntity>
    
    @Query("SELECT * FROM library_items WHERE type = 'SHOW' ORDER BY addedTimestamp DESC")
    suspend fun getAllLibraryEntries(): List<LibraryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToLibrary(libraryEntry: LibraryEntity)
    
    @Query("DELETE FROM library_items")
    suspend fun clearLibrary()
}