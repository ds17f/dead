package com.deadarchive.core.data.repository

import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.database.LibraryEntity
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.LibraryItem  
import com.deadarchive.core.model.LibraryItemType
import com.deadarchive.core.model.Track
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
     * Get library items by type (recordings or tracks)
     */
    fun getLibraryItemsByType(type: LibraryItemType): Flow<List<LibraryItem>>
    
    /**
     * Check if a recording is in library
     */
    suspend fun isRecordingInLibrary(recordingId: String): Boolean
    
    /**
     * Check if a track is in library
     */
    suspend fun isTrackInLibrary(recordingId: String, trackFilename: String): Boolean
    
    /**
     * Add recording to library
     */
    suspend fun addRecordingToLibrary(recording: Recording): Boolean
    
    /**
     * Remove recording from library
     */
    suspend fun removeRecordingFromLibrary(recordingId: String)
    
    /**
     * Toggle recording library status
     */
    suspend fun toggleRecordingLibrary(recording: Recording): Boolean
    
    /**
     * Add track to library
     */
    suspend fun addTrackToLibrary(recordingId: String, track: Track): Boolean
    
    /**
     * Remove track from library
     */
    suspend fun removeTrackFromLibrary(recordingId: String, trackFilename: String)
    
    /**
     * Toggle track library status
     */
    suspend fun toggleTrackLibrary(recordingId: String, track: Track): Boolean
    
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
    
    override suspend fun isRecordingInLibrary(recordingId: String): Boolean {
        return libraryDao.isRecordingInLibrary(recordingId)
    }
    
    override suspend fun isTrackInLibrary(recordingId: String, trackFilename: String): Boolean {
        return libraryDao.isTrackInLibrary(recordingId, trackFilename)
    }
    
    override suspend fun addRecordingToLibrary(recording: Recording): Boolean {
        val libraryItem = LibraryItem.fromRecording(recording)
        val entity = LibraryEntity.fromLibraryItem(libraryItem)
        libraryDao.insertLibraryItem(entity)
        return true
    }
    
    override suspend fun removeRecordingFromLibrary(recordingId: String) {
        val libraryItemId = "recording_$recordingId"
        libraryDao.deleteLibraryItemById(libraryItemId)
    }
    
    override suspend fun toggleRecordingLibrary(recording: Recording): Boolean {
        val isCurrentlyInLibrary = isRecordingInLibrary(recording.identifier)
        
        return if (isCurrentlyInLibrary) {
            removeRecordingFromLibrary(recording.identifier)
            false
        } else {
            addRecordingToLibrary(recording)
            true
        }
    }
    
    override suspend fun addTrackToLibrary(recordingId: String, track: Track): Boolean {
        val libraryItem = LibraryItem.fromTrack(recordingId, track)
        val entity = LibraryEntity.fromLibraryItem(libraryItem)
        libraryDao.insertLibraryItem(entity)
        return true
    }
    
    override suspend fun removeTrackFromLibrary(recordingId: String, trackFilename: String) {
        val libraryItemId = "track_${recordingId}_$trackFilename"
        libraryDao.deleteLibraryItemById(libraryItemId)
    }
    
    override suspend fun toggleTrackLibrary(recordingId: String, track: Track): Boolean {
        val isCurrentlyInLibrary = isTrackInLibrary(recordingId, track.filename)
        
        return if (isCurrentlyInLibrary) {
            removeTrackFromLibrary(recordingId, track.filename)
            false
        } else {
            addTrackToLibrary(recordingId, track)
            true
        }
    }
    
    override suspend fun getLibraryItemCount(): Int {
        return libraryDao.getLibraryItemCount()
    }
}