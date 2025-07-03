package com.deadarchive.core.data.repository

import com.deadarchive.core.data.mapper.DataMappers.toFavoriteEntity
import com.deadarchive.core.data.mapper.DataMappers.toFavoriteItem
import com.deadarchive.core.data.mapper.DataMappers.toFavoriteItems
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.FavoriteItem  
import com.deadarchive.core.model.FavoriteType
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface FavoriteRepository {
    /**
     * Get all favorites with real-time updates
     */
    fun getAllFavorites(): Flow<List<FavoriteItem>>
    
    /**
     * Get favorites by type (concerts or tracks)
     */
    fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteItem>>
    
    /**
     * Get all favorite concerts
     */
    fun getFavoriteConcerts(): Flow<List<FavoriteItem>>
    
    /**
     * Get all favorite tracks
     */
    fun getFavoriteTracks(): Flow<List<FavoriteItem>>
    
    /**
     * Get favorite tracks for a specific concert
     */
    fun getFavoriteTracksForConcert(concertId: String): Flow<List<FavoriteItem>>
    
    /**
     * Get all favorites for a specific concert (concert + tracks)
     */
    fun getFavoritesForConcert(concertId: String): Flow<List<FavoriteItem>>
    
    /**
     * Check if a concert is favorited
     */
    suspend fun isConcertFavorite(concertId: String): Boolean
    
    /**
     * Check if a track is favorited
     */
    suspend fun isTrackFavorite(concertId: String, trackFilename: String): Boolean
    
    /**
     * Check if any item is favorited by ID
     */
    suspend fun isFavorite(id: String): Boolean
    
    /**
     * Get favorite by ID
     */
    suspend fun getFavoriteById(id: String): FavoriteItem?
    
    /**
     * Add a recording to favorites
     */
    suspend fun addRecordingToFavorites(recording: Recording, notes: String? = null)
    
    /**
     * Add a track to favorites
     */
    suspend fun addTrackToFavorites(concertId: String, track: Track, notes: String? = null)
    
    /**
     * Add a favorite item
     */
    suspend fun addFavorite(favoriteItem: FavoriteItem)
    
    /**
     * Remove a recording from favorites
     */
    suspend fun removeRecordingFromFavorites(recordingId: String)
    
    /**
     * Remove a track from favorites
     */
    suspend fun removeTrackFromFavorites(concertId: String, trackFilename: String)
    
    /**
     * Remove favorite by ID
     */
    suspend fun removeFavorite(id: String)
    
    /**
     * Toggle recording favorite status
     */
    suspend fun toggleRecordingFavorite(recording: Recording): Boolean
    
    /**
     * Toggle track favorite status
     */
    suspend fun toggleTrackFavorite(concertId: String, track: Track): Boolean
    
    /**
     * Update favorite notes
     */
    suspend fun updateFavoriteNotes(id: String, notes: String?)
    
    /**
     * Remove all favorites for a concert (both concert and tracks)
     */
    suspend fun removeAllFavoritesForConcert(concertId: String)
    
    /**
     * Get favorite counts
     */
    suspend fun getFavoriteCount(): Int
    suspend fun getConcertFavoriteCount(): Int
    suspend fun getTrackFavoriteCount(): Int
    
    /**
     * Clear all favorites of a specific type
     */
    suspend fun clearFavoritesByType(type: FavoriteType)
    
    /**
     * Batch operations for managing multiple favorites
     */
    suspend fun addFavorites(favoriteItems: List<FavoriteItem>)
    suspend fun removeFavorites(favoriteIds: List<String>)
    suspend fun toggleFavorites(recordings: List<Recording>): List<Boolean>
    
    /**
     * Get favorite recordings with enhanced data from ShowRepository integration
     */
    fun getFavoriteRecordingsWithData(): Flow<List<Recording>>
    
    /**
     * Export favorites for backup/sharing
     */
    suspend fun exportFavorites(): List<FavoriteItem>
    suspend fun importFavorites(favorites: List<FavoriteItem>)
}

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val showRepository: ShowRepository
) : FavoriteRepository {

    override fun getAllFavorites(): Flow<List<FavoriteItem>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.toFavoriteItems()
        }
    }

    override fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByType(type.name).map { entities ->
            entities.toFavoriteItems()
        }
    }

    override fun getFavoriteConcerts(): Flow<List<FavoriteItem>> {
        return getFavoritesByType(FavoriteType.CONCERT)
    }

    override fun getFavoriteTracks(): Flow<List<FavoriteItem>> {
        return getFavoritesByType(FavoriteType.TRACK)
    }

    override fun getFavoriteTracksForConcert(concertId: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesForConcert(concertId).map { entities ->
            entities.filter { it.type == FavoriteType.TRACK.name }
                .toFavoriteItems()
        }
    }

    override fun getFavoritesForConcert(concertId: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesForConcert(concertId).map { entities ->
            entities.toFavoriteItems()
        }
    }

    override suspend fun isConcertFavorite(concertId: String): Boolean {
        return favoriteDao.isRecordingFavorite(concertId)
    }

    override suspend fun isTrackFavorite(concertId: String, trackFilename: String): Boolean {
        return favoriteDao.isTrackFavorite(concertId, trackFilename)
    }

    override suspend fun isFavorite(id: String): Boolean {
        return favoriteDao.isFavorite(id)
    }

    override suspend fun getFavoriteById(id: String): FavoriteItem? {
        return favoriteDao.getFavoriteById(id)?.toFavoriteItem()
    }

    override suspend fun addRecordingToFavorites(recording: Recording, notes: String?) {
        val favoriteItem = FavoriteItem.fromRecording(recording).copy(notes = notes)
        val entity = favoriteItem.toFavoriteEntity()
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun addTrackToFavorites(concertId: String, track: Track, notes: String?) {
        val favoriteItem = FavoriteItem.fromTrack(concertId, track).copy(notes = notes)
        val entity = favoriteItem.toFavoriteEntity()
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun addFavorite(favoriteItem: FavoriteItem) {
        val entity = favoriteItem.toFavoriteEntity()
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun removeRecordingFromFavorites(recordingId: String) {
        val favoriteId = "recording_$recordingId"
        favoriteDao.deleteFavoriteById(favoriteId)
    }

    override suspend fun removeTrackFromFavorites(concertId: String, trackFilename: String) {
        val favoriteId = "track_${concertId}_$trackFilename"
        favoriteDao.deleteFavoriteById(favoriteId)
    }

    override suspend fun removeFavorite(id: String) {
        favoriteDao.deleteFavoriteById(id)
    }

    override suspend fun toggleRecordingFavorite(recording: Recording): Boolean {
        val isFavorite = isConcertFavorite(recording.identifier)
        
        if (isFavorite) {
            removeRecordingFromFavorites(recording.identifier)
        } else {
            addRecordingToFavorites(recording)
        }
        
        return !isFavorite // Return new favorite status
    }

    override suspend fun toggleTrackFavorite(concertId: String, track: Track): Boolean {
        val isFavorite = isTrackFavorite(concertId, track.filename)
        
        if (isFavorite) {
            removeTrackFromFavorites(concertId, track.filename)
        } else {
            addTrackToFavorites(concertId, track)
        }
        
        return !isFavorite // Return new favorite status
    }

    override suspend fun updateFavoriteNotes(id: String, notes: String?) {
        favoriteDao.updateFavoriteNotes(id, notes)
    }

    override suspend fun removeAllFavoritesForConcert(concertId: String) {
        favoriteDao.deleteFavoritesForRecording(concertId)
    }

    override suspend fun getFavoriteCount(): Int {
        return favoriteDao.getFavoriteCount()
    }

    override suspend fun getConcertFavoriteCount(): Int {
        return favoriteDao.getFavoriteCountByType(FavoriteType.CONCERT.name)
    }

    override suspend fun getTrackFavoriteCount(): Int {
        return favoriteDao.getFavoriteCountByType(FavoriteType.TRACK.name)
    }

    override suspend fun clearFavoritesByType(type: FavoriteType) {
        favoriteDao.deleteFavoritesByType(type.name)
    }

    // ============ Enhanced Batch Operations ============

    override suspend fun addFavorites(favoriteItems: List<FavoriteItem>) {
        val entities = favoriteItems.map { it.toFavoriteEntity() }
        favoriteDao.insertFavorites(entities)
    }

    override suspend fun removeFavorites(favoriteIds: List<String>) {
        favoriteDao.deleteFavoritesByIds(favoriteIds)
    }

    override suspend fun toggleFavorites(recordings: List<Recording>): List<Boolean> {
        val results = mutableListOf<Boolean>()
        for (recording in recordings) {
            val newStatus = toggleRecordingFavorite(recording)
            results.add(newStatus)
        }
        return results
    }

    // ============ Integration with ConcertRepository ============

    override fun getFavoriteRecordingsWithData(): Flow<List<Recording>> {
        return combine(
            getFavoriteConcerts(),
            showRepository.getAllCachedRecordings()
        ) { favorites, cachedRecordings ->
            val favoriteRecordingIds = favorites.map { it.recordingId }.toSet()
            cachedRecordings.filter { recording ->
                favoriteRecordingIds.contains(recording.identifier)
            }.map { recording ->
                recording.copy(isFavorite = true)
            }
        }
    }

    // ============ Import/Export Operations ============

    override suspend fun exportFavorites(): List<FavoriteItem> {
        return favoriteDao.getAllFavoritesSync().toFavoriteItems()
    }

    override suspend fun importFavorites(favorites: List<FavoriteItem>) {
        val entities = favorites.map { it.toFavoriteEntity() }
        favoriteDao.insertFavorites(entities)
    }
}