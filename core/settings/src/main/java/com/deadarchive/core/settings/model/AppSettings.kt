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
    val showDebugInfo: Boolean = false
)

/**
 * Theme mode options for the application
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

