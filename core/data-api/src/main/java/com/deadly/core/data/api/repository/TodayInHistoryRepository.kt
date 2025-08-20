package com.deadly.core.data.api.repository

import com.deadly.core.model.Show
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for "Today in Grateful Dead History" functionality.
 * Provides methods to find shows that occurred on the current date in past years.
 */
interface TodayInHistoryRepository {
    /**
     * Get shows that occurred on today's date (month/day) across all years.
     * @return List of shows ordered by year descending (most recent first)
     */
    suspend fun getTodaysShowsInHistory(): List<Show>
    
    /**
     * Get shows that occurred on a specific month/day across all years.
     * @param monthDay String in "MM-dd" format (e.g., "08-09" for August 9th)
     * @return List of shows ordered by year descending (most recent first)
     */
    suspend fun getShowsByMonthDay(monthDay: String): List<Show>
    
    /**
     * Get shows that occurred on today's date as a Flow for reactive UI updates.
     * @return Flow of show lists that updates when database changes
     */
    fun getTodaysShowsInHistoryFlow(): Flow<List<Show>>
    
    /**
     * Get a random historical show from today's date.
     * Useful for featuring a single show prominently.
     * @return A random show from today's date in history, or null if none exist
     */
    suspend fun getRandomTodayShow(): Show?
    
    /**
     * Get shows from this month across all years.
     * @return List of shows from the current month in all years
     */
    suspend fun getThisMonthInHistory(): List<Show>
    
    /**
     * Get a formatted string describing today's date for display.
     * @return String like "August 9th" for display purposes
     */
    fun getTodaysDateFormatted(): String
    
    /**
     * Get count of shows that occurred on today's date across all years.
     * @return Number of shows found for today's date
     */
    suspend fun getTodaysShowCount(): Int
    
    /**
     * Check if there are any shows for today's date in history.
     * @return true if shows exist for today, false otherwise
     */
    suspend fun hasShowsToday(): Boolean
    
    /**
     * Get years that had shows on today's date.
     * @return List of years (as strings) that had shows on this date
     */
    suspend fun getYearsWithShowsToday(): List<String>
    
    /**
     * Get statistics about historical shows for a given month/day.
     * @param monthDay Month-day in MM-dd format
     * @return Map of statistics (total shows, years span, etc.)
     */
    suspend fun getHistoryStatistics(monthDay: String): Map<String, Any?>
}