package com.deadarchive.core.settings.data

import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.model.RepeatMode
import com.deadarchive.core.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for application settings management
 * Provides centralized access to user preferences with reactive updates
 */
interface SettingsRepository {
    
    /**
     * Observe all application settings as a reactive stream
     * @return Flow of current AppSettings that updates whenever any setting changes
     */
    fun getSettings(): Flow<AppSettings>
    
    /**
     * Update the audio format preference order
     * @param formatOrder List of audio formats in priority order
     */
    suspend fun updateAudioFormatPreference(formatOrder: List<String>)
    
    /**
     * Update the application theme mode
     * @param themeMode Selected theme mode (Light, Dark, or System)
     */
    suspend fun updateThemeMode(themeMode: ThemeMode)
    
    /**
     * Update the download WiFi-only setting
     * @param wifiOnly True to download only on WiFi, false to allow cellular
     */
    suspend fun updateDownloadWifiOnly(wifiOnly: Boolean)
    
    /**
     * Update the media player repeat mode
     * @param repeatMode Selected repeat mode (Off, One, or All)
     */
    suspend fun updateRepeatMode(repeatMode: RepeatMode)
    
    /**
     * Update the media player shuffle setting
     * @param enabled True to enable shuffle, false to disable
     */
    suspend fun updateShuffleEnabled(enabled: Boolean)
    
    /**
     * Reset all settings to their default values
     */
    suspend fun resetToDefaults()
}