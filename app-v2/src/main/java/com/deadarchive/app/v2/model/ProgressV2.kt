package com.deadarchive.app.v2.model

/**
 * V2 database initialization progress tracking
 */
data class ProgressV2(
    val phase: PhaseV2,
    val totalShows: Int,
    val processedShows: Int,
    val currentShow: String,
    val totalVenues: Int = 0,
    val processedVenues: Int = 0,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (totalShows > 0) (processedShows.toFloat() / totalShows) * 100f else 0f
        
    val isInProgress: Boolean
        get() = phase in listOf(PhaseV2.CHECKING, PhaseV2.EXTRACTING, PhaseV2.IMPORTING_SHOWS, PhaseV2.COMPUTING_VENUES)
}