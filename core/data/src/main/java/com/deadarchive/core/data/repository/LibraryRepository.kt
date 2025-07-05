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
        return libraryDao.isShowInLibrary(showId)
    }
    
    override suspend fun addShowToLibrary(show: Show): Boolean {
        try {
            // 1. Create LibraryItem (as before)
            val libraryItem = LibraryItem.fromShow(show)
            val libraryEntity = LibraryEntity.fromLibraryItem(libraryItem)
            
            // 2. Create ShowEntity if it doesn't exist
            if (!showDao.showExists(show.showId)) {
                val showEntity = ShowEntity(
                    showId = show.showId,
                    date = show.date,
                    venue = show.venue,
                    location = show.location,
                    year = show.year,
                    setlistRaw = show.setlistRaw,
                    setsJson = null, // Could be serialized if needed
                    isInLibrary = true,
                    cachedTimestamp = System.currentTimeMillis()
                )
                showDao.insertShow(showEntity)
                println("DEBUG LibraryRepository: Created ShowEntity for ${show.showId}")
            } else {
                // Update existing show to mark as in library
                showDao.updateLibraryStatus(show.showId, true)
                println("DEBUG LibraryRepository: Updated existing ShowEntity ${show.showId} to in library")
            }
            
            // 3. Save recordings if they don't exist
            if (show.recordings.isNotEmpty()) {
                val existingRecordings = recordingDao.getRecordingsByConcertId(show.showId)
                val existingIds = existingRecordings.map { it.identifier }.toSet()
                
                val newRecordings = show.recordings.filter { it.identifier !in existingIds }
                if (newRecordings.isNotEmpty()) {
                    val recordingEntities = newRecordings.map { recording ->
                        RecordingEntity.fromRecording(recording, show.showId)
                    }
                    recordingDao.insertRecordings(recordingEntities)
                    println("DEBUG LibraryRepository: Saved ${newRecordings.size} new recordings for ${show.showId}")
                }
            }
            
            // 4. Finally add to library
            libraryDao.insertLibraryItem(libraryEntity)
            println("DEBUG LibraryRepository: Added show ${show.showId} to library")
            return true
        } catch (e: Exception) {
            println("ERROR LibraryRepository: Failed to add show to library: ${e.message}")
            return false
        }
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