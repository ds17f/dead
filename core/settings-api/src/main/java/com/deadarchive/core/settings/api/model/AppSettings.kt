package com.deadarchive.core.settings.api.model

import kotlinx.serialization.Serializable

/**
 * Data class representing all application settings.
 * This is a pure data model that can be shared across modules.
 */
@Serializable
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
    val enableResumeLastTrack: Boolean = true,
    
    // UI settings
    val useLibraryV2: Boolean = false,
    val usePlayerV2: Boolean = false,
    val useSearchV2: Boolean = false,
    val useHomeV2: Boolean = false,
    val usePlaylistV2: Boolean = false,
    val useMiniPlayerV2: Boolean = false,
    val useSplashV2: Boolean = false,
    
    // Update settings
    val autoUpdateCheckEnabled: Boolean = true,
    val lastUpdateCheckTimestamp: Long = 0L,
    val skippedVersions: Set<String> = emptySet()
) {
    companion object {
        // Default order for audio format preferences - matches AppConstants.PREFERRED_AUDIO_FORMATS
        val defaultAudioFormatPreferences = listOf(
            "VBR MP3",
            "Ogg Vorbis", 
            "MP3",
            "Flac"
        )
    }
}

/**
 * Enum representing theme mode options
 */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;
    
    val displayName: String
        get() = when(this) {
            LIGHT -> "Light"
            DARK -> "Dark"
            SYSTEM -> "System Default"
        }
}