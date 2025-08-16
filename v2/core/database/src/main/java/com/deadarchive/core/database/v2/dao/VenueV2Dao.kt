package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.deadarchive.v2.core.database.entities.VenueV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueV2Dao {
    
    // Core operations for import
    @Query("SELECT * FROM venues_v2 WHERE venueId = :venueId")
    suspend fun findByVenueId(venueId: String): VenueV2Entity?
    
    @Insert
    suspend fun insert(venue: VenueV2Entity)
    
    @Insert
    suspend fun insertAll(venues: List<VenueV2Entity>)
    
    @Update
    suspend fun update(venue: VenueV2Entity)
    
    // Basic queries for verification
    @Query("SELECT * FROM venues_v2 ORDER BY name")
    suspend fun getAllVenues(): List<VenueV2Entity>
    
    @Query("SELECT * FROM venues_v2 ORDER BY name")
    fun getAllVenuesFlow(): Flow<List<VenueV2Entity>>
    
    @Query("SELECT COUNT(*) FROM venues_v2")
    suspend fun getVenueCount(): Int
    
    // Search queries
    @Query("SELECT * FROM venues_v2 WHERE normalizedName LIKE '%' || :searchQuery || '%' ORDER BY name")
    suspend fun searchVenues(searchQuery: String): List<VenueV2Entity>
    
    @Query("SELECT * FROM venues_v2 WHERE city = :city ORDER BY name")
    suspend fun getVenuesByCity(city: String): List<VenueV2Entity>
    
    @Query("SELECT * FROM venues_v2 WHERE state = :state ORDER BY city, name")
    suspend fun getVenuesByState(state: String): List<VenueV2Entity>
    
    // Management operations
    @Query("DELETE FROM venues_v2")
    suspend fun deleteAll()
}