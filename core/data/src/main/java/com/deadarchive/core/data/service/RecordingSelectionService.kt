package com.deadarchive.core.data.service

import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.RecordingOption
import com.deadarchive.core.model.Show
import com.deadarchive.core.settings.api.model.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized service for recording selection logic.
 * Provides consistent "best recording" selection across all features.
 */
@Singleton
class RecordingSelectionService @Inject constructor() {
    
    companion object {
        private const val TAG = "RecordingSelectionService"
    }
    
    /**
     * Get the best recording for a show using centralized selection logic.
     * This is the single source of truth for recording selection across the app.
     * 
     * @param show The show to select the best recording from
     * @param settings User preferences for recording selection (optional)
     * @return The best recording, or null if no recordings available
     */
    fun getBestRecording(show: Show, settings: AppSettings? = null): Recording? {
        val recordings = show.recordings
        if (recordings.isEmpty()) return null
        if (recordings.size == 1) return recordings.first()
        
        // First, check if there's a specific bestRecordingId set (from user preference or database)
        if (!show.bestRecordingId.isNullOrEmpty()) {
            val preferredRecording = recordings.find { it.identifier == show.bestRecordingId }
            if (preferredRecording != null) {
                android.util.Log.d(TAG, "getBestRecording: Using database/preference bestRecordingId: ${show.bestRecordingId}")
                return preferredRecording
            }
        }
        
        // Use settings-aware selection if settings provided, otherwise fall back to algorithm-based selection
        return if (settings != null) {
            selectBestRecordingWithSettings(recordings, settings)
        } else {
            selectBestRecordingAlgorithmic(recordings)
        }
    }
    
    /**
     * Selects the best recording from a list based on user preferences
     */
    fun selectBestRecording(
        recordings: List<Recording>, 
        settings: AppSettings
    ): Recording? {
        if (recordings.isEmpty()) return null
        if (recordings.size == 1) return recordings.first()
        
        return selectBestRecordingWithSettings(recordings, settings)
    }
    
    /**
     * Settings-aware recording selection that respects user preferences
     */
    private fun selectBestRecordingWithSettings(recordings: List<Recording>, settings: AppSettings): Recording? {
        val filteredRecordings = applyFilters(recordings, settings)
        val recordingsToUse = if (filteredRecordings.isEmpty()) {
            // If filters are too restrictive, fall back to original list
            recordings
        } else {
            filteredRecordings
        }
        
        return selectFromRecordings(recordingsToUse, settings)
    }
    
    /**
     * Gets alternative recording options with recommendation info
     */
    fun getRecordingOptions(
        recordings: List<Recording>,
        currentRecording: Recording,
        settings: AppSettings,
        ratingsBestRecordingId: String? = null
    ): List<RecordingOption> {
        android.util.Log.d(TAG, "getRecordingOptions: recommendedRecordingId = $ratingsBestRecordingId")
        android.util.Log.d(TAG, "getRecordingOptions: currentRecording = ${currentRecording.identifier}")
        android.util.Log.d(TAG, "getRecordingOptions: total recordings = ${recordings.size}")
        
        // Check if the recommended recording actually exists in the available recordings  
        val recommendedExists = ratingsBestRecordingId != null && 
            recordings.any { it.identifier == ratingsBestRecordingId }
        android.util.Log.d(TAG, "getRecordingOptions: recommendedExists = $recommendedExists")
        
        return recordings
            .filter { it.identifier != currentRecording.identifier }
            .map { recording ->
                val isRecommended = isRecordingRecommended(recording, settings)
                val isShowRecommended = recommendedExists && recording.identifier == ratingsBestRecordingId
                val matchReason = getMatchReason(recording, settings, isShowRecommended)
                
                android.util.Log.d(TAG, "Recording ${recording.identifier}: isRecommended=$isRecommended, isShowRecommended=$isShowRecommended, matchReason='$matchReason'")
                
                RecordingOption(
                    recording = recording,
                    isRecommended = isRecommended || isShowRecommended,
                    matchReason = matchReason
                )
            }
            .sortedWith(
                compareByDescending<RecordingOption> { it.matchReason == "Recommended" } // Ratings-based best first
                    .thenByDescending { it.isRecommended }
                    .thenByDescending { it.recording.rawRating ?: 0f }
                    .thenBy { getSourcePriority(it.recording.source, settings) }
            )
    }
    
