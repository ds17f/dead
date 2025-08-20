package com.deadly.core.data.service

import com.deadly.core.model.Show
import com.deadly.core.model.Recording

/**
 * Service responsible for enriching shows and recordings with additional data
 * such as ratings, user preferences, and recording attachments.
 */
interface ShowEnrichmentService {
    
    /**
     * Enriches a show with ratings information and user preferences.
     * Attaches recordings and applies user recording preferences.
     */
    suspend fun enrichShowWithRatings(
        showEntity: com.deadly.core.database.ShowEntity,
        userPreferences: Map<String, String> = emptyMap()
    ): Show
    
    /**
     * Enriches a recording with rating information.
     */
    suspend fun enrichRecordingWithRating(recording: Recording): Recording
    
    /**
     * Gets and attaches recordings to a show entity.
     */
    suspend fun attachRecordingsToShow(showId: String): List<Recording>
    
    /**
     * Applies user recording preferences to determine the best recording for a show.
     */
    fun applyUserPreferences(
        recordings: List<Recording>,
        showId: String,
        userPreferences: Map<String, String>,
        ratingBasedBestRecordingId: String?
    ): String?
}