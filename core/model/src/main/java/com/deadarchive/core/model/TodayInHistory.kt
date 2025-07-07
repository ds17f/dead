package com.deadarchive.core.model

/**
 * Data class representing "Today in Grateful Dead History" information.
 * Contains shows and metadata for a specific date across multiple years.
 */
data class TodayInHistory(
    val dateFormatted: String, // e.g., "August 9th"
    val monthDay: String,      // e.g., "08-09" 
    val shows: List<Show>,
    val totalShows: Int,
    val yearsSpan: IntRange?,  // e.g., 1965..1995
    val featuredShow: Show?,   // Highlighted show for the day
    val statistics: HistoryStatistics
) {
    
    /**
     * Check if there are shows for this date.
     */
    val hasShows: Boolean
        get() = shows.isNotEmpty()
    
    /**
     * Get shows grouped by decade for display.
     */
    val showsByDecade: Map<String, List<Show>>
        get() = shows.groupBy { show ->
            val year = show.year?.toIntOrNull() ?: 0
            val decade = (year / 10) * 10
            "${decade}s"
        }
    
    /**
     * Get the most recent show for this date.
     */
    val mostRecentShow: Show?
        get() = shows.maxByOrNull { it.year?.toIntOrNull() ?: 0 }
    
    /**
     * Get the earliest show for this date.
     */
    val earliestShow: Show?
        get() = shows.minByOrNull { it.year?.toIntOrNull() ?: 0 }
}

/**
 * Statistics about shows for a particular date in history.
 */
data class HistoryStatistics(
    val totalShows: Int,
    val yearsWithShows: List<String>,
    val uniqueVenues: List<String>,
    val venueCount: Int,
    val yearRange: Pair<String?, String?> // earliest to latest year
) {
    
    /**
     * Get a summary string for display.
     */
    val summaryText: String
        get() = when {
            totalShows == 0 -> "No shows found for this date"
            totalShows == 1 -> "1 show found"
            else -> "$totalShows shows across ${yearsWithShows.size} years"
        }
    
    /**
     * Get venue diversity description.
     */
    val venueDescription: String
        get() = when {
            uniqueVenues.isEmpty() -> "No venues"
            uniqueVenues.size == 1 -> "at ${uniqueVenues.first()}"
            uniqueVenues.size <= 3 -> "at ${uniqueVenues.joinToString(", ")}"
            else -> "at ${uniqueVenues.size} different venues"
        }
}

/**
 * State for Today in History UI.
 */
sealed class TodayInHistoryState {
    object Loading : TodayInHistoryState()
    data class Success(val todayInHistory: TodayInHistory) : TodayInHistoryState()
    data class Error(val message: String) : TodayInHistoryState()
    object NoShows : TodayInHistoryState()
}

/**
 * Different display modes for Today in History.
 */
enum class HistoryDisplayMode {
    COMPACT,    // Single featured show
    SUMMARY,    // Brief list with statistics
    DETAILED    // Full list with year grouping
}