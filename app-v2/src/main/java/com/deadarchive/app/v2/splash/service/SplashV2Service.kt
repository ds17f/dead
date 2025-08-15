package com.deadarchive.app.v2.splash.service

import android.util.Log
import com.deadarchive.app.v2.model.PhaseV2
import com.deadarchive.app.v2.model.ProgressV2
import com.deadarchive.core.database.v2.service.DatabaseManagerV2
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
class SplashV2Service @Inject constructor(
    private val v2DatabaseManager: DatabaseManagerV2
) {
    companion object {
        private const val TAG = "SplashV2Service"
    }
    
    private val _uiState = MutableStateFlow(SplashV2UiState())
    val uiState: StateFlow<SplashV2UiState> = _uiState.asStateFlow()
    
    /**
     * Convert DatabaseManagerV2 progress to V2 splash progress
     */
    fun getV2Progress(): Flow<ProgressV2> {
        return v2DatabaseManager.progress.map { v2Progress ->
            val phase = when (v2Progress.phase) {
                "IDLE" -> PhaseV2.IDLE
                "CHECKING" -> PhaseV2.CHECKING
                "EXTRACTING" -> PhaseV2.EXTRACTING
                "IMPORTING_SHOWS" -> PhaseV2.IMPORTING_SHOWS
                "COMPUTING_VENUES" -> PhaseV2.COMPUTING_VENUES
                "IMPORTING_RECORDINGS" -> PhaseV2.IMPORTING_RECORDINGS
                "COMPLETED" -> PhaseV2.COMPLETED
                "ERROR" -> PhaseV2.ERROR
                else -> PhaseV2.IDLE
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
                    error = v2Progress.error
                )
                PhaseV2.COMPUTING_VENUES -> ProgressV2(
                    phase = phase,
                    totalShows = 0,
                    processedShows = 0,
                    currentShow = "",
                    totalVenues = v2Progress.totalItems,
                    processedVenues = v2Progress.processedItems,
                    error = v2Progress.error
                )
                else -> ProgressV2(
                    phase = phase,
                    totalShows = v2Progress.totalItems,
                    processedShows = v2Progress.processedItems,
                    currentShow = v2Progress.currentItem,
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
            val result = v2DatabaseManager.initializeV2DataIfNeeded()
            
            if (result.success) {
                Log.d(TAG, "V2 database initialization completed: ${result.showsImported} shows, ${result.venuesImported} venues")
                V2InitResult.Success(result.showsImported, result.venuesImported)
            } else {
                Log.e(TAG, "V2 database initialization failed: ${result.error}")
                V2InitResult.Error(result.error ?: "Unknown error")
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
            v2DatabaseManager.isV2DataInitialized()
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
        message: String = _uiState.value.message,
        errorMessage: String? = _uiState.value.errorMessage,
        progress: ProgressV2 = _uiState.value.progress
    ) {
        _uiState.value = SplashV2UiState(
            isReady = isReady,
            showError = showError,
            showProgress = showProgress,
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
}

/**
 * UI state for SplashV2 screen
 */
data class SplashV2UiState(
    val isReady: Boolean = false,
    val showError: Boolean = false,
    val showProgress: Boolean = false,
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