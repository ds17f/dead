package com.deadly.v2.feature.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.core.backup.MigrationExportService
import com.deadly.v2.core.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * SettingsViewModel - Business logic for V2 Settings screen
 *
 * Handles theme import operations and coordinates with ThemeManager
 * to update available themes and potentially switch to imported themes.
 *
 * Following V2 architecture patterns with minimal state management
 * since most UI feedback is handled directly by ThemeChooser component.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val migrationExportService: MigrationExportService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    // Migration export state
    sealed class MigrationExportState {
        data object Idle : MigrationExportState()
        data object Exporting : MigrationExportState()
        data class Success(val libraryCount: Int, val recentCount: Int) : MigrationExportState()
        data class Error(val message: String) : MigrationExportState()
    }

    private val _migrationExportState = MutableStateFlow<MigrationExportState>(MigrationExportState.Idle)
    val migrationExportState: StateFlow<MigrationExportState> = _migrationExportState

    private var pendingMigrationJson: String? = null

    fun onPrepareMigrationExport(onReady: (suggestedFilename: String) -> Unit) {
        viewModelScope.launch {
            try {
                _migrationExportState.value = MigrationExportState.Exporting
                val data = withContext(Dispatchers.IO) {
                    migrationExportService.createMigrationData(getAppVersion())
                }
                pendingMigrationJson = withContext(Dispatchers.IO) {
                    migrationExportService.serialize(data)
                }
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
                onReady("dead_migration_$timestamp.json")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare migration export", e)
                _migrationExportState.value = MigrationExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun onWriteMigrationToUri(uri: Uri) {
        val jsonData = pendingMigrationJson
        if (jsonData == null) {
            _migrationExportState.value = MigrationExportState.Error("No migration data prepared")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(jsonData.toByteArray())
                    } ?: throw IllegalStateException("Could not open output stream")
                }
                // Parse back to get counts for the success message
                val data = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    .decodeFromString<com.deadly.core.backup.model.MigrationData>(jsonData)
                _migrationExportState.value = MigrationExportState.Success(
                    libraryCount = data.library.size,
                    recentCount = data.recentPlays.size
                )
                pendingMigrationJson = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write migration file", e)
                _migrationExportState.value = MigrationExportState.Error(e.message ?: "Write failed")
            }
        }
    }

    fun onDismissMigrationResult() {
        _migrationExportState.value = MigrationExportState.Idle
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }
    
    /**
     * Handle theme file import completion
     * 
     * Called by ThemeChooser when a ZIP file has been successfully
     * copied to the themes directory. Triggers theme scanning to
     * make the new theme available.
     * 
     * @param themeFile The imported theme ZIP file
     */
    fun onThemeImported(themeFile: File) {
        viewModelScope.launch {
            try {
                // Trigger theme manager to scan for new themes
                themeManager.scanForThemes()
                
                // Future: Could automatically switch to the imported theme
                // or show a dialog asking if user wants to switch
                // For now, just make it available in the theme list
                
            } catch (e: Exception) {
                // ThemeChooser handles user feedback for import errors
                // Log error for debugging
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Clear all themes and restart the app
     * 
     * Deletes all imported theme files and exits the app so user can restart
     * with the builtin theme restored.
     */
    fun onClearThemes() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "onClearThemes: Clearing all themes")
                themeManager.clearAllThemes()
                Log.d(TAG, "onClearThemes: Themes cleared, exiting app")
                
                // Exit the app so user can restart with builtin theme
                exitProcess(0)
                
            } catch (e: Exception) {
                Log.e(TAG, "onClearThemes: Failed to clear themes", e)
                // Could show error toast here, but keeping it simple for now
            }
        }
    }
    
    /**
     * Delete the data.zip file from the files directory
     * 
     * @param onComplete Callback with success/failure result
     */
    fun onDeleteDataZip(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val dataZipFile = File(context.filesDir, "data.zip")
                    if (dataZipFile.exists()) {
                        val deleted = dataZipFile.delete()
                        Log.d(TAG, "onDeleteDataZip: data.zip deletion result: $deleted")
                        deleted
                    } else {
                        Log.d(TAG, "onDeleteDataZip: data.zip file does not exist")
                        true // Consider non-existent file as success
                    }
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e(TAG, "onDeleteDataZip: Failed to delete data.zip", e)
                onComplete(false)
            }
        }
    }
    
    /**
     * Delete all deady_db* files from the databases directory
     * 
     * @param onComplete Callback with success/failure result
     */
    fun onDeleteDatabaseFiles(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    // Try multiple possible database locations
                    val possibleDirs = listOf(
                        File(context.getDatabasePath("dummy").parent!!), // Standard Android databases location
                        File(context.applicationInfo.dataDir, "databases"), // Alternative location
                        File(context.filesDir, "databases") // Another possible location
                    )
                    
                    var foundAnyFiles = false
                    var allDeleted = true
                    
                    for (databaseDir in possibleDirs) {
                        Log.d(TAG, "onDeleteDatabaseFiles: Checking directory: ${databaseDir.absolutePath}")
                        
                        if (!databaseDir.exists()) {
                            Log.d(TAG, "onDeleteDatabaseFiles: Directory does not exist: ${databaseDir.absolutePath}")
                            continue
                        }
                        
                        val dbFiles = databaseDir.listFiles { file ->
                            file.name.startsWith("deadly_db")
                        } ?: emptyArray()
                        
                        Log.d(TAG, "onDeleteDatabaseFiles: Found ${dbFiles.size} deadly_db* files in ${databaseDir.absolutePath}")
                        
                        if (dbFiles.isNotEmpty()) {
                            foundAnyFiles = true
                        }
                        
                        for (file in dbFiles) {
                            try {
                                Log.d(TAG, "onDeleteDatabaseFiles: Attempting to delete ${file.absolutePath}")
                                val deleted = file.delete()
                                Log.d(TAG, "onDeleteDatabaseFiles: ${file.name} deletion result: $deleted")
                                if (!deleted) {
                                    allDeleted = false
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "onDeleteDatabaseFiles: Failed to delete ${file.name}", e)
                                allDeleted = false
                            }
                        }
                    }
                    
                    // If no files were found at all, consider it success (nothing to delete)
                    if (!foundAnyFiles) {
                        Log.d(TAG, "onDeleteDatabaseFiles: No deadly_db* files found in any location")
                        true
                    } else {
                        allDeleted
                    }
                }
                onComplete(success)
            } catch (e: Exception) {
                Log.e(TAG, "onDeleteDatabaseFiles: Failed to delete database files", e)
                onComplete(false)
            }
        }
    }
}