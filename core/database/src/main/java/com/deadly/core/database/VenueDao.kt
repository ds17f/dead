package com.deadly.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for venue operations.
 */
@Dao
interface VenueDao {
    
    // Basic CRUD operations
    
    @Query("SELECT * FROM venues WHERE venueId = :venueId")
    suspend fun getVenue(venueId: String): VenueEntity?
    
    @Query("SELECT * FROM venues WHERE venueId IN (:venueIds)")
    suspend fun getVenues(venueIds: List<String>): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE name = :name")
    suspend fun getVenueByName(name: String): VenueEntity?
    
    @Query("SELECT * FROM venues WHERE normalizedName = :normalizedName")
    suspend fun getVenueByNormalizedName(normalizedName: String): VenueEntity?
    
    @Query("SELECT * FROM venues ORDER BY name")
    suspend fun getAllVenues(): List<VenueEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenue(venue: VenueEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenues(venues: List<VenueEntity>)
    
    @Query("DELETE FROM venues WHERE venueId = :venueId")
    suspend fun deleteVenue(venueId: String)
    
    @Query("DELETE FROM venues WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldVenues(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM venues")
    suspend fun getVenueCount(): Int
    
    // Search operations
    
    @Query("""
        SELECT * FROM venues 
        WHERE name LIKE '%' || :query || '%' 
           OR normalizedName LIKE '%' || :query || '%'
           OR aliasesJson LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
           OR state LIKE '%' || :query || '%'
           OR country LIKE '%' || :query || '%'
           OR fullLocation LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN name = :query THEN 1
                 WHEN normalizedName = :query THEN 2  
                 WHEN name LIKE :query || '%' THEN 3
                 WHEN normalizedName LIKE :query || '%' THEN 4
                 ELSE 5 
            END, name
    """)
    suspend fun searchVenues(query: String): List<VenueEntity>
    
    @Query("""
        SELECT * FROM venues 
        WHERE name LIKE :query || '%' 
           OR normalizedName LIKE :query || '%'
        ORDER BY name
        LIMIT :limit
    """)
    suspend fun searchVenuesStartingWith(query: String, limit: Int = 20): List<VenueEntity>
    
    // Location-based queries
    
    @Query("SELECT * FROM venues WHERE city = :city ORDER BY name")
    suspend fun getVenuesByCity(city: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE state = :state ORDER BY city, name")
    suspend fun getVenuesByState(state: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE country = :country ORDER BY state, city, name")
    suspend fun getVenuesByCountry(country: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE isUSVenue = 1 ORDER BY state, city, name")
    suspend fun getUSVenues(): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE isInternational = 1 ORDER BY country, city, name")
    suspend fun getInternationalVenues(): List<VenueEntity>
    
    @Query("""
        SELECT * FROM venues 
        WHERE city = :city AND state = :state 
        ORDER BY name
    """)
    suspend fun getVenuesByCityState(city: String, state: String): List<VenueEntity>
    
    // Type and capacity-based queries
    
    @Query("SELECT * FROM venues WHERE venueType = :venueType ORDER BY name")
    suspend fun getVenuesByType(venueType: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE capacity >= :minCapacity AND capacity <= :maxCapacity ORDER BY capacity, name")
    suspend fun getVenuesByCapacityRange(minCapacity: Int, maxCapacity: Int): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE capacity >= :minCapacity ORDER BY capacity DESC, name")
    suspend fun getVenuesWithMinCapacity(minCapacity: Int): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE capacity IS NOT NULL ORDER BY capacity DESC LIMIT :limit")
    suspend fun getLargestVenues(limit: Int = 50): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE capacity IS NOT NULL ORDER BY capacity ASC LIMIT :limit")
    suspend fun getSmallestVenues(limit: Int = 50): List<VenueEntity>
    
    // Show count-based queries
    
    @Query("SELECT * FROM venues WHERE totalShows >= :minShows ORDER BY totalShows DESC, name")
    suspend fun getVenuesWithMinShows(minShows: Int): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE totalShows IS NOT NULL ORDER BY totalShows DESC LIMIT :limit")
    suspend fun getMostPopularVenues(limit: Int = 50): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE totalShows = 1 ORDER BY name")
    suspend fun getOneTimeVenues(): List<VenueEntity>
    
    // Date-based queries
    
    @Query("SELECT * FROM venues WHERE firstShow = :date ORDER BY name")
    suspend fun getVenuesFirstPlayedOn(date: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE lastShow = :date ORDER BY name")
    suspend fun getVenuesLastPlayedOn(date: String): List<VenueEntity>
    
    @Query("SELECT * FROM venues WHERE firstShow LIKE :year || '%' ORDER BY firstShow, name")
    suspend fun getVenuesFirstPlayedInYear(year: String): List<VenueEntity>
    
    @Query("""
        SELECT * FROM venues 
        WHERE firstShow BETWEEN :startDate AND :endDate 
        ORDER BY firstShow, name
    """)
    suspend fun getVenuesFirstPlayedInPeriod(startDate: String, endDate: String): List<VenueEntity>
    
    // Reactive queries with Flow
    
    @Query("SELECT * FROM venues ORDER BY name")
    fun getAllVenuesFlow(): Flow<List<VenueEntity>>
    
    @Query("SELECT * FROM venues WHERE isUSVenue = 1 ORDER BY state, city, name")
    fun getUSVenuesFlow(): Flow<List<VenueEntity>>
    
    @Query("SELECT * FROM venues WHERE isInternational = 1 ORDER BY country, city, name")
    fun getInternationalVenuesFlow(): Flow<List<VenueEntity>>
    
    @Query("SELECT * FROM venues WHERE totalShows IS NOT NULL ORDER BY totalShows DESC LIMIT :limit")
    fun getMostPopularVenuesFlow(limit: Int = 50): Flow<List<VenueEntity>>
    
    // Bulk operations
    
    @Transaction
    suspend fun replaceAllVenues(venues: List<VenueEntity>) {
        clearVenues()
        insertVenues(venues)
    }
    
    @Query("DELETE FROM venues")
    suspend fun clearVenues()
    
    // Statistics queries
    
    @Query("SELECT COUNT(*) FROM venues WHERE isUSVenue = 1")
    suspend fun getUSVenueCount(): Int
    
    @Query("SELECT COUNT(*) FROM venues WHERE isInternational = 1")
    suspend fun getInternationalVenueCount(): Int
    
    @Query("SELECT COUNT(*) FROM venues WHERE venueType = :venueType")
    suspend fun getVenueCountByType(venueType: String): Int
    
    @Query("SELECT AVG(totalShows) FROM venues WHERE totalShows IS NOT NULL")
    suspend fun getAverageShowCount(): Float?
    
    @Query("SELECT AVG(capacity) FROM venues WHERE capacity IS NOT NULL")
    suspend fun getAverageCapacity(): Float?
    
    @Query("SELECT MAX(totalShows) FROM venues")
    suspend fun getMaxShowCount(): Int?
    
    @Query("SELECT MIN(totalShows) FROM venues WHERE totalShows > 0")
    suspend fun getMinShowCount(): Int?
    
    @Query("""
        SELECT state, COUNT(*) as count, AVG(totalShows) as avgShows 
        FROM venues 
        WHERE state IS NOT NULL AND totalShows IS NOT NULL
        GROUP BY state
        ORDER BY count DESC
    """)
    suspend fun getVenueStatsByState(): List<VenueStateStats>
    
    @Query("""
        SELECT venueType, COUNT(*) as count, AVG(capacity) as avgCapacity
        FROM venues 
        WHERE venueType IS NOT NULL
        GROUP BY venueType
        ORDER BY count DESC
    """)
    suspend fun getVenueStatsByType(): List<VenueTypeStats>
    
    @Query("""
        SELECT city, state, COUNT(*) as count
        FROM venues 
        WHERE city IS NOT NULL AND state IS NOT NULL
        GROUP BY city, state
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getTopCitiesByVenueCount(limit: Int = 20): List<VenueCityStats>
}

/**
 * Data class for venue statistics by state.
 */
data class VenueStateStats(
    val state: String,
    val count: Int,
    val avgShows: Float
)

/**
 * Data class for venue statistics by type.
 */
data class VenueTypeStats(
    val venueType: String,
    val count: Int,
    val avgCapacity: Float
)

/**
 * Data class for venue statistics by city.
 */
data class VenueCityStats(
    val city: String,
    val state: String,
    val count: Int
)