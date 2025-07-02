package com.deadarchive.core.data.sync

import com.deadarchive.core.database.RecordingDao
import com.deadarchive.core.database.RecordingEntity
import com.deadarchive.core.database.ShowDao
import com.deadarchive.core.database.ShowEntity
import com.deadarchive.core.database.SyncMetadataDao
import com.deadarchive.core.database.SyncMetadataEntity
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.mapper.ArchiveMapper
import com.deadarchive.core.network.model.ArchiveSearchResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for downloading and syncing the complete Grateful Dead concert catalog
 */
interface DataSyncService {
    /**
     * Download the complete concert catalog from Archive.org
     * This is typically called on first app launch
     */
    suspend fun downloadCompleteCatalog(): SyncResult
    
    /**
     * Get real-time progress of the download operation
     */
    fun getDownloadProgress(): Flow<SyncProgress>
    
    /**
     * Check if the initial complete sync has been completed
     */
    suspend fun isInitialSyncComplete(): Boolean
    
    /**
     * Perform incremental sync to get new/updated recordings
     * This is called periodically in the background
     */
    suspend fun performIncrementalSync(): SyncResult
    
    /**
     * Get the last sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long
    
    /**
     * Get total number of recordings in local database
     */
    suspend fun getTotalRecordingCount(): Int
    
    /**
     * DEBUG: Force fresh sync by clearing sync metadata
     */
    suspend fun forceRefreshCatalog(): SyncResult
}

