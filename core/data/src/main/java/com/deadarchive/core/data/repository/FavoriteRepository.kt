package com.deadarchive.core.data.repository

import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.database.FavoriteEntity
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.FavoriteItem  
import com.deadarchive.core.model.FavoriteType
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.Flow
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
     * Add a concert to favorites
     */
    suspend fun addConcertToFavorites(concert: Concert, notes: String? = null)
    
    /**
     * Add a track to favorites
     */
    suspend fun addTrackToFavorites(concertId: String, track: Track, notes: String? = null)
    
    /**
     * Add a favorite item
     */
    suspend fun addFavorite(favoriteItem: FavoriteItem)
    
    /**
     * Remove a concert from favorites
     */
    suspend fun removeConcertFromFavorites(concertId: String)
    
    /**
     * Remove a track from favorites
     */
    suspend fun removeTrackFromFavorites(concertId: String, trackFilename: String)
    
    /**
     * Remove favorite by ID
     */
    suspend fun removeFavorite(id: String)
    
    /**
     * Toggle concert favorite status
     */
    suspend fun toggleConcertFavorite(concert: Concert): Boolean
    
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
}

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getAllFavorites(): Flow<List<FavoriteItem>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toFavoriteItem() }
        }
    }

    override fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesByType(type.name).map { entities ->
            entities.map { it.toFavoriteItem() }
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
                .map { it.toFavoriteItem() }
        }
    }

    override fun getFavoritesForConcert(concertId: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesForConcert(concertId).map { entities ->
            entities.map { it.toFavoriteItem() }
        }
    }

    override suspend fun isConcertFavorite(concertId: String): Boolean {
        return favoriteDao.isConcertFavorite(concertId)
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

    override suspend fun addConcertToFavorites(concert: Concert, notes: String?) {
        val favoriteItem = FavoriteItem.fromConcert(concert).copy(notes = notes)
        val entity = FavoriteEntity.fromFavoriteItem(favoriteItem)
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun addTrackToFavorites(concertId: String, track: Track, notes: String?) {
        val favoriteItem = FavoriteItem.fromTrack(concertId, track).copy(notes = notes)
        val entity = FavoriteEntity.fromFavoriteItem(favoriteItem)
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun addFavorite(favoriteItem: FavoriteItem) {
        val entity = FavoriteEntity.fromFavoriteItem(favoriteItem)
        favoriteDao.insertFavorite(entity)
    }

    override suspend fun removeConcertFromFavorites(concertId: String) {
        val favoriteId = "concert_$concertId"
        favoriteDao.deleteFavoriteById(favoriteId)
    }

    override suspend fun removeTrackFromFavorites(concertId: String, trackFilename: String) {
        val favoriteId = "track_${concertId}_$trackFilename"
        favoriteDao.deleteFavoriteById(favoriteId)
    }

    override suspend fun removeFavorite(id: String) {
        favoriteDao.deleteFavoriteById(id)
    }

    override suspend fun toggleConcertFavorite(concert: Concert): Boolean {
        val isFavorite = isConcertFavorite(concert.identifier)
        
        if (isFavorite) {
            removeConcertFromFavorites(concert.identifier)
        } else {
            addConcertToFavorites(concert)
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
        favoriteDao.deleteFavoritesForConcert(concertId)
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
}