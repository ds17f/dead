package com.deadarchive.feature.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.library.api.LibraryV2Service
import com.deadarchive.core.library.api.LibraryStats
import com.deadarchive.core.download.api.DownloadV2Service
import com.deadarchive.core.download.api.DownloadStatus
import com.deadarchive.core.model.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for Library V2 using the stub-first development approach.
 * 
 * This ViewModel demonstrates clean architecture by depending only on service interfaces,
 * allowing for easy testing and development with stub implementations before real services
 * are implemented.
 */
@HiltViewModel
class LibraryV2ViewModel @Inject constructor(
    @Named("stub") private val libraryV2Service: LibraryV2Service,
    @Named("stub") private val downloadV2Service: DownloadV2Service
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryV2ViewModel"
    }
    
    private val _uiState = MutableStateFlow<LibraryV2UiState>(LibraryV2UiState.Loading)
    val uiState: StateFlow<LibraryV2UiState> = _uiState.asStateFlow()
    
    private val _libraryStats = MutableStateFlow<LibraryStats?>(null)
    val libraryStats: StateFlow<LibraryStats?> = _libraryStats.asStateFlow()
    
    // Service logs for debug panel
    private val _serviceLogs = MutableStateFlow<List<String>>(emptyList())
    val serviceLogs: StateFlow<List<String>> = _serviceLogs.asStateFlow()
    
    init {
        Log.d(TAG, "LibraryV2ViewModel initialized with STUB services")
        addServiceLog("ViewModel initialized with @Named(\"stub\") services")
        loadLibrary()
        loadLibraryStats()
    }
    
    private fun addServiceLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _serviceLogs.value = _serviceLogs.value + logEntry
        Log.d(TAG, "SERVICE_LOG: $logEntry")
    }
    
    private fun loadLibrary() {
        viewModelScope.launch {
            Log.d(TAG, "Loading library via stub service...")
            addServiceLog("loadLibrary() -> calling libraryV2Service.getLibraryShows()")
            try {
                libraryV2Service.getLibraryShows()
                    .collect { shows ->
                        Log.d(TAG, "Received ${shows.size} shows from stub service")
                        addServiceLog("getLibraryShows() returned ${shows.size} shows")
                        _uiState.value = LibraryV2UiState.Success(shows)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading library from stub", e)
                addServiceLog("ERROR: getLibraryShows() failed: ${e.message}")
                _uiState.value = LibraryV2UiState.Error(e.message ?: "Failed to load")
            }
        }
    }
    
    private fun loadLibraryStats() {
        viewModelScope.launch {
            addServiceLog("loadLibraryStats() -> calling libraryV2Service.getLibraryStats()")
            try {
                val stats = libraryV2Service.getLibraryStats()
                _libraryStats.value = stats
                Log.d(TAG, "Loaded stats from stub: $stats")
                addServiceLog("getLibraryStats() returned: shows=${stats.totalShows}, downloaded=${stats.totalDownloaded}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load stats from stub", e)
                addServiceLog("ERROR: getLibraryStats() failed: ${e.message}")
            }
        }
    }
    
    // All actions just call stubs and log
    fun addToLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: addToLibrary('$showId') -> calling stub")
            addServiceLog("addToLibrary('$showId') -> calling libraryV2Service.addShowToLibrary()")
            libraryV2Service.addShowToLibrary(showId)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: addToLibrary succeeded")
                    addServiceLog("addShowToLibrary('$showId') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: addToLibrary failed: ${it.message}")
                    addServiceLog("ERROR: addShowToLibrary('$showId') failed: ${it.message}")
                }
        }
    }
    
    fun removeFromLibrary(showId: String) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: removeFromLibrary('$showId') -> calling stub")
            addServiceLog("removeFromLibrary('$showId') -> calling libraryV2Service.removeShowFromLibrary()")
            libraryV2Service.removeShowFromLibrary(showId)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: removeFromLibrary succeeded")
                    addServiceLog("removeShowFromLibrary('$showId') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: removeFromLibrary failed: ${it.message}")
                    addServiceLog("ERROR: removeShowFromLibrary('$showId') failed: ${it.message}")
                }
        }
    }
    
    fun downloadShow(show: Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: downloadShow('${show.showId}') -> calling stub")
            addServiceLog("downloadShow('${show.showId}') -> calling downloadV2Service.downloadShow()")
            downloadV2Service.downloadShow(show)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: downloadShow succeeded")
                    addServiceLog("downloadShow('${show.showId}') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: downloadShow failed: ${it.message}")
                    addServiceLog("ERROR: downloadShow('${show.showId}') failed: ${it.message}")
                }
        }
    }
    
    fun getDownloadStatus(show: Show) = downloadV2Service.getDownloadStatus(show).also {
        Log.d(TAG, "ViewModel: getDownloadStatus('${show.showId}') -> calling stub")
    }
    
    fun clearLibrary() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: clearLibrary() -> calling stub")
            addServiceLog("clearLibrary() -> calling libraryV2Service.clearLibrary()")
            libraryV2Service.clearLibrary()
                .onSuccess { 
                    Log.d(TAG, "ViewModel: clearLibrary succeeded")
                    addServiceLog("clearLibrary() succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: clearLibrary failed: ${it.message}")
                    addServiceLog("ERROR: clearLibrary() failed: ${it.message}")
                }
        }
    }
    
    fun populateTestData() {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: populateTestData() -> calling stub")
            addServiceLog("populateTestData() -> calling libraryV2Service.populateTestData()")
            libraryV2Service.populateTestData()
                .onSuccess { 
                    Log.d(TAG, "ViewModel: populateTestData succeeded")
                    addServiceLog("populateTestData() succeeded - test data populated")
                    // Refresh stats after populating data
                    loadLibraryStats()
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: populateTestData failed: ${it.message}")
                    addServiceLog("ERROR: populateTestData() failed: ${it.message}")
                }
        }
    }
    
    fun retry() {
        Log.d(TAG, "ViewModel: retry() -> reloading with stubs")
        addServiceLog("retry() -> reloading library and stats")
        loadLibrary()
        loadLibraryStats()
    }
}

/**
 * UI state for Library V2 screen
 */
sealed class LibraryV2UiState {
    object Loading : LibraryV2UiState()
    data class Success(val shows: List<Show>) : LibraryV2UiState()
    data class Error(val message: String) : LibraryV2UiState()
}