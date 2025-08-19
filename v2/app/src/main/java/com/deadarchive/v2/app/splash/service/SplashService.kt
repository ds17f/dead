package com.deadarchive.v2.app.splash.service

import android.util.Log
import com.deadarchive.v2.app.model.PhaseV2
import com.deadarchive.v2.app.model.ProgressV2
import com.deadarchive.v2.core.database.service.DatabaseManager
import com.deadarchive.v2.core.database.service.DatabaseImportResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for coordinating V2 database initialization during splash screen
 */
@Singleton
class SplashService @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    companion object {
        private const val TAG = "SplashV2Service"
    }
    
    private val _uiState = MutableStateFlow(SplashV2UiState())
    val uiState: StateFlow<SplashV2UiState> = _uiState.asStateFlow()
    
    // Track the start time of the initialization process
    private var initStartTimeMs: Long = 0L
    
    /**
     * Convert DatabaseManager progress to V2 splash progress
     */
    fun getV2Progress(): Flow<ProgressV2> {
        return databaseManager.progress.map { v2Progress ->
            val phase = when (v2Progress.phase) {
                "IDLE" -> PhaseV2.IDLE
                "CHECKING" -> PhaseV2.CHECKING
                "USING_LOCAL" -> PhaseV2.USING_LOCAL
                "DOWNLOADING" -> PhaseV2.DOWNLOADING
                "EXTRACTING" -> PhaseV2.EXTRACTING
                "IMPORTING_SHOWS" -> PhaseV2.IMPORTING_SHOWS
                "COMPUTING_VENUES" -> PhaseV2.COMPUTING_VENUES
                "IMPORTING_RECORDINGS" -> PhaseV2.IMPORTING_RECORDINGS
                "COMPLETED" -> PhaseV2.COMPLETED
                "ERROR" -> PhaseV2.ERROR
                else -> PhaseV2.IDLE
            }
            
            // Initialize start time when we begin processing
            if (initStartTimeMs == 0L && phase in listOf(PhaseV2.CHECKING, PhaseV2.USING_LOCAL, PhaseV2.DOWNLOADING, PhaseV2.EXTRACTING, PhaseV2.IMPORTING_SHOWS)) {
                initStartTimeMs = System.currentTimeMillis()
                Log.d(TAG, "Database initialization started at ${initStartTimeMs}")
            }
            
            // Map progress based on phase type
            when (phase) {
                PhaseV2.IMPORTING_RECORDINGS -> ProgressV2(
                    phase = phase,
                    totalShows = 0,
                    processedShows = 0,
                    currentShow = "",
                    totalRecordings = v2Progress.totalItems,
                    processedRecordings = v2Progress.processedItems,
                    currentRecording = v2Progress.currentItem,
                    startTimeMs = initStartTimeMs,
                    error = v2Progress.error
                )
                PhaseV2.COMPUTING_VENUES -> ProgressV2(
                    phase = phase,
                    totalShows = 0,
                    processedShows = 0,
                    currentShow = "",
                    totalVenues = v2Progress.totalItems,
                    processedVenues = v2Progress.processedItems,
                    startTimeMs = initStartTimeMs,
                    error = v2Progress.error
                )
                else -> ProgressV2(
                    phase = phase,
                    totalShows = v2Progress.totalItems,
                    processedShows = v2Progress.processedItems,
                    currentShow = v2Progress.currentItem,
                    startTimeMs = initStartTimeMs,
                    error = v2Progress.error
                )
            }
        }
    }
    
    /**
     * Initialize V2 database with progress tracking
     */
    suspend fun initializeV2Database(): V2InitResult {
        return try {
            Log.d(TAG, "Starting V2 database initialization")
            
            // Test GitHub API integration
            databaseManager.testGitHubIntegration()
            
            // Test database health checking
            databaseManager.testDatabaseHealth()
            
            // Test file discovery
            databaseManager.testFileDiscovery()
            
            // Test download functionality (optional - can be slow)
            // databaseManager.testDownloadFunctionality()
            
            val result = databaseManager.initializeV2DataIfNeeded()
            
            when (result) {
                is DatabaseImportResult.Success -> {
                    Log.d(TAG, "V2 database initialization completed: ${result.showsImported} shows, ${result.venuesImported} venues")
                    V2InitResult.Success(result.showsImported, result.venuesImported)
                }
                is DatabaseImportResult.Error -> {
                    Log.e(TAG, "V2 database initialization failed: ${result.error}")
                    V2InitResult.Error(result.error)
                }
                is DatabaseImportResult.RequiresUserChoice -> {
                    Log.d(TAG, "Multiple database sources available, requiring user choice")
                    // Update UI to show source selection
                    updateUiState(
                        showProgress = false,
                        showSourceSelection = true,
                        availableSources = result.availableSources.sources,
                        message = "Choose database source"
                    )
                    V2InitResult.Error("User choice required") // Will be handled by source selection UI
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "V2 database initialization exception", e)
            V2InitResult.Error(e.message ?: "Initialization failed")
        }
    }
    
    /**
     * Check if V2 data is already initialized
     */
    suspend fun isV2DataInitialized(): Boolean {
        return try {
            databaseManager.isV2DataInitialized()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check V2 data initialization status", e)
            false
        }
    }
    
    /**
     * Update splash UI state
     */
    fun updateUiState(
        isReady: Boolean = _uiState.value.isReady,
        showError: Boolean = _uiState.value.showError,
        showProgress: Boolean = _uiState.value.showProgress,
        showSourceSelection: Boolean = _uiState.value.showSourceSelection,
        availableSources: List<DatabaseManager.DatabaseSource> = _uiState.value.availableSources,
        message: String = _uiState.value.message,
        errorMessage: String? = _uiState.value.errorMessage,
        progress: ProgressV2 = _uiState.value.progress
    ) {
        _uiState.value = SplashV2UiState(
            isReady = isReady,
            showError = showError,
            showProgress = showProgress,
            showSourceSelection = showSourceSelection,
            availableSources = availableSources,
            message = message,
            errorMessage = errorMessage,
            progress = progress
        )
    }
    
    /**
     * Retry V2 database initialization
     */
    fun retryInitialization(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            updateUiState(
                showError = false,
                showProgress = true,
                message = "Retrying V2 database initialization...",
                errorMessage = null
            )
            
            val result = initializeV2Database()
            when (result) {
                is V2InitResult.Success -> {
                    updateUiState(
                        isReady = true,
                        showProgress = false,
                        message = "V2 database ready: ${result.showsImported} shows loaded"
                    )
                }
                is V2InitResult.Error -> {
                    updateUiState(
                        showError = true,
                        showProgress = false,
                        message = "V2 database initialization failed",
                        errorMessage = result.error
                    )
                }
            }
        }
    }
    
    /**
     * Skip V2 initialization and proceed
     */
    fun skipInitialization() {
        updateUiState(
            isReady = true,
            showError = false,
            showProgress = false,
            message = "Skipped V2 database initialization"
        )
    }
    
    /**
     * Abort current V2 initialization and proceed to main screen
     */
    fun abortInitialization() {
        updateUiState(
            isReady = true,
            showError = false,
            showProgress = false,
            showSourceSelection = false,
            message = "V2 database import aborted"
        )
    }
    
    /**
     * User selected a database source for initialization
     */
    fun selectDatabaseSource(source: DatabaseManager.DatabaseSource, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            Log.d(TAG, "User selected database source: $source")
            
            // Hide source selection and show progress
            updateUiState(
                showSourceSelection = false,
                showProgress = true,
                message = when (source) {
                    DatabaseManager.DatabaseSource.ZIP_BACKUP -> "Restoring from backup..."
                    DatabaseManager.DatabaseSource.DATA_IMPORT -> "Importing fresh data..."
                }
            )
            
            try {
                val result = databaseManager.initializeFromSource(source)
                
                when (result) {
                    is DatabaseImportResult.Success -> {
                        updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = when (source) {
                                DatabaseManager.DatabaseSource.ZIP_BACKUP -> "Database restored from backup"
                                DatabaseManager.DatabaseSource.DATA_IMPORT -> "V2 database ready: ${result.showsImported} shows loaded"
                            }
                        )
                    }
                    is DatabaseImportResult.Error -> {
                        updateUiState(
                            showError = true,
                            showProgress = false,
                            message = "Database initialization failed",
                            errorMessage = result.error
                        )
                    }
                    is DatabaseImportResult.RequiresUserChoice -> {
                        // This shouldn't happen when selecting a specific source
                        updateUiState(
                            showError = true,
                            showProgress = false,
                            message = "Unexpected error: user choice required again",
                            errorMessage = "Internal error"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize from selected source: $source", e)
                updateUiState(
                    showError = true,
                    showProgress = false,
                    message = "Database initialization failed",
                    errorMessage = e.message
                )
            }
        }
    }
}

/**
 * UI state for SplashV2 screen
 */
data class SplashV2UiState(
    val isReady: Boolean = false,
    val showError: Boolean = false,
    val showProgress: Boolean = false,
    val showSourceSelection: Boolean = false,
    val availableSources: List<DatabaseManager.DatabaseSource> = emptyList(),
    val message: String = "Loading V2 database...",
    val errorMessage: String? = null,
    val progress: ProgressV2 = ProgressV2(
        phase = PhaseV2.IDLE,
        totalShows = 0,
        processedShows = 0,
        currentShow = ""
    )
)

/**
 * Result of V2 database initialization
 */
sealed class V2InitResult {
    data class Success(val showsImported: Int, val venuesImported: Int) : V2InitResult()
    data class Error(val error: String) : V2InitResult()
}