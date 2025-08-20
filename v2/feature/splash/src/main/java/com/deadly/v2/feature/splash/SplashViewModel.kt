package com.deadly.v2.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadly.v2.feature.splash.model.PhaseV2
import com.deadly.v2.feature.splash.service.SplashService
import com.deadly.v2.feature.splash.service.SplashV2UiState
import com.deadly.v2.feature.splash.service.V2InitResult
import com.deadly.v2.core.database.service.DatabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SplashV2 screen
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val splashService: SplashService
) : ViewModel() {
    
    val uiState: StateFlow<SplashV2UiState> = splashService.uiState
    
    init {
        initializeV2Database()
    }
    
    private fun initializeV2Database() {
        viewModelScope.launch {
            try {
                // Always show progress first and let the service handle restoration/import
                // The service will check for database ZIP, existing data, etc.
                
                // Show progress and start initialization
                splashService.updateUiState(
                    showProgress = true,
                    message = "Initializing V2 database..."
                )
                
                // Collect progress updates
                launch {
                    splashService.getV2Progress().collect { progress ->
                        val message = when (progress.phase) {
                            PhaseV2.IDLE -> "Preparing V2 database..."
                            PhaseV2.CHECKING -> "Checking existing data..."
                            PhaseV2.USING_LOCAL -> "Using local files..."
                            PhaseV2.DOWNLOADING -> "Downloading files..."
                            PhaseV2.EXTRACTING -> "Extracting data files..."
                            PhaseV2.IMPORTING_SHOWS -> "Importing shows (${progress.processedShows}/${progress.totalShows})"
                            PhaseV2.COMPUTING_VENUES -> "Computing venue statistics..."
                            PhaseV2.IMPORTING_RECORDINGS -> "Importing recordings (${progress.processedRecordings}/${progress.totalRecordings})"
                            PhaseV2.COMPLETED -> "V2 database ready!"
                            PhaseV2.ERROR -> "V2 database error"
                        }
                        
                        splashService.updateUiState(
                            showProgress = progress.phase != PhaseV2.COMPLETED && progress.phase != PhaseV2.ERROR,
                            showError = progress.phase == PhaseV2.ERROR,
                            message = message,
                            errorMessage = progress.error,
                            progress = progress
                        )
                        
                        if (progress.phase == PhaseV2.COMPLETED) {
                            splashService.updateUiState(isReady = true)
                        }
                    }
                }
                
                // Start the initialization
                val result = splashService.initializeV2Database()
                
                when (result) {
                    is V2InitResult.Success -> {
                        // Handle immediate success (e.g., database already initialized)
                        splashService.updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = "V2 database ready: ${result.showsImported} shows loaded"
                        )
                    }
                    is V2InitResult.Error -> {
                        splashService.updateUiState(
                            showError = true,
                            showProgress = false,
                            message = "V2 database initialization failed",
                            errorMessage = result.error
                        )
                    }
                }
                
            } catch (e: Exception) {
                splashService.updateUiState(
                    showError = true,
                    showProgress = false,
                    message = "V2 database initialization failed",
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun retryInitialization() {
        splashService.retryInitialization(viewModelScope)
    }
    
    fun skipInitialization() {
        splashService.skipInitialization()
    }
    
    fun abortInitialization() {
        splashService.abortInitialization()
    }
    
    fun selectDatabaseSource(source: DatabaseManager.DatabaseSource) {
        splashService.selectDatabaseSource(source, viewModelScope)
    }
}