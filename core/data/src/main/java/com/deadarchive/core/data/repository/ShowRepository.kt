package com.deadarchive.core.data.repository

import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.RecordingEntity
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.database.LibraryDao
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.mapper.*
import com.deadarchive.core.data.service.AudioFormatFilterService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

interface ShowRepository {
    // Show-based methods
    fun searchShows(query: String): Flow<List<Show>>
    fun getAllShows(): Flow<List<Show>>
    suspend fun getRecordingsByShowId(showId: String): List<Recording>
    
    // Recording-based methods (individual recordings)
    fun searchRecordings(query: String): Flow<List<Recording>>
    suspend fun getRecordingById(id: String): Recording?
    suspend fun getRecordingByIdWithFormatFilter(id: String, formatPreferences: List<String>): Recording?
    fun getLibraryRecordings(): Flow<List<Recording>>
    fun getAllCachedRecordings(): Flow<List<Recording>>
    
    // Streaming URL generation methods
    suspend fun getRecordingMetadata(identifier: String): ArchiveMetadataResponse?
    suspend fun getStreamingUrl(identifier: String, filename: String): String?
    suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>>
    suspend fun getPreferredStreamingUrl(identifier: String): String?
    suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String?
    
    // Debug methods
    suspend fun debugDatabaseState(): String
    suspend fun getShowEntityById(showId: String): ShowEntity?
    suspend fun getShowById(showId: String): Show?
}

