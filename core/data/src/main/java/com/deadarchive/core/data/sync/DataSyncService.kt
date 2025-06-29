package com.deadarchive.core.data.sync

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.SyncMetadataDao
import com.deadarchive.core.database.SyncMetadataEntity
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.mapper.ArchiveMapper
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
     * Perform incremental sync to get new/updated concerts
     * This is called periodically in the background
     */
    suspend fun performIncrementalSync(): SyncResult
    
    /**
     * Get the last sync timestamp
     */
    suspend fun getLastSyncTimestamp(): Long
    
    /**
     * Get total number of concerts in local database
     */
    suspend fun getTotalConcertCount(): Int
    
    /**
     * DEBUG: Force fresh sync by clearing sync metadata
     */
    suspend fun forceRefreshCatalog(): SyncResult
}

@Singleton
class DataSyncServiceImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService,
    private val concertDao: ConcertDao,
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
        
        // Archive.org search query to get all Grateful Dead concerts
        private const val COMPLETE_CATALOG_QUERY = "collection:GratefulDead"
        private const val MAX_ROWS = 10000 // Should be more than enough for GD catalog
    }
    
    override fun getDownloadProgress(): Flow<SyncProgress> = _downloadProgress.asStateFlow()
    
    override suspend fun isInitialSyncComplete(): Boolean {
        val metadata = syncMetadataDao.getSyncMetadata()
        return metadata?.lastFullSync != null && metadata.totalConcerts > 0
    }
    
    override suspend fun downloadCompleteCatalog(): SyncResult {
        return try {
            updateProgress(SyncPhase.STARTING, totalItems = 0, currentItem = "Initializing...")
            
            // Step 1: Fetch complete catalog from Archive.org
            updateProgress(SyncPhase.FETCHING, totalItems = 0, currentItem = "Fetching concert list from Archive.org...")
            
            val response = archiveApiService.searchConcerts(
                query = COMPLETE_CATALOG_QUERY,
                rows = MAX_ROWS
            )
            
            if (!response.isSuccessful) {
                return SyncResult.Error("Failed to fetch catalog: HTTP ${response.code()}")
            }
            
            val searchResponse = response.body() 
                ?: return SyncResult.Error("Empty response from Archive.org")
            
            val totalConcerts = searchResponse.response.docs.size
            updateProgress(SyncPhase.PROCESSING, totalItems = totalConcerts, currentItem = "Processing concerts...")
            
            // Step 2: Process and save concerts in batches
            val concerts = searchResponse.response.docs
            var processedCount = 0
            var savedCount = 0
            
            concerts.chunked(BATCH_SIZE).forEach { batch ->
                try {
                    val concertEntities = batch.map { doc ->
                        updateProgress(
                            SyncPhase.PROCESSING,
                            totalItems = totalConcerts,
                            processedItems = processedCount,
                            currentItem = "Processing: ${doc.title ?: doc.identifier}"
                        )
                        
                        val concert = ArchiveMapper.run { doc.toConcert() }
                        ConcertEntity.fromConcert(concert, isFavorite = false)
                    }
                    
                    // Save batch to database
                    concertDao.insertConcerts(concertEntities)
                    savedCount += concertEntities.size
                    processedCount += batch.size
                    
                } catch (e: Exception) {
                    // Log error but continue with other batches
                    println("ERROR: Failed to process batch at position $processedCount: ${e.message}")
                }
            }
            
            // Step 3: Update sync metadata
            updateProgress(SyncPhase.FINALIZING, totalItems = totalConcerts, processedItems = processedCount, currentItem = "Finalizing...")
            
            val now = System.currentTimeMillis()
            val metadata = SyncMetadataEntity(
                id = 1,
                lastFullSync = now,
                lastDeltaSync = now,
                totalConcerts = savedCount,
                syncVersion = 1
            )
            syncMetadataDao.insertOrUpdateSyncMetadata(metadata)
            
            updateProgress(SyncPhase.COMPLETED, totalItems = totalConcerts, processedItems = processedCount, currentItem = "Sync completed!")
            
            SyncResult.Success(
                message = "Downloaded $savedCount out of $totalConcerts concerts",
                concertsProcessed = savedCount
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
            
            // For now, we'll do a simple check for new concerts
            // In a more sophisticated implementation, we could use modification dates
            updateProgress(SyncPhase.FETCHING, totalItems = 0, currentItem = "Checking for new concerts...")
            
            val response = archiveApiService.searchConcerts(
                query = COMPLETE_CATALOG_QUERY,
                rows = 100 // Just check recent additions
            )
            
            if (!response.isSuccessful) {
                return SyncResult.Error("Incremental sync failed: HTTP ${response.code()}")
            }
            
            val searchResponse = response.body() 
                ?: return SyncResult.Error("Empty response during incremental sync")
            
            // Filter for concerts not already in our database
            var newConcerts = 0
            searchResponse.response.docs.forEach { doc ->
                val exists = concertDao.concertExists(doc.identifier) > 0
                if (!exists) {
                    val concert = ArchiveMapper.run { doc.toConcert() }
                    val entity = ConcertEntity.fromConcert(concert, isFavorite = false)
                    concertDao.insertConcert(entity)
                    newConcerts++
                }
            }
            
            // Update last delta sync timestamp
            syncMetadataDao.updateLastDeltaSync(System.currentTimeMillis())
            
            SyncResult.Success(
                message = "Incremental sync completed. Added $newConcerts new concerts",
                concertsProcessed = newConcerts
            )
            
        } catch (e: Exception) {
            SyncResult.Error("Incremental sync failed: ${e.message}")
        }
    }
    
    override suspend fun getLastSyncTimestamp(): Long {
        return syncMetadataDao.getSyncMetadata()?.lastFullSync ?: 0L
    }
    
    override suspend fun getTotalConcertCount(): Int {
        return syncMetadataDao.getSyncMetadata()?.totalConcerts ?: 0
    }
    
    override suspend fun forceRefreshCatalog(): SyncResult {
        return try {
            // Clear sync metadata to force fresh download
            syncMetadataDao.clearSyncMetadata()
            
            // Clear existing concerts (except favorites)
            val cutoffTimestamp = System.currentTimeMillis() + 1000 // Future timestamp to clear all non-favorites
            concertDao.cleanupOldCachedConcerts(cutoffTimestamp)
            
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
        val concertsProcessed: Int
    ) : SyncResult()
    
    data class Error(
        val error: String
    ) : SyncResult()
}