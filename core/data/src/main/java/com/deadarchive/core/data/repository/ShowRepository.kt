package com.deadarchive.core.data.repository

import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.RecordingEntity
import com.deadarchive.core.database.ShowDao
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
        try {
            // Get all shows from database with their recordings
            val showEntities = showDao.getAllShows()
            val shows = showEntities.map { showEntity ->
                // Get recordings for this show
                val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { it.toRecording() }
                println("DEBUG: Show ${showEntity.showId} has ${recordings.size} recordings")
                showEntity.toShow(recordings)
            }
            emit(shows)
        } catch (e: Exception) {
            // Fallback to grouping all recordings if show entities don't exist
            val allRecordings = recordingDao.getAllRecordings().firstOrNull()?.map { it.toRecording() } ?: emptyList()
            val shows = groupRecordingsIntoShows(allRecordings)
            emit(shows)
        }
    }
    
    override fun searchShows(query: String): Flow<List<Show>> = flow {
        try {
            // First try to search shows directly in the database
            val cachedShows = showDao.searchShows(query)
            
            if (cachedShows.isNotEmpty()) {
                // Use actual Show entities with their recordings
                val shows = cachedShows.map { showEntity ->
                    val recordings = recordingDao.getRecordingsByConcertId(showEntity.showId).map { it.toRecording() }
                    println("DEBUG: Search - Show ${showEntity.showId} has ${recordings.size} recordings")
                    showEntity.toShow(recordings)
                }
                emit(shows)
            } else {
                // Fallback: search recordings and group into shows
                val cachedRecordings = recordingDao.searchRecordings(query).map { it.toRecording() }
                
                if (cachedRecordings.isNotEmpty()) {
                    val shows = groupRecordingsIntoShows(cachedRecordings)
                    emit(shows)
                } else {
                    // If no cached results, search via API
                    val apiRecordings = searchRecordingsViaApi(query)
                    val shows = groupRecordingsIntoShows(apiRecordings)
                    emit(shows)
                }
            }
        } catch (e: Exception) {
            // On error, emit empty list
            emit(emptyList())
        }
    }
    
    override fun searchRecordings(query: String): Flow<List<Recording>> = flow {
        try {
            // First try to search locally cached recordings
            val cachedRecordings = recordingDao.searchRecordings(query).map { it.toRecording() }
            
            if (cachedRecordings.isNotEmpty()) {
                emit(cachedRecordings)
            } else {
                // If no cached results, search via API
                val apiRecordings = searchRecordingsViaApi(query)
                emit(apiRecordings)
            }
        } catch (e: Exception) {
            // On error, emit empty list
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
                val isInLibrary = libraryDao.isRecordingInLibrary(id)
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
    
    private suspend fun searchRecordingsViaApi(query: String): List<Recording> {
        return try {
            val searchQuery = when {
                query.matches(Regex("\\d{4}")) -> "collection:GratefulDead AND date:$query*"
                query.isBlank() -> "collection:GratefulDead"
                else -> "collection:GratefulDead AND ($query)"
            }
            
            val response = archiveApiService.searchRecordings(searchQuery, rows = 50)
            if (response.isSuccessful) {
                response.body()?.response?.docs?.map { doc ->
                    com.deadarchive.core.network.mapper.ArchiveMapper.run { 
                        doc.toRecording() 
                    }
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
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
    
    private fun groupRecordingsIntoShows(recordings: List<Recording>): List<Show> {
        return recordings
            .groupBy { "${it.concertDate}_${it.concertVenue}" }
            .map { (showKey, groupedRecordings) ->
                Show(
                    date = groupedRecordings.first().concertDate,
                    venue = groupedRecordings.first().concertVenue,
                    location = groupedRecordings.first().concertLocation,
                    year = groupedRecordings.first().concertDate.take(4),
                    recordings = groupedRecordings,
                    isInLibrary = groupedRecordings.any { it.isInLibrary }
                )
            }
            .sortedByDescending { it.date }
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        val expiryTime = timestamp + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
        return System.currentTimeMillis() > expiryTime
    }
}