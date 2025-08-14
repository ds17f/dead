package com.deadarchive.core.database.v2.service

import android.util.Log
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import com.deadarchive.core.database.v2.entities.DataVersionEntity
import com.deadarchive.core.database.v2.entities.ShowV2Entity
import com.deadarchive.core.database.v2.entities.VenueV2Entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ShowJsonData(
    @SerialName("show_id")
    val showId: String,
    val band: String,
    val venue: String,
    @SerialName("location_raw")
    val locationRaw: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null, // Allow null values from JSON
    val date: String,
    val url: String? = null,
    @SerialName("setlist_status")
    val setlistStatus: String? = null,
    val setlist: List<SetData>? = null
)

@Serializable
data class SetData(
    @SerialName("set_name")
    val setName: String,
    val songs: List<SongData>
)

@Serializable
data class SongData(
    val name: String,
    val url: String? = null,
    @SerialName("segue_into_next")
    val segueIntoNext: Boolean = false
)

data class ImportResult(
    val success: Boolean,
    val showsImported: Int,
    val venuesImported: Int,
    val error: String? = null
) {
    companion object {
        fun success(shows: Int, venues: Int) = ImportResult(true, shows, venues)
        fun error(message: String) = ImportResult(false, 0, 0, message)
    }
}

@Singleton
class V2DataImportService @Inject constructor(
    private val assetManager: V2AssetManager,
    private val showDao: ShowV2Dao,
    private val venueDao: VenueV2Dao,
    private val dataVersionDao: DataVersionDao
) {
    companion object {
        private const val TAG = "V2DataImportService"
        private const val SHOWS_DIR = "shows"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
    }
    
    /**
     * Main import orchestrator - imports from assets if needed
     */
    suspend fun importFromAssetsIfNeeded(): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Check if we already have data
            val currentVersion = dataVersionDao.getCurrentVersion()
            Log.d(TAG, "Current data version: $currentVersion")
            
            if (!assetManager.isDataZipAvailable()) {
                return@withContext ImportResult.error("Data zip file not found in assets")
            }
            
            // Extract zip to temp directory
            val tempDir = assetManager.extractDataZip()
            val zipVersion = assetManager.getDataVersion(tempDir)
            
            try {
                // Check if we need to import
                if (currentVersion == zipVersion) {
                    Log.d(TAG, "Data already up to date (version: $zipVersion)")
                    return@withContext ImportResult.success(0, 0)
                }
                
                Log.d(TAG, "Importing data version $zipVersion (current: $currentVersion)")
                
                // Import shows and venues
                val result = importShowsAndVenues(tempDir)
                
                if (result.success) {
                    // Update version tracking
                    updateDataVersion(tempDir, result.showsImported, result.venuesImported)
                    Log.d(TAG, "Import completed: ${result.showsImported} shows, ${result.venuesImported} venues")
                }
                
                result
                
            } finally {
                assetManager.cleanupTempFiles(tempDir)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportResult.error("Import failed: ${e.message}")
        }
    }
    
    /**
     * Memory-efficient import using select-then-insert pattern
     */
    private suspend fun importShowsAndVenues(tempDir: File): ImportResult {
        val showsDir = File(tempDir, SHOWS_DIR)
        if (!showsDir.exists() || !showsDir.isDirectory) {
            return ImportResult.error("Shows directory not found in extracted data")
        }
        
        val showFiles = showsDir.listFiles { file -> file.name.endsWith(".json") }
        if (showFiles.isNullOrEmpty()) {
            return ImportResult.error("No show JSON files found")
        }
        
        Log.d(TAG, "Processing ${showFiles.size} show files")
        
        var showCount = 0
        var venueCount = 0
        var failedCount = 0
        
        showFiles.forEach { showFile ->
            try {
                // Parse show JSON
                val showContent = showFile.readText()
                val showData = json.decodeFromString<ShowJsonData>(showContent)
                
                // Generate deterministic venue key
                val venueKey = generateVenueKey(showData.venue, showData.city, showData.state)
                
                // Check if venue exists, insert only if not found
                if (venueDao.findByVenueId(venueKey) == null) {
                    val venue = createVenueEntity(venueKey, showData)
                    venueDao.insert(venue)
                    venueCount++
                }
                
                // Insert show with venue reference
                val show = createShowEntity(showData, venueKey)
                showDao.insert(show)
                showCount++
                
                if (showCount % 100 == 0) {
                    Log.d(TAG, "Processed $showCount shows, $venueCount venues")
                }
                
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "Failed to process show file: ${showFile.name} (${e.message})")
                Log.d(TAG, "Error details for ${showFile.name}", e)
                // Continue processing other files
            }
        }
        
        Log.d(TAG, "Import completed: $showCount shows, $venueCount venues imported, $failedCount failed")
        
        // Update venue statistics after all shows are imported
        Log.d(TAG, "Computing venue statistics...")
        updateVenueStatistics()
        
        return ImportResult.success(showCount, venueCount)
    }
    
    /**
     * Generate deterministic venue key from location data
     */
    private fun generateVenueKey(venue: String, city: String?, state: String?): String {
        val normalized = "${venue.trim().lowercase()}-${city?.trim()?.lowercase()}-${state?.trim()?.lowercase()}"
        return "venue_${normalized.hashCode()}"
    }
    
    /**
     * Create venue entity from show data
     */
    private fun createVenueEntity(venueKey: String, showData: ShowJsonData): VenueV2Entity {
        val currentTime = System.currentTimeMillis()
        return VenueV2Entity(
            venueId = venueKey,
            name = showData.venue,
            normalizedName = showData.venue.lowercase(),
            city = showData.city,
            state = showData.state,
            country = showData.country ?: "USA", // Default to USA if null
            showCount = 0, // Will be computed later if needed
            firstShowDate = showData.date, // Will be updated with actual first/last
            lastShowDate = showData.date,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
    
    /**
     * Create show entity from JSON data
     */
    private fun createShowEntity(showData: ShowJsonData, venueKey: String): ShowV2Entity {
        val currentTime = System.currentTimeMillis()
        
        // Parse date components
        val dateParts = showData.date.split("-")
        val year = dateParts.getOrNull(0)?.toIntOrNull() ?: 1965
        val month = dateParts.getOrNull(1)?.toIntOrNull() ?: 1
        val yearMonth = "${year}-${month.toString().padStart(2, '0')}"
        
        // Build song list for search
        val songList = showData.setlist?.flatMap { set ->
            set.songs.map { song -> song.name }
        }?.joinToString(",")
        
        return ShowV2Entity(
            showId = showData.showId,
            date = showData.date,
            year = year,
            month = month,
            yearMonth = yearMonth,
            band = showData.band,
            url = showData.url,
            venueId = venueKey,
            city = showData.city,
            state = showData.state,
            country = showData.country ?: "USA", // Default to USA if null
            locationRaw = showData.locationRaw,
            setlistStatus = showData.setlistStatus,
            setlistRaw = showData.setlist?.let { json.encodeToString(SetData.serializer(), it.first()) }, // For now, just first set
            songList = songList,
            showSequence = 1, // Default, could be improved later
            recordingCount = 0,
            bestRecordingId = null,
            averageRating = null,
            totalReviews = 0,
            isInLibrary = false,
            libraryAddedAt = null,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
    
    /**
     * Update version tracking after successful import
     */
    private suspend fun updateDataVersion(tempDir: File, showCount: Int, venueCount: Int) {
        val manifest = assetManager.getManifest(tempDir)
        if (manifest != null) {
            val dataVersion = DataVersionEntity(
                dataVersion = manifest.packageInfo.version,
                packageName = manifest.packageInfo.name,
                versionType = manifest.packageInfo.versionType,
                description = manifest.packageInfo.description,
                importedAt = System.currentTimeMillis(),
                gitCommit = null, // Could be added from manifest later
                gitTag = null,
                buildTimestamp = manifest.packageInfo.created,
                totalShows = showCount,
                totalVenues = venueCount,
                totalFiles = 0, // Could be computed
                totalSizeBytes = 0
            )
            
            dataVersionDao.insertOrUpdate(dataVersion)
            Log.d(TAG, "Updated data version to ${manifest.packageInfo.version}")
        }
    }
    
    /**
     * Update venue statistics after all shows are imported
     */
    private suspend fun updateVenueStatistics() {
        try {
            // Get all venues to update
            val venues = venueDao.getAllVenues()
            Log.d(TAG, "Updating statistics for ${venues.size} venues")
            
            venues.forEach { venue ->
                try {
                    Log.d(TAG, "Processing venue statistics: ${venue.name} (${venue.venueId})")
                    
                    // Get all shows for this venue
                    val venueShows = showDao.getShowsByVenue(venue.venueId)
                    
                    if (venueShows.isNotEmpty()) {
                        // Sort shows by date to find first and last
                        val sortedShows = venueShows.sortedBy { it.date }
                        val firstShow = sortedShows.first()
                        val lastShow = sortedShows.last()
                        
                        // Update venue with computed statistics
                        val updatedVenue = venue.copy(
                            showCount = venueShows.size,
                            firstShowDate = firstShow.date,
                            lastShowDate = lastShow.date,
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        venueDao.update(updatedVenue) // Update existing venue with statistics
                        Log.d(TAG, "Updated venue ${venue.name}: ${venueShows.size} shows (${firstShow.date} - ${lastShow.date})")
                    } else {
                        Log.w(TAG, "No shows found for venue: ${venue.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update statistics for venue: ${venue.name} (${venue.venueId})", e)
                }
            }
            
            Log.d(TAG, "✅ Venue statistics updated for ${venues.size} venues")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update venue statistics", e)
        }
    }
}