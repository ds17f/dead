package com.deadarchive.core.data.repository

import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.database.LibraryEntity
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.LibraryItem  
import com.deadarchive.core.model.LibraryItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface LibraryRepository {
    /**
     * Get all library items with real-time updates
     */
    fun getAllLibraryItems(): Flow<List<LibraryItem>>
    
    /**
     * Get library items by type
     */
    fun getLibraryItemsByType(type: LibraryItemType): Flow<List<LibraryItem>>
    
    /**
     * Check if a show is in library
     */
    suspend fun isShowInLibrary(showId: String): Boolean
    
    /**
     * Add show to library
     */
    suspend fun addShowToLibrary(show: Show): Boolean
    
    /**
     * Remove show from library
     */
    suspend fun removeShowFromLibrary(showId: String)
    
    /**
     * Toggle show library status
     */
    suspend fun toggleShowLibrary(show: Show): Boolean
    
    /**
     * Get library item count
     */
    suspend fun getLibraryItemCount(): Int
}

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {
    
    override fun getAllLibraryItems(): Flow<List<LibraryItem>> {
        return libraryDao.getAllLibraryItems().map { entities ->
            entities.map { it.toLibraryItem() }
        }
    }
    
    override fun getLibraryItemsByType(type: LibraryItemType): Flow<List<LibraryItem>> {
        return libraryDao.getLibraryItemsByType(type.name).map { entities ->
            entities.map { it.toLibraryItem() }
        }
    }
    
    override suspend fun isShowInLibrary(showId: String): Boolean {
        return libraryDao.isShowInLibrary(showId)
    }
    
    override suspend fun addShowToLibrary(show: Show): Boolean {
        val libraryItem = LibraryItem.fromShow(show)
        val entity = LibraryEntity.fromLibraryItem(libraryItem)
        libraryDao.insertLibraryItem(entity)
        return true
    }
    
    override suspend fun removeShowFromLibrary(showId: String) {
        val libraryItemId = "show_$showId"
        libraryDao.deleteLibraryItemById(libraryItemId)
    }
    
    override suspend fun toggleShowLibrary(show: Show): Boolean {
        val isCurrentlyInLibrary = isShowInLibrary(show.showId)
        
        return if (isCurrentlyInLibrary) {
            removeShowFromLibrary(show.showId)
            false
        } else {
            addShowToLibrary(show)
            true
        }
    }
    
    override suspend fun getLibraryItemCount(): Int {
        return libraryDao.getLibraryItemCount()
    }
}