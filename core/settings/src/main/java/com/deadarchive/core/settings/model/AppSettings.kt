package com.deadarchive.core.settings.model

import com.deadarchive.core.model.AppConstants

/**
 * Application-wide settings data class
 * Provides centralized configuration for user preferences
 */
data class AppSettings(
    val audioFormatPreference: List<String> = AppConstants.PREFERRED_AUDIO_FORMATS,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val downloadOnWifiOnly: Boolean = true,
    val showDebugInfo: Boolean = false,
    // Soft delete settings
    val deletionGracePeriodDays: Int = 7,
    val lowStorageThresholdMB: Long = 500L,
    // Recording preferences - maps showId to preferred recordingId
    val recordingPreferences: Map<String, String> = emptyMap(),
    // Recording selection criteria preferences
    val preferredAudioSource: String = "Any", // Soundboard, Audience, Any
    val minimumRating: Float = 0f, // Minimum rating filter (0-5)
    val preferHigherRated: Boolean = true // Prefer higher rated recordings when multiple options
)

/**
 * Theme mode options for the application
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