@Singleton
class DataSyncServiceImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService,
    private val recordingDao: RecordingDao,
    private val showDao: ShowDao,
    private val syncMetadataDao: SyncMetadataDao
) : DataSyncService {
    
    private val _downloadProgress = MutableStateFlow(
        SyncProgress(
            phase = SyncPhase.IDLE,
            totalItems = 0,
            processedItems = 0,
            currentItem = "",
            error = null
        )
    )
    
    companion object {
        private const val BATCH_SIZE = 50
        private const val MAX_RETRIES = 3
        
        // Archive.org search queries to get all Grateful Dead recordings
        // Split by decades to avoid 10k result limit per query
        private val CATALOG_QUERIES = listOf(
            "collection:GratefulDead AND date:[1965-01-01 TO 1969-12-31]", // 1960s
            "collection:GratefulDead AND date:[1970-01-01 TO 1974-12-31]", // Early 70s
            "collection:GratefulDead AND date:[1975-01-01 TO 1979-12-31]", // Late 70s
            "collection:GratefulDead AND date:[1980-01-01 TO 1984-12-31]", // Early 80s
            "collection:GratefulDead AND date:[1985-01-01 TO 1989-12-31]", // Late 80s
            "collection:GratefulDead AND date:[1990-01-01 TO 1995-12-31]", // 90s
            "collection:GratefulDead AND NOT date:[1965-01-01 TO 1995-12-31]" // Catch undated or post-95
        )
        private const val MAX_ROWS = 10000 // Archive.org limit per query
    }
    
    override fun getDownloadProgress(): Flow<SyncProgress> = _downloadProgress.asStateFlow()
    
    override suspend fun isInitialSyncComplete(): Boolean {
        val metadata = syncMetadataDao.getSyncMetadata()
        return metadata?.lastFullSync != null && metadata.totalConcerts > 0
    }
    
    override suspend fun downloadCompleteCatalog(): SyncResult {
        return try {
            updateProgress(SyncPhase.STARTING, totalItems = 0, currentItem = "Initializing...")
            
            // Step 1: Fetch complete catalog using multiple date-range queries
            updateProgress(SyncPhase.FETCHING, totalItems = 0, currentItem = "Fetching concert list from Archive.org...")
            
            val allRecordings = mutableListOf<ArchiveSearchResponse.ArchiveDoc>()
            var totalFound = 0
            var queryCount = 0
            
            // Execute each date-range query
            for (query in CATALOG_QUERIES) {
                queryCount++
                val queryPeriod = when(queryCount) {
                    1 -> "1960s"
                    2 -> "Early 1970s" 
                    3 -> "Late 1970s"
                    4 -> "Early 1980s"
                    5 -> "Late 1980s"
                    6 -> "1990s"
                    7 -> "Undated/Other"
                    else -> "Query $queryCount"
                }
                
                updateProgress(
                    SyncPhase.FETCHING,
                    totalItems = totalFound,
                    processedItems = allRecordings.size,
                    currentItem = "Fetching $queryPeriod recordings..."
                )
                
                var retryCount = 0
                var querySuccess = false
                
                // Retry logic for each query
                while (!querySuccess && retryCount < MAX_RETRIES) {
                    try {
                        println("DEBUG: Executing query $queryCount/${CATALOG_QUERIES.size}: $query")
                        
                        val response = archiveApiService.searchRecordings(
                            query = query,
                            rows = MAX_ROWS,
                            start = 0
                        )
                        
                        if (!response.isSuccessful) {
                            throw Exception("HTTP ${response.code()}: ${response.message()}")
                        }
                        
                        val searchResponse = response.body() 
                            ?: throw Exception("Empty response body")
                        
                        val recordings = searchResponse.response.docs
                        val queryFound = searchResponse.response.numFound
                        
                        allRecordings.addAll(recordings)
                        totalFound += queryFound
                        
                        querySuccess = true
                        println("DEBUG: Query $queryCount ($queryPeriod): ${recordings.size} recordings downloaded, ${queryFound} total found")
                        
                        updateProgress(
                            SyncPhase.FETCHING,
                            totalItems = totalFound,
                            processedItems = allRecordings.size,
                            currentItem = "Downloaded ${allRecordings.size} recordings from $queryPeriod"
                        )
                        
                    } catch (e: Exception) {
                        retryCount++
                        println("DEBUG: Query $queryCount ($queryPeriod) failed (attempt $retryCount/$MAX_RETRIES): ${e.message}")
                        
                        if (retryCount < MAX_RETRIES) {
                            updateProgress(
                                SyncPhase.FETCHING,
                                totalItems = totalFound,
                                processedItems = allRecordings.size,
                                currentItem = "Retrying $queryPeriod (attempt $retryCount/$MAX_RETRIES)..."
                            )
                            kotlinx.coroutines.delay(2000) // Longer delay between queries
                        } else {
                            println("DEBUG: Giving up on $queryPeriod after $MAX_RETRIES attempts")
                        }
                    }
                }
            }
            
            println("DEBUG: Multiple query approach completed. Final stats:")
            println("DEBUG: - Queries executed: ${CATALOG_QUERIES.size}")
            println("DEBUG: - Total recordings downloaded: ${allRecordings.size}")
            println("DEBUG: - Estimated total available: $totalFound")
            
            val finalTotalRecordings = allRecordings.size
            
            if (finalTotalRecordings == 0) {
                return SyncResult.Error("No recordings downloaded from any date range query")
            }
            
            updateProgress(SyncPhase.PROCESSING, totalItems = finalTotalRecordings, currentItem = "Processing $finalTotalRecordings recordings...")
            
            // Step 2: Process recordings and group by show (but don't save recordings yet)
            val recordings = allRecordings
            var processedCount = 0
            val showsMap = mutableMapOf<String, MutableList<RecordingEntity>>()
            val allRecordingEntities = mutableListOf<RecordingEntity>()
            
            recordings.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    val recordingEntities = batch.map { doc ->
                        updateProgress(
                            SyncPhase.PROCESSING,
                            totalItems = finalTotalRecordings,
                            processedItems = processedCount,
                            currentItem = "Processing: ${doc.title ?: doc.identifier}"
                        )
                        
                        val recording = ArchiveMapper.run { doc.toRecording() }
                        // Fix date format - convert ISO date to simple YYYY-MM-DD format
                        val simpleDateFormat = recording.concertDate.take(10) // Take only YYYY-MM-DD part
                        val showId = "${simpleDateFormat}_${recording.concertVenue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and") ?: "Unknown"}"
                        println("DEBUG: Processing recording ${recording.identifier} -> showId: $showId (originalDate=${recording.concertDate}, simplifiedDate=${simpleDateFormat}, venue=${recording.concertVenue})")
                        RecordingEntity.fromRecording(recording, showId)
                    }
                    
                    // Group recordings by show for show creation
                    recordingEntities.forEach { recordingEntity ->
                        val showId = recordingEntity.concertId
                        showsMap.computeIfAbsent(showId) { mutableListOf() }.add(recordingEntity)
                    }
                    
                    // Collect all recordings for later batch insert (after shows are created)
                    allRecordingEntities.addAll(recordingEntities)
                    processedCount += batch.size
                    
                } catch (e: Exception) {
                    // Log error but continue with other batches
                    println("ERROR: Failed to process batch at position $processedCount: ${e.message}")
                }
            }
            
            // Step 2.5: Create and save Show entities FIRST (to satisfy foreign key constraints)
            updateProgress(SyncPhase.PROCESSING, totalItems = finalTotalRecordings, processedItems = processedCount, currentItem = "Creating shows from recordings...")
            
            val showEntities = showsMap.map { (showId, recordingEntities) ->
                val firstRecording = recordingEntities.first()
                // Fix date format for show entity
                val simpleDateFormat = firstRecording.concertDate.take(10) // Take only YYYY-MM-DD part
                ShowEntity(
                    showId = showId,
                    date = simpleDateFormat,
                    venue = firstRecording.concertVenue,
                    location = firstRecording.concertLocation,
                    year = simpleDateFormat.take(4),
                    setlistRaw = null, // We don't have setlist data from the recordings
                    setsJson = null,
                    isFavorite = false,
                    cachedTimestamp = System.currentTimeMillis()
                )
            }
            
            try {
                showDao.insertShows(showEntities)
                println("DEBUG: Created ${showEntities.size} shows from ${allRecordingEntities.size} recordings")
                
                // Debug: Show some example show IDs
                showEntities.take(5).forEach { show ->
                    println("DEBUG: Created show: ${show.showId} (date=${show.date}, venue=${show.venue})")
                }
            } catch (e: Exception) {
                println("ERROR: Failed to save shows: ${e.message}")
                e.printStackTrace()
            }
            
            // Step 3: Now save all recordings (after shows exist to satisfy foreign key constraints)
            updateProgress(SyncPhase.PROCESSING, totalItems = finalTotalRecordings, processedItems = processedCount, currentItem = "Saving recordings...")
            
            var savedCount = 0
            try {
                // Save recordings in batches to avoid memory issues
                allRecordingEntities.chunked(BATCH_SIZE).forEach { batch ->
                    recordingDao.insertRecordings(batch)
                    savedCount += batch.size
                    println("DEBUG: Saved ${batch.size} recordings, total saved: $savedCount")
                }
                println("DEBUG: Successfully saved $savedCount recordings total")
            } catch (e: Exception) {
                println("ERROR: Failed to save recordings: ${e.message}")
                e.printStackTrace()
            }
            
            // Step 4: Update sync metadata
            updateProgress(SyncPhase.FINALIZING, totalItems = finalTotalRecordings, processedItems = processedCount, currentItem = "Finalizing...")
            
            val now = System.currentTimeMillis()
            val metadata = SyncMetadataEntity(
                id = 1,
                lastFullSync = now,
                lastDeltaSync = now,
                totalConcerts = savedCount,
                syncVersion = 1
            )
            syncMetadataDao.insertOrUpdateSyncMetadata(metadata)
            
            updateProgress(SyncPhase.COMPLETED, totalItems = finalTotalRecordings, processedItems = processedCount, currentItem = "Sync completed!")
            
            SyncResult.Success(
                message = "Downloaded $savedCount out of $finalTotalRecordings recordings",
                recordingsProcessed = savedCount
            )
            
        } catch (e: Exception) {
            val errorMsg = "Sync failed: ${e.message}"
            updateProgress(SyncPhase.ERROR, error = errorMsg)
            SyncResult.Error(errorMsg)
        }
    }
    
    override suspend fun performIncrementalSync(): SyncResult {
        return try {
            val lastSync = getLastSyncTimestamp()
            
            // For now, we'll do a simple check for new recordings
            // In a more sophisticated implementation, we could use modification dates
            updateProgress(SyncPhase.FETCHING, totalItems = 0, currentItem = "Checking for new recordings...")
            
            val response = archiveApiService.searchRecordings(
                query = "collection:GratefulDead",
                rows = 100 // Just check recent additions
            )
            
            if (!response.isSuccessful) {
                return SyncResult.Error("Incremental sync failed: HTTP ${response.code()}")
            }
            
            val searchResponse = response.body() 
                ?: return SyncResult.Error("Empty response during incremental sync")
            
            // Filter for recordings not already in our database and collect new recordings
            val newRecordingEntities = mutableListOf<RecordingEntity>()
            val newShowsMap = mutableMapOf<String, MutableList<RecordingEntity>>()
            
            searchResponse.response.docs.forEach { doc ->
                val exists = recordingDao.recordingExists(doc.identifier)
                if (!exists) {
                    val recording = ArchiveMapper.run { doc.toRecording() }
                    // Fix date format - convert ISO date to simple YYYY-MM-DD format
                    val simpleDateFormat = recording.concertDate.take(10) // Take only YYYY-MM-DD part
                    val showId = "${simpleDateFormat}_${recording.concertVenue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and") ?: "Unknown"}"
                    val entity = RecordingEntity.fromRecording(recording, showId)
                    
                    // Collect new recordings (don't save yet)
                    newRecordingEntities.add(entity)
                    
                    // Group new recordings by show
                    newShowsMap.computeIfAbsent(showId) { mutableListOf() }.add(entity)
                }
            }
            
            // Create new show entities FIRST (for shows that don't exist yet)
            newShowsMap.forEach { (showId, recordingEntities) ->
                val showExists = showDao.showExists(showId)
                if (!showExists) {
                    val firstRecording = recordingEntities.first()
                    // Fix date format for show entity
                    val simpleDateFormat = firstRecording.concertDate.take(10) // Take only YYYY-MM-DD part
                    val showEntity = ShowEntity(
                        showId = showId,
                        date = simpleDateFormat,
                        venue = firstRecording.concertVenue,
                        location = firstRecording.concertLocation,
                        year = simpleDateFormat.take(4),
                        setlistRaw = null,
                        setsJson = null,
                        isFavorite = false,
                        cachedTimestamp = System.currentTimeMillis()
                    )
                    showDao.insertShow(showEntity)
                }
            }
            
            // Now save all new recordings (after shows exist to satisfy foreign key constraints)
            var newRecordings = 0
            try {
                if (newRecordingEntities.isNotEmpty()) {
                    recordingDao.insertRecordings(newRecordingEntities)
                    newRecordings = newRecordingEntities.size
                    println("DEBUG: Incremental sync saved $newRecordings new recordings")
                }
            } catch (e: Exception) {
                println("ERROR: Failed to save new recordings during incremental sync: ${e.message}")
            }
            
            // Update last delta sync timestamp
            syncMetadataDao.updateLastDeltaSync(System.currentTimeMillis())
            
            SyncResult.Success(
                message = "Incremental sync completed. Added $newRecordings new recordings",
                recordingsProcessed = newRecordings
            )
            
        } catch (e: Exception) {
            SyncResult.Error("Incremental sync failed: ${e.message}")
        }
    }
    
    override suspend fun getLastSyncTimestamp(): Long {
        return syncMetadataDao.getSyncMetadata()?.lastFullSync ?: 0L
    }
    
    override suspend fun getTotalRecordingCount(): Int {
        return syncMetadataDao.getSyncMetadata()?.totalConcerts ?: 0
    }
    
    override suspend fun forceRefreshCatalog(): SyncResult {
        return try {
            // Clear sync metadata to force fresh download
            syncMetadataDao.clearSyncMetadata()
            
            // Clear existing recordings and shows (except favorites)
            val cutoffTimestamp = System.currentTimeMillis() + 1000 // Future timestamp to clear all non-favorites
            recordingDao.cleanupOldCachedRecordings(cutoffTimestamp)
            showDao.cleanupOldCachedShows(cutoffTimestamp)
            
            // Download fresh catalog
            downloadCompleteCatalog()
        } catch (e: Exception) {
            SyncResult.Error("Force refresh failed: ${e.message}")
        }
    }
    
    private fun updateProgress(
        phase: SyncPhase,
        totalItems: Int = _downloadProgress.value.totalItems,
        processedItems: Int = _downloadProgress.value.processedItems,
        currentItem: String = _downloadProgress.value.currentItem,
        error: String? = null
    ) {
        _downloadProgress.value = SyncProgress(
            phase = phase,
            totalItems = totalItems,
            processedItems = processedItems,
            currentItem = currentItem,
            error = error
        )
    }
}

/**
 * Represents the current state of a sync operation
 */
data class SyncProgress(
    val phase: SyncPhase,
    val totalItems: Int,
    val processedItems: Int,
    val currentItem: String,
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) (processedItems.toFloat() / totalItems) * 100f else 0f
        
    val isInProgress: Boolean
        get() = phase in listOf(SyncPhase.STARTING, SyncPhase.FETCHING, SyncPhase.PROCESSING, SyncPhase.FINALIZING)
}

/**
 * Different phases of the sync operation
 */
enum class SyncPhase {
    IDLE,
    STARTING,
    FETCHING,
    PROCESSING,
    FINALIZING,
    COMPLETED,
    ERROR
}

/**
 * Result of a sync operation
 */
sealed class SyncResult {
    data class Success(
        val message: String,
        val recordingsProcessed: Int
    ) : SyncResult()
    
    data class Error(
        val error: String
    ) : SyncResult()
}