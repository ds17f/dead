package com.deadly.feature.playlist.data

/**
 * Represents a user's recording preference for a specific show
 */
data class RecordingPreference(
    val showId: String,
    val preferredRecordingId: String,
    val setByUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Container for all recording preferences
 */
data class RecordingPreferences(
    val preferences: Map<String, RecordingPreference> = emptyMap()
) {
    fun getPreferredRecording(showId: String): String? {
        return preferences[showId]?.preferredRecordingId
    }
    
    fun setPreference(showId: String, recordingId: String): RecordingPreferences {
        val newPreference = RecordingPreference(
            showId = showId,
            preferredRecordingId = recordingId,
            setByUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        return copy(
            preferences = preferences + (showId to newPreference)
        )
    }
    
    fun removePreference(showId: String): RecordingPreferences {
        return copy(
            preferences = preferences - showId
        )
    }
}