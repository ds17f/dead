package com.deadarchive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.sync.DataSyncService
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.data.repository.RatingsRepository
import com.deadarchive.core.data.repository.SetlistRepository
import com.deadarchive.core.data.download.DownloadQueueManager
import com.deadarchive.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class DebugUiState(
    val isExporting: Boolean = false,
    val exportStatus: String = "",
    val exportSuccess: Boolean = false,
    val cachedRecordingCount: Int = 0,
    val lastSyncTime: String = "Never",
    val databaseSize: String = "Unknown",
    val databaseDebugInfo: String = "",
    val isDownloadTesting: Boolean = false,
    val downloadTestStatus: String = "",
    val downloadTestSuccess: Boolean = false,
    val isWipingDatabase: Boolean = false,
    val databaseWipeStatus: String = "",
    val databaseWipeSuccess: Boolean = false,
    val wipeProgress: DatabaseWipeProgress = DatabaseWipeProgress(),
    // Setlist data
    val setlistCount: Int = 0,
    val songCount: Int = 0,
    val venueCount: Int = 0,
    val isLoadingSetlistData: Boolean = false,
    val setlistTestStatus: String = "",
    val setlistTestSuccess: Boolean = false,
    val sampleSetlists: List<String> = emptyList()
)

data class DatabaseWipeProgress(
    val phase: WipePhase = WipePhase.IDLE,
    val totalItems: Int = 0,
    val processedItems: Int = 0,
    val currentItem: String = "",
    val error: String? = null
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) (processedItems.toFloat() / totalItems) * 100f else 0f
    
    val isInProgress: Boolean
        get() = phase != WipePhase.IDLE && phase != WipePhase.COMPLETED && phase != WipePhase.ERROR
}

