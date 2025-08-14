package com.deadarchive.core.database.v2.service

import android.util.Log
import com.deadarchive.core.database.v2.DeadArchiveV2Database
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManagerV2 @Inject constructor(
    private val database: DeadArchiveV2Database,
    private val showDao: ShowV2Dao,
    private val venueDao: VenueV2Dao,
    private val dataVersionDao: DataVersionDao,
    private val importService: DataImportServiceV2
) {
    companion object {
        private const val TAG = "DatabaseManagerV2"
    }
    
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
            _progress.value = ProgressV2(
                phase = "CHECKING",
                totalItems = 0,
                processedItems = 0,
                currentItem = "Checking existing data..."
            )
            
            if (isV2DataInitialized()) {
                Log.d(TAG, "V2 data already initialized")
                _progress.value = ProgressV2(
                    phase = "COMPLETED",
                    totalItems = 1,
                    processedItems = 1,
                    currentItem = "Data already initialized",
                    isComplete = true
                )
                return@withContext ImportResult.success(0, 0)
            }
            
            Log.d(TAG, "Initializing V2 data...")
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
                _progress.value = ProgressV2(
                    phase = "COMPLETED",
                    totalItems = result.showsImported,
                    processedItems = result.showsImported,
                    currentItem = "Import completed: ${result.showsImported} shows, ${result.venuesImported} venues",
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
     * Clear all V2 database data
     */
    suspend fun clearV2Database(): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing V2 database...")
            
            // Delete in reverse foreign key order
            showDao.deleteAll()
            venueDao.deleteAll()
            dataVersionDao.deleteAll()
            
            Log.d(TAG, "V2 database cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear V2 database", e)
            throw e
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