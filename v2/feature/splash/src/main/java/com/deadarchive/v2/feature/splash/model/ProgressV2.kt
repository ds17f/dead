package com.deadarchive.v2.feature.splash.model

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
    val totalRecordings: Int = 0,
    val processedRecordings: Int = 0,
    val currentRecording: String = "",
    val totalTracks: Int = 0,
    val processedTracks: Int = 0,
    val startTimeMs: Long = 0L,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = when (phase) {
            PhaseV2.IMPORTING_SHOWS -> if (totalShows > 0) (processedShows.toFloat() / totalShows) * 100f else 0f
            PhaseV2.IMPORTING_RECORDINGS -> if (totalRecordings > 0) (processedRecordings.toFloat() / totalRecordings) * 100f else 0f
            PhaseV2.COMPUTING_VENUES -> if (totalVenues > 0) (processedVenues.toFloat() / totalVenues) * 100f else 0f
            else -> 0f
        }
        
    val currentItem: String
        get() = when (phase) {
            PhaseV2.IMPORTING_SHOWS -> currentShow
            PhaseV2.IMPORTING_RECORDINGS -> currentRecording
            else -> currentShow // fallback
        }
        
    val isInProgress: Boolean
        get() = phase in listOf(PhaseV2.CHECKING, PhaseV2.EXTRACTING, PhaseV2.IMPORTING_SHOWS, PhaseV2.COMPUTING_VENUES, PhaseV2.IMPORTING_RECORDINGS)
        
    /**
     * Get elapsed time since start in a human-readable format
     */
    fun getElapsedTimeString(currentTimeMs: Long = System.currentTimeMillis()): String {
        if (startTimeMs == 0L) return "00:00"
        
        val elapsedMs = currentTimeMs - startTimeMs
        val totalSeconds = elapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        
        return String.format("%02d:%02d", minutes, seconds)
    }
}