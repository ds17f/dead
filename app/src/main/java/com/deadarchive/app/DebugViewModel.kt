package com.deadarchive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.data.sync.DataSyncService
import com.deadarchive.core.data.repository.ConcertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val cachedConcertCount: Int = 0,
    val lastSyncTime: String = "Never",
    val databaseSize: String = "Unknown"
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val dataSyncService: DataSyncService,
    private val concertRepository: ConcertRepository,
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
                val concertCount = dataSyncService.getTotalConcertCount()
                val lastSync = dataSyncService.getLastSyncTimestamp()
                val lastSyncFormatted = if (lastSync > 0) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastSync))
                } else {
                    "Never"
                }
                
                _uiState.value = _uiState.value.copy(
                    cachedConcertCount = concertCount,
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
            val sampleConcerts = concertRepository.getAllCachedConcerts().first().take(10)
            
            val catalogData = buildString {
                appendLine("{")
                appendLine("  \"metadata\": {")
                appendLine("    \"exported_at\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}\",")
                appendLine("    \"source\": \"Real app data via DataSyncService\",")
                appendLine("    \"total_concerts\": ${sampleConcerts.size}")
                appendLine("  },")
                appendLine("  \"sample_concerts\": [")
                
                sampleConcerts.forEachIndexed { index, concert ->
                    appendLine("    {")
                    appendLine("      \"identifier\": \"${concert.identifier}\",")
                    appendLine("      \"title\": \"${concert.title.replace("\"", "\\\"")}\",")
                    appendLine("      \"date\": \"${concert.date}\",")
                    appendLine("      \"venue\": \"${concert.venue?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"location\": \"${concert.location?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"year\": ${concert.year ?: 0},")
                    appendLine("      \"source\": \"${concert.source?.replace("\"", "\\\"") ?: ""}\",")
                    appendLine("      \"description\": \"${concert.description?.take(100)?.replace("\"", "\\\"") ?: ""}\"")
                    append("    }")
                    if (index < sampleConcerts.size - 1) appendLine(",")
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
            val sampleConcerts = concertRepository.getAllCachedConcerts().first().take(3)
            
            val metadataData = buildString {
                appendLine("[")
                
                sampleConcerts.forEachIndexed { index, concert ->
                    // Try to fetch real metadata using the concert repository
                    val metadata = concertRepository.getConcertMetadata(concert.identifier)
                    
                    appendLine("  {")
                    appendLine("    \"identifier\": \"${concert.identifier}\",")
                    appendLine("    \"metadata_available\": ${metadata != null},")
                    if (metadata != null) {
                        appendLine("    \"server\": \"${metadata.server ?: ""}\",")
                        appendLine("    \"directory\": \"${metadata.directory ?: ""}\",")
                        appendLine("    \"files_count\": ${metadata.files.size}")
                    }
                    append("  }")
                    if (index < sampleConcerts.size - 1) appendLine(",")
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
}