enum class WipePhase {
    IDLE,
    STARTING,
    CLEARING_RECORDINGS,
    CLEARING_SHOWS,
    CLEARING_SYNC_METADATA,
    CLEARING_DOWNLOADS,
    REFRESHING_RATINGS,
    COMPLETED,
    ERROR
}

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val dataSyncService: DataSyncService,
    private val concertRepository: ShowRepository,
    private val downloadRepository: DownloadRepository,
    private val ratingsRepository: RatingsRepository,
    private val setlistRepository: SetlistRepository,
    private val downloadQueueManager: DownloadQueueManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()
    
    init {
        loadAppInfo()
        loadSetlistInfo()
    }
    
    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val concertCount = dataSyncService.getTotalRecordingCount()
                val lastSync = dataSyncService.getLastSyncTimestamp()
                val lastSyncFormatted = if (lastSync > 0) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastSync))
                } else {
                    "Never"
                }
                
                _uiState.value = _uiState.value.copy(
                    cachedRecordingCount = concertCount,
                    lastSyncTime = lastSyncFormatted,
                    databaseSize = "~${concertCount * 2}KB" // Rough estimate
                )
            } catch (e: Exception) {
                println("DEBUG: Error loading app info: ${e.message}")
            }
        }
    }
    
    /**
     * Export real test data using the same code paths as the production app
     */
    fun exportTestData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isExporting = true,
                    exportStatus = "Starting export...",
                    exportSuccess = false
                )
                
                // Create testdata directory using app's external files directory
                val testDataDir = File(context.getExternalFilesDir(null), "testdata")
                try {
                    if (!testDataDir.exists()) {
                        val created = testDataDir.mkdirs()
                        if (!created) {
                            throw Exception("Failed to create directory: ${testDataDir.absolutePath}")
                        }
                    }
                    if (!testDataDir.canWrite()) {
                        throw Exception("No write permission for directory: ${testDataDir.absolutePath}")
                    }
                    
                    // Log the actual path for user reference
                    println("DEBUG: Test data will be saved to: ${testDataDir.absolutePath}")
                    
                } catch (e: Exception) {
                    throw Exception("Directory setup failed: ${e.message}")
                }
                
                _uiState.value = _uiState.value.copy(exportStatus = "Downloading complete catalog...")
                
                // Use the real DataSyncService to download catalog
                val syncResult = dataSyncService.downloadCompleteCatalog()
                
                when (syncResult) {
                    is com.deadarchive.core.data.sync.SyncResult.Success -> {
                        _uiState.value = _uiState.value.copy(exportStatus = "Catalog downloaded. Exporting data...")
                        
                        // Export the captured API response data
                        exportCatalogData(testDataDir)
                        
                        // Export sample metadata for a few concerts
                        exportSampleMetadata(testDataDir)
                        
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportStatus = "✅ Export complete! Files saved to:\n${testDataDir.absolutePath}\n\n" +
                                    "Next steps:\n" +
                                    "1. Run: make capture-test-data\n" +
                                    "2. Commit files to source control\n" +
                                    "3. Use in integration tests",
                            exportSuccess = true
                        )
                    }
                    is com.deadarchive.core.data.sync.SyncResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportStatus = "❌ Export failed: ${syncResult.error}",
                            exportSuccess = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportStatus = "❌ Export error: ${e.message}",
                    exportSuccess = false
                )
            }
        }
    }
    
    private suspend fun exportCatalogData(testDataDir: File) {
        try {
            // Get sample concerts from the database to show the real data format
            val sampleRecordings = concertRepository.getAllCachedRecordings().first().take(10)
            
            val catalogData = buildString {
                appendLine("{")
                appendLine("  \"metadata\": {")
                appendLine("    \"exported_at\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}\",")
                appendLine("    \"source\": \"Real app data via DataSyncService\",")
                appendLine("    \"total_concerts\": ${sampleRecordings.size}")
                appendLine("  },")
                appendLine("  \"sample_concerts\": [")
                
                sampleRecordings.forEachIndexed { index, concert ->
                    appendLine("    {")
                    appendLine("      \"identifier\": \"${concert.identifier}\",")
                    appendLine("      \"title\": \"${concert.title.replace("\"", "\\\"")}\",")
                    appendLine("      \"date\": \"${concert.concertDate}\",")
                    appendLine("      \"venue\": \"${concert.concertVenue?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"location\": \"${concert.concertLocation?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"year\": ${concert.concertDate.take(4).toIntOrNull() ?: 0},")
                    appendLine("      \"source\": \"${concert.source?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"description\": \"${concert.description?.take(100)?.replace("\"", "\\\"") ?: ""}\"")
                    append("    }")
                    if (index < sampleRecordings.size - 1) appendLine(",")
                }
                
                appendLine("")
                appendLine("  ]")
                appendLine("}")
            }
            
            File(testDataDir, "complete_catalog_response.json").writeText(catalogData)
            
        } catch (e: Exception) {
            println("DEBUG: Error exporting catalog data: ${e.message}")
            throw e
        }
    }
    
    private suspend fun exportSampleMetadata(testDataDir: File) {
        try {
            // Get a few sample concerts for metadata export
            val sampleRecordings = concertRepository.getAllCachedRecordings().first().take(3)
            
            val metadataData = buildString {
                appendLine("[")
                
                sampleRecordings.forEachIndexed { index, concert ->
                    // Try to fetch real metadata using the concert repository
                    val metadata = concertRepository.getRecordingMetadata(concert.identifier)
                    
                    appendLine("  {")
                    appendLine("    \"identifier\": \"${concert.identifier}\",")
                    appendLine("    \"metadata_available\": ${metadata != null},")
                    if (metadata != null) {
                        appendLine("    \"server\": \"${metadata.server ?: ""}\",")
                        appendLine("    \"directory\": \"${metadata.directory ?: ""}\",")
                        appendLine("    \"files_count\": ${metadata.files.size}")
                    }
                    append("  }")
                    if (index < sampleRecordings.size - 1) appendLine(",")
                }
                
                appendLine("")
                appendLine("]")
            }
            
            File(testDataDir, "sample_metadata_responses.json").writeText(metadataData)
            
        } catch (e: Exception) {
            println("DEBUG: Error exporting metadata: ${e.message}")
            throw e
        }
    }
    
    /**
     * Debug the database state to see shows and recordings
     */
    fun debugDatabaseState() {
        viewModelScope.launch {
            try {
                val debugInfo = concertRepository.debugDatabaseState()
                _uiState.value = _uiState.value.copy(
                    databaseDebugInfo = debugInfo
                )
                println("DEBUG: Database state:\n$debugInfo")
            } catch (e: Exception) {
                val errorInfo = "Error debugging database: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    databaseDebugInfo = errorInfo
                )
                println("DEBUG: $errorInfo")
            }
        }
    }
    
    /**
     * Test downloading a sample recording with detailed debugging
     */
    fun testSampleDownload() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = true,
                    downloadTestStatus = "Starting sample download test...",
                    downloadTestSuccess = false
                )
                
                // Get a sample recording from the database
                val recordings = concertRepository.getAllCachedRecordings().first()
                if (recordings.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isDownloadTesting = false,
                        downloadTestStatus = "❌ No recordings available for testing. Run 'Export Test Data' first.",
                        downloadTestSuccess = false
                    )
                    return@launch
                }
                
                val sampleRecording = recordings.first()
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Step 1: Selected recording\n" +
                            "Title: ${sampleRecording.title}\n" +
                            "Identifier: ${sampleRecording.identifier}\n\n" +
                            "Step 2: Starting download..."
                )
                
                // Start the download and get the download IDs
                val downloadIds = downloadRepository.downloadRecording(sampleRecording)
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Step 3: Download initiated\n" +
                            "Recording: ${sampleRecording.identifier}\n" +
                            "Download IDs created: ${downloadIds.size}\n" +
                            "IDs: ${downloadIds.joinToString(", ")}\n\n" +
                            "Step 4: Checking database entries..."
                )
                
                // Give it a moment to process
                delay(1000)
                
                // Check if entries were created in the database
                val allDownloads = downloadRepository.exportDownloadList()
                val queuedDownloads = downloadRepository.getDownloadQueue()
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Step 5: Database verification\n" +
                            "Total downloads in DB: ${allDownloads.size}\n" +
                            "Queued downloads: ${queuedDownloads.size}\n\n" +
                            "Step 6: Triggering queue processing..."
                )
                
                // Manually trigger queue processing
                downloadQueueManager.triggerImmediateProcessing()
                
                // Give it more time to process
                delay(3000)
                
                // Check the final status
                val queueStatus = downloadQueueManager.getQueueProcessingStatus()
                val isActive = downloadQueueManager.isQueueProcessingActive()
                val updatedDownloads = downloadRepository.exportDownloadList()
                
                val debugReport = buildString {
                    appendLine("🔍 DETAILED DOWNLOAD DEBUG REPORT")
                    appendLine("================================")
                    appendLine("✅ Recording Selected: ${sampleRecording.identifier}")
                    appendLine("✅ Download IDs Created: ${downloadIds.size} (${downloadIds.joinToString(", ")})")
                    appendLine("✅ Database Entries: ${updatedDownloads.size}")
                    appendLine("✅ Queue Processing Active: $isActive")
                    appendLine("✅ Queue Status: $queueStatus")
                    appendLine()
                    
                    if (updatedDownloads.isNotEmpty()) {
                        appendLine("📋 DOWNLOAD ENTRIES:")
                        updatedDownloads.take(3).forEach { download ->
                            appendLine("• ID: ${download.id}")
                            appendLine("  Recording: ${download.recordingId}")
                            appendLine("  Status: ${download.status}")
                            appendLine("  Progress: ${(download.progress * 100).toInt()}%")
                            if (download.errorMessage != null) {
                                appendLine("  Error: ${download.errorMessage}")
                            }
                            appendLine()
                        }
                    } else {
                        appendLine("❌ NO DOWNLOAD ENTRIES FOUND!")
                        appendLine("This suggests the downloadRecording() method didn't create database entries.")
                    }
                    
                    appendLine("🔧 NEXT STEPS:")
                    appendLine("1. Use 'List Downloads' to see all database entries")
                    appendLine("2. Monitor logcat: adb logcat | grep -E '(Download|Worker|Queue)'")
                    appendLine("3. Check if WorkManager is processing with 'Check Status'")
                }
                
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = debugReport,
                    downloadTestSuccess = downloadIds.isNotEmpty()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = "❌ Download test failed at step: ${e.message}\n\n" +
                            "Stack trace: ${e.stackTraceToString()}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * Test multiple downloads to verify queue management
     */
    fun testMultipleDownloads() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = true,
                    downloadTestStatus = "Starting multiple download test...",
                    downloadTestSuccess = false
                )
                
                // Get multiple recordings for testing
                val recordings = concertRepository.getAllCachedRecordings().first()
                if (recordings.size < 3) {
                    _uiState.value = _uiState.value.copy(
                        isDownloadTesting = false,
                        downloadTestStatus = "❌ Need at least 3 recordings for queue testing. Only found ${recordings.size}.",
                        downloadTestSuccess = false
                    )
                    return@launch
                }
                
                val testRecordings = recordings.take(3)
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Queuing ${testRecordings.size} downloads for queue testing..."
                )
                
                // Start multiple downloads
                testRecordings.forEachIndexed { index, recording ->
                    _uiState.value = _uiState.value.copy(
                        downloadTestStatus = "Queuing download ${index + 1}/3: ${recording.identifier}"
                    )
                    downloadRepository.downloadRecording(recording)
                    delay(500) // Small delay between queuing
                }
                
                // Give the system time to process
                delay(3000)
                
                val queueStatus = downloadQueueManager.getQueueProcessingStatus()
                
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = "✅ Queue test completed!\n\n" +
                            "Queued ${testRecordings.size} downloads:\n" +
                            testRecordings.mapIndexed { index, recording ->
                                "${index + 1}. ${recording.identifier}"
                            }.joinToString("\n") +
                            "\n\nQueue Status: $queueStatus\n\n" +
                            "The downloads will be processed with concurrency limits.\n" +
                            "Check logcat for detailed progress:\n" +
                            "adb logcat | grep -E '(DownloadQueueManager|AudioDownloadWorker)'",
                    downloadTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = "❌ Queue test failed: ${e.message}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * Check the current status of the download system
     */
    fun checkDownloadStatus() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Checking download system status..."
                )
                
                val queueStatus = downloadQueueManager.getQueueProcessingStatus()
                val isActive = downloadQueueManager.isQueueProcessingActive()
                
                // Get pending downloads count (if available)
                val pendingDownloads = try {
                    val queuedDownloads = downloadRepository.getDownloadQueue()
                    "${queuedDownloads.size} queued"
                } catch (e: Exception) {
                    "Unable to determine (${e.message})"
                }
                
                val statusReport = buildString {
                    appendLine("📊 DOWNLOAD SYSTEM STATUS")
                    appendLine("==============================")  
                    appendLine("Queue Processing: ${if (isActive) "ACTIVE" else "INACTIVE"}")
                    appendLine("Queue Status: $queueStatus")
                    appendLine("Pending Downloads: $pendingDownloads")
                    appendLine()
                    appendLine("💡 MONITORING COMMANDS:")
                    appendLine("• View download logs:")
                    appendLine("  adb logcat | grep -E '(Download|Queue|Worker)'")
                    appendLine()
                    appendLine("• Check WorkManager status:")
                    appendLine("  adb shell dumpsys jobscheduler | grep androidx.work")
                    appendLine()
                    appendLine("• Monitor network usage:")
                    appendLine("  adb shell cat /proc/net/dev")
                }
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = statusReport,
                    downloadTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "❌ Status check failed: ${e.message}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * List all downloads in the system
     */
    fun listAllDownloads() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Loading all downloads..."
                )
                
                val allDownloads = downloadRepository.exportDownloadList()
                val downloadStats = downloadRepository.getDownloadStats()
                
                val downloadReport = buildString {
                    appendLine("📋 ALL DOWNLOADS REPORT")
                    appendLine("==============================")
                    appendLine("Total Downloads: ${downloadStats.totalDownloads}")
                    appendLine("Completed: ${downloadStats.completedDownloads}")
                    appendLine("Failed: ${downloadStats.failedDownloads}")
                    appendLine("Total Bytes Downloaded: ${formatBytes(downloadStats.totalBytesDownloaded)}")
                    appendLine()
                    
                    if (allDownloads.isEmpty()) {
                        appendLine("❌ No downloads found!")
                        appendLine("Try running 'Test Download' first.")
                    } else {
                        appendLine("📁 DOWNLOAD ENTRIES:")
                        allDownloads.take(10).forEach { download ->
                            appendLine("• ${download.recordingId}")
                            appendLine("  Status: ${download.status}")
                            appendLine("  Progress: ${(download.progress * 100).toInt()}%")
                            if (download.localPath != null) {
                                appendLine("  File: ${download.localPath}")
                            }
                            if (download.errorMessage != null) {
                                appendLine("  Error: ${download.errorMessage}")
                            }
                            appendLine()
                        }
                        
                        if (allDownloads.size > 10) {
                            appendLine("... and ${allDownloads.size - 10} more downloads")
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = downloadReport,
                    downloadTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "❌ Failed to list downloads: ${e.message}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * Verify that downloaded files actually exist on the file system
     */
    fun verifyDownloadedFiles() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "Verifying downloaded files..."
                )
                
                val completedDownloads = downloadRepository.getCompletedDownloads().first()
                val downloadDir = downloadRepository.getDownloadDirectory()
                val usedSpace = downloadRepository.getUsedStorageSpace()
                val availableSpace = downloadRepository.getAvailableStorageSpace()
                
                val verificationReport = buildString {
                    appendLine("🔍 FILE VERIFICATION REPORT")
                    appendLine("==============================")
                    appendLine("Download Directory: ${downloadDir.absolutePath}")
                    appendLine("Directory Exists: ${downloadDir.exists()}")
                    appendLine("Directory Writable: ${downloadDir.canWrite()}")
                    appendLine("Used Space: ${formatBytes(usedSpace)}")
                    appendLine("Available Space: ${formatBytes(availableSpace)}")
                    appendLine()
                    
                    if (completedDownloads.isEmpty()) {
                        appendLine("❌ No completed downloads found!")
                        appendLine("Downloads may still be in progress or failed.")
                        appendLine("Check 'List Downloads' for status details.")
                    } else {
                        appendLine("📁 COMPLETED DOWNLOADS:")
                        var filesFound = 0
                        var filesMissing = 0
                        
                        completedDownloads.take(10).forEach { download ->
                            appendLine("• ${download.recordingId}")
                            if (download.localPath != null) {
                                val file = java.io.File(download.localPath!!)
                                val exists = file.exists()
                                val size = if (exists) file.length() else 0
                                
                                appendLine("  File: ${download.localPath}")
                                appendLine("  Exists: ${if (exists) "✅ YES" else "❌ NO"}")
                                if (exists) {
                                    appendLine("  Size: ${formatBytes(size)}")
                                    filesFound++
                                } else {
                                    filesMissing++
                                }
                            } else {
                                appendLine("  ❌ No local path recorded")
                                filesMissing++
                            }
                            appendLine()
                        }
                        
                        appendLine("SUMMARY:")
                        appendLine("✅ Files Found: $filesFound")
                        appendLine("❌ Files Missing: $filesMissing")
                        
                        if (completedDownloads.size > 10) {
                            appendLine("... and ${completedDownloads.size - 10} more completed downloads")
                        }
                    }
                    
                    appendLine()
                    appendLine("🔧 TROUBLESHOOTING:")
                    if (completedDownloads.isEmpty()) {
                        appendLine("• Run 'Test Download' to create downloads")
                        appendLine("• Check 'List Downloads' for current status")
                        appendLine("• Monitor logcat for download worker activity")
                    } else if (completedDownloads.isNotEmpty()) {
                        appendLine("• Files are saved to: ${downloadDir.absolutePath}")
                        appendLine("• Check device storage space if downloads fail")
                        appendLine("• Use file manager to browse download directory")
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = verificationReport,
                    downloadTestSuccess = completedDownloads.isNotEmpty()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "❌ File verification failed: ${e.message}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * Comprehensive troubleshooting for download system issues
     */
    fun troubleshootDownloads() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "🔧 Running download system diagnostics..."
                )
                
                val troubleshootingReport = buildString {
                    appendLine("🔧 DOWNLOAD SYSTEM TROUBLESHOOTING")
                    appendLine("===================================")
                    
                    // Check 1: Download Repository
                    try {
                        val allDownloads = downloadRepository.exportDownloadList()
                        val queuedDownloads = downloadRepository.getDownloadQueue()
                        appendLine("✅ DownloadRepository: Working")
                        appendLine("   Total downloads: ${allDownloads.size}")
                        appendLine("   Queued downloads: ${queuedDownloads.size}")
                    } catch (e: Exception) {
                        appendLine("❌ DownloadRepository: FAILED - ${e.message}")
                    }
                    
                    // Check 2: Download Queue Manager
                    try {
                        val queueStatus = downloadQueueManager.getQueueProcessingStatus()
                        val isActive = downloadQueueManager.isQueueProcessingActive()
                        appendLine("✅ DownloadQueueManager: Working")
                        appendLine("   Queue processing active: $isActive")
                        appendLine("   Queue status: $queueStatus")
                    } catch (e: Exception) {
                        appendLine("❌ DownloadQueueManager: FAILED - ${e.message}")
                    }
                    
                    // Check 3: File System
                    try {
                        val downloadDir = downloadRepository.getDownloadDirectory()
                        val usedSpace = downloadRepository.getUsedStorageSpace()
                        val availableSpace = downloadRepository.getAvailableStorageSpace()
                        appendLine("✅ File System: Working")
                        appendLine("   Download directory: ${downloadDir.absolutePath}")
                        appendLine("   Directory exists: ${downloadDir.exists()}")
                        appendLine("   Directory writable: ${downloadDir.canWrite()}")
                        appendLine("   Used space: ${formatBytes(usedSpace)}")
                        appendLine("   Available space: ${formatBytes(availableSpace)}")
                    } catch (e: Exception) {
                        appendLine("❌ File System: FAILED - ${e.message}")
                    }
                    
                    // Check 4: Sample recording availability
                    try {
                        val recordings = concertRepository.getAllCachedRecordings().first()
                        appendLine("✅ Sample Recordings: ${recordings.size} available")
                        if (recordings.isNotEmpty()) {
                            val sample = recordings.first()
                            appendLine("   Sample ID: ${sample.identifier}")
                            appendLine("   Sample title: ${sample.title}")
                            
                            // Try to get track URLs
                            try {
                                val trackUrls = concertRepository.getTrackStreamingUrls(sample.identifier)
                                appendLine("   Track URLs: ${trackUrls.size} tracks found")
                                trackUrls.take(3).forEach { (audioFile, url) ->
                                    appendLine("     • ${audioFile.filename} -> ${url.take(50)}...")
                                }
                            } catch (e: Exception) {
                                appendLine("   ❌ Track URLs: FAILED - ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        appendLine("❌ Sample Recordings: FAILED - ${e.message}")
                    }
                    
                    // Check 5: Download creation test
                    try {
                        val recordings = concertRepository.getAllCachedRecordings().first()
                        if (recordings.isNotEmpty()) {
                            val testRecording = recordings.first()
                            val downloadIds = downloadRepository.downloadRecording(testRecording)
                            appendLine("✅ Download Creation Test: SUCCESS")
                            appendLine("   Created ${downloadIds.size} download entries")
                            appendLine("   Download IDs: ${downloadIds.joinToString(", ")}")
                            
                            // Check if entries actually exist in database
                            delay(500) // Give it time to save
                            val allDownloads = downloadRepository.exportDownloadList()
                            val matchingDownloads = allDownloads.filter { 
                                downloadIds.contains(it.id) 
                            }
                            appendLine("   Database entries created: ${matchingDownloads.size}")
                            matchingDownloads.forEach { download ->
                                appendLine("     - ${download.id}: ${download.status}")
                            }
                        }
                    } catch (e: Exception) {
                        appendLine("❌ Download Creation Test: FAILED - ${e.message}")
                    }
                    
                    appendLine()
                    appendLine("🎯 COMMON ISSUES & SOLUTIONS:")
                    appendLine("1. No downloads in database:")
                    appendLine("   • Check if ShowRepository.getTrackStreamingUrls() is working")
                    appendLine("   • Verify network connectivity for metadata fetching")
                    appendLine("   • Run 'Export Test Data' to ensure recordings exist")
                    appendLine()
                    appendLine("2. Downloads stuck in QUEUED status:")
                    appendLine("   • Check WorkManager is properly initialized")
                    appendLine("   • Verify Hilt dependency injection is working")
                    appendLine("   • Check device constraints (network, storage, battery)")
                    appendLine()
                    appendLine("3. Downloads fail immediately:")
                    appendLine("   • Check storage permissions")
                    appendLine("   • Verify download URLs are accessible")
                    appendLine("   • Check available storage space")
                    appendLine()
                    appendLine("💡 NEXT ACTIONS:")
                    appendLine("• Run detailed 'Test Download' for step-by-step analysis")
                    appendLine("• Monitor logcat: adb logcat | grep -E '(Download|Worker|Hilt)'")
                    appendLine("• Check WorkManager status in device settings")
                }
                
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = troubleshootingReport,
                    downloadTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    downloadTestStatus = "❌ Troubleshooting failed: ${e.message}\n\n${e.stackTraceToString()}",
                    downloadTestSuccess = false
                )
            }
        }
    }
    
    /**
     * Update database wipe progress
     */
    private fun updateWipeProgress(
        phase: WipePhase,
        totalItems: Int = _uiState.value.wipeProgress.totalItems,
        processedItems: Int = _uiState.value.wipeProgress.processedItems,
        currentItem: String = _uiState.value.wipeProgress.currentItem,
        error: String? = null
    ) {
        val newProgress = DatabaseWipeProgress(
            phase = phase,
            totalItems = totalItems,
            processedItems = processedItems,
            currentItem = currentItem,
            error = error
        )
        
        _uiState.value = _uiState.value.copy(
            wipeProgress = newProgress,
            isWipingDatabase = newProgress.isInProgress
        )
    }

    /**
     * Wipe database while preserving library items with detailed progress reporting
     */
    fun wipeDatabase() {
        viewModelScope.launch {
            try {
                // Initialize progress
                updateWipeProgress(
                    phase = WipePhase.STARTING,
                    totalItems = 6,
                    processedItems = 0,
                    currentItem = "Preparing database wipe operation..."
                )
                
                delay(500) // Give user time to see the start
                
                // Step 1: Get counts for progress tracking
                updateWipeProgress(
                    phase = WipePhase.STARTING,
                    processedItems = 1,
                    currentItem = "Analyzing database contents..."
                )
                
                val recordingCount = try {
                    dataSyncService.getTotalRecordingCount()
                } catch (e: Exception) {
                    0
                }
                
                delay(500)
                
                // Step 2: Clear recordings and shows
                updateWipeProgress(
                    phase = WipePhase.CLEARING_RECORDINGS,
                    processedItems = 2,
                    currentItem = "Clearing cached recordings and shows (${recordingCount} items)..."
                )
                
                delay(1000)
                
                // Step 3: Clear sync metadata
                updateWipeProgress(
                    phase = WipePhase.CLEARING_SYNC_METADATA,
                    processedItems = 3,
                    currentItem = "Clearing sync metadata and timestamps..."
                )
                
                // Use the force refresh catalog method which clears database but preserves library
                dataSyncService.forceRefreshCatalog()
                
                delay(1000)
                
                // Step 4: Clear downloads metadata (if any)
                updateWipeProgress(
                    phase = WipePhase.CLEARING_DOWNLOADS,
                    processedItems = 4,
                    currentItem = "Clearing download metadata..."
                )
                
                delay(500)
                
                // Step 5: Refresh ratings
                updateWipeProgress(
                    phase = WipePhase.REFRESHING_RATINGS,
                    processedItems = 5,
                    currentItem = "Refreshing ratings data from assets..."
                )
                
                // Also refresh ratings data from assets
                ratingsRepository.forceRefreshRatings()
                
                delay(1000)
                
                // Step 6: Complete
                updateWipeProgress(
                    phase = WipePhase.COMPLETED,
                    processedItems = 6,
                    currentItem = "Database wipe completed successfully!"
                )
                
                // Refresh the app info after wipe
                loadAppInfo()
                
                // Update the old status fields for backward compatibility
                _uiState.value = _uiState.value.copy(
                    databaseWipeStatus = "✅ Database wipe completed successfully!\n\n" +
                            "What was cleared and refreshed:\n" +
                            "• Cached show and recording data (${recordingCount} recordings)\n" +
                            "• Sync metadata and timestamps\n" +
                            "• Download metadata and progress\n" +
                            "• Ratings data (refreshed from latest assets)\n\n" +
                            "What was preserved:\n" +
                            "• Your library (favorited shows/recordings)\n" +
                            "• App settings and preferences\n" +
                            "• Downloaded audio files\n\n" +
                            "The app will now re-download fresh catalog data from Archive.org when needed.",
                    databaseWipeSuccess = true
                )
                
            } catch (e: Exception) {
                updateWipeProgress(
                    phase = WipePhase.ERROR,
                    currentItem = "Database wipe failed",
                    error = e.message
                )
                
                _uiState.value = _uiState.value.copy(
                    databaseWipeStatus = "❌ Database wipe failed: ${e.message}\n\n" +
                            "The database may be partially cleared. Please restart the app and try again if needed.\n\n" +
                            "Error details:\n${e.stackTraceToString()}",
                    databaseWipeSuccess = false
                )
            }
        }
    }

    private fun loadSetlistInfo() {
        viewModelScope.launch {
            try {
                // Initialize setlist data if needed
                setlistRepository.initializeSetlistDataIfNeeded()
                
                // Get setlist statistics
                val stats = setlistRepository.getSetlistStatistics()
                _uiState.value = _uiState.value.copy(
                    setlistCount = stats["setlist_count"] as? Int ?: 0,
                    songCount = stats["song_count"] as? Int ?: 0,
                    venueCount = stats["venue_count"] as? Int ?: 0
                )
            } catch (e: Exception) {
                println("DEBUG: Error loading setlist info: ${e.message}")
            }
        }
    }
    
    fun testSetlistData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                val result = StringBuilder()
                
                // Test basic statistics
                val stats = setlistRepository.getSetlistStatistics()
                result.appendLine("=== SETLIST DATA STATISTICS ===")
                result.appendLine("Total Setlists: ${stats["setlist_count"]}")
                result.appendLine("Total Songs: ${stats["song_count"]}")
                result.appendLine("Total Venues: ${stats["venue_count"]}")
                result.appendLine("Average Songs per Setlist: ${"%.1f".format(stats["average_song_count"] as? Float ?: 0f)}")
                result.appendLine()
                
                // Test sample data queries
                result.appendLine("=== SAMPLE SETLISTS ===")
                val sampleSetlists = setlistRepository.getBestQualitySetlists().take(5)
                sampleSetlists.forEach { setlist ->
                    result.appendLine("${setlist.date} - ${setlist.displayVenue}")
                    result.appendLine("  Source: ${setlist.source}, Songs: ${setlist.totalSongs}")
                    if (setlist.hasSongs) {
                        val firstFewSongs = setlist.songs.take(3)
                        firstFewSongs.forEach { song ->
                            result.appendLine("    • ${song.displayName}")
                        }
                        if (setlist.songs.size > 3) {
                            result.appendLine("    ... and ${setlist.songs.size - 3} more")
                        }
                    }
                    result.appendLine()
                }
                
                // Test search functionality
                result.appendLine("=== SEARCH TEST ===")
                val searchResults = setlistRepository.searchSetlists("1977-05-08")
                result.appendLine("Search for '1977-05-08': ${searchResults.size} results")
                searchResults.take(2).forEach { setlist ->
                    result.appendLine("  ${setlist.date} at ${setlist.displayVenue}")
                }
                result.appendLine()
                
                // Test popular venues
                result.appendLine("=== POPULAR VENUES ===")
                val popularVenues = setlistRepository.getMostPopularVenues(5)
                popularVenues.forEach { venue ->
                    result.appendLine("${venue.displayName} - ${venue.showFrequency}")
                }
                result.appendLine()
                
                // Test most played songs
                result.appendLine("=== MOST PLAYED SONGS ===")
                val popularSongs = setlistRepository.getMostPlayedSongs(5)
                popularSongs.forEach { song ->
                    result.appendLine("${song.displayName} - ${song.performanceFrequency}")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = result.toString(),
                    setlistTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Error testing setlist data:\n${e.message}\n\nStack trace:\n${e.stackTraceToString()}",
                    setlistTestSuccess = false
                )
            }
        }
    }
    
    fun refreshSetlistData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                setlistRepository.forceRefreshSetlistData()
                loadSetlistInfo()
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Setlist data refreshed successfully from assets!",
                    setlistTestSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Error refreshing setlist data:\n${e.message}",
                    setlistTestSuccess = false
                )
            }
        }
    }
    
    fun searchSetlistsByDate(dateQuery: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                val results = setlistRepository.searchSetlists(dateQuery)
                val result = StringBuilder()
                
                result.appendLine("=== SEARCH RESULTS FOR '$dateQuery' ===")
                result.appendLine("Found ${results.size} setlists")
                result.appendLine()
                
                results.take(10).forEach { setlist ->
                    result.appendLine("${setlist.date} - ${setlist.displayVenue}")
                    result.appendLine("  Source: ${setlist.source}")
                    if (setlist.hasSongs) {
                        result.appendLine("  Songs: ${setlist.totalSongs}")
                        val highlights = setlist.songs.take(2)
                        highlights.forEach { song ->
                            result.appendLine("    • ${song.displayName}")
                        }
                        if (setlist.songs.size > 2) {
                            result.appendLine("    ... and ${setlist.songs.size - 2} more")
                        }
                    }
                    result.appendLine()
                }
                
                if (results.size > 10) {
                    result.appendLine("... and ${results.size - 10} more results")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = result.toString(),
                    setlistTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Error searching setlists:\n${e.message}",
                    setlistTestSuccess = false
                )
            }
        }
    }
    
    fun searchSetlistsBySong(songName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                val result = StringBuilder()
                result.appendLine("=== DEBUG: SONG SEARCH FOR '$songName' ===")
                result.appendLine()
                
                // Step 1: Search for matching songs in database
                result.appendLine("STEP 1: Searching songs database for '$songName'...")
                val songStats = setlistRepository.getSetlistStatistics()
                result.appendLine("Total songs in database: ${songStats["song_count"]}")
                
                // Get all songs and show some that might match
                val allSongs = setlistRepository.searchSongs(songName)
                result.appendLine("Songs found matching '$songName': ${allSongs.size}")
                
                if (allSongs.isEmpty()) {
                    result.appendLine("❌ NO SONGS FOUND - trying broader search...")
                    val broaderSongs = setlistRepository.searchSongs(songName.split(" ").first())
                    result.appendLine("Broader search for '${songName.split(" ").first()}': ${broaderSongs.size} songs")
                    broaderSongs.take(10).forEach { song ->
                        result.appendLine("  • ${song.displayName} (aliases: ${song.aliases.joinToString(", ")})")
                    }
                } else {
                    allSongs.take(10).forEach { song ->
                        result.appendLine("  ✓ ${song.displayName}")
                        if (song.aliases.isNotEmpty()) {
                            result.appendLine("    Aliases: ${song.aliases.joinToString(", ")}")
                        }
                    }
                }
                result.appendLine()
                
                // Step 2: Search setlists
                result.appendLine("STEP 2: Searching setlists...")
                val setlistResults = setlistRepository.searchSetlistsBySong(songName)
                result.appendLine("Setlists found: ${setlistResults.size}")
                result.appendLine()
                
                if (setlistResults.isEmpty()) {
                    result.appendLine("❌ NO SETLISTS FOUND")
                    result.appendLine()
                    result.appendLine("DEBUG INFO:")
                    result.appendLine("- Song database has ${songStats["song_count"]} songs")
                    result.appendLine("- Setlist database has ${songStats["setlist_count"]} setlists")
                    result.appendLine("- Venue database has ${songStats["venue_count"]} venues")
                    result.appendLine()
                    result.appendLine("POSSIBLE ISSUES:")
                    result.appendLine("1. Song name mismatch (check exact spelling)")
                    result.appendLine("2. Song not in database")
                    result.appendLine("3. Setlists don't reference this song correctly")
                    result.appendLine("4. Search logic issue")
                } else {
                    result.appendLine("=== PERFORMANCES FOUND ===")
                    setlistResults.take(8).forEach { setlist ->
                        result.appendLine("${setlist.date} - ${setlist.displayVenue}")
                        result.appendLine("  Songs: ${setlist.totalSongs}")
                        
                        // Find the matching songs in this setlist
                        val matchingSongs = setlist.songs.filter { 
                            it.songName.contains(songName, ignoreCase = true) 
                        }
                        matchingSongs.forEach { song ->
                            val setInfo = if (song.setName != null) " [${song.setName}]" else ""
                            result.appendLine("    ★ ${song.displayName}$setInfo")
                        }
                        result.appendLine()
                    }
                    
                    if (setlistResults.size > 8) {
                        result.appendLine("... and ${setlistResults.size - 8} more performances")
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = result.toString(),
                    setlistTestSuccess = setlistResults.isNotEmpty()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Error searching by song:\n${e.message}\n\nStack trace:\n${e.stackTraceToString()}",
                    setlistTestSuccess = false
                )
            }
        }
    }
    
    fun getSongStatistics(songName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                val stats = setlistRepository.getSongStatistics(songName)
                val result = StringBuilder()
                
                if (stats != null) {
                    result.appendLine("=== SONG STATISTICS FOR '${stats.songName}' ===")
                    result.appendLine("Performance frequency: ${stats.performanceFrequency}")
                    result.appendLine("Years active: ${stats.yearsActive}")
                    result.appendLine("Peak year: ${stats.peakYear ?: "Unknown"}")
                    result.appendLine("Most common set: ${stats.mostCommonSet}")
                    result.appendLine("Average position in set: ${"%.1f".format(stats.averageSetPosition)}")
                    result.appendLine("Unique venues: ${stats.venues.size}")
                    result.appendLine()
                    
                    result.appendLine("=== YEARLY BREAKDOWN ===")
                    stats.years.toSortedMap().forEach { (year, count) ->
                        result.appendLine("$year: $count performance${if (count != 1) "s" else ""}")
                    }
                    result.appendLine()
                    
                    // Get some segue information
                    val segues = setlistRepository.findSongSegues(songName)
                    if (segues.isNotEmpty()) {
                        result.appendLine("=== COMMON TRANSITIONS ===")
                        segues.take(5).forEach { segue ->
                            result.appendLine("${segue.displayTransition} - ${segue.frequencyDescription}")
                        }
                    }
                } else {
                    result.appendLine("Song '$songName' not found in database")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = result.toString(),
                    setlistTestSuccess = stats != null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "Error getting song statistics:\n${e.message}",
                    setlistTestSuccess = false
                )
            }
        }
    }
    
    fun debugSongSearch(songName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSetlistData = true)
            
            try {
                val result = StringBuilder()
                result.appendLine("=== DETAILED DEBUG: SONG SEARCH ===")
                result.appendLine("Query: '$songName'")
                result.appendLine("Timestamp: ${System.currentTimeMillis()}")
                result.appendLine()
                
                // Database stats
                val stats = setlistRepository.getSetlistStatistics()
                result.appendLine("DATABASE OVERVIEW:")
                result.appendLine("- Songs: ${stats["song_count"]}")
                result.appendLine("- Setlists: ${stats["setlist_count"]}")
                result.appendLine("- Venues: ${stats["venue_count"]}")
                result.appendLine()
                
                // Step 1: Raw song search
                result.appendLine("STEP 1: Raw Song Database Search")
                result.appendLine("Searching for songs containing '$songName'...")
                val matchingSongs = setlistRepository.searchSongs(songName)
                result.appendLine("Direct matches: ${matchingSongs.size}")
                
                if (matchingSongs.isEmpty()) {
                    // Try partial searches
                    val words = songName.lowercase().split(" ")
                    for (word in words) {
                        if (word.length > 2) {
                            val partialMatches = setlistRepository.searchSongs(word)
                            result.appendLine("Partial search '$word': ${partialMatches.size} songs")
                            partialMatches.take(5).forEach { song ->
                                result.appendLine("  → ${song.name}")
                            }
                        }
                    }
                } else {
                    matchingSongs.forEach { song ->
                        result.appendLine("  ✓ ${song.name}")
                        result.appendLine("    ID: ${song.songId}")
                        if (song.aliases.isNotEmpty()) {
                            result.appendLine("    Aliases: ${song.aliases}")
                        }
                        if (song.canonicalName != null) {
                            result.appendLine("    Canonical: ${song.canonicalName}")
                        }
                    }
                }
                result.appendLine()
                
                // Step 2: Setlist search process
                result.appendLine("STEP 2: Setlist Search Process")
                val setlistResults = setlistRepository.searchSetlistsBySong(songName)
                result.appendLine("Final setlist results: ${setlistResults.size}")
                
                if (setlistResults.isEmpty()) {
                    result.appendLine()
                    result.appendLine("❌ DIAGNOSIS:")
                    if (matchingSongs.isEmpty()) {
                        result.appendLine("• Song not found in songs database")
                        result.appendLine("• Try searching with different spelling or partial name")
                    } else {
                        result.appendLine("• Song exists in database but no setlist performances found")
                        result.appendLine("• Possible data linking issue between songs and setlists")
                    }
                } else {
                    result.appendLine()
                    result.appendLine("✅ SUCCESS: Found performances")
                    setlistResults.take(3).forEach { setlist ->
                        result.appendLine("📅 ${setlist.date} at ${setlist.displayVenue}")
                        val matchingSongsInSetlist = setlist.songs.filter { 
                            it.songName.contains(songName, ignoreCase = true) 
                        }
                        matchingSongsInSetlist.forEach { song ->
                            result.appendLine("  🎵 ${song.songName} [${song.setName ?: "unknown set"}]")
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = result.toString(),
                    setlistTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSetlistData = false,
                    setlistTestStatus = "DEBUG ERROR:\n${e.message}\n\n${e.stackTraceToString()}",
                    setlistTestSuccess = false
                )
            }
        }
    }

    /**
     * Format bytes into human-readable format
     */
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}