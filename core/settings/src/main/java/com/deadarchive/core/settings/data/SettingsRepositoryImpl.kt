package com.deadarchive.core.settings.data

import android.util.Log
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore for persistence
 * Provides centralized access to application settings with reactive updates
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    companion object {
        private const val TAG = "SettingsRepository"
    }
    
    /**
     * Observe all application settings as a reactive stream
     */
    override fun getSettings(): Flow<AppSettings> {
        return settingsDataStore.settingsFlow
            .catch { exception ->
                Log.e(TAG, "Error reading settings", exception)
                // Emit default settings on error
                emit(AppSettings())
            }
    }
    
    /**
     * Update the audio format preference order
     */
    override suspend fun updateAudioFormatPreference(formatOrder: List<String>) {
        try {
            Log.d(TAG, "Updating audio format preference: $formatOrder")
            settingsDataStore.updateAudioFormatPreference(formatOrder)
            Log.d(TAG, "Audio format preference updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update audio format preference", e)
            throw e
        }
    }
    
    /**
     * Update the application theme mode
     */
    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        try {
            Log.d(TAG, "Updating theme mode: $themeMode")
            settingsDataStore.updateThemeMode(themeMode)
            Log.d(TAG, "Theme mode updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update theme mode", e)
            throw e
        }
    }
    
    /**
     * Update the download WiFi-only setting
     */
    override suspend fun updateDownloadWifiOnly(wifiOnly: Boolean) {
        try {
            Log.d(TAG, "Updating download WiFi-only: $wifiOnly")
            settingsDataStore.updateDownloadWifiOnly(wifiOnly)
            Log.d(TAG, "Download WiFi-only updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update download WiFi-only setting", e)
            throw e
        }
    }
    
    
    /**
     * Reset all settings to their default values
     */
    override suspend fun resetToDefaults() {
        try {
            Log.d(TAG, "Resetting all settings to defaults")
            settingsDataStore.resetToDefaults()
            Log.d(TAG, "All settings reset to defaults successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset settings to defaults", e)
            throw e
        }
    }
}