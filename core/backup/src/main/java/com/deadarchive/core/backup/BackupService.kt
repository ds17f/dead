package com.deadarchive.core.backup

import android.content.Context
import android.os.Environment
import com.deadarchive.core.backup.model.*
import java.io.File
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.settings.api.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupService @Inject constructor(
    private val showRepository: ShowRepository,
    private val settingsRepository: SettingsRepository,
    private val showDao: ShowDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "BackupService"
        private const val BACKUP_FILE_PREFIX = "dead_archive_backup"
        private const val BACKUP_FILE_EXTENSION = "json"
    }
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * Creates a backup of user's library and settings only
     */
    suspend fun createBackup(appVersion: String): BackupData {
        android.util.Log.d(TAG, "Creating backup of user library and settings...")
        
        // Get current settings
        val settings = settingsRepository.getSettings().firstOrNull() 
            ?: com.deadarchive.core.settings.api.model.AppSettings()
        
        // Get library shows from the actual source used by the app (ShowEntity.addedToLibraryAt)
        val libraryShows = showDao.getLibraryShows()
        val backupShows = mutableListOf<BackupLibraryShow>()
        
        for (showEntity in libraryShows) {
            try {
                // Get user's preferred recording for this show
                val preferredRecordingId = settingsRepository.getRecordingPreference(showEntity.showId)
                
                val backupShow = BackupLibraryShow(
                    showId = showEntity.showId,
                    date = showEntity.date,
                    venue = showEntity.venue,
                    location = showEntity.location,
                    addedAt = showEntity.addedToLibraryAt ?: throw IllegalStateException("Expected non-null addedToLibraryAt from getLibraryShows() query"),
                    preferredRecordingId = preferredRecordingId
                )
                backupShows.add(backupShow)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Failed to backup library show ${showEntity.showId}: ${e.message}")
            }
        }
        
        val backup = BackupData(
            appVersion = appVersion,
            settings = settings,
            libraryShows = backupShows
        )
        
        android.util.Log.d(TAG, "Backup created: ${backupShows.size} library shows, settings included")
        return backup
    }
    
    /**
     * Exports backup data to a JSON file in the Downloads directory
     */
    suspend fun exportBackup(context: Context, backup: BackupData): Pair<String, File?> {
        val json = json.encodeToString(backup)
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val filename = "${BACKUP_FILE_PREFIX}_${timestamp}.${BACKUP_FILE_EXTENSION}"
        
        return try {
            // Save to app's external files directory (accessible to app and visible to user)
            val appExternalDir = context.getExternalFilesDir(null)
            val backupFile = File(appExternalDir, filename)
            
            // Ensure directory exists
            appExternalDir?.mkdirs()
            
            // Write JSON to file
            backupFile.writeText(json)
            
            android.util.Log.d(TAG, "Backup saved to: ${backupFile.absolutePath}")
            android.util.Log.d(TAG, "Backup JSON generated: ${json.length} characters")
            
            json to backupFile
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save backup file: ${e.message}", e)
            // Return JSON without file if saving fails
            json to null
        }
    }
    
    /**
     * Gets a suggested filename for the backup
     */
    fun getBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        return "${BACKUP_FILE_PREFIX}_${timestamp}.${BACKUP_FILE_EXTENSION}"
    }
    
    /**
     * Finds the most recent backup file in Downloads directory
     */
    fun findLatestBackupFile(): File? {
        return try {
            // Look in app's external files directory instead of Downloads
            val appExternalDir = context.getExternalFilesDir(null)
            android.util.Log.d(TAG, "Looking for backup files in: ${appExternalDir?.absolutePath}")
            android.util.Log.d(TAG, "App external directory exists: ${appExternalDir?.exists()}")
            
            if (appExternalDir?.exists() != true) {
                android.util.Log.w(TAG, "App external directory does not exist")
                return null
            }
            
            val allFiles = appExternalDir.listFiles()
            android.util.Log.d(TAG, "Total files in app directory: ${allFiles?.size ?: 0}")
            allFiles?.forEach { file ->
                android.util.Log.d(TAG, "Found file: ${file.name}")
            }
            
            val backupFiles = appExternalDir.listFiles { file ->
                val matches = file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(".$BACKUP_FILE_EXTENSION")
                android.util.Log.d(TAG, "File ${file.name} matches backup pattern: $matches")
                matches
            }
            
            android.util.Log.d(TAG, "Found ${backupFiles?.size ?: 0} backup files")
            backupFiles?.forEach { file ->
                android.util.Log.d(TAG, "Backup file: ${file.name}, modified: ${file.lastModified()}")
            }
            
            // Return the most recently modified backup file
            val latestFile = backupFiles?.maxByOrNull { it.lastModified() }
            android.util.Log.d(TAG, "Latest backup file: ${latestFile?.name}")
            latestFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to find latest backup file: ${e.message}", e)
            null
        }
    }
    
    /**
     * Restores user's library and settings from a backup
     * Only restores the library entries - assumes the show data already exists in the database
     */
    suspend fun restoreFromBackup(backupData: BackupData): RestoreResult {
        android.util.Log.d(TAG, "Starting restore of library and settings from backup...")
        
        return try {
            var restoredShows = 0
            var restoredSettings = false
            var skippedShows = 0
            
            // 1. Restore settings
            try {
                // Restore individual settings since there's no bulk update method
                val settings = backupData.settings
                settingsRepository.updateAudioFormatPreference(settings.audioFormatPreferences)
                settingsRepository.updateThemeMode(settings.themeMode)
                settingsRepository.updateDownloadWifiOnly(settings.downloadWifiOnly)
                settingsRepository.updateShowDebugInfo(settings.showDebugInfo)
                settingsRepository.updateDeletionGracePeriod(settings.deletionGracePeriodDays)
                settingsRepository.updateLowStorageThreshold(settings.lowStorageThresholdMB)
                settingsRepository.updatePreferredAudioSource(settings.preferredAudioSource)
                settingsRepository.updateMinimumRating(settings.minimumRating)
                settingsRepository.updatePreferHigherRated(settings.preferHigherRated)
                settingsRepository.updateEnableResumeLastTrack(settings.enableResumeLastTrack)
                
                // Restore recording preferences
                for ((showId, recordingId) in settings.recordingPreferences) {
                    settingsRepository.updateRecordingPreference(showId, recordingId)
                }
                
                restoredSettings = true
                android.util.Log.d(TAG, "Settings restored successfully")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to restore settings: ${e.message}")
            }
            
            // 2. Restore library shows
            for (backupShow in backupData.libraryShows) {
                try {
                    // Check if show exists in the database (from the catalog)
                    val existingShow = showRepository.getShowById(backupShow.showId)
                    if (existingShow != null) {
                        // Add to library with original timestamp
                        showDao.addShowToLibrary(backupShow.showId, backupShow.addedAt)
                        
                        // Check if show has recordings, if not, try to fetch them from API
                        if (existingShow.recordings.isEmpty()) {
                            android.util.Log.d(TAG, "Show ${backupShow.showId} has no recordings, attempting to fetch from API")
                            try {
                                // Extract date from showId (format: YYYY-MM-DD_venue)
                                val date = backupShow.showId.split("_").firstOrNull()
                                if (date != null && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                    // Use the searchRecordings flow to fetch and cache recordings for this date
                                    showRepository.searchRecordings(date).collect { recordings ->
                                        if (recordings.isNotEmpty()) {
                                            android.util.Log.d(TAG, "Fetched ${recordings.size} recordings for date $date")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w(TAG, "Failed to fetch recordings for show ${backupShow.showId}: ${e.message}")
                            }
                        }
                        
                        // Restore preferred recording preference if set
                        backupShow.preferredRecordingId?.let { recordingId ->
                            settingsRepository.updateRecordingPreference(backupShow.showId, recordingId)
                        }
                        
                        restoredShows++
                        android.util.Log.d(TAG, "Restored library show: ${backupShow.showId}")
                    } else {
                        skippedShows++
                        android.util.Log.w(TAG, "Skipped show ${backupShow.showId} - not found in database catalog")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to restore library show ${backupShow.showId}: ${e.message}")
                    skippedShows++
                }
            }
            
            val message = if (skippedShows > 0) {
                "Restored $restoredShows shows, skipped $skippedShows shows not found in catalog"
            } else {
                "Restored $restoredShows shows"
            }
            
            android.util.Log.d(TAG, "Restore completed: $message, settings: $restoredSettings")
            
            RestoreResult.Success(
                restoredShows = restoredShows,
                restoredSettings = restoredSettings,
                skippedShows = skippedShows
            )
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Restore failed: ${e.message}", e)
            RestoreResult.Error(e.message ?: "Unknown error occurred during restore")
        }
    }
    
    /**
     * Parses backup JSON string into BackupData
     */
    fun parseBackup(jsonString: String): BackupData? {
        return try {
            json.decodeFromString<BackupData>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse backup JSON: ${e.message}")
            null
        }
    }
}

sealed class RestoreResult {
    data class Success(
        val restoredShows: Int,
        val restoredSettings: Boolean,
        val skippedShows: Int = 0
    ) : RestoreResult()
    
    data class Error(val message: String) : RestoreResult()
}