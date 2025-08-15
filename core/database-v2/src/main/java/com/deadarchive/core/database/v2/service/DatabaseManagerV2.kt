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
     * Initialize V2 data if needed
     */
    suspend fun initializeV2DataIfNeeded(): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Record start time
            initStartTimeMs = System.currentTimeMillis()
            Log.d(TAG, "Starting V2 database initialization at $initStartTimeMs")
            
            _progress.value = ProgressV2(
                phase = "CHECKING",
                totalItems = 0,
                processedItems = 0,
                currentItem = "Checking existing data..."
            )
            
            // Check if database file exists, if not try to extract from ZIP
            val dbFile = context.getDatabasePath(DeadArchiveV2Database.DATABASE_NAME)
            if (!dbFile.exists()) {
                Log.d(TAG, "Database file doesn't exist, trying to extract from ZIP")
                val extractSuccess = extractDatabaseFromZip()
                if (extractSuccess) {
                    Log.i(TAG, "Database extracted from ZIP successfully")
                    // Database now exists, we can return success
                    _progress.value = ProgressV2(
                        phase = "COMPLETED",
                        totalItems = 1,
                        processedItems = 1,
                        currentItem = "Database restored from ZIP file",
                        isComplete = true
                    )
                    return@withContext ImportResult.success(0, 0)
                } else {
                    Log.d(TAG, "ZIP extraction failed or ZIP not available, proceeding with normal import")
                }
            }
            
            val isInitialized = isV2DataInitialized()
            Log.d(TAG, "V2 data initialization check: $isInitialized")
            
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
            
            Log.d(TAG, "Initializing V2 data from assets...")
            _progress.value = ProgressV2(
                phase = "EXTRACTING",
                totalItems = 0,
                processedItems = 0,
                currentItem = "Extracting data files..."
            )
            
            val result = importService.importFromAssetsIfNeeded { phase, total, processed, current ->
                _progress.value = ProgressV2(
                    phase = phase,
                    totalItems = total,
                    processedItems = processed,
                    currentItem = current
                )
            }
            
            if (result.success) {
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
            } else {
                _progress.value = ProgressV2(
                    phase = "ERROR",
                    totalItems = 0,
                    processedItems = 0,
                    currentItem = "Import failed",
                    error = result.error
                )
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize V2 data", e)
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