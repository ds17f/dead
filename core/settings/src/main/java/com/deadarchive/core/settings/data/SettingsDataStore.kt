package com.deadarchive.core.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.deadarchive.core.model.AppConstants
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * DataStore wrapper for application settings
 * Provides type-safe access to persistent user preferences
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Preference keys based on AppConstants
    private val audioFormatPreferenceKey = stringPreferencesKey(AppConstants.PREF_AUDIO_QUALITY)
    private val themeModeKey = stringPreferencesKey(AppConstants.PREF_THEME_MODE)
    private val downloadWifiOnlyKey = booleanPreferencesKey(AppConstants.PREF_DOWNLOAD_WIFI_ONLY)
    private val showDebugInfoKey = booleanPreferencesKey(AppConstants.PREF_SHOW_DEBUG_INFO)
    private val deletionGracePeriodKey = intPreferencesKey("deletion_grace_period_days")
    private val lowStorageThresholdKey = longPreferencesKey("low_storage_threshold_mb")
    private val preferredAudioSourceKey = stringPreferencesKey("preferred_audio_source")
    private val minimumRatingKey = stringPreferencesKey("minimum_rating") // Store as string to preserve precision
    private val preferHigherRatedKey = booleanPreferencesKey("prefer_higher_rated")
    private val recordingPreferencesKey = stringPreferencesKey("recording_preferences") // JSON string of showId -> recordingId map
    private val enableResumeLastTrackKey = booleanPreferencesKey("enable_resume_last_track")
    private val useLibraryV2Key = booleanPreferencesKey("use_library_v2")
    private val usePlayerV2Key = booleanPreferencesKey("use_player_v2")
    private val useSearchV2Key = booleanPreferencesKey("use_search_v2")
    private val useHomeV2Key = booleanPreferencesKey("use_home_v2")
    private val usePlaylistV2Key = booleanPreferencesKey("use_playlist_v2")
    
    // Update-related preference keys
    private val autoUpdateCheckEnabledKey = booleanPreferencesKey("auto_update_check_enabled")
    private val lastUpdateCheckTimestampKey = longPreferencesKey("last_update_check_timestamp")
    private val skippedVersionsKey = stringPreferencesKey("skipped_versions") // Comma-separated string
    
    /**
     * Reactive flow of application settings
     */
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { _ ->
            // If there's an error reading preferences, emit empty preferences
            emit(emptyPreferences())
        }
        .map { preferences ->
            preferences.toAppSettings()
        }
    
    /**
     * Update audio format preference order
     */
    suspend fun updateAudioFormatPreference(formatOrder: List<String>) {
        dataStore.edit { preferences ->
            preferences[audioFormatPreferenceKey] = formatOrder.joinToString(",")
        }
    }
    
    /**
     * Update theme mode setting
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.name
        }
    }
    
    /**
     * Update download WiFi-only setting
     */
    suspend fun updateDownloadWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[downloadWifiOnlyKey] = wifiOnly
        }
    }
    
    /**
     * Update debug info visibility setting
     */
    suspend fun updateShowDebugInfo(showDebugInfo: Boolean) {
        dataStore.edit { preferences ->
            preferences[showDebugInfoKey] = showDebugInfo
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Update deletion grace period setting
     */
    suspend fun updateDeletionGracePeriod(days: Int) {
        dataStore.edit { preferences ->
            preferences[deletionGracePeriodKey] = days
        }
    }
    
    /**
     * Update low storage threshold setting
     */
    suspend fun updateLowStorageThreshold(thresholdMB: Long) {
        dataStore.edit { preferences ->
            preferences[lowStorageThresholdKey] = thresholdMB
        }
    }
    
    /**
     * Update preferred audio source setting
     */
    suspend fun updatePreferredAudioSource(source: String) {
        dataStore.edit { preferences ->
            preferences[preferredAudioSourceKey] = source
        }
    }
    
    /**
     * Update minimum rating filter
     */
    suspend fun updateMinimumRating(rating: Float) {
        dataStore.edit { preferences ->
            preferences[minimumRatingKey] = rating.toString()
        }
    }
    
    /**
     * Update prefer higher rated setting
     */
    suspend fun updatePreferHigherRated(prefer: Boolean) {
        dataStore.edit { preferences ->
            preferences[preferHigherRatedKey] = prefer
        }
    }
    
    /**
     * Update recording preference for a specific show
     */
    suspend fun updateRecordingPreference(showId: String, recordingId: String) {
        dataStore.edit { preferences ->
            // Get current preferences
            val currentPreferencesString = preferences[recordingPreferencesKey] ?: ""
            val currentPreferences = if (currentPreferencesString.isBlank()) {
                emptyMap()
            } else {
                try {
                    currentPreferencesString.split(",")
                        .mapNotNull { pair ->
                            val parts = pair.split(":")
                            if (parts.size == 2) parts[0] to parts[1] else null
                        }.toMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
            
            // Update with new preference
            val updatedPreferences = currentPreferences + (showId to recordingId)
            
            // Convert back to string format
            val updatedPreferencesString = updatedPreferences.entries
                .joinToString(",") { "${it.key}:${it.value}" }
            
            preferences[recordingPreferencesKey] = updatedPreferencesString
        }
    }
    
    /**
     * Remove recording preference for a specific show
     */
    suspend fun removeRecordingPreference(showId: String) {
        dataStore.edit { preferences ->
            // Get current preferences
            val currentPreferencesString = preferences[recordingPreferencesKey] ?: ""
            val currentPreferences = if (currentPreferencesString.isBlank()) {
                emptyMap()
            } else {
                try {
                    currentPreferencesString.split(",")
                        .mapNotNull { pair ->
                            val parts = pair.split(":")
                            if (parts.size == 2) parts[0] to parts[1] else null
                        }.toMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
            
            // Remove the preference for this show
            val updatedPreferences = currentPreferences - showId
            
            // Convert back to string format
            val updatedPreferencesString = if (updatedPreferences.isEmpty()) {
                ""
            } else {
                updatedPreferences.entries.joinToString(",") { "${it.key}:${it.value}" }
            }
            
            preferences[recordingPreferencesKey] = updatedPreferencesString
        }
    }
    
    /**
     * Update enable resume last track setting
     */
    suspend fun updateEnableResumeLastTrack(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[enableResumeLastTrackKey] = enabled
        }
    }
    
    /**
     * Update use Library V2 setting
     */
    suspend fun updateUseLibraryV2(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[useLibraryV2Key] = enabled
        }
    }
    
    /**
     * Update use Player V2 setting
     */
    suspend fun updateUsePlayerV2(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[usePlayerV2Key] = enabled
        }
    }
    
    /**
     * Update use Search V2 setting
     */
    suspend fun updateUseSearchV2(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[useSearchV2Key] = enabled
        }
    }
    
    /**
     * Update use Home V2 setting
     */
    suspend fun updateUseHomeV2(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[useHomeV2Key] = enabled
        }
    }
    
    /**
     * Update use Playlist V2 setting
     */
    suspend fun updateUsePlaylistV2(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[usePlaylistV2Key] = enabled
        }
    }
    
    // Update-related methods
    
    /**
     * Update the timestamp of the last update check
     */
    suspend fun updateLastUpdateCheck(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[lastUpdateCheckTimestampKey] = timestamp
        }
    }
    
    /**
     * Add a version to the skipped versions set
     */
    suspend fun addSkippedVersion(version: String) {
        dataStore.edit { preferences ->
            val currentVersionsString = preferences[skippedVersionsKey] ?: ""
            val currentVersions = if (currentVersionsString.isBlank()) {
                emptySet()
            } else {
                currentVersionsString.split(",").toSet()
            }
            
            val updatedVersions = currentVersions + version
            preferences[skippedVersionsKey] = updatedVersions.joinToString(",")
        }
    }
    
    /**
     * Clear all skipped versions
     */
    suspend fun clearSkippedVersions() {
        dataStore.edit { preferences ->
            preferences[skippedVersionsKey] = ""
        }
    }
    
    /**
     * Set whether automatic update checking is enabled
     */
    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoUpdateCheckEnabledKey] = enabled
        }
    }
    
    /**
     * Convert DataStore preferences to AppSettings
     */
    private fun Preferences.toAppSettings(): AppSettings {
        val audioFormatString = this[audioFormatPreferenceKey] ?: ""
        val audioFormatPreference = if (audioFormatString.isBlank()) {
            AppSettings.defaultAudioFormatPreferences
        } else {
            audioFormatString.split(",").filter { it.isNotBlank() }
        }
        
        val themeModeString = this[themeModeKey] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        
        val minimumRatingString = this[minimumRatingKey] ?: "0"
        val minimumRating = try {
            minimumRatingString.toFloat()
        } catch (e: NumberFormatException) {
            0f
        }
        
        // Parse recording preferences from JSON string
        val recordingPreferencesString = this[recordingPreferencesKey] ?: ""
        val recordingPreferences = if (recordingPreferencesString.isBlank()) {
            emptyMap()
        } else {
            try {
                // Simple parsing: "showId1:recordingId1,showId2:recordingId2"
                recordingPreferencesString.split(",")
                    .mapNotNull { pair ->
                        val parts = pair.split(":")
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
        
        // Parse skipped versions from comma-separated string
        val skippedVersionsString = this[skippedVersionsKey] ?: ""
        val skippedVersions = if (skippedVersionsString.isBlank()) {
            emptySet()
        } else {
            skippedVersionsString.split(",").toSet()
        }
        
        return AppSettings(
            audioFormatPreferences = audioFormatPreference,
            themeMode = themeMode,
            downloadWifiOnly = this[downloadWifiOnlyKey] ?: true,
            showDebugInfo = this[showDebugInfoKey] ?: false,
            deletionGracePeriodDays = this[deletionGracePeriodKey] ?: 7,
            lowStorageThresholdMB = this[lowStorageThresholdKey] ?: 500L,
            recordingPreferences = recordingPreferences,
            preferredAudioSource = this[preferredAudioSourceKey] ?: "Any",
            minimumRating = minimumRating,
            preferHigherRated = this[preferHigherRatedKey] ?: true,
            enableResumeLastTrack = this[enableResumeLastTrackKey] ?: true,
            useLibraryV2 = this[useLibraryV2Key] ?: false,
            usePlayerV2 = this[usePlayerV2Key] ?: false,
            useSearchV2 = this[useSearchV2Key] ?: false,
            useHomeV2 = this[useHomeV2Key] ?: false,
            usePlaylistV2 = this[usePlaylistV2Key] ?: false,
            autoUpdateCheckEnabled = this[autoUpdateCheckEnabledKey] ?: true, 
            lastUpdateCheckTimestamp = this[lastUpdateCheckTimestampKey] ?: 0L,
            skippedVersions = skippedVersions
        )
    }
}