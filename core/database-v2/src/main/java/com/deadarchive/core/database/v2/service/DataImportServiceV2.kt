package com.deadarchive.core.database.v2.service

import android.util.Log
import com.deadarchive.core.database.v2.dao.DataVersionDao
import com.deadarchive.core.database.v2.dao.SetlistSongV2Dao
import com.deadarchive.core.database.v2.dao.SetlistV2Dao
import com.deadarchive.core.database.v2.dao.ShowV2Dao
import com.deadarchive.core.database.v2.dao.SongV2Dao
import com.deadarchive.core.database.v2.dao.VenueV2Dao
import com.deadarchive.core.database.v2.dao.RecordingV2Dao
import com.deadarchive.core.database.v2.dao.TrackV2Dao
import com.deadarchive.core.database.v2.dao.TrackFormatV2Dao
import com.deadarchive.core.database.v2.entities.DataVersionEntity
import com.deadarchive.core.database.v2.entities.SetlistSongV2Entity
import com.deadarchive.core.database.v2.entities.SetlistV2Entity
import com.deadarchive.core.database.v2.entities.ShowV2Entity
import com.deadarchive.core.database.v2.entities.VenueV2Entity
import com.deadarchive.core.database.v2.entities.RecordingV2Entity
import com.deadarchive.core.database.v2.entities.TrackV2Entity
import com.deadarchive.core.database.v2.entities.TrackFormatV2Entity
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
    val setlist: List<SetDataV2>? = null
)

@Serializable
data class SetDataV2(
    @SerialName("set_name")
    val setName: String,
    val songs: List<SongDataV2>
)

@Serializable
data class SongDataV2(
    val name: String,
    val url: String? = null,
    @SerialName("segue_into_next")
    val segueIntoNext: Boolean = false
)

@Serializable
data class RecordingJsonData(
    val rating: Double = 0.0,
    @SerialName("review_count")
    val reviewCount: Int = 0,
    @SerialName("source_type")
    val sourceType: String? = null,
    val confidence: Double = 0.0,
    val date: String,
    val venue: String,
    val location: String? = null,
    @SerialName("raw_rating")
    val rawRating: Double = 0.0,
    @SerialName("high_ratings")
    val highRatings: Int = 0,
    @SerialName("low_ratings")
    val lowRatings: Int = 0,
    val tracks: List<TrackJsonData> = emptyList()
)

@Serializable
data class TrackJsonData(
    val track: String, // Track number like "01", "02"
    val title: String,
    val duration: Double? = null,
    val formats: List<FormatJsonData> = emptyList()
)

@Serializable
data class FormatJsonData(
    val format: String, // "Flac", "VBR MP3", etc.
    val filename: String,
    val bitrate: String? = null
)

sealed class ImportResult {
    data class Success(
        val showsImported: Int,
        val venuesImported: Int,
        val songsImported: Int = 0,
        val setlistsImported: Int = 0,
        val setlistSongsImported: Int = 0,
        val recordingsImported: Int = 0,
        val tracksImported: Int = 0,
        val trackFormatsImported: Int = 0,
        val songSearchImported: Int = 0,
        val venueSearchImported: Int = 0,
        val showSearchImported: Int = 0,
        val memberSearchImported: Int = 0,
        val collectionsImported: Int = 0,
        val collectionShowsImported: Int = 0
    ) : ImportResult() {
        val success: Boolean = true
    }
    
    data class Error(val error: String) : ImportResult() {
        val success: Boolean = false
    }
    
    data class RequiresUserChoice(
        val availableSources: com.deadarchive.core.database.v2.service.DatabaseManagerV2.AvailableSources
    ) : ImportResult() {
        val success: Boolean = false
    }
    
    companion object {
        fun success(shows: Int, venues: Int, songs: Int = 0, setlists: Int = 0, setlistSongs: Int = 0, 
                   recordings: Int = 0, tracks: Int = 0, trackFormats: Int = 0,
                   songSearch: Int = 0, venueSearch: Int = 0, showSearch: Int = 0, memberSearch: Int = 0,
                   collections: Int = 0, collectionShows: Int = 0) = 
            Success(shows, venues, songs, setlists, setlistSongs, recordings, tracks, trackFormats,
                   songSearch, venueSearch, showSearch, memberSearch, collections, collectionShows)
        fun error(message: String) = Error(message)
        fun requiresUserChoice(sources: com.deadarchive.core.database.v2.service.DatabaseManagerV2.AvailableSources) = 
            RequiresUserChoice(sources)
    }
}

