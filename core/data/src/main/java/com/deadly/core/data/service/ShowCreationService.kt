package com.deadly.core.data.service

import com.deadly.core.model.Show
import com.deadly.core.model.Recording

/**
 * Service responsible for creating new show entities from recordings
 * with proper date and venue normalization.
 */
interface ShowCreationService {
    
    /**
     * Creates and saves show entities from a list of recordings.
     * Groups recordings by normalized date + venue and creates ShowEntity records.
     * This is the canonical method for creating shows from recordings.
     */
    suspend fun createAndSaveShowsFromRecordings(recordings: List<Recording>): List<Show>
    
    /**
     * Normalizes a date from potentially timestamped format to simple YYYY-MM-DD format.
     */
    fun normalizeDate(date: String?): String
    
    /**
     * Groups recordings by their show ID (normalized date + venue).
     */
    fun groupRecordingsByShow(recordings: List<Recording>): Map<String, List<Recording>>
    
    /**
     * Creates ShowEntity objects from grouped recordings without saving them.
     * Used for validation and preview purposes.
     */
    suspend fun createShowEntities(groupedRecordings: Map<String, List<Recording>>): List<com.deadly.core.database.ShowEntity>
}