package com.deadarchive.feature.playlist.data

import com.deadarchive.core.model.Recording
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.feature.playlist.components.RecordingOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingSelectionService @Inject constructor() {
    
    /**
     * Selects the best recording from a list based on user preferences
     */
    fun selectBestRecording(
        recordings: List<Recording>, 
        settings: AppSettings
    ): Recording? {
        if (recordings.isEmpty()) return null
        if (recordings.size == 1) return recordings.first()
        
        val filteredRecordings = applyFilters(recordings, settings)
        if (filteredRecordings.isEmpty()) {
            // If filters are too restrictive, fall back to original list
            return selectFromRecordings(recordings, settings)
        }
        
        return selectFromRecordings(filteredRecordings, settings)
    }
    
    /**
     * Gets alternative recording options with recommendation info
     */
    fun getRecordingOptions(
        recordings: List<Recording>,
        currentRecording: Recording,
        settings: AppSettings
    ): List<RecordingOption> {
        return recordings
            .filter { it.identifier != currentRecording.identifier }
            .map { recording ->
                val isRecommended = isRecordingRecommended(recording, settings)
                val matchReason = getMatchReason(recording, settings)
                
                RecordingOption(
                    recording = recording,
                    isRecommended = isRecommended,
                    matchReason = matchReason
                )
            }
            .sortedWith(
                compareByDescending<RecordingOption> { it.isRecommended }
                    .thenByDescending { it.recording.rawRating ?: 0f }
                    .thenBy { getSourcePriority(it.recording.source, settings) }
            )
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
    
    private fun isRecordingRecommended(recording: Recording, settings: AppSettings): Boolean {
        val hasGoodRating = (recording.rawRating ?: 0f) >= 4.0f
        val matchesSourcePreference = settings.preferredAudioSource == "Any" ||
            matchesSourcePreference(recording.source, settings.preferredAudioSource)
        val meetsMinRating = (recording.rawRating ?: 0f) >= settings.minimumRating
        
        return hasGoodRating && matchesSourcePreference && meetsMinRating
    }
    
    private fun getMatchReason(recording: Recording, settings: AppSettings): String? {
        val reasons = mutableListOf<String>()
        
        // Check rating
        val rating = recording.rawRating ?: 0f
        when {
            rating >= 4.5f -> reasons.add("Excellent Rating")
            rating >= 4.0f -> reasons.add("Great Rating")
            rating >= 3.5f && settings.minimumRating <= 3.5f -> reasons.add("Good Rating")
        }
        
        // Check source match
        if (settings.preferredAudioSource != "Any" && 
            matchesSourcePreference(recording.source, settings.preferredAudioSource)) {
            reasons.add("Matches ${settings.preferredAudioSource} Preference")
        }
        
        // Check if it's the highest rated
        // Note: This would need the full list to determine, so we'll skip for now
        
        return reasons.firstOrNull()
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
}