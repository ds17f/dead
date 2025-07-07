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
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.model.ThemeMode
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
     * Convert DataStore preferences to AppSettings
     */
    private fun Preferences.toAppSettings(): AppSettings {
        val audioFormatString = this[audioFormatPreferenceKey] ?: ""
        val audioFormatPreference = if (audioFormatString.isBlank()) {
            AppConstants.PREFERRED_AUDIO_FORMATS
        } else {
            audioFormatString.split(",").filter { it.isNotBlank() }
        }
        
        val themeModeString = this[themeModeKey] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        
        return AppSettings(
            audioFormatPreference = audioFormatPreference,
            themeMode = themeMode,
            downloadOnWifiOnly = this[downloadWifiOnlyKey] ?: true,
            showDebugInfo = this[showDebugInfoKey] ?: false,
            deletionGracePeriodDays = this[deletionGracePeriodKey] ?: 7,
            lowStorageThresholdMB = this[lowStorageThresholdKey] ?: 500L
        )
    }
}