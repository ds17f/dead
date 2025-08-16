package com.deadarchive.core.database.v2.service

import android.content.Context
import android.util.Log
import com.deadarchive.core.database.v2.DeadArchiveV2Database
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import com.deadarchive.core.database.v2.dao.SongV2Dao
import com.deadarchive.core.database.v2.dao.SetlistV2Dao
import com.deadarchive.core.database.v2.dao.SetlistSongV2Dao
import com.deadarchive.core.database.v2.dao.RecordingV2Dao
import com.deadarchive.core.database.v2.dao.TrackV2Dao
import com.deadarchive.core.database.v2.dao.TrackFormatV2Dao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManagerV2 @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DeadArchiveV2Database,
    private val showDao: ShowV2Dao,
    private val venueDao: VenueV2Dao,
    private val dataVersionDao: DataVersionDao,
    private val songDao: SongV2Dao,
    private val setlistDao: SetlistV2Dao,
    private val setlistSongDao: SetlistSongV2Dao,
    private val recordingDao: RecordingV2Dao,
    private val trackDao: TrackV2Dao,
    private val trackFormatDao: TrackFormatV2Dao,
    private val importService: DataImportServiceV2
) {
    companion object {
        private const val TAG = "DatabaseManagerV2"
        private const val DB_ZIP_FILENAME = "dead_archive_v2_database.db.zip"
    }
    
    /**
     * Available database sources for initialization
     */
    enum class DatabaseSource {
        ZIP_BACKUP,    // Fast restoration from pre-built database
        DATA_IMPORT    // Full import from data-v2 JSON files
    }
    
    /**
     * Result of checking available database sources
     */
    data class AvailableSources(
        val hasZipBackup: Boolean,
        val hasDataFiles: Boolean
    ) {
        val sources: List<DatabaseSource> get() = buildList {
            if (hasZipBackup) add(DatabaseSource.ZIP_BACKUP)
            if (hasDataFiles) add(DatabaseSource.DATA_IMPORT)
        }
        
        val requiresUserChoice: Boolean get() = sources.size > 1
    }
    
    // Track initialization timing
    private var initStartTimeMs: Long = 0L
    
    private val _progress = MutableStateFlow(
        ProgressV2(
            phase = "IDLE",
            totalItems = 0,
            processedItems = 0,
            currentItem = ""
        )
    )
    val progress: StateFlow<ProgressV2> = _progress.asStateFlow()
    
    /**
     * Check what database sources are available for initialization
     */
    suspend fun checkAvailableSources(): AvailableSources = withContext(Dispatchers.IO) {
        val hasZipBackup = try {
            context.assets.open(DB_ZIP_FILENAME).use { true }
        } catch (e: Exception) {
            Log.d(TAG, "ZIP backup not available: $DB_ZIP_FILENAME")
            false
        }
        
        val hasDataFiles = try {
            importService.hasDataFiles()
        } catch (e: Exception) {
            Log.d(TAG, "Data files not available for import")
            false
        }
        
        Log.d(TAG, "Available sources - ZIP backup: $hasZipBackup, Data files: $hasDataFiles")
        AvailableSources(hasZipBackup, hasDataFiles)
    }
    
    /**
     * Check if V2 data has been initialized
     */
    suspend fun isV2DataInitialized(): Boolean = withContext(Dispatchers.IO) {
        try {
            dataVersionDao.hasDataVersion()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check V2 data initialization", e)
            false
        }
    }
    
    /**
     * Initialize V2 data if needed - checks sources and may require user choice
     */
    suspend fun initializeV2DataIfNeeded(): ImportResult = withContext(Dispatchers.IO) {
        // Check if database already exists
        val dbFile = context.getDatabasePath(DeadArchiveV2Database.DATABASE_NAME)
        if (dbFile.exists()) {
            val isInitialized = isV2DataInitialized()
            Log.d(TAG, "Database file exists, initialization check: $isInitialized")
            
            if (isInitialized) {
                Log.d(TAG, "V2 data already initialized, skipping initialization")
                _progress.value = ProgressV2(
                    phase = "COMPLETED",
                    totalItems = 1,
                    processedItems = 1,
                    currentItem = "Data already initialized",
                    isComplete = true
                )
                return@withContext ImportResult.success(0, 0)
            }
        }
        
        // Database doesn't exist or is empty, check available sources
        val availableSources = checkAvailableSources()
        
        return@withContext when {
            availableSources.sources.isEmpty() -> {
                Log.e(TAG, "No database sources available for initialization")
                ImportResult.error("No database sources available")
            }
            availableSources.requiresUserChoice -> {
                Log.d(TAG, "Multiple sources available, requiring user choice")
                ImportResult.requiresUserChoice(availableSources)
            }
            availableSources.hasZipBackup -> {
                Log.d(TAG, "Only ZIP backup available, proceeding with restoration")
                initializeFromSource(DatabaseSource.ZIP_BACKUP)
            }
            availableSources.hasDataFiles -> {
                Log.d(TAG, "Only data files available, proceeding with import")
                initializeFromSource(DatabaseSource.DATA_IMPORT)
            }
            else -> {
                ImportResult.error("No valid database sources found")
            }
        }
    }
    
    /**
     * Initialize V2 data from a specific source
     */
    suspend fun initializeFromSource(source: DatabaseSource): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Record start time
            initStartTimeMs = System.currentTimeMillis()
            Log.d(TAG, "Starting V2 database initialization from source: $source at $initStartTimeMs")
            
            when (source) {
                DatabaseSource.ZIP_BACKUP -> {
                    _progress.value = ProgressV2(
                        phase = "CHECKING",
                        totalItems = 0,
                        processedItems = 0,
                        currentItem = "Restoring from backup..."
                    )
                    
                    val extractSuccess = extractDatabaseFromZip()
                    if (extractSuccess) {
                        Log.i(TAG, "Database extracted from ZIP successfully")
                        _progress.value = ProgressV2(
                            phase = "COMPLETED",
                            totalItems = 1,
                            processedItems = 1,
                            currentItem = "Database restored from backup",
                            isComplete = true
                        )
                        return@withContext ImportResult.success(0, 0)
                    } else {
                        Log.e(TAG, "Failed to extract database from ZIP")
                        return@withContext ImportResult.error("Failed to restore from backup")
                    }
                }
                
                DatabaseSource.DATA_IMPORT -> {
                    _progress.value = ProgressV2(
                        phase = "EXTRACTING",
                        totalItems = 0,
                        processedItems = 0,
                        currentItem = "Importing from data files..."
                    )
                    
                    return@withContext performDataImport()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize V2 data from source: $source", e)
            _progress.value = ProgressV2(
                phase = "ERROR",
                totalItems = 0,
                processedItems = 0,
                currentItem = "Initialization failed",
                error = e.message
            )
            ImportResult.error("Failed to initialize V2 data: ${e.message}")
        }
    }
    
    /**
     * Perform the actual data import from JSON files
     */
    private suspend fun performDataImport(): ImportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Performing data import from JSON files...")
        
        val result = importService.importFromAssetsIfNeeded { phase, total, processed, current ->
            _progress.value = ProgressV2(
                phase = phase,
                totalItems = total,
                processedItems = processed,
                currentItem = current
            )
        }
        
        when (result) {
            is ImportResult.Success -> {
                val endTimeMs = System.currentTimeMillis()
                val elapsedMs = endTimeMs - initStartTimeMs
                val elapsedSeconds = elapsedMs / 1000.0
                
                val completionMessage = buildString {
                    append("Import completed: ")
                    append("${result.showsImported} shows, ")
                    append("${result.venuesImported} venues")
                    if (result.recordingsImported > 0) {
                        append(", ${result.recordingsImported} recordings")
                    }
                    if (result.tracksImported > 0) {
                        append(", ${result.tracksImported} tracks")
                    }
                }
                
                // Log completion with timing information
                Log.i(TAG, "V2 database initialization completed successfully!")
                Log.i(TAG, "Total time: ${String.format("%.2f", elapsedSeconds)} seconds")
                Log.i(TAG, "Results: ${result.showsImported} shows, ${result.venuesImported} venues, ${result.recordingsImported} recordings, ${result.tracksImported} tracks")
                
                _progress.value = ProgressV2(
                    phase = "COMPLETED",
                    totalItems = result.showsImported + result.recordingsImported,
                    processedItems = result.showsImported + result.recordingsImported,
                    currentItem = completionMessage,
                    isComplete = true
                )
                
                return@withContext result
            }
            is ImportResult.Error -> {
                _progress.value = ProgressV2(
                    phase = "ERROR",
                    totalItems = 0,
                    processedItems = 0,
                    currentItem = "Import failed",
                    error = result.error
                )
                
                return@withContext result
            }
            is ImportResult.RequiresUserChoice -> {
                // This shouldn't happen in performDataImport, but handle it
                return@withContext result
            }
        }
    }
    
    /**
     * Force re-import of V2 data (clears existing data first)
     */
    suspend fun reimportV2Data(): ImportResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting V2 data re-import...")
            clearV2Database()
            importService.importFromAssetsIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-import V2 data", e)
            ImportResult.error("Failed to re-import V2 data: ${e.message}")
        }
    }
    
    /**
     * Clear all V2 database data by deleting the database file
     */
    suspend fun clearV2Database(): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing V2 database by deleting database file...")
            
            // Close the database first
            database.close()
            
            // Get the database file path
            val dbFile = context.getDatabasePath(DeadArchiveV2Database.DATABASE_NAME)
            
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                if (deleted) {
                    Log.i(TAG, "V2 database file deleted successfully: ${dbFile.absolutePath}")
                } else {
                    Log.e(TAG, "Failed to delete V2 database file: ${dbFile.absolutePath}")
                    throw RuntimeException("Failed to delete database file")
                }
            } else {
                Log.d(TAG, "V2 database file does not exist: ${dbFile.absolutePath}")
            }
            
            // Also delete any associated files (WAL, SHM)
            val walFile = context.getDatabasePath("${DeadArchiveV2Database.DATABASE_NAME}-wal")
            val shmFile = context.getDatabasePath("${DeadArchiveV2Database.DATABASE_NAME}-shm")
            
            if (walFile.exists()) {
                walFile.delete()
                Log.d(TAG, "Deleted WAL file: ${walFile.absolutePath}")
            }
            
            if (shmFile.exists()) {
                shmFile.delete()
                Log.d(TAG, "Deleted SHM file: ${shmFile.absolutePath}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear V2 database", e)
            throw e
        }
    }
    
    /**
     * Extract database from ZIP file in assets if available
     */
    private suspend fun extractDatabaseFromZip(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if ZIP file exists in assets
            val zipExists = try {
                context.assets.open(DB_ZIP_FILENAME).use { true }
            } catch (e: Exception) {
                Log.d(TAG, "ZIP file not found in assets: $DB_ZIP_FILENAME")
                false
            }
            
            if (!zipExists) {
                return@withContext false
            }
            
            Log.d(TAG, "Extracting database from $DB_ZIP_FILENAME")
            
            // Get the database file path
            val dbFile = context.getDatabasePath(DeadArchiveV2Database.DATABASE_NAME)
            
            // Ensure parent directory exists
            dbFile.parentFile?.mkdirs()
            
            // Extract database file from ZIP
            context.assets.open(DB_ZIP_FILENAME).use { assetStream ->
                ZipInputStream(assetStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.endsWith(".db")) {
                            // Found the database file, extract it directly
                            FileOutputStream(dbFile).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }
                            
                            Log.d(TAG, "Extracted database to: ${dbFile.absolutePath} (${dbFile.length()} bytes)")
                            return@withContext true
                        }
                        
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
            
            Log.e(TAG, "No database file found in ZIP")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract database from ZIP", e)
            false
        }
    }
    
    /**
     * Get database statistics for debugging/monitoring
     */
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            DatabaseStats(
                showCount = showDao.getShowCount(),
                venueCount = venueDao.getVenueCount(),
                dataVersion = dataVersionDao.getCurrentVersion(),
                isInitialized = dataVersionDao.hasDataVersion()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get database stats", e)
            DatabaseStats(
                showCount = -1,
                venueCount = -1,
                dataVersion = null,
                isInitialized = false,
                error = e.message
            )
        }
    }
    
    /**
     * Verify database integrity - check for Cornell '77 and other expected shows
     */
    suspend fun verifyDatabaseIntegrity(): DatabaseVerification = withContext(Dispatchers.IO) {
        try {
            val cornell77 = showDao.getCornell77()
            val totalShows = showDao.getShowCount()
            val totalVenues = venueDao.getVenueCount()
            
            val issues = mutableListOf<String>()
            
            // Check for Cornell '77
            if (cornell77.isEmpty()) {
                issues.add("Cornell '77 (1977-05-08) not found")
            }
            
            // Check reasonable data counts
            if (totalShows < 2000) {
                issues.add("Show count seems low: $totalShows (expected 2000+)")
            }
            
            if (totalVenues < 200) {
                issues.add("Venue count seems low: $totalVenues (expected 200+)")
            }
            
            DatabaseVerification(
                isValid = issues.isEmpty(),
                showCount = totalShows,
                venueCount = totalVenues,
                hasCornell77 = cornell77.isNotEmpty(),
                issues = issues
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify database integrity", e)
            DatabaseVerification(
                isValid = false,
                showCount = 0,
                venueCount = 0,
                hasCornell77 = false,
                issues = listOf("Verification failed: ${e.message}")
            )
        }
    }
}

data class DatabaseStats(
    val showCount: Int,
    val venueCount: Int,
    val dataVersion: String?,
    val isInitialized: Boolean,
    val error: String? = null
)

data class DatabaseVerification(
    val isValid: Boolean,
    val showCount: Int,
    val venueCount: Int,
    val hasCornell77: Boolean,
    val issues: List<String>
)