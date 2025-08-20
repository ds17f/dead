package com.deadly.core.settings.data

import android.util.Log
import com.deadly.core.settings.api.SettingsRepository
import com.deadly.core.settings.api.model.AppSettings
import com.deadly.core.settings.api.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
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
     * Update the debug info visibility setting
     */
    override suspend fun updateShowDebugInfo(showDebugInfo: Boolean) {
        try {
            Log.d(TAG, "Updating show debug info: $showDebugInfo")
            settingsDataStore.updateShowDebugInfo(showDebugInfo)
            Log.d(TAG, "Show debug info updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update show debug info setting", e)
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
    
    override suspend fun updateDeletionGracePeriod(days: Int) {
        try {
            Log.d(TAG, "Updating deletion grace period to $days days")
            settingsDataStore.updateDeletionGracePeriod(days)
            Log.d(TAG, "Deletion grace period updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update deletion grace period", e)
            throw e
        }
    }
    
    override suspend fun updateLowStorageThreshold(thresholdMB: Long) {
        try {
            Log.d(TAG, "Updating low storage threshold to ${thresholdMB}MB")
            settingsDataStore.updateLowStorageThreshold(thresholdMB)
            Log.d(TAG, "Low storage threshold updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update low storage threshold", e)
            throw e
        }
    }
    
    override suspend fun updatePreferredAudioSource(source: String) {
        try {
            Log.d(TAG, "Updating preferred audio source to: $source")
            settingsDataStore.updatePreferredAudioSource(source)
            Log.d(TAG, "Preferred audio source updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update preferred audio source", e)
            throw e
        }
    }
    
    override suspend fun updateMinimumRating(rating: Float) {
        try {
            Log.d(TAG, "Updating minimum rating to: $rating")
            settingsDataStore.updateMinimumRating(rating)
            Log.d(TAG, "Minimum rating updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update minimum rating", e)
            throw e
        }
    }
    
    override suspend fun updatePreferHigherRated(prefer: Boolean) {
        try {
            Log.d(TAG, "Updating prefer higher rated to: $prefer")
            settingsDataStore.updatePreferHigherRated(prefer)
            Log.d(TAG, "Prefer higher rated updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update prefer higher rated", e)
            throw e
        }
    }
    
    override suspend fun updateRecordingPreference(showId: String, recordingId: String) {
        try {
            Log.d(TAG, "Updating recording preference: showId=$showId, recordingId=$recordingId")
            settingsDataStore.updateRecordingPreference(showId, recordingId)
            Log.d(TAG, "Recording preference updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update recording preference", e)
            throw e
        }
    }
    
    override suspend fun getRecordingPreference(showId: String): String? {
        return try {
            getSettings().firstOrNull()?.recordingPreferences?.get(showId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recording preference for showId: $showId", e)
            null
        }
    }
    
    override suspend fun removeRecordingPreference(showId: String) {
        try {
            Log.d(TAG, "Removing recording preference for showId: $showId")
            settingsDataStore.removeRecordingPreference(showId)
            Log.d(TAG, "Recording preference removed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove recording preference for showId: $showId", e)
            throw e
        }
    }
    
    override suspend fun updateEnableResumeLastTrack(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating enable resume last track to: $enabled")
            settingsDataStore.updateEnableResumeLastTrack(enabled)
            Log.d(TAG, "Enable resume last track updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update enable resume last track", e)
            throw e
        }
    }
    
    override suspend fun updateUseLibraryV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Library V2 to: $enabled")
            settingsDataStore.updateUseLibraryV2(enabled)
            Log.d(TAG, "Use Library V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Library V2", e)
            throw e
        }
    }
    
    override suspend fun updateUsePlayerV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Player V2 to: $enabled")
            settingsDataStore.updateUsePlayerV2(enabled)
            Log.d(TAG, "Use Player V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Player V2", e)
            throw e
        }
    }
    
    override suspend fun updateUseSearchV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Search V2 to: $enabled")
            settingsDataStore.updateUseSearchV2(enabled)
            Log.d(TAG, "Use Search V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Search V2", e)
            throw e
        }
    }
    
    override suspend fun updateUseHomeV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Home V2 to: $enabled")
            settingsDataStore.updateUseHomeV2(enabled)
            Log.d(TAG, "Use Home V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Home V2", e)
            throw e
        }
    }
    
    override suspend fun updateUsePlaylistV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Playlist V2 to: $enabled")
            settingsDataStore.updateUsePlaylistV2(enabled)
            Log.d(TAG, "Use Playlist V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Playlist V2", e)
            throw e
        }
    }
    
    override suspend fun updateUseMiniPlayerV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use MiniPlayer V2 to: $enabled")
            settingsDataStore.updateUseMiniPlayerV2(enabled)
            Log.d(TAG, "Use MiniPlayer V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use MiniPlayer V2", e)
            throw e
        }
    }
    
    override suspend fun updateUseSplashV2(enabled: Boolean) {
        try {
            Log.d(TAG, "Updating use Splash V2 to: $enabled")
            settingsDataStore.updateUseSplashV2(enabled)
            Log.d(TAG, "Use Splash V2 updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update use Splash V2", e)
            throw e
        }
    }
    
    // Update-related methods
    
    override suspend fun updateLastUpdateCheck(timestamp: Long) {
        try {
            Log.d(TAG, "Updating last update check timestamp to: $timestamp")
            settingsDataStore.updateLastUpdateCheck(timestamp)
            Log.d(TAG, "Last update check timestamp updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last update check timestamp", e)
            throw e
        }
    }
    
    override fun getLastUpdateCheck(): Flow<Long> {
        return settingsDataStore.settingsFlow
            .catch { exception ->
                Log.e(TAG, "Error reading last update check", exception)
                emit(AppSettings())
            }
            .map { it.lastUpdateCheckTimestamp }
    }
    
    override suspend fun addSkippedVersion(version: String) {
        try {
            Log.d(TAG, "Adding skipped version: $version")
            settingsDataStore.addSkippedVersion(version)
            Log.d(TAG, "Skipped version added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add skipped version", e)
            throw e
        }
    }
    
    override fun getSkippedVersions(): Flow<Set<String>> {
        return settingsDataStore.settingsFlow
            .catch { exception ->
                Log.e(TAG, "Error reading skipped versions", exception)
                emit(AppSettings())
            }
            .map { it.skippedVersions }
    }
    
    override suspend fun clearSkippedVersions() {
        try {
            Log.d(TAG, "Clearing all skipped versions")
            settingsDataStore.clearSkippedVersions()
            Log.d(TAG, "All skipped versions cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear skipped versions", e)
            throw e
        }
    }
    
    override suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        try {
            Log.d(TAG, "Setting auto update check enabled to: $enabled")
            settingsDataStore.setAutoUpdateCheckEnabled(enabled)
            Log.d(TAG, "Auto update check enabled setting updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set auto update check enabled", e)
            throw e
        }
    }
    
    override fun isAutoUpdateCheckEnabled(): Flow<Boolean> {
        return settingsDataStore.settingsFlow
            .catch { exception ->
                Log.e(TAG, "Error reading auto update check enabled", exception)
                emit(AppSettings())
            }
            .map { it.autoUpdateCheckEnabled }
    }
}