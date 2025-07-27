package com.deadarchive.core.settings.api

import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
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
     * Update the debug info visibility setting
     * @param showDebugInfo True to show debug panels, false to hide them
     */
    suspend fun updateShowDebugInfo(showDebugInfo: Boolean)
    
    /**
     * Reset all settings to their default values
     */
    suspend fun resetToDefaults()
    
    /**
     * Update the deletion grace period for soft delete
     * @param days Grace period in days before permanent deletion
     */
    suspend fun updateDeletionGracePeriod(days: Int)
    
    /**
     * Update the low storage threshold for cleanup triggering
     * @param thresholdMB Threshold in megabytes below which cleanup is triggered
     */
    suspend fun updateLowStorageThreshold(thresholdMB: Long)
    
    /**
     * Update the preferred audio source for recording selection
     * @param source Preferred audio source (Soundboard, Audience, Any)
     */
    suspend fun updatePreferredAudioSource(source: String)
    
    /**
     * Update the minimum rating filter for recording selection
     * @param rating Minimum rating (0-5)
     */
    suspend fun updateMinimumRating(rating: Float)
    
    /**
     * Update the prefer higher rated setting
     * @param prefer True to prefer higher rated recordings, false otherwise
     */
    suspend fun updatePreferHigherRated(prefer: Boolean)
    
    /**
     * Update recording preference for a specific show
     * @param showId The show identifier
     * @param recordingId The preferred recording identifier
     */
    suspend fun updateRecordingPreference(showId: String, recordingId: String)
    
    /**
     * Get the preferred recording ID for a specific show
     * @param showId The show identifier
     * @return The preferred recording ID, or null if no preference is set
     */
    suspend fun getRecordingPreference(showId: String): String?
    
    /**
     * Remove the recording preference for a specific show
     * @param showId The show identifier
     */
    suspend fun removeRecordingPreference(showId: String)
    
    /**
     * Update the resume last track setting
     * @param enabled True to enable resuming last track on app startup, false to disable
     */
    suspend fun updateEnableResumeLastTrack(enabled: Boolean)
    
    /**
     * Update the Library V2 interface setting
     * @param enabled True to use Library V2 interface, false to use legacy library
     */
    suspend fun updateUseLibraryV2(enabled: Boolean)
}