    /**
     * Algorithm-based recording selection (original Show.bestRecording logic)
     * Used when no user settings are available
     */
    private fun selectBestRecordingAlgorithmic(recordings: List<Recording>): Recording? {
        return recordings.minByOrNull { recording ->
            // Multi-tier priority system for selecting the best recording:
            //
            // Tier 1: Rating Status (most important)
            //   - Rated recordings always preferred over unrated ones
            //   - Example: 2.5★ SBD beats unrated SBD
            //
            // Tier 2: Source Type (within same rating tier)  
            //   - SBD > MATRIX > FM > AUD > Unknown
            //   - Example: Rated SBD beats rated AUD
            //
            // Tier 3: Rating Value (tie-breaker)
            //   - Higher rating wins within same rating+source group
            //   - Example: 4.2★ SBD beats 3.1★ SBD
            
            val ratingPriority = if (recording.hasRawRating) 0 else 1 // Rated recordings first
            
            val sourcePriority = when (recording.cleanSource?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2  
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
            
            // Small rating bonus for tie-breaking (inverted since minByOrNull picks smallest)
            val ratingValue = recording.rawRating ?: 0f
            val ratingBonus = (5f - ratingValue) / 10f // 0.0-0.4 range
            
            // Combined score: rating status dominates, then source, then rating value
            ratingPriority * 10 + sourcePriority + ratingBonus
        }
    }
    
    private fun applyFilters(recordings: List<Recording>, settings: AppSettings): List<Recording> {
        return recordings.filter { recording ->
            // Apply minimum rating filter
            val meetsRatingRequirement = settings.minimumRating <= 0f || 
                (recording.rawRating ?: 0f) >= settings.minimumRating
            
            // Apply source preference filter (only filter if specific preference is set)
            val meetsSourceRequirement = settings.preferredAudioSource == "Any" ||
                matchesSourcePreference(recording.source, settings.preferredAudioSource)
            
            meetsRatingRequirement && meetsSourceRequirement
        }
    }
    
    private fun selectFromRecordings(recordings: List<Recording>, settings: AppSettings): Recording {
        return if (settings.preferHigherRated) {
            // Sort by rating first, then by source preference
            recordings.maxWithOrNull(
                compareBy<Recording> { it.rawRating ?: 0f }
                    .thenBy { getSourcePriority(it.source, settings) }
            ) ?: recordings.first()
        } else {
            // Sort by source preference first, then by rating
            recordings.minWithOrNull(
                compareBy<Recording> { getSourcePriority(it.source, settings) }
                    .thenByDescending { it.rawRating ?: 0f }
            ) ?: recordings.first()
        }
    }
    
    private fun matchesSourcePreference(source: String?, preference: String): Boolean {
        if (source == null || preference == "Any") return true
        
        return when (preference) {
            "Soundboard" -> source.contains("SBD", ignoreCase = true) || 
                          source.contains("Soundboard", ignoreCase = true) ||
                          source.contains("Board", ignoreCase = true)
            "Audience" -> source.contains("AUD", ignoreCase = true) || 
                         source.contains("Audience", ignoreCase = true)
            else -> true
        }
    }
    
    private fun getSourcePriority(source: String?, settings: AppSettings): Int {
        if (source == null) return 999
        
        return when {
            settings.preferredAudioSource == "Soundboard" && 
                matchesSourcePreference(source, "Soundboard") -> 1
            settings.preferredAudioSource == "Audience" && 
                matchesSourcePreference(source, "Audience") -> 1
            matchesSourcePreference(source, "Soundboard") -> 2
            matchesSourcePreference(source, "Audience") -> 3
            else -> 4
        }
    }
    
    private fun isRecordingRecommended(recording: Recording, settings: AppSettings): Boolean {
        val hasGoodRating = (recording.rawRating ?: 0f) >= 4.0f
        val matchesSourcePreference = settings.preferredAudioSource == "Any" ||
            matchesSourcePreference(recording.source, settings.preferredAudioSource)
        val meetsMinRating = (recording.rawRating ?: 0f) >= settings.minimumRating
        
        return hasGoodRating && matchesSourcePreference && meetsMinRating
    }
    
    private fun getMatchReason(recording: Recording, settings: AppSettings, isShowRecommended: Boolean = false): String? {
        val reasons = mutableListOf<String>()
        
        // Prioritize show-recommended recording
        if (isShowRecommended) {
            reasons.add("Recommended")
            return reasons.first()
        }
        
        // Check rating
        val rating = recording.rawRating ?: 0f
        when {
            rating >= 4.5f -> {
                reasons.add("Excellent Rating")
                // Also mark highest rated as recommended if no explicit ratings-based best recording
                if (rating >= 4.8f) reasons.add("Top Rated")
            }
            rating >= 4.0f -> reasons.add("Great Rating")
            rating >= 3.5f && settings.minimumRating <= 3.5f -> reasons.add("Good Rating")
        }
        
        // Check source match
        if (settings.preferredAudioSource != "Any" && 
            matchesSourcePreference(recording.source, settings.preferredAudioSource)) {
            reasons.add("Matches ${settings.preferredAudioSource} Preference")
        }
        
        return reasons.firstOrNull()
    }
}