package com.deadarchive.core.settings.api.model

/**
 * Data class representing all application settings.
 * This is a pure data model that can be shared across modules.
 */
data class AppSettings(
    // Audio format preferences
    val audioFormatPreferences: List<String> = defaultAudioFormatPreferences,
    
    // Recording preferences for specific shows (showId -> recordingId)
    val recordingPreferences: Map<String, String> = emptyMap(),
    
    // Theme preferences
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    
    // Download preferences
    val downloadWifiOnly: Boolean = true,
    
    // Debug options
    val showDebugInfo: Boolean = false,
    
    // Data management settings
    val deletionGracePeriodDays: Int = 7,
    val lowStorageThresholdMB: Long = 500,
    
    // Audio source preferences
    val preferredAudioSource: String = "Any",
    val minimumRating: Float = 3.0f,
    val preferHigherRated: Boolean = true,
    
    // Playback settings
    val enableResumeLastTrack: Boolean = true
) {
    companion object {
        // Default order for audio format preferences
        val defaultAudioFormatPreferences = listOf(
            "flac",
            "mp3",
            "ogg",
            "m4a",
            "wav"
        )
    }
}

/**
 * Enum representing theme mode options
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}