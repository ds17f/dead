package com.deadarchive.core.model

/**
 * Complete information about the currently playing track.
 * Includes denormalized show and track data for immediate display use.
 */
data class CurrentTrackInfo(
    // Track identification
    val trackUrl: String,
    val recordingId: String,
    val showId: String,
    
    // Denormalized show data for immediate display
    val showDate: String,           // e.g., "1977-05-08"
    val venue: String?,             // e.g., "Barton Hall"
    val location: String?,          // e.g., "Cornell University, Ithaca, NY"
    
    // Track-specific data
    val songTitle: String,          // e.g., "Scarlet Begonias"
    val trackNumber: Int?,          // e.g., 5
    val filename: String,           // Original filename
    
    // Playback state
    val isPlaying: Boolean,
    val position: Long,             // Current position in milliseconds
    val duration: Long              // Track duration in milliseconds
) {
    companion object {
        /**
         * Create CurrentTrackInfo from existing models
         */
        fun fromModels(
            trackUrl: String,
            show: Show,
            recording: Recording,
            track: Track,
            isPlaying: Boolean = false,
            position: Long = 0L,
            duration: Long = 0L
        ): CurrentTrackInfo {
            return CurrentTrackInfo(
                trackUrl = trackUrl,
                recordingId = recording.identifier,
                showId = show.showId,
                showDate = show.displayDate,
                venue = show.venue,
                location = show.location,
                songTitle = track.displayTitle,
                trackNumber = track.trackNumber?.toIntOrNull(),
                filename = track.filename,
                isPlaying = isPlaying,
                position = position,
                duration = duration
            )
        }
    }
    /**
     * Formatted display title for notifications - just the song title
     * Format: "Song Title"
     */
    val displayTitle: String
        get() = songTitle
    
    /**
     * Formatted show date for display
     * Format: "Jul 17, 1976"
     */
    val displayDate: String
        get() = formatShowDate(showDate)
    
    /**
     * Formatted subtitle with date and venue for notifications
     * Format: "Date - Venue"
     */
    val displaySubtitle: String
        get() = buildString {
            append(formatShowDate(showDate))
            if (!venue.isNullOrBlank()) {
                append(" - ")
                append(venue)
            }
        }
    
    /**
     * Formatted artist line for notifications - combines date, venue, and location
     * Format: "Date - Venue - City, State" (optimized for 2-line notifications)
     */
    val displayArtist: String
        get() = buildString {
            append(formatShowDate(showDate))
            if (!venue.isNullOrBlank()) {
                append(" - ")
                append(venue)
            }
            if (!location.isNullOrBlank()) {
                append(" - ")
                append(location)
            }
        }
    
    /**
     * Album title for media metadata
     * Format: "Show Date - Venue" or just show date if no venue
     */
    val albumTitle: String
        get() = if (!venue.isNullOrBlank()) {
            "${formatShowDate(showDate)} - $venue"
        } else {
            formatShowDate(showDate)
        }
    
    private fun formatShowDate(dateString: String): String {
        return try {
            // Convert from YYYY-MM-DD to more readable format
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                
                "${monthNames[month - 1]} $day, $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}