@Singleton
class ShowRepositoryImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService,
    private val recordingDao: RecordingDao,
    private val showDao: ShowDao,
    private val libraryDao: LibraryDao,
    private val audioFormatFilterService: AudioFormatFilterService
) : ShowRepository {
    
    companion object {
        private const val TAG = "ShowRepository"
        private const val CACHE_EXPIRY_HOURS = 24
    }
    
    override fun getAllShows(): Flow<List<Show>> = flow {
        android.util.Log.d("ShowRepository", "📋 getAllShows: Starting to retrieve all shows from database")
        try {
            // Get all shows from database with their recordings
            val showEntities = showDao.getAllShows()
            android.util.Log.d("ShowRepository", "📋 Found ${showEntities.size} show entities in database")
            
            val shows = showEntities.map { showEntity ->
                // Get recordings for this show
                val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { it.toRecording() }
                android.util.Log.d("ShowRepository", "📋 Show '${showEntity.showId}' has ${recordings.size} recordings from database")
                showEntity.toShow(recordings)
            }
            
            android.util.Log.d("ShowRepository", "📋 Successfully retrieved ${shows.size} shows from database")
            emit(shows)
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "📋 ❌ Failed to get shows from database: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    override suspend fun getRecordingsByShowId(showId: String): List<Recording> {
        return try {
            recordingDao.getRecordingsByConcertId(showId).map { it.toRecording() }
        } catch (e: Exception) {
            println("ERROR: Failed to get recordings for show $showId: ${e.message}")
            emptyList()
        }
    }
    
    override fun searchShows(query: String): Flow<List<Show>> = flow {
        android.util.Log.d("ShowRepository", "🔍 searchShows called with query: '$query'")
        try {
            // ONLY search shows from database - shows should already exist from initial setup
            val cachedShows = showDao.searchShows(query)
            android.util.Log.d("ShowRepository", "🔍 Found ${cachedShows.size} show entities from database search")
            
            // Use actual Show entities with their recordings
            val databaseShows = cachedShows.map { showEntity ->
                val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { it.toRecording() }
                android.util.Log.d("ShowRepository", "🔍 Database show '${showEntity.showId}' has ${recordings.size} recordings")
                showEntity.toShow(recordings)
            }
            
            android.util.Log.d("ShowRepository", "🔍 ✅ Emitting ${databaseShows.size} shows from database")
            databaseShows.forEach { show ->
                android.util.Log.d("ShowRepository", "🔍   Show: '${show.showId}', date='${show.date}', venue='${show.venue}', ${show.recordings.size} recordings")
            }
            emit(databaseShows.sortedByDescending { it.date })
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "🔍 ❌ Error in searchShows: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    override fun searchRecordings(query: String): Flow<List<Recording>> = flow {
        try {
            // ONLY search locally cached recordings - no API fallback
            val cachedRecordings = recordingDao.searchRecordings(query).map { it.toRecording() }
            android.util.Log.d("ShowRepository", "🔍 searchRecordings found ${cachedRecordings.size} recordings from database")
            emit(cachedRecordings)
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "🔍 ❌ Error in searchRecordings: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    override suspend fun getRecordingById(id: String): Recording? {
        println("DEBUG: getRecordingById called with ID: '$id'")
        return try {
            // Check local cache first
            val cachedEntity = recordingDao.getRecordingById(id)
            println("DEBUG: getRecordingById found entity: ${cachedEntity != null}")
            if (cachedEntity != null) {
                println("DEBUG: Entity identifier: '${cachedEntity.identifier}', concertId: '${cachedEntity.concertId}'")
            }
            
            if (cachedEntity != null && !isCacheExpired(cachedEntity.cachedTimestamp)) {
                val recording = cachedEntity.toRecording()
                println("DEBUG: getRecordingById cached recording - title: ${recording.title}, tracks: ${recording.tracks.size}")
                println("DEBUG: getRecordingById entity tracksJson: ${if(cachedEntity.tracksJson != null) "not null" else "null"}")
                
                // Force refresh if cached recording has no tracks (likely cached before track fetching)
                if (recording.tracks.isNotEmpty()) {
                    println("DEBUG: getRecordingById using cached recording (not expired, has tracks)")
                    return recording
                } else {
                    println("DEBUG: getRecordingById cached recording has no tracks, forcing API refresh")
                }
            } else {
                println("DEBUG: getRecordingById cache miss or expired - entity: ${cachedEntity != null}, expired: ${cachedEntity?.let { isCacheExpired(it.cachedTimestamp) }}")
            }
            
            // Fetch from API if not cached, expired, or has no tracks
            println("DEBUG: getRecordingById fetching from API (cache miss, expired, or no tracks)")
            val metadata = getRecordingMetadata(id)
            metadata?.let { 
                println("DEBUG: getRecordingById API metadata received")
                val recording = ArchiveMapper.run { it.toRecording() }
                println("DEBUG: getRecordingById mapped recording - title: ${recording.title}, tracks: ${recording.tracks.size}")
                
                // Cache the result with tracks
                val isInLibrary = false // Individual recordings don't have library status anymore
                val newEntity = RecordingEntity.fromRecording(recording, cachedEntity?.concertId ?: "unknown").copy(
                    isInLibrary = isInLibrary,
                    cachedTimestamp = System.currentTimeMillis()
                )
                println("DEBUG: getRecordingById caching recording entity with ${recording.tracks.size} tracks")
                recordingDao.insertRecording(newEntity)
                
                val finalRecording = recording.copy(isInLibrary = isInLibrary)
                println("DEBUG: getRecordingById returning API recording - tracks: ${finalRecording.tracks.size}")
                finalRecording
            } ?: run {
                println("WARN: getRecordingById no metadata received from API")
                null
            }
        } catch (e: Exception) {
            println("ERROR: getRecordingById exception occurred: ${e.message}")
            // Return cached version if available, even if expired
            recordingDao.getRecordingById(id)?.let { entity ->
                println("DEBUG: getRecordingById falling back to expired cache")
                val recording = entity.toRecording()
                println("DEBUG: getRecordingById fallback recording - tracks: ${recording.tracks.size}")
                recording
            }
        }
    }
    
    override suspend fun getRecordingByIdWithFormatFilter(id: String, formatPreferences: List<String>): Recording? {
        println("DEBUG: getRecordingByIdWithFormatFilter called with ID: '$id', preferences: $formatPreferences")
        
        // First get the full recording with all tracks
        val recording = getRecordingById(id) ?: return null
        
        // Apply format filtering to tracks
        val filteredTracks = audioFormatFilterService.filterTracksByPreferredFormat(
            tracks = recording.tracks,
            formatPreferences = formatPreferences
        )
        
        println("DEBUG: getRecordingByIdWithFormatFilter filtered ${recording.tracks.size} tracks to ${filteredTracks.size}")
        
        // Return recording with filtered tracks
        return recording.copy(tracks = filteredTracks)
    }
    
    override fun getLibraryRecordings(): Flow<List<Recording>> {
        return recordingDao.getLibraryRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }
    
    override fun getAllCachedRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings().map { entities ->
            entities.map { it.toRecording() }
        }
    }
    
    override suspend fun getRecordingMetadata(identifier: String): ArchiveMetadataResponse? {
        return try {
            val response = archiveApiService.getRecordingMetadata(identifier)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getStreamingUrl(identifier: String, filename: String): String? {
        return "https://archive.org/download/$identifier/$filename"
    }
    
    override suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>> {
        return emptyList()
    }
    
    override suspend fun getPreferredStreamingUrl(identifier: String): String? {
        return getStreamingUrl(identifier, "")
    }
    
    override suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String? {
        return getStreamingUrl(identifier, trackQuery)
    }
    
    // NOTE: This method is kept for potential future use in database initialization
    // but should NOT be called during normal browse operations
    private suspend fun searchRecordingsViaApi(query: String): List<Recording> {
        android.util.Log.d("ShowRepository", "🌐 searchRecordingsViaApi: Starting API search for query '$query'")
        return try {
            val searchQuery = when {
                query.matches(Regex("\\d{4}")) -> "collection:GratefulDead AND date:$query*"
                query.isBlank() -> "collection:GratefulDead"
                else -> "collection:GratefulDead AND ($query)"
            }
            android.util.Log.d("ShowRepository", "🌐 Transformed query: '$searchQuery'")
            
            val response = archiveApiService.searchRecordings(searchQuery, rows = 50)
            if (response.isSuccessful) {
                val recordings = response.body()?.response?.docs?.map { doc ->
                    com.deadarchive.core.network.mapper.ArchiveMapper.run { 
                        doc.toRecording() 
                    }
                } ?: emptyList()
                
                android.util.Log.d("ShowRepository", "🌐 API returned ${recordings.size} recordings")
                
                // Save recordings to database (shows will be created by the emergency fallback logic)
                if (recordings.isNotEmpty()) {
                    android.util.Log.d("ShowRepository", "🌐 About to save ${recordings.size} API recordings to database")
                    try {
                        // Save recordings with proper showId associations
                        val recordingEntities = recordings.map { recording ->
                            val normalizedDate = normalizeDate(recording.concertDate)
                            val showId = "${normalizedDate}_${normalizeVenue(recording.concertVenue)}"
                            android.util.Log.d("ShowRepository", "🌐 Mapping recording ${recording.identifier} to showId '$showId'")
                            RecordingEntity.fromRecording(recording, showId)
                        }
                        
                        recordingDao.insertRecordings(recordingEntities)
                        android.util.Log.d("ShowRepository", "🌐 ✅ Saved ${recordings.size} API recordings to database")
                    } catch (e: Exception) {
                        android.util.Log.e("ShowRepository", "🌐 ❌ Failed to save API recordings: ${e.message}", e)
                    }
                } else {
                    android.util.Log.d("ShowRepository", "🌐 No recordings returned from API")
                }
                
                android.util.Log.d("ShowRepository", "🌐 Returning ${recordings.size} recordings from API")
                recordings
            } else {
                android.util.Log.w("ShowRepository", "🌐 API response was not successful: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "🌐 ❌ Exception in searchRecordingsViaApi: ${e.message}", e)
            emptyList()
        }
    }
    
    override suspend fun debugDatabaseState(): String {
        val showCount = showDao.getShowCount()
        val allShows = showDao.getAllShows()
        val recordingCount = recordingDao.getAllRecordings().firstOrNull()?.size ?: 0
        
        val report = StringBuilder()
        report.append("=== DATABASE STATE DEBUG ===\n")
        report.append("Total shows: $showCount\n")
        report.append("Total recordings: $recordingCount\n")
        report.append("\nFirst 5 shows:\n")
        
        allShows.take(5).forEach { show ->
            val recordings = recordingDao.getRecordingsByConcertId(show.showId)
            report.append("- Show: ${show.showId} (${show.date} at ${show.venue}) -> ${recordings.size} recordings\n")
            recordings.take(3).forEach { recording ->
                report.append("  * Recording: ${recording.identifier} (concertId: ${recording.concertId})\n")
            }
        }
        
        report.append("\nFirst 5 recordings:\n")
        recordingDao.getAllRecordings().firstOrNull()?.take(5)?.forEach { recording ->
            report.append("- Recording: ${recording.identifier} -> concertId: ${recording.concertId}\n")
        }
        
        return report.toString()
    }
    
    override suspend fun getShowEntityById(showId: String): ShowEntity? {
        return try {
            showDao.getShowById(showId)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching ShowEntity: $showId", e)
            null
        }
    }
    
    override suspend fun getShowById(showId: String): Show? {
        return try {
            val showEntity = showDao.getShowById(showId) ?: return null
            val recordings = recordingDao.getRecordingsByConcertId(showId).map { it.toRecording() }
            val isInLibrary = libraryDao.isShowInLibrary(showId)
            
            Show(
                date = showEntity.date,
                venue = showEntity.venue,
                location = showEntity.location,
                year = showEntity.year,
                recordings = recordings,
                isInLibrary = isInLibrary
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching Show: $showId", e)
            null
        }
    }
    
    /**
     * CANONICAL show creation method - the ONLY way to create shows from recordings
     * Creates ShowEntity records in database and returns Show objects
     */
    private suspend fun createAndSaveShowsFromRecordings(recordings: List<Recording>): List<Show> {
        android.util.Log.d("ShowRepository", "🔧 createAndSaveShowsFromRecordings: Processing ${recordings.size} recordings")
        android.util.Log.d("ShowRepository", "🔧 Recording identifiers: ${recordings.map { it.identifier }}")
        
        // Group recordings by normalized date + venue
        val groupedRecordings = recordings.groupBy { recording ->
            val normalizedDate = normalizeDate(recording.concertDate)
            val normalizedVenue = normalizeVenue(recording.concertVenue)
            val showId = "${normalizedDate}_${normalizedVenue}"
            android.util.Log.d("ShowRepository", "🔧 Recording ${recording.identifier}: date='${recording.concertDate}' → '$normalizedDate', venue='${recording.concertVenue}' → '$normalizedVenue', showId='$showId'")
            showId
        }
        
        android.util.Log.d("ShowRepository", "🔧 Grouped recordings into ${groupedRecordings.size} potential shows:")
        groupedRecordings.forEach { (showId, recordings) ->
            android.util.Log.d("ShowRepository", "🔧   Show '$showId': ${recordings.size} recordings [${recordings.map { it.identifier }}]")
        }
        
        val shows = mutableListOf<Show>()
        val newShowEntities = mutableListOf<ShowEntity>()
        val failedShows = mutableListOf<String>()
        
        for ((_, recordingGroup) in groupedRecordings) {
            try {
                val firstRecording = recordingGroup.first()
                val normalizedDate = normalizeDate(firstRecording.concertDate)
                
                // Validate we can create a proper show
                if (normalizedDate.isBlank()) {
                    failedShows.add("Invalid date for recordings: ${recordingGroup.map { it.identifier }}")
                    continue
                }
                
                val normalizedVenue = normalizeVenue(firstRecording.concertVenue)
                val showId = "${normalizedDate}_${normalizedVenue}"
                
                // Debug: Show venue normalization in action
                if (firstRecording.concertVenue != normalizedVenue) {
                    android.util.Log.d("ShowRepository", "Venue normalized: '${firstRecording.concertVenue}' → '$normalizedVenue'")
                }
                
                // Check if ShowEntity already exists
                val showExists = showDao.showExists(showId)
                android.util.Log.d("ShowRepository", "🔧 Checking if show '$showId' exists in database: $showExists")
                
                if (showExists) {
                    android.util.Log.d("ShowRepository", "🔧 SKIPPING show creation for '$showId' - already exists in database")
                } else {
                    android.util.Log.d("ShowRepository", "🔧 WILL CREATE new show entity for '$showId'")
                }
                
                if (!showExists) {
                    // Create new ShowEntity - use original venue name for display
                    val showEntity = ShowEntity(
                        showId = showId, // Uses normalized venue for deduplication
                        date = normalizedDate,
                        venue = firstRecording.concertVenue, // Keep original readable venue name
                        location = firstRecording.concertLocation,
                        year = normalizedDate.take(4),
                        setlistRaw = null,
                        setsJson = null,
                        isInLibrary = false, // Will be updated when added to library
                        cachedTimestamp = System.currentTimeMillis()
                    )
                    newShowEntities.add(showEntity)
                    android.util.Log.d("ShowRepository", "🔧 Added new ShowEntity to creation list: $showId with ${recordingGroup.size} recordings")
                    android.util.Log.d("ShowRepository", "🔧   ShowEntity details: date='$normalizedDate', venue='${firstRecording.concertVenue}', location='${firstRecording.concertLocation}'")
                }
                
                // Create Show object - use ORIGINAL venue name for display, normalized only for showId
                val isInLibrary = libraryDao.isShowInLibrary(showId)
                val show = Show(
                    date = normalizedDate,
                    venue = firstRecording.concertVenue, // Keep original readable venue name
                    location = firstRecording.concertLocation,
                    year = normalizedDate.take(4),
                    recordings = recordingGroup,
                    isInLibrary = isInLibrary
                )
                shows.add(show)
                
            } catch (e: Exception) {
                failedShows.add("Failed to create show from recordings ${recordingGroup.map { it.identifier }}: ${e.message}")
                android.util.Log.e("ShowRepository", "Failed to create show from recordings: ${e.message}")
            }
        }
        
        // Save new ShowEntity records
        if (newShowEntities.isNotEmpty()) {
            android.util.Log.d("ShowRepository", "🔧 About to save ${newShowEntities.size} new ShowEntity records to database:")
            newShowEntities.forEach { entity ->
                android.util.Log.d("ShowRepository", "🔧   Saving ShowEntity: showId='${entity.showId}', date='${entity.date}', venue='${entity.venue}'")
            }
            try {
                showDao.insertShows(newShowEntities)
                android.util.Log.d("ShowRepository", "🔧 ✅ Successfully saved ${newShowEntities.size} new ShowEntity records to database")
                
                // Verify what we actually saved
                newShowEntities.forEach { entity ->
                    val exists = showDao.showExists(entity.showId)
                    android.util.Log.d("ShowRepository", "🔧 Post-save verification: '${entity.showId}' exists in database: $exists")
                }
            } catch (e: Exception) {
                android.util.Log.e("ShowRepository", "🔧 ❌ CRITICAL: Failed to save ShowEntity records: ${e.message}")
                android.util.Log.e("ShowRepository", "🔧 Exception details:", e)
                failedShows.add("Database save failed: ${e.message}")
            }
        } else {
            android.util.Log.d("ShowRepository", "🔧 No new ShowEntity records to save - all shows already exist")
        }
        
        // Log any failures
        if (failedShows.isNotEmpty()) {
            android.util.Log.w("ShowRepository", "Failed to create ${failedShows.size} shows:")
            failedShows.forEach { failure ->
                android.util.Log.w("ShowRepository", "  - $failure")
            }
        }
        
        android.util.Log.d("ShowRepository", "🔧 ✅ Successfully created ${shows.size} shows from ${recordings.size} recordings")
        android.util.Log.d("ShowRepository", "🔧 Final show summary:")
        shows.forEach { show ->
            android.util.Log.d("ShowRepository", "🔧   Show: showId='${show.showId}', date='${show.date}', venue='${show.venue}', ${show.recordings.size} recordings")
        }
        return shows.sortedByDescending { it.date }
    }
    
    /**
     * Normalize date from potentially timestamped format to simple YYYY-MM-DD format
     */
    private fun normalizeDate(date: String?): String {
        if (date.isNullOrBlank()) return ""
        return if (date.contains("T")) {
            date.substringBefore("T")
        } else {
            date
        }
    }
    
    /**
     * Normalize venue name to eliminate duplicates caused by inconsistent venue names
     */
    private fun normalizeVenue(venue: String?): String {
        if (venue.isNullOrBlank()) return "Unknown"
        
        return venue
            // Remove punctuation that causes issues
            .replace("'", "")      // Veterans' -> Veterans
            .replace("'", "")      // Smart quote
            .replace(".", "")      // U.C.S.B. -> UCSB
            .replace("\"", "")     // Remove quotes
            .replace("(", "_")     // Convert parens to underscores
            .replace(")", "_")
            
            // Normalize separators
            .replace(" - ", "_")   // Common separator
            .replace(" – ", "_")   // Em dash
            .replace(", ", "_")    // Comma separator
            .replace(" & ", "_and_")
            .replace("&", "_and_")
            
            // Standardize common word variations
            .replace("Theatre", "Theater", ignoreCase = true)
            .replace("Center", "Center", ignoreCase = true)  // Keep consistent
            .replace("Coliseum", "Coliseum", ignoreCase = true)
            
            // University abbreviations (most common cases)
            .replace(" University", "_U", ignoreCase = true)
            .replace(" College", "_C", ignoreCase = true)
            .replace(" State", "_St", ignoreCase = true)
            .replace("Memorial", "Mem", ignoreCase = true)
            .replace("Auditorium", "Aud", ignoreCase = true)
            .replace("Stadium", "Stad", ignoreCase = true)
            
            // Remove common filler words
            .replace(" The ", "_", ignoreCase = true)
            .replace("The ", "", ignoreCase = true)
            .replace(" of ", "_", ignoreCase = true)
            .replace(" at ", "_", ignoreCase = true)
            
            // Clean up and normalize
            .replace(Regex("\\s+"), "_")     // Any whitespace to underscore
            .replace(Regex("_+"), "_")       // Multiple underscores to single
            .trim('_')                       // Remove leading/trailing underscores
            .lowercase()                     // Consistent case
    }

    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        val expiryTime = timestamp + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
        return System.currentTimeMillis() > expiryTime
    }
}