package com.deadarchive.v2.app.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.v2.app.model.PhaseV2
import com.deadarchive.v2.app.splash.service.SplashV2Service
import com.deadarchive.v2.app.splash.service.SplashV2UiState
import com.deadarchive.v2.app.splash.service.V2InitResult
import com.deadarchive.v2.core.database.service.DatabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SplashV2 screen
 */
@HiltViewModel
class SplashViewModelV2 @Inject constructor(
    private val splashV2Service: SplashV2Service
) : ViewModel() {
    
    val uiState: StateFlow<SplashV2UiState> = splashV2Service.uiState
    
    init {
        initializeV2Database()
    }
    
    private fun initializeV2Database() {
        viewModelScope.launch {
            try {
                // Always show progress first and let the service handle restoration/import
                // The service will check for database ZIP, existing data, etc.
                
                // Show progress and start initialization
                splashV2Service.updateUiState(
                    showProgress = true,
                    message = "Initializing V2 database..."
                )
                
                // Collect progress updates
                launch {
                    splashV2Service.getV2Progress().collect { progress ->
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
                        
                        splashV2Service.updateUiState(
                            showProgress = progress.phase != PhaseV2.COMPLETED && progress.phase != PhaseV2.ERROR,
                            showError = progress.phase == PhaseV2.ERROR,
                            message = message,
                            errorMessage = progress.error,
                            progress = progress
                        )
                        
                        if (progress.phase == PhaseV2.COMPLETED) {
                            splashV2Service.updateUiState(isReady = true)
                        }
                    }
                }
                
                // Start the initialization
                val result = splashV2Service.initializeV2Database()
                
                when (result) {
                    is V2InitResult.Success -> {
                        // Handle immediate success (e.g., database already initialized)
                        splashV2Service.updateUiState(
                            isReady = true,
                            showProgress = false,
                            message = "V2 database ready: ${result.showsImported} shows loaded"
                        )
                    }
                    is V2InitResult.Error -> {
                        splashV2Service.updateUiState(
                            showError = true,
                            showProgress = false,
                            message = "V2 database initialization failed",
                            errorMessage = result.error
                        )
                    }
                }
                
            } catch (e: Exception) {
                splashV2Service.updateUiState(
                    showError = true,
                    showProgress = false,
                    message = "V2 database initialization failed",
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun retryInitialization() {
        splashV2Service.retryInitialization(viewModelScope)
    }
    
    fun skipInitialization() {
        splashV2Service.skipInitialization()
    }
    
    fun abortInitialization() {
        splashV2Service.abortInitialization()
    }
    
    fun selectDatabaseSource(source: DatabaseManager.DatabaseSource) {
        splashV2Service.selectDatabaseSource(source, viewModelScope)
    }
}