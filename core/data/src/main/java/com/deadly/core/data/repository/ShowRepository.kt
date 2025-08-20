package com.deadly.core.data.repository

import com.deadly.core.database.RecordingDao
import com.deadly.core.database.RecordingEntity
import com.deadly.core.database.ShowDao
import com.deadly.core.database.ShowEntity
import com.deadly.core.database.LibraryDao
import com.deadly.core.model.Recording
import com.deadly.core.model.Show
import com.deadly.core.model.AudioFile
import com.deadly.core.network.ArchiveApiService
import com.deadly.core.network.model.ArchiveMetadataResponse
import com.deadly.core.network.mapper.*
import com.deadly.core.model.util.VenueUtil
import com.deadly.core.data.service.AudioFormatFilterService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShowRepositoryImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService,
    private val recordingDao: RecordingDao,
    private val showDao: ShowDao,
    private val libraryDao: LibraryDao,
    private val audioFormatFilterService: AudioFormatFilterService,
    private val ratingsRepository: RatingsRepository,
    private val settingsRepository: com.deadly.core.settings.api.SettingsRepository,
    private val showEnrichmentService: com.deadly.core.data.service.ShowEnrichmentService,
    private val showCacheService: com.deadly.core.data.service.ShowCacheService,
    private val showCreationService: com.deadly.core.data.service.ShowCreationService
) : com.deadly.core.data.api.repository.ShowRepository {
    
    companion object {
        private const val TAG = "ShowRepository"
    }
    
    override fun getAllShows(): Flow<List<Show>> = flow {
        android.util.Log.d("ShowRepository", "📋 getAllShows: Starting to retrieve all shows from database")
        
        // Get user preferences once for all shows
        val userPreferences = settingsRepository.getSettings().firstOrNull()?.recordingPreferences ?: emptyMap()
        
        // Get all shows from database with their recordings
        val showEntities = showDao.getAllShows()
        android.util.Log.d("ShowRepository", "📋 Found ${showEntities.size} show entities in database")
        
        val shows = showEntities.map { showEntity ->
            showEnrichmentService.enrichShowWithRatings(showEntity, userPreferences)
        }
        
        android.util.Log.d("ShowRepository", "📋 Successfully retrieved ${shows.size} shows from database")
        emit(shows)
    }.catch { e ->
        android.util.Log.e("ShowRepository", "📋 ❌ Failed to get shows from database: ${e.message}", e)
        emit(emptyList())
    }
    
    override fun getLibraryShows(): Flow<List<Show>> = 
        // Use reactive flow that automatically updates when library changes
        showDao.getLibraryShowsFlow()
            .flatMapLatest { libraryShowEntities ->
                flow {
                    android.util.Log.d("ShowRepository", "📚 getLibraryShows: Starting to retrieve library shows from database")
                    
                    // Get user preferences once for all shows
                    val userPreferences = settingsRepository.getSettings().firstOrNull()?.recordingPreferences ?: emptyMap()
                    
                    android.util.Log.d("ShowRepository", "📚 Found ${libraryShowEntities.size} library show entities in database")
        
                    val shows = libraryShowEntities.map { showEntity ->
                        showEnrichmentService.enrichShowWithRatings(showEntity, userPreferences)
                    }
        
                    android.util.Log.d("ShowRepository", "📚 Successfully retrieved ${shows.size} library shows from database")
                    emit(shows)
                }
            }
            .catch { e ->
                android.util.Log.e("ShowRepository", "📚 ❌ Failed to get library shows from database: ${e.message}", e)
                emit(emptyList())
            }
    
    override suspend fun getLibraryShowsList(): List<Show> {
        return try {
            android.util.Log.d("ShowRepository", "📚 getLibraryShowsList: Starting to retrieve library shows from database")
            
            // Get only library shows from database
            val libraryShowEntities = showDao.getLibraryShows()
            android.util.Log.d("ShowRepository", "📚 Found ${libraryShowEntities.size} library show entities in database")
            
            libraryShowEntities.map { showEntity ->
                showEnrichmentService.enrichShowWithRatings(showEntity, emptyMap())
            }.also { shows ->
                android.util.Log.d("ShowRepository", "📚 Successfully retrieved ${shows.size} library shows from database")
            }
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "📚 ❌ Failed to get library shows list from database: ${e.message}", e)
            emptyList()
        }
    }
    
    override suspend fun getRecordingsByShowId(showId: String): List<Recording> {
        return try {
            showEnrichmentService.attachRecordingsToShow(showId)
        } catch (e: Exception) {
            println("ERROR: Failed to get recordings for show $showId: ${e.message}")
            emptyList()
        }
    }
    
    override fun searchShowsLimited(query: String, limit: Int): Flow<List<Show>> = 
        // Use reactive flow that automatically updates when library changes
        showDao.searchShowsLimitedFlow(query, limit)
            .flatMapLatest { limitedSearchEntities ->
                flow {
                    android.util.Log.d("ShowRepository", "🔍 searchShowsLimited called with query: '$query', limit: $limit")
                    
                    // Get user preferences once for all shows
                    val userPreferences = settingsRepository.getSettings().firstOrNull()?.recordingPreferences ?: emptyMap()
                    
                    android.util.Log.d("ShowRepository", "🔍 Found ${limitedSearchEntities.size} show entities from reactive limited database search")
                    
                    // Process only the limited results with ratings
                    val databaseShows = limitedSearchEntities.map { showEntity ->
                        showEnrichmentService.enrichShowWithRatings(showEntity, userPreferences)
                    }
                    
                    android.util.Log.d("ShowRepository", "🔍 ✅ Emitting ${databaseShows.size} limited shows from reactive database")
                    emit(databaseShows.sortedByDescending { it.date })
                }
            }
            .catch { e ->
                android.util.Log.e("ShowRepository", "🔍 ❌ Error in reactive searchShowsLimited: ${e.message}", e)
                emit(emptyList())
            }

    override fun searchShows(query: String): Flow<List<Show>> = 
        // Use reactive flow that automatically updates when library changes
        showDao.searchShowsFlow(query)
            .flatMapLatest { searchResultEntities ->
                flow {
                    android.util.Log.d("ShowRepository", "🔍 searchShows called with query: '$query'")
                    
                    // Get user preferences once for all shows
                    val userPreferences = settingsRepository.getSettings().firstOrNull()?.recordingPreferences ?: emptyMap()
                    
                    android.util.Log.d("ShowRepository", "🔍 Found ${searchResultEntities.size} show entities from reactive database search")
                    
                    // Use actual Show entities with their recordings
                    val databaseShows = searchResultEntities.map { showEntity ->
                        val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, userPreferences)
                        
                        // Log warning for orphaned shows but still return them for debugging
                        if (enrichedShow.recordings.isEmpty()) {
                            android.util.Log.w("ShowRepository", "🔍 ⚠️ Found orphaned show '${showEntity.showId}' with 0 recordings")
                        }
                        
                        enrichedShow
                    }
                    
                    android.util.Log.d("ShowRepository", "🔍 ✅ Emitting ${databaseShows.size} shows from reactive database")
                    databaseShows.take(3).forEach { show ->
                        android.util.Log.d("ShowRepository", "🔍   Show: '${show.showId}', date='${show.date}', venue='${show.venue}', ${show.recordings.size} recordings, isInLibrary='${show.isInLibrary}'")
                    }
                    emit(databaseShows.sortedByDescending { it.date })
                }
            }
            .catch { e ->
                android.util.Log.e("ShowRepository", "🔍 ❌ Error in reactive searchShows: ${e.message}", e)
                emit(emptyList())
            }
    
    override fun searchRecordings(query: String): Flow<List<Recording>> = flow {
        // ONLY search locally cached recordings - no API fallback
        val cachedRecordings = recordingDao.searchRecordings(query).map { recordingEntity ->
            val recording = recordingEntity.toRecording()
            showEnrichmentService.enrichRecordingWithRating(recording)
        }
        android.util.Log.d("ShowRepository", "🔍 searchRecordings found ${cachedRecordings.size} recordings from database")
        emit(cachedRecordings)
    }.catch { e ->
        android.util.Log.e("ShowRepository", "🔍 ❌ Error in searchRecordings: ${e.message}", e)
        emit(emptyList())
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
            
            if (cachedEntity != null && !showCacheService.isCacheExpired(cachedEntity.cachedTimestamp)) {
                val recording = cachedEntity.toRecording()
                println("DEBUG: getRecordingById cached recording - title: ${recording.title}, tracks: ${recording.tracks.size}")
                println("DEBUG: getRecordingById entity tracksJson: ${if(cachedEntity.tracksJson != null) "not null" else "null"}")
                
                // Force refresh if cached recording has no tracks (likely cached before track fetching)
                if (recording.tracks.isNotEmpty()) {
                    println("DEBUG: getRecordingById using cached recording (not expired, has tracks)")
                    // Add recording rating
                    return showEnrichmentService.enrichRecordingWithRating(recording)
                } else {
                    println("DEBUG: getRecordingById cached recording has no tracks, forcing API refresh")
                }
            } else {
                println("DEBUG: getRecordingById cache miss or expired - entity: ${cachedEntity != null}, expired: ${cachedEntity?.let { showCacheService.isCacheExpired(it.cachedTimestamp) }}")
            }
            
            // Fetch from API if not cached, expired, or has no tracks
            println("DEBUG: getRecordingById fetching from API (cache miss, expired, or no tracks)")
            val metadata = getRecordingMetadata(id)
            metadata?.let { 
                println("DEBUG: getRecordingById API metadata received")
                val recording = ArchiveMapper.run { it.toRecording() }
                println("DEBUG: getRecordingById mapped recording - title: ${recording.title}, tracks: ${recording.tracks.size}")
                
                // Cache the result with tracks  
                val newEntity = RecordingEntity.fromRecording(recording, cachedEntity?.concertId ?: "unknown").copy(
                    cachedTimestamp = System.currentTimeMillis()
                )
                println("DEBUG: getRecordingById caching recording entity with ${recording.tracks.size} tracks")
                recordingDao.insertRecording(newEntity)
                
                // Add recording rating
                val finalRecording = showEnrichmentService.enrichRecordingWithRating(recording)
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
                // Add recording rating
                showEnrichmentService.enrichRecordingWithRating(recording)
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
    
    
    override fun getAllCachedRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings().map { entities ->
            entities.map { recordingEntity ->
                val recording = recordingEntity.toRecording()
                showEnrichmentService.enrichRecordingWithRating(recording)
            }
        }
    }
    
    suspend fun getRecordingMetadata(identifier: String): ArchiveMetadataResponse? {
        return showCacheService.getRecordingMetadata(identifier)
    }
    
    override suspend fun getStreamingUrl(identifier: String, filename: String): String? {
        return "https://archive.org/download/$identifier/$filename"
    }
    
    override suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>> {
        return try {
            android.util.Log.d("ShowRepository", "🎵 getTrackStreamingUrls: Fetching metadata for $identifier")
            
            // Get metadata from Archive.org
            val metadata = getRecordingMetadata(identifier)
            if (metadata == null) {
                android.util.Log.w("ShowRepository", "🎵 getTrackStreamingUrls: No metadata found for $identifier")
                return emptyList()
            }
            
            android.util.Log.d("ShowRepository", "🎵 getTrackStreamingUrls: Found ${metadata.files.size} files for $identifier")
            
            // Filter audio files and create streaming URLs
            val audioFiles = metadata.files.filter { file ->
                val isAudio = showCacheService.isAudioFile(file.name)
                val hasSize = (file.size?.toLongOrNull() ?: 0) > 0
                isAudio && hasSize
            }
            
            android.util.Log.d("ShowRepository", "🎵 getTrackStreamingUrls: Found ${audioFiles.size} audio files for $identifier")
            
            val trackUrls = audioFiles.map { file ->
                val audioFile = AudioFile(
                    filename = file.name,
                    format = file.format ?: "unknown",
                    sizeBytes = file.size,
                    durationSeconds = file.length
                )
                val streamingUrl = "https://archive.org/download/$identifier/${file.name}"
                audioFile to streamingUrl
            }
            
            android.util.Log.d("ShowRepository", "🎵 getTrackStreamingUrls: Created ${trackUrls.size} streaming URLs for $identifier")
            trackUrls.take(3).forEach { (audioFile, url) ->
                android.util.Log.d("ShowRepository", "🎵   • ${audioFile.filename} (${audioFile.format}) -> $url")
            }
            
            trackUrls
            
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "🎵 getTrackStreamingUrls: Failed for $identifier", e)
            emptyList()
        }
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
                    com.deadly.core.network.mapper.ArchiveMapper.run {
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
                            val normalizedDate = showCreationService.normalizeDate(recording.concertDate)
                            val showId = "${normalizedDate}_${VenueUtil.normalizeVenue(recording.concertVenue)}"
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
    
    suspend fun getShowEntityById(showId: String): ShowEntity? {
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
            val recordings = showEnrichmentService.attachRecordingsToShow(showId)
            // Get user preferences for this specific show
            val userPreferredRecordingId = settingsRepository.getRecordingPreference(showId)
            val userPreferences = if (userPreferredRecordingId != null) {
                mapOf(showId to userPreferredRecordingId)
            } else {
                emptyMap()
            }
            
            // Enrich show with ratings and user preferences
            val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, userPreferences)
            
            // Set library status
            val isInLibrary = libraryDao.isShowInLibrary(showId)
            enrichedShow.copy(isInLibrary = isInLibrary)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching Show: $showId", e)
            null
        }
    }
    
    override suspend fun getTopRatedShows(limit: Int): List<Show> {
        return try {
            val topShowRatings = ratingsRepository.getTopShows(minRating = 4.0f, limit = limit)
            topShowRatings.mapNotNull { showRating ->
                // Find the corresponding show entity
                val showEntity = showDao.getShowById(showRating.showKey)
                showEntity?.let { showEntity ->
                    val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, emptyMap())
                    val isInLibrary = libraryDao.isShowInLibrary(showEntity.showId)
                    enrichedShow.copy(isInLibrary = isInLibrary)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching top rated shows", e)
            emptyList()
        }
    }
    
    override suspend fun getTopRatedRecordings(limit: Int): List<Recording> {
        return try {
            val topRecordingRatings = ratingsRepository.getTopRecordings(minRating = 4.0f)
            topRecordingRatings.take(limit).mapNotNull { recordingRating ->
                val recordingEntity = recordingDao.getRecordingById(recordingRating.identifier)
                recordingEntity?.let { entity ->
                    val recording = entity.toRecording()
                    showEnrichmentService.enrichRecordingWithRating(recording)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching top rated recordings", e)
            emptyList()
        }
    }
    
    override suspend fun getShowsWithRatings(minRating: Float, limit: Int): List<Show> {
        return try {
            val ratedShows = ratingsRepository.getTopShows(minRating = minRating, limit = limit)
            ratedShows.mapNotNull { showRating ->
                val showEntity = showDao.getShowById(showRating.showKey)
                showEntity?.let { showEntity ->
                    val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, emptyMap())
                    val isInLibrary = libraryDao.isShowInLibrary(showEntity.showId)
                    enrichedShow.copy(isInLibrary = isInLibrary)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching shows with ratings", e)
            emptyList()
        }
    }
    
    /**
     * CANONICAL show creation method - the ONLY way to create shows from recordings
     * Creates ShowEntity records in database and returns Show objects
     */
    private suspend fun createAndSaveShowsFromRecordings(recordings: List<Recording>): List<Show> {
        return showCreationService.createAndSaveShowsFromRecordings(recordings)
    }
    
    /**
     * Populate songNames field for existing shows from setlist data.
     * This denormalizes song names for efficient search.
     */
    suspend fun populateSongNamesFromSetlists(setlistDao: com.deadly.core.database.SetlistDao) {
        try {
            android.util.Log.i("ShowRepository", "🎵 Starting song names population from setlist data...")
            
            // Get all shows
            val shows = showDao.getAllShows()
            android.util.Log.i("ShowRepository", "🎵 Found ${shows.size} shows to process")
            
            // Get all setlists with songs
            val setlists = setlistDao.getSetlistsWithSongs()
            android.util.Log.i("ShowRepository", "🎵 Found ${setlists.size} setlists with songs")
            
            // Create lookup map by date for quick matching
            val setlistsByDate = setlists.groupBy { it.date }
            
            var updatedCount = 0
            val updatedShows = mutableListOf<ShowEntity>()
            
            shows.forEach { show ->
                // Try to find matching setlist for this show's date
                val matchingSetlists = setlistsByDate[show.date]
                
                if (matchingSetlists != null && matchingSetlists.isNotEmpty()) {
                    // Use the best quality setlist (prefer GDSets)
                    val bestSetlist = matchingSetlists
                        .sortedByDescending { if (it.source == "gdsets") 1 else 0 }
                        .first()
                        .toSetlist()
                    
                    if (bestSetlist.songs.isNotEmpty()) {
                        // Extract song names
                        val songNames = bestSetlist.songs.map { it.songName }.joinToString(", ")
                        
                        // Create updated show entity
                        val updatedShow = show.copy(songNames = songNames)
                        updatedShows.add(updatedShow)
                        updatedCount++
                        
                        android.util.Log.d("ShowRepository", "🎵 Updated ${show.date}: ${bestSetlist.songs.size} songs")
                    }
                }
            }
            
            // Batch update the shows
            if (updatedShows.isNotEmpty()) {
                showDao.insertShows(updatedShows)
                android.util.Log.i("ShowRepository", "🎵 ✅ Successfully updated ${updatedCount} shows with song names")
            } else {
                android.util.Log.i("ShowRepository", "🎵 No shows needed song name updates")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ShowRepository", "🎵 ❌ Failed to populate song names: ${e.message}", e)
            throw e
        }
    }
    
    override suspend fun getNextShowByDate(currentDate: String): Show? {
        return try {
            val showEntity = showDao.getNextShowByDate(currentDate)
            if (showEntity != null) {
                val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, emptyMap())
                val isInLibrary = libraryDao.isShowInLibrary(showEntity.showId)
                enrichedShow.copy(isInLibrary = isInLibrary)
            } else null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching next show by date: $currentDate", e)
            null
        }
    }
    
    override suspend fun getPreviousShowByDate(currentDate: String): Show? {
        return try {
            val showEntity = showDao.getPreviousShowByDate(currentDate)
            if (showEntity != null) {
                val enrichedShow = showEnrichmentService.enrichShowWithRatings(showEntity, emptyMap())
                val isInLibrary = libraryDao.isShowInLibrary(showEntity.showId)
                enrichedShow.copy(isInLibrary = isInLibrary)
            } else null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching previous show by date: $currentDate", e)
            null
        }
    }
}