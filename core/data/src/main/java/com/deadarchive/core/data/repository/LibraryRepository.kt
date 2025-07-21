package com.deadarchive.core.data.repository

import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.database.LibraryEntity
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.RecordingEntity
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
     * Observe if a show is in library with real-time updates
     */
    fun isShowInLibraryFlow(showId: String): Flow<Boolean>
    
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
    
    /**
     * Clear all items from library
     */
    suspend fun clearLibrary()
    
}

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao,
    private val showDao: ShowDao,
    private val recordingDao: RecordingDao
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
        val show = showDao.getShowById(showId)
        return show?.addedToLibraryAt != null
    }
    
    override fun isShowInLibraryFlow(showId: String): Flow<Boolean> {
        return showDao.isShowInLibraryFlow(showId)
    }
    
    override suspend fun addShowToLibrary(show: Show): Boolean {
        return try {
            val timestamp = System.currentTimeMillis()
            showDao.addShowToLibrary(show.showId, timestamp)
            println("DEBUG LibraryRepository: Added show ${show.showId} to library at $timestamp")
            true
        } catch (e: Exception) {
            println("ERROR LibraryRepository: Failed to add show to library: ${e.message}")
            false
        }
    }
    
    
    override suspend fun removeShowFromLibrary(showId: String) {
        try {
            showDao.removeShowFromLibrary(showId)
            println("DEBUG LibraryRepository: Removed show $showId from library")
        } catch (e: Exception) {
            println("ERROR LibraryRepository: Failed to remove show from library: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override suspend fun toggleShowLibrary(show: Show): Boolean {
        return try {
            val currentlyInLibrary = isShowInLibrary(show.showId)
            println("DEBUG LibraryRepository: Toggling library for show ${show.showId}, currently in library: $currentlyInLibrary")
            
            if (currentlyInLibrary) {
                removeShowFromLibrary(show.showId)
                false
            } else {
                addShowToLibrary(show)
                true
            }
        } catch (e: Exception) {
            println("ERROR LibraryRepository: Exception in toggleShowLibrary: ${e.message}")
            e.printStackTrace()
            // If operation fails, check current state and return it
            isShowInLibrary(show.showId)
        }
    }
    
    override suspend fun getLibraryItemCount(): Int {
        return showDao.getLibraryShows().size
    }
    
    override suspend fun clearLibrary() {
        try {
            println("DEBUG LibraryRepository: Starting to clear entire library")
            showDao.clearAllLibraryTimestamps()
            println("DEBUG LibraryRepository: Successfully cleared all library timestamps")
        } catch (e: Exception) {
            println("ERROR LibraryRepository: Failed to clear library: ${e.message}")
            e.printStackTrace()
        }
    }
}