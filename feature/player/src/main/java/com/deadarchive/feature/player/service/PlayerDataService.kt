package com.deadarchive.feature.player.service

import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show

/**
 * Service responsible for loading recording data and managing current recording state.
 * Handles data fetching, format filtering, and recording metadata.
 */
interface PlayerDataService {
    
    /**
     * Load a recording by ID with format filtering applied
     * @param recordingId The recording identifier
     * @param formatPreferences User's preferred audio formats
     * @return The loaded recording with filtered tracks, or null if not found
     */
    suspend fun loadRecording(recordingId: String, formatPreferences: List<String>): Recording?
    
    /**
     * Generate a show ID from recording data
     * @param recording The recording to generate show ID for
     * @return Generated show ID string
     */
    fun generateShowId(recording: Recording): String
    
    /**
     * Get alternative recordings for the same show as the current recording
     * @param currentRecording The recording to find alternatives for
     * @return List of alternative recordings
     */
    suspend fun getAlternativeRecordings(currentRecording: Recording): List<Recording>
    
    /**
     * Find the next show by date from the current recording
     * @param currentRecording The current recording
     * @return Next show, or null if none found
     */
    suspend fun findNextShowByDate(currentRecording: Recording): Show?
    
    /**
     * Find the previous show by date from the current recording
     * @param currentRecording The current recording
     * @return Previous show, or null if none found
     */
    suspend fun findPreviousShowByDate(currentRecording: Recording): Show?
    
    /**
     * Get the best recording for a show based on ratings
     * @param show The show to get best recording for
     * @return Best recording for the show, or null if none found
     */
    suspend fun getBestRecordingForShow(show: Show): Recording?
    
    /**
     * Get the best recording for a show by show ID
     * @param showId The show identifier
     * @return Best recording for the show, or null if none found
     */
    suspend fun getBestRecordingByShowId(showId: String): Recording?
}