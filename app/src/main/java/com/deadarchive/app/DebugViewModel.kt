package com.deadarchive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.sync.DataSyncService
import com.deadarchive.core.data.repository.ShowRepository
import com.deadarchive.core.data.repository.DownloadRepository
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
    val downloadTestSuccess: Boolean = false
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val dataSyncService: DataSyncService,
    private val concertRepository: ShowRepository,
    private val downloadRepository: DownloadRepository,
    private val downloadQueueManager: DownloadQueueManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()
    
    init {
        loadAppInfo()
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
     * Test downloading a sample recording
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
                    downloadTestStatus = "Testing download for: ${sampleRecording.title}\nIdentifier: ${sampleRecording.identifier}"
                )
                
                // Start the download
                downloadRepository.downloadRecording(sampleRecording)
                
                // Give it a moment to start
                delay(2000)
                
                // Check the status
                val queueStatus = downloadQueueManager.getQueueProcessingStatus()
                
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = "✅ Download initiated successfully!\n\n" +
                            "Recording: ${sampleRecording.title}\n" +
                            "Identifier: ${sampleRecording.identifier}\n" +
                            "Queue Status: $queueStatus\n\n" +
                            "Check logcat for detailed progress:\n" +
                            "adb logcat | grep -E '(DownloadRepository|DownloadQueueManager|AudioDownloadWorker)'",
                    downloadTestSuccess = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadTesting = false,
                    downloadTestStatus = "❌ Download test failed: ${e.message}",
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
}