@Singleton
class DataImportServiceV2 @Inject constructor(
    private val assetManager: AssetManagerV2,
    private val showDao: ShowV2Dao,
    private val venueDao: VenueV2Dao,
    private val dataVersionDao: DataVersionDao,
    private val songDao: SongV2Dao,
    private val setlistDao: SetlistV2Dao,
    private val setlistSongDao: SetlistSongV2Dao,
    private val recordingDao: RecordingV2Dao,
    private val trackDao: TrackV2Dao,
    private val trackFormatDao: TrackFormatV2Dao,
    private val searchTableProcessor: SearchTableProcessorV2,
    private val collectionProcessor: CollectionProcessorV2
) {
    companion object {
        private const val TAG = "DataImportServiceV2"
        private const val SHOWS_DIR = "shows"
        private const val RECORDINGS_DIR = "recordings"
        private const val SEARCH_DIR = "search"
        private const val COLLECTIONS_FILE = "collections.json"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
    }
    
    /**
     * Check if data-v2 files are available for import
     */
    fun hasDataFiles(): Boolean {
        return try {
            // Check if data.zip exists in assets
            assetManager.isDataZipAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check data files availability", e)
            false
        }
    }
    
    /**
     * Main import orchestrator - imports from assets if needed
     */
    suspend fun importFromAssetsIfNeeded(
        progressCallback: ((phase: String, total: Int, processed: Int, current: String) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Check if we already have data
            val currentVersion = dataVersionDao.getCurrentVersion()
            Log.d(TAG, "Current data version: $currentVersion")
            
            if (!assetManager.isDataZipAvailable()) {
                return@withContext ImportResult.error("Data zip file not found in assets")
            }
            
            // Extract zip to temp directory
            progressCallback?.invoke("EXTRACTING", 0, 0, "Extracting data files...")
            val tempDir = assetManager.extractDataZip()
            val zipVersion = assetManager.getDataVersion(tempDir)
            
            try {
                // Check if we need to import
                if (currentVersion == zipVersion) {
                    Log.d(TAG, "Data already up to date (version: $zipVersion)")
                    return@withContext ImportResult.success(0, 0)
                }
                
                Log.d(TAG, "Importing data version $zipVersion (current: $currentVersion)")
                
                // Import shows, venues, and setlists
                val showResult = importShowsVenuesAndSetlists(tempDir, progressCallback)
                
                when (showResult) {
                    is ImportResult.Success -> {
                        // Import recordings and tracks
                        val recordingResult = importRecordingsAndTracks(tempDir, progressCallback)
                        
                        when (recordingResult) {
                            is ImportResult.Success -> {
                                // Import search tables
                                val searchResult = importSearchTables(tempDir, progressCallback)
                                
                                when (searchResult) {
                                    is ImportResult.Success -> {
                                        // Import collections
                                        val collectionsResult = importCollections(tempDir, progressCallback)
                                        
                                        when (collectionsResult) {
                                            is ImportResult.Success -> {
                                                // Combine all results
                                                val combinedResult = ImportResult.success(
                                                    shows = showResult.showsImported,
                                                    venues = showResult.venuesImported,
                                                    songs = showResult.songsImported,
                                                    setlists = showResult.setlistsImported,
                                                    setlistSongs = showResult.setlistSongsImported,
                                                    recordings = recordingResult.recordingsImported,
                                                    tracks = recordingResult.tracksImported,
                                                    trackFormats = recordingResult.trackFormatsImported,
                                                    songSearch = searchResult.songSearchImported,
                                                    venueSearch = searchResult.venueSearchImported,
                                                    showSearch = searchResult.showSearchImported,
                                                    memberSearch = searchResult.memberSearchImported,
                                                    collections = collectionsResult.collectionsImported,
                                                    collectionShows = collectionsResult.collectionShowsImported
                                                )
                                                
                                                // Update version tracking
                                                updateDataVersion(tempDir, combinedResult.showsImported, combinedResult.venuesImported)
                                                Log.d(TAG, "Import completed: ${combinedResult.showsImported} shows, ${combinedResult.venuesImported} venues, ${combinedResult.songsImported} songs, ${combinedResult.setlistsImported} setlists, ${combinedResult.setlistSongsImported} song performances, ${combinedResult.recordingsImported} recordings, ${combinedResult.tracksImported} tracks, ${combinedResult.trackFormatsImported} formats, ${combinedResult.songSearchImported} song searches, ${combinedResult.venueSearchImported} venue searches, ${combinedResult.showSearchImported} show searches, ${combinedResult.memberSearchImported} member searches, ${combinedResult.collectionsImported} collections, ${combinedResult.collectionShowsImported} collection relationships")
                                                
                                                combinedResult
                                            }
                                            is ImportResult.Error -> collectionsResult
                                            is ImportResult.RequiresUserChoice -> collectionsResult
                                        }
                                    }
                                    is ImportResult.Error -> searchResult
                                    is ImportResult.RequiresUserChoice -> searchResult
                                }
                            }
                            is ImportResult.Error -> recordingResult
                            is ImportResult.RequiresUserChoice -> recordingResult
                        }
                    }
                    is ImportResult.Error -> showResult
                    is ImportResult.RequiresUserChoice -> showResult
                }
                
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
    private suspend fun importShowsVenuesAndSetlists(
        tempDir: File,
        progressCallback: ((phase: String, total: Int, processed: Int, current: String) -> Unit)? = null
    ): ImportResult {
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
        var songCount = 0
        var setlistCount = 0
        var setlistSongCount = 0
        var failedCount = 0
        val totalShows = showFiles.size
        
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
                
                // Import setlist data if available
                if (showData.setlistStatus == "found" && !showData.setlist.isNullOrEmpty()) {
                    val setlistResults = importSetlistsForShow(showData)
                    songCount += setlistResults.first
                    setlistCount += setlistResults.second
                    setlistSongCount += setlistResults.third
                }
                
                // Progress callback every 10 shows for more responsive UI
                if (showCount % 10 == 0 || showCount == totalShows) {
                    val currentShowName = "${showData.date} - ${showData.venue}"
                    progressCallback?.invoke("IMPORTING_SHOWS", totalShows, showCount, currentShowName)
                    Log.d(TAG, "Processed $showCount/$totalShows shows, $venueCount venues, $songCount songs, $setlistCount setlists, $setlistSongCount performances")
                }
                
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "Failed to process show file: ${showFile.name} (${e.message})")
                Log.d(TAG, "Error details for ${showFile.name}", e)
                // Continue processing other files
            }
        }
        
        Log.d(TAG, "Import completed: $showCount shows, $venueCount venues, $songCount songs, $setlistCount setlists, $setlistSongCount performances imported, $failedCount failed")
        
        // Update venue statistics after all shows are imported
        Log.d(TAG, "Computing venue statistics...")
        progressCallback?.invoke("COMPUTING_VENUES", venueCount, 0, "Computing venue statistics...")
        updateVenueStatistics()
        progressCallback?.invoke("COMPUTING_VENUES", venueCount, venueCount, "Venue statistics completed")
        
        return ImportResult.success(showCount, venueCount, songCount, setlistCount, setlistSongCount)
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
            setlistRaw = showData.setlist?.let { json.encodeToString(SetDataV2.serializer(), it.first()) }, // For now, just first set
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
    
    /**
     * Import setlists for a single show
     * Returns Triple(songCount, setlistCount, setlistSongCount)
     */
    private suspend fun importSetlistsForShow(showData: ShowJsonData): Triple<Int, Int, Int> {
        var songCount = 0
        var setlistCount = 0
        var setlistSongCount = 0
        
        try {
            showData.setlist?.forEachIndexed { setIndex, setData ->
                // Create or get setlist
                val setlist = setlistDao.getOrCreateSetlist(
                    showId = showData.showId,
                    setName = setData.setName,
                    setOrder = setIndex
                )
                setlistCount++
                
                // Process songs in this set
                setData.songs.forEachIndexed { songIndex, songData ->
                    // Check if song exists first
                    val existingSong = songDao.getSongByName(songData.name)
                    if (existingSong == null) songCount++ // New song will be created
                    
                    // Create or get song
                    val song = songDao.getOrCreateSong(songData.name, songData.url)
                    
                    // Create setlist-song relationship
                    val setlistSong = SetlistSongV2Entity(
                        setlistId = setlist.id,
                        songId = song.id,
                        position = songIndex + 1, // 1-based position
                        segueIntoNext = songData.segueIntoNext
                    )
                    setlistSongDao.insertSetlistSong(setlistSong)
                    setlistSongCount++
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import setlists for show: ${showData.showId}", e)
        }
        
        return Triple(songCount, setlistCount, setlistSongCount)
    }
    
    /**
     * Import recordings and tracks data from JSON files
     */
    private suspend fun importRecordingsAndTracks(
        tempDir: File,
        progressCallback: ((phase: String, total: Int, processed: Int, current: String) -> Unit)? = null
    ): ImportResult {
        val recordingsDir = File(tempDir, RECORDINGS_DIR)
        if (!recordingsDir.exists() || !recordingsDir.isDirectory) {
            Log.w(TAG, "Recordings directory not found, skipping recording import")
            return ImportResult.success(0, 0) // No recordings is not a failure
        }
        
        val recordingFiles = recordingsDir.listFiles { file -> file.name.endsWith(".json") }
        if (recordingFiles.isNullOrEmpty()) {
            Log.w(TAG, "No recording JSON files found")
            return ImportResult.success(0, 0)
        }
        
        Log.d(TAG, "Processing ${recordingFiles.size} recording files")
        
        var recordingCount = 0
        var trackCount = 0
        var trackFormatCount = 0
        var failedCount = 0
        val totalRecordings = recordingFiles.size
        
        recordingFiles.forEach { recordingFile ->
            try {
                // Extract recording identifier from filename
                val identifier = recordingFile.nameWithoutExtension
                
                // Parse recording JSON
                val recordingContent = recordingFile.readText()
                val recordingData = json.decodeFromString<RecordingJsonData>(recordingContent)
                
                // Map recording to show_id via date/venue matching
                val showId = findShowIdForRecording(recordingData)
                if (showId == null) {
                    Log.w(TAG, "Could not map recording $identifier to show (${recordingData.date} - ${recordingData.venue})")
                    failedCount++
                    return@forEach
                }
                
                // Create and insert recording entity
                val recording = RecordingV2Entity(
                    identifier = identifier,
                    showId = showId,
                    title = null, // Not available in current JSON structure
                    sourceType = recordingData.sourceType,
                    date = recordingData.date,
                    venue = recordingData.venue,
                    location = recordingData.location,
                    rating = recordingData.rating,
                    rawRating = recordingData.rawRating,
                    reviewCount = recordingData.reviewCount,
                    confidence = recordingData.confidence,
                    highRatings = recordingData.highRatings,
                    lowRatings = recordingData.lowRatings
                )
                
                recordingDao.insertRecording(recording)
                recordingCount++
                
                // Import tracks for this recording
                val trackResults = importTracksForRecording(identifier, recordingData.tracks)
                trackCount += trackResults.first
                trackFormatCount += trackResults.second
                
                // Progress callback every 100 recordings for performance
                if (recordingCount % 100 == 0 || recordingCount == totalRecordings) {
                    val currentRecordingName = "${recordingData.date} - ${recordingData.venue} ($identifier)"
                    progressCallback?.invoke("IMPORTING_RECORDINGS", totalRecordings, recordingCount, currentRecordingName)
                    Log.d(TAG, "Processed $recordingCount/$totalRecordings recordings, $trackCount tracks, $trackFormatCount formats")
                }
                
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "Failed to process recording file: ${recordingFile.name} (${e.message})")
                Log.d(TAG, "Error details for ${recordingFile.name}", e)
                // Continue processing other files
            }
        }
        
        Log.d(TAG, "Recording import completed: $recordingCount recordings, $trackCount tracks, $trackFormatCount formats imported, $failedCount failed")
        
        return ImportResult.success(
            shows = 0, venues = 0, songs = 0, setlists = 0, setlistSongs = 0,
            recordings = recordingCount, tracks = trackCount, trackFormats = trackFormatCount
        )
    }
    
    /**
     * Find show_id for a recording based on date and venue matching
     */
    private suspend fun findShowIdForRecording(recordingData: RecordingJsonData): String? {
        try {
            // Get all shows for this date
            val dateMatches = showDao.getShowsByDate(recordingData.date)
            
            if (dateMatches.isEmpty()) {
                Log.w(TAG, "No shows found for date: ${recordingData.date}")
                return null
            }
            
            if (dateMatches.size == 1) {
                // Single show on this date - use it
                Log.d(TAG, "Using single show match for recording: ${recordingData.date} - ${recordingData.venue}")
                return dateMatches.first().showId
            }
            
            // Multiple shows on same date - try to match by venue
            // For now, just use the first one since venue matching is complex
            // In a production system, we'd need more sophisticated venue normalization
            Log.d(TAG, "Multiple shows found for ${recordingData.date}, using first match. Recording venue: ${recordingData.venue}")
            return dateMatches.first().showId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding show for recording: ${recordingData.date} - ${recordingData.venue}", e)
            return null
        }
    }
    
    /**
     * Import tracks for a single recording
     * Returns Pair(trackCount, trackFormatCount)
     */
    private suspend fun importTracksForRecording(
        recordingId: String, 
        tracksData: List<TrackJsonData>
    ): Pair<Int, Int> {
        var trackCount = 0
        var trackFormatCount = 0
        
        try {
            tracksData.forEach { trackData ->
                // Create and insert track entity
                val track = TrackV2Entity(
                    recordingId = recordingId,
                    trackNumber = trackData.track,
                    title = trackData.title,
                    duration = trackData.duration
                )
                
                val trackId = trackDao.insertTrack(track)
                trackCount++
                
                // Import formats for this track
                trackData.formats.forEach { formatData ->
                    val trackFormat = TrackFormatV2Entity(
                        trackId = trackId,
                        format = formatData.format,
                        filename = formatData.filename,
                        bitrate = formatData.bitrate
                    )
                    
                    trackFormatDao.insertTrackFormat(trackFormat)
                    trackFormatCount++
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import tracks for recording: $recordingId", e)
        }
        
        return Pair(trackCount, trackFormatCount)
    }
    
    /**
     * Import search tables data from JSON files
     */
    private suspend fun importSearchTables(
        tempDir: File,
        progressCallback: ((phase: String, total: Int, processed: Int, current: String) -> Unit)? = null
    ): ImportResult {
        val searchDir = File(tempDir, SEARCH_DIR)
        if (!searchDir.exists() || !searchDir.isDirectory) {
            Log.w(TAG, "Search directory not found, skipping search table import")
            return ImportResult.success(0, 0) // No search tables is not a failure
        }
        
        Log.d(TAG, "Processing search tables from directory: ${searchDir.absolutePath}")
        
        var songSearchCount = 0
        var venueSearchCount = 0
        var showSearchCount = 0
        var memberSearchCount = 0
        var processedTables = 0
        val totalTables = 4 // songs, venues, shows_index, members
        
        try {
            // Process songs.json
            val songsFile = File(searchDir, "songs.json")
            if (songsFile.exists()) {
                progressCallback?.invoke("IMPORTING_SEARCH", totalTables, processedTables, "Processing song search data...")
                val songsContent = songsFile.readText()
                if (searchTableProcessor.processSongsJson(songsContent)) {
                    songSearchCount = 1 // Represents successful processing
                    Log.d(TAG, "Successfully processed songs search table")
                } else {
                    Log.w(TAG, "Failed to process songs search table")
                }
                processedTables++
            } else {
                Log.w(TAG, "songs.json not found in search directory")
            }
            
            // Process venues.json
            val venuesFile = File(searchDir, "venues.json")
            if (venuesFile.exists()) {
                progressCallback?.invoke("IMPORTING_SEARCH", totalTables, processedTables, "Processing venue search data...")
                val venuesContent = venuesFile.readText()
                if (searchTableProcessor.processVenuesJson(venuesContent)) {
                    venueSearchCount = 1
                    Log.d(TAG, "Successfully processed venues search table")
                } else {
                    Log.w(TAG, "Failed to process venues search table")
                }
                processedTables++
            } else {
                Log.w(TAG, "venues.json not found in search directory")
            }
            
            // Process shows_index.json
            val showsIndexFile = File(searchDir, "shows_index.json")
            if (showsIndexFile.exists()) {
                progressCallback?.invoke("IMPORTING_SEARCH", totalTables, processedTables, "Processing show search data...")
                val showsIndexContent = showsIndexFile.readText()
                if (searchTableProcessor.processShowsIndexJson(showsIndexContent)) {
                    showSearchCount = 1
                    Log.d(TAG, "Successfully processed shows index search table")
                } else {
                    Log.w(TAG, "Failed to process shows index search table")
                }
                processedTables++
            } else {
                Log.w(TAG, "shows_index.json not found in search directory")
            }
            
            // Process members.json
            val membersFile = File(searchDir, "members.json")
            if (membersFile.exists()) {
                progressCallback?.invoke("IMPORTING_SEARCH", totalTables, processedTables, "Processing member search data...")
                val membersContent = membersFile.readText()
                if (searchTableProcessor.processMembersJson(membersContent)) {
                    memberSearchCount = 1
                    Log.d(TAG, "Successfully processed members search table")
                } else {
                    Log.w(TAG, "Failed to process members search table")
                }
                processedTables++
            } else {
                Log.w(TAG, "members.json not found in search directory")
            }
            
            progressCallback?.invoke("IMPORTING_SEARCH", totalTables, processedTables, "Search tables import completed")
            Log.d(TAG, "Search tables import completed: $songSearchCount song tables, $venueSearchCount venue tables, $showSearchCount show tables, $memberSearchCount member tables")
            
            return ImportResult.success(
                shows = 0, venues = 0, songs = 0, setlists = 0, setlistSongs = 0,
                recordings = 0, tracks = 0, trackFormats = 0,
                songSearch = songSearchCount, venueSearch = venueSearchCount, 
                showSearch = showSearchCount, memberSearch = memberSearchCount
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing search tables", e)
            return ImportResult.error("Search tables import failed: ${e.message}")
        }
    }
    
    /**
     * Import collections data from collections.json
     */
    private suspend fun importCollections(
        tempDir: File,
        progressCallback: ((phase: String, total: Int, processed: Int, current: String) -> Unit)? = null
    ): ImportResult {
        try {
            Log.d(TAG, "Starting collections import...")
            progressCallback?.invoke("IMPORTING_COLLECTIONS", 0, 0, "Loading collections data...")
            
            val collectionsFile = File(tempDir, COLLECTIONS_FILE)
            if (!collectionsFile.exists()) {
                Log.w(TAG, "collections.json not found, skipping collections import")
                return ImportResult.success(
                    shows = 0, venues = 0, songs = 0, setlists = 0, setlistSongs = 0,
                    recordings = 0, tracks = 0, trackFormats = 0,
                    songSearch = 0, venueSearch = 0, showSearch = 0, memberSearch = 0,
                    collections = 0, collectionShows = 0
                )
            }
            
            progressCallback?.invoke("IMPORTING_COLLECTIONS", 1, 0, "Processing collections.json...")
            
            val collectionsContent = collectionsFile.readText()
            Log.d(TAG, "Read collections.json file (${collectionsContent.length} characters)")
            
            val success = collectionProcessor.processCollectionsJson(collectionsContent)
            
            if (success) {
                // Get stats to report counts
                val stats = collectionProcessor.getCollectionStats()
                
                progressCallback?.invoke("IMPORTING_COLLECTIONS", 1, 1, "Collections import completed")
                Log.d(TAG, "Collections import completed: ${stats.collectionCount} collections, ${stats.relationshipCount} relationships")
                
                return ImportResult.success(
                    shows = 0, venues = 0, songs = 0, setlists = 0, setlistSongs = 0,
                    recordings = 0, tracks = 0, trackFormats = 0,
                    songSearch = 0, venueSearch = 0, showSearch = 0, memberSearch = 0,
                    collections = stats.collectionCount, collectionShows = stats.relationshipCount
                )
            } else {
                Log.e(TAG, "Failed to process collections.json")
                return ImportResult.error("Collections processing failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing collections", e)
            return ImportResult.error("Collections import failed: ${e.message}")
        }
    }
    
}