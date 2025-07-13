package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for "Today in Grateful Dead History" functionality.
 * Provides methods to find shows that occurred on the current date in past years.
 */
@Singleton
class TodayInHistoryRepository @Inject constructor(
    private val showDao: ShowDao,
    private val recordingDao: RecordingDao,
    private val ratingsRepository: RatingsRepository
) {
    
    companion object {
        private const val TAG = "TodayInHistoryRepository"
        private const val MONTH_DAY_FORMAT = "MM-dd"
        private const val FULL_DATE_FORMAT = "yyyy-MM-dd"
    }
    
    /**
     * Get shows that occurred on today's date (month/day) across all years.
     * @return List of shows ordered by year descending (most recent first)
     */
    suspend fun getTodaysShowsInHistory(): List<Show> {
        val todayMonthDay = getCurrentMonthDay()
        val showEntities = showDao.getShowsByMonthDay(todayMonthDay)
        return showEntities.map { showEntity ->
            // Get recordings for this show
            val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { recordingEntity ->
                val recording = recordingEntity.toRecording()
                // Apply rating data to recording
                val recordingRating = ratingsRepository.getRecordingRating(recording.identifier)
                recording.copy(
                    rating = recordingRating?.rating,
                    rawRating = recordingRating?.rawRating,
                    ratingConfidence = recordingRating?.confidence,
                    reviewCount = recordingRating?.reviewCount,
                    sourceType = recordingRating?.sourceType,
                    ratingDistribution = recordingRating?.ratingDistribution,
                    highRatings = recordingRating?.highRatings,
                    lowRatings = recordingRating?.lowRatings
                )
            }
            
            // Apply rating data to show
            val showRating = ratingsRepository.getShowRatingByDateVenue(
                showEntity.date, showEntity.venue ?: ""
            )
            
            showEntity.toShow(recordings).copy(
                rating = showRating?.rating,
                rawRating = showRating?.rawRating,
                ratingConfidence = showRating?.confidence,
                totalHighRatings = showRating?.totalHighRatings,
                totalLowRatings = showRating?.totalLowRatings,
                bestRecordingId = showRating?.bestRecordingId
            )
        }
    }
    
    /**
     * Get shows that occurred on a specific month/day across all years.
     * @param monthDay String in "MM-dd" format (e.g., "08-09" for August 9th)
     * @return List of shows ordered by year descending (most recent first)
     */
    suspend fun getShowsByMonthDay(monthDay: String): List<Show> {
        if (!isValidMonthDay(monthDay)) {
            return emptyList()
        }
        val showEntities = showDao.getShowsByMonthDay(monthDay)
        return showEntities.map { showEntity ->
            // Get recordings for this show
            val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { recordingEntity ->
                val recording = recordingEntity.toRecording()
                // Apply rating data to recording
                val recordingRating = ratingsRepository.getRecordingRating(recording.identifier)
                recording.copy(
                    rating = recordingRating?.rating,
                    rawRating = recordingRating?.rawRating,
                    ratingConfidence = recordingRating?.confidence,
                    reviewCount = recordingRating?.reviewCount,
                    sourceType = recordingRating?.sourceType,
                    ratingDistribution = recordingRating?.ratingDistribution,
                    highRatings = recordingRating?.highRatings,
                    lowRatings = recordingRating?.lowRatings
                )
            }
            
            // Apply rating data to show
            val showRating = ratingsRepository.getShowRatingByDateVenue(
                showEntity.date, showEntity.venue ?: ""
            )
            
            showEntity.toShow(recordings).copy(
                rating = showRating?.rating,
                rawRating = showRating?.rawRating,
                ratingConfidence = showRating?.confidence,
                totalHighRatings = showRating?.totalHighRatings,
                totalLowRatings = showRating?.totalLowRatings,
                bestRecordingId = showRating?.bestRecordingId
            )
        }
    }
    
    /**
     * Get shows that occurred on today's date as a Flow for reactive UI updates.
     * @return Flow of show lists that updates when database changes
     */
    fun getTodaysShowsInHistoryFlow(): Flow<List<Show>> = flow {
        val todayMonthDay = getCurrentMonthDay()
        val showEntities = showDao.getShowsByMonthDay(todayMonthDay)
        emit(showEntities.map { showEntity ->
            // Get recordings for this show
            val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { recordingEntity ->
                val recording = recordingEntity.toRecording()
                // Apply rating data to recording
                val recordingRating = ratingsRepository.getRecordingRating(recording.identifier)
                recording.copy(
                    rating = recordingRating?.rating,
                    rawRating = recordingRating?.rawRating,
                    ratingConfidence = recordingRating?.confidence,
                    reviewCount = recordingRating?.reviewCount,
                    sourceType = recordingRating?.sourceType,
                    ratingDistribution = recordingRating?.ratingDistribution,
                    highRatings = recordingRating?.highRatings,
                    lowRatings = recordingRating?.lowRatings
                )
            }
            
            // Apply rating data to show
            val showRating = ratingsRepository.getShowRatingByDateVenue(
                showEntity.date, showEntity.venue ?: ""
            )
            
            showEntity.toShow(recordings).copy(
                rating = showRating?.rating,
                rawRating = showRating?.rawRating,
                ratingConfidence = showRating?.confidence,
                totalHighRatings = showRating?.totalHighRatings,
                totalLowRatings = showRating?.totalLowRatings,
                bestRecordingId = showRating?.bestRecordingId
            )
        })
    }
    
    /**
     * Get a random historical show from today's date.
     * Useful for featuring a single show prominently.
     * @return A random show from today's date in history, or null if none exist
     */
    suspend fun getRandomTodayShow(): Show? {
        val todaysShows = getTodaysShowsInHistory()
        return if (todaysShows.isNotEmpty()) {
            todaysShows.random()
        } else {
            null
        }
    }
    
    /**
     * Get shows from this month across all years.
     * @return List of shows from the current month in all years
     */
    suspend fun getThisMonthInHistory(): List<Show> {
        val currentMonth = getCurrentMonth()
        val showEntities = showDao.getShowsByMonth(currentMonth)
        return showEntities.map { showEntity ->
            // Get recordings for this show
            val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { recordingEntity ->
                val recording = recordingEntity.toRecording()
                // Apply rating data to recording
                val recordingRating = ratingsRepository.getRecordingRating(recording.identifier)
                recording.copy(
                    rating = recordingRating?.rating,
                    rawRating = recordingRating?.rawRating,
                    ratingConfidence = recordingRating?.confidence,
                    reviewCount = recordingRating?.reviewCount,
                    sourceType = recordingRating?.sourceType,
                    ratingDistribution = recordingRating?.ratingDistribution,
                    highRatings = recordingRating?.highRatings,
                    lowRatings = recordingRating?.lowRatings
                )
            }
            
            // Apply rating data to show
            val showRating = ratingsRepository.getShowRatingByDateVenue(
                showEntity.date, showEntity.venue ?: ""
            )
            
            showEntity.toShow(recordings).copy(
                rating = showRating?.rating,
                rawRating = showRating?.rawRating,
                ratingConfidence = showRating?.confidence,
                totalHighRatings = showRating?.totalHighRatings,
                totalLowRatings = showRating?.totalLowRatings,
                bestRecordingId = showRating?.bestRecordingId
            )
        }
    }
    
    /**
     * Get a formatted string describing today's date for display.
     * @return String like "August 9th" for display purposes
     */
    fun getTodaysDateFormatted(): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        
        val dayWithSuffix = when (dayOfMonth % 10) {
            1 -> if (dayOfMonth == 11) "${dayOfMonth}th" else "${dayOfMonth}st"
            2 -> if (dayOfMonth == 12) "${dayOfMonth}th" else "${dayOfMonth}nd"
            3 -> if (dayOfMonth == 13) "${dayOfMonth}th" else "${dayOfMonth}rd"
            else -> "${dayOfMonth}th"
        }
        
        return "$monthName $dayWithSuffix"
    }
    
    /**
     * Get count of shows that occurred on today's date across all years.
     * @return Number of shows found for today's date
     */
    suspend fun getTodaysShowCount(): Int {
        val todaysShows = getTodaysShowsInHistory()
        return todaysShows.size
    }
    
    /**
     * Check if there are any shows for today's date in history.
     * @return true if shows exist for today, false otherwise
     */
    suspend fun hasShowsToday(): Boolean {
        return getTodaysShowCount() > 0
    }
    
    /**
     * Get years that had shows on today's date.
     * @return List of years (as strings) that had shows on this date
     */
    suspend fun getYearsWithShowsToday(): List<String> {
        val todaysShows = getTodaysShowsInHistory()
        return todaysShows.mapNotNull { it.year }.distinct().sorted()
    }
    
    // Private helper methods
    
    /**
     * Get current month and day in MM-dd format for database queries.
     */
    private fun getCurrentMonthDay(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat(MONTH_DAY_FORMAT, Locale.US)
        return formatter.format(calendar.time)
    }
    
    /**
     * Get current month in MM format for database queries.
     */
    private fun getCurrentMonth(): String {
        val calendar = Calendar.getInstance()
        return String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
    }
    
    /**
     * Validate month-day format (MM-dd).
     */
    private fun isValidMonthDay(monthDay: String): Boolean {
        if (monthDay.length != 5 || monthDay[2] != '-') {
            return false
        }
        
        return try {
            val month = monthDay.substring(0, 2).toInt()
            val day = monthDay.substring(3, 5).toInt()
            month in 1..12 && day in 1..31
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Get statistics about historical shows for a given month/day.
     * @param monthDay Month-day in MM-dd format
     * @return Map of statistics (total shows, years span, etc.)
     */
    suspend fun getHistoryStatistics(monthDay: String): Map<String, Any?> {
        val shows = getShowsByMonthDay(monthDay)
        
        if (shows.isEmpty()) {
            return mapOf(
                "totalShows" to 0,
                "yearsWithShows" to emptyList<String>(),
                "earliestYear" to null,
                "latestYear" to null,
                "venueCount" to 0,
                "uniqueVenues" to emptyList<String>()
            )
        }
        
        val years = shows.mapNotNull { it.year }.distinct().sorted()
        val venues = shows.mapNotNull { it.venue }.distinct()
        
        return mapOf(
            "totalShows" to shows.size,
            "yearsWithShows" to years,
            "earliestYear" to years.firstOrNull(),
            "latestYear" to years.lastOrNull(),
            "venueCount" to venues.size,
            "uniqueVenues" to venues
        )
    }
}