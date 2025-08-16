package com.deadarchive.v2.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.deadarchive.v2.core.database.entities.ShowSearchV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowSearchV2Dao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shows: List<ShowSearchV2Entity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(show: ShowSearchV2Entity)
    
    @Query("DELETE FROM show_search_v2")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM show_search_v2 WHERE searchText LIKE '%' || :query || '%' ORDER BY rating DESC, date DESC")
    suspend fun searchShows(query: String): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE showId = :showId")
    suspend fun getShow(showId: String): ShowSearchV2Entity?
    
    @Query("SELECT * FROM show_search_v2 WHERE year = :year ORDER BY date DESC")
    suspend fun getShowsByYear(year: Int): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE year = :year AND month = :month ORDER BY date DESC")
    suspend fun getShowsByYearAndMonth(year: Int, month: Int): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getShowsInDateRange(startDate: String, endDate: String): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE venue LIKE '%' || :venue || '%' ORDER BY date DESC")
    suspend fun getShowsByVenue(venue: String): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE city = :city ORDER BY date DESC")
    suspend fun getShowsByCity(city: String): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE state = :state ORDER BY date DESC")
    suspend fun getShowsByState(state: String): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE rating > :minRating ORDER BY rating DESC, date DESC")
    suspend fun getHighRatedShows(minRating: Double): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE hasSetlist = 1 ORDER BY rating DESC, date DESC")
    suspend fun getShowsWithSetlists(): List<ShowSearchV2Entity>
    
    @Query("SELECT * FROM show_search_v2 WHERE recordingCount > 0 ORDER BY rating DESC, date DESC")
    suspend fun getShowsWithRecordings(): List<ShowSearchV2Entity>
    
    @Query("SELECT DISTINCT year FROM show_search_v2 ORDER BY year DESC")
    suspend fun getAllYears(): List<Int>
    
    @Query("SELECT DISTINCT city FROM show_search_v2 WHERE country = :country ORDER BY city")
    suspend fun getCitiesInCountry(country: String): List<String>
    
    @Query("SELECT DISTINCT state FROM show_search_v2 WHERE country = :country ORDER BY state")
    suspend fun getStatesInCountry(country: String): List<String>
    
    @Query("SELECT COUNT(*) FROM show_search_v2")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM show_search_v2 WHERE hasSetlist = 1")
    suspend fun getShowsWithSetlistCount(): Int
    
    @Query("SELECT COUNT(*) FROM show_search_v2 WHERE recordingCount > 0")
    suspend fun getShowsWithRecordingCount(): Int
}