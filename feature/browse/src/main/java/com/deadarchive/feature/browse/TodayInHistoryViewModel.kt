package com.deadarchive.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.repository.TodayInHistoryRepository
import com.deadarchive.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for "Today in Grateful Dead History" feature.
 * Manages loading and displaying shows that occurred on the current date in past years.
 */
@HiltViewModel
class TodayInHistoryViewModel @Inject constructor(
    private val todayInHistoryRepository: TodayInHistoryRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "TodayInHistoryViewModel"
    }
    
    private val _uiState = MutableStateFlow<TodayInHistoryState>(TodayInHistoryState.Loading)
    val uiState: StateFlow<TodayInHistoryState> = _uiState.asStateFlow()
    
    private val _displayMode = MutableStateFlow(HistoryDisplayMode.COMPACT)
    val displayMode: StateFlow<HistoryDisplayMode> = _displayMode.asStateFlow()
    
    init {
        loadTodaysHistory()
    }
    
    /**
     * Load shows for today's date in history.
     */
    fun loadTodaysHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = TodayInHistoryState.Loading
                
                val shows = todayInHistoryRepository.getTodaysShowsInHistory()
                val dateFormatted = todayInHistoryRepository.getTodaysDateFormatted()
                
                if (shows.isEmpty()) {
                    _uiState.value = TodayInHistoryState.NoShows
                    return@launch
                }
                
                // Get statistics for this date
                val monthDay = getCurrentMonthDay()
                val statisticsMap = todayInHistoryRepository.getHistoryStatistics(monthDay)
                
                val statistics = HistoryStatistics(
                    totalShows = statisticsMap["totalShows"] as Int,
                    yearsWithShows = statisticsMap["yearsWithShows"] as List<String>,
                    uniqueVenues = statisticsMap["uniqueVenues"] as List<String>,
                    venueCount = statisticsMap["venueCount"] as Int,
                    yearRange = Pair(
                        statisticsMap["earliestYear"] as String?,
                        statisticsMap["latestYear"] as String?
                    )
                )
                
                // Select a featured show (most recent or random)
                val featuredShow = selectFeaturedShow(shows)
                
                // Create year range
                val years = shows.mapNotNull { it.year?.toIntOrNull() }.sorted()
                val yearsSpan = if (years.isNotEmpty()) {
                    years.first()..years.last()
                } else null
                
                val todayInHistory = TodayInHistory(
                    dateFormatted = dateFormatted,
                    monthDay = monthDay,
                    shows = shows,
                    totalShows = shows.size,
                    yearsSpan = yearsSpan,
                    featuredShow = featuredShow,
                    statistics = statistics
                )
                
                _uiState.value = TodayInHistoryState.Success(todayInHistory)
                
            } catch (e: Exception) {
                _uiState.value = TodayInHistoryState.Error(
                    "Failed to load today's history: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Load shows for a specific month/day.
     * @param monthDay Date in MM-dd format (e.g., "08-09")
     */
    fun loadHistoryForDate(monthDay: String) {
        viewModelScope.launch {
            try {
                _uiState.value = TodayInHistoryState.Loading
                
                val shows = todayInHistoryRepository.getShowsByMonthDay(monthDay)
                
                if (shows.isEmpty()) {
                    _uiState.value = TodayInHistoryState.NoShows
                    return@launch
                }
                
                // Get statistics for this date
                val statisticsMap = todayInHistoryRepository.getHistoryStatistics(monthDay)
                
                val statistics = HistoryStatistics(
                    totalShows = statisticsMap["totalShows"] as Int,
                    yearsWithShows = statisticsMap["yearsWithShows"] as List<String>,
                    uniqueVenues = statisticsMap["uniqueVenues"] as List<String>,
                    venueCount = statisticsMap["venueCount"] as Int,
                    yearRange = Pair(
                        statisticsMap["earliestYear"] as String?,
                        statisticsMap["latestYear"] as String?
                    )
                )
                
                val featuredShow = selectFeaturedShow(shows)
                
                val years = shows.mapNotNull { it.year?.toIntOrNull() }.sorted()
                val yearsSpan = if (years.isNotEmpty()) {
                    years.first()..years.last()
                } else null
                
                val dateFormatted = formatMonthDay(monthDay)
                
                val todayInHistory = TodayInHistory(
                    dateFormatted = dateFormatted,
                    monthDay = monthDay,
                    shows = shows,
                    totalShows = shows.size,
                    yearsSpan = yearsSpan,
                    featuredShow = featuredShow,
                    statistics = statistics
                )
                
                _uiState.value = TodayInHistoryState.Success(todayInHistory)
                
            } catch (e: Exception) {
                _uiState.value = TodayInHistoryState.Error(
                    "Failed to load history for date: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * Change the display mode for the history view.
     */
    fun setDisplayMode(mode: HistoryDisplayMode) {
        _displayMode.value = mode
    }
    
    /**
     * Refresh today's history data.
     */
    fun refresh() {
        loadTodaysHistory()
    }
    
    /**
     * Get a new featured show (useful for "shuffle" functionality).
     */
    fun shuffleFeaturedShow() {
        val currentState = _uiState.value
        if (currentState is TodayInHistoryState.Success) {
            val shows = currentState.todayInHistory.shows
            if (shows.size > 1) {
                val newFeaturedShow = shows.filter { 
                    it != currentState.todayInHistory.featuredShow 
                }.randomOrNull()
                
                if (newFeaturedShow != null) {
                    val updatedHistory = currentState.todayInHistory.copy(
                        featuredShow = newFeaturedShow
                    )
                    _uiState.value = TodayInHistoryState.Success(updatedHistory)
                }
            }
        }
    }
    
    // Private helper methods
    
    private fun selectFeaturedShow(shows: List<Show>): Show? {
        return when {
            shows.isEmpty() -> null
            shows.size == 1 -> shows.first()
            else -> {
                // Prioritize shows with more recordings, then most recent
                shows.maxWithOrNull(compareBy<Show> { it.recordingCount }.thenBy { it.year })
            }
        }
    }
    
    private fun getCurrentMonthDay(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            java.util.Locale.US, 
            "%02d-%02d", 
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    private fun formatMonthDay(monthDay: String): String {
        return try {
            val parts = monthDay.split("-")
            val month = parts[0].toInt()
            val day = parts[1].toInt()
            
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            
            val dayWithSuffix = when (day % 10) {
                1 -> if (day == 11) "${day}th" else "${day}st"
                2 -> if (day == 12) "${day}th" else "${day}nd"
                3 -> if (day == 13) "${day}th" else "${day}rd"
                else -> "${day}th"
            }
            
            "${monthNames[month - 1]} $dayWithSuffix"
        } catch (e: Exception) {
            "Unknown Date"
        }
    }
}