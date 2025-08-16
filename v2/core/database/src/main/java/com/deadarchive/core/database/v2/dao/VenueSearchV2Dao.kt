package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.VenueSearchV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueSearchV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(venues: List<VenueSearchV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venue: VenueSearchV2Entity)
    
    @Query("DELETE FROM venue_search_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM venue_search_v2 WHERE venueName LIKE '%' || :query || '%' OR venueKey LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' ORDER BY rating DESC, date DESC")
    suspend fun searchVenues(query: String): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 WHERE venueKey = :venueKey ORDER BY date DESC")
    suspend fun getShowsForVenue(venueKey: String): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 GROUP BY venueKey ORDER BY venueName")
    suspend fun getAllUniqueVenues(): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 WHERE city = :city ORDER BY rating DESC, date DESC")
    suspend fun getVenuesByCity(city: String): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 WHERE state = :state ORDER BY rating DESC, date DESC")
    suspend fun getVenuesByState(state: String): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 WHERE country = :country ORDER BY rating DESC, date DESC")
    suspend fun getVenuesByCountry(country: String): List<VenueSearchV2Entity>
    
    @Query("SELECT * FROM venue_search_v2 WHERE date BETWEEN :startDate AND :endDate ORDER BY rating DESC")
    suspend fun getVenuesInDateRange(startDate: String, endDate: String): List<VenueSearchV2Entity>
    
    @Query("SELECT DISTINCT city FROM venue_search_v2 WHERE country = :country ORDER BY city")
    suspend fun getCitiesInCountry(country: String): List<String>
    
    @Query("SELECT DISTINCT state FROM venue_search_v2 WHERE country = :country ORDER BY state")
    suspend fun getStatesInCountry(country: String): List<String>
    
    @Query("SELECT COUNT(DISTINCT venueKey) FROM venue_search_v2")
    suspend fun getUniqueVenueCount(): Int
    
    @Query("SELECT COUNT(*) FROM venue_search_v2")
    suspend fun getTotalShowCount(): Int
}