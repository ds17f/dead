package com.deadarchive.feature.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deadarchive.core.library.api.LibraryV2Service
import com.deadarchive.core.library.api.LibraryStats
import com.deadarchive.core.download.api.DownloadV2Service
import com.deadarchive.core.download.api.DownloadStatus
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.LibraryV2Show
import com.deadarchive.core.common.service.ShareService
import com.deadarchive.core.model.Recording
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    @Named("stub") private val downloadV2Service: DownloadV2Service,
    private val shareService: ShareService
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
            addServiceLog("loadLibrary() -> calling libraryV2Service.getLibraryV2Shows()")
            try {
                // Combine library shows with real-time download status
                libraryV2Service.getLibraryV2Shows()
                    .flatMapLatest { libraryShows ->
                        if (libraryShows.isEmpty()) {
                            flowOf(emptyList<LibraryV2Show>())
                        } else {
                            // For each show, combine with its real download status
                            val showFlows = libraryShows.map { libraryShow ->
                                combine(
                                    flowOf(libraryShow),
                                    downloadV2Service.getDownloadStatus(libraryShow.show)
                                ) { show, downloadApiStatus ->
                                    // Convert API DownloadStatus to model DownloadStatus
                                    val modelDownloadStatus = when (downloadApiStatus) {
                                        com.deadarchive.core.download.api.DownloadStatus.NOT_DOWNLOADED -> com.deadarchive.core.model.DownloadStatus.QUEUED
                                        com.deadarchive.core.download.api.DownloadStatus.DOWNLOADING -> com.deadarchive.core.model.DownloadStatus.DOWNLOADING
                                        com.deadarchive.core.download.api.DownloadStatus.COMPLETED -> com.deadarchive.core.model.DownloadStatus.COMPLETED
                                        com.deadarchive.core.download.api.DownloadStatus.FAILED -> com.deadarchive.core.model.DownloadStatus.FAILED
                                    }
                                    
                                    // Update the LibraryV2Show with real download status
                                    show.copy(downloadStatus = modelDownloadStatus)
                                }
                            }
                            
                            // Combine all show flows into a single list flow
                            combine(showFlows) { showArray ->
                                showArray.toList()
                            }
                        }
                    }
                    .collect { libraryShows ->
                        Log.d(TAG, "Received ${libraryShows.size} library shows with real download status")
                        addServiceLog("getLibraryV2Shows() returned ${libraryShows.size} shows with real download status")
                        _uiState.value = LibraryV2UiState.Success(libraryShows)
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
    
    fun downloadShow(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: downloadShow('${show.showId}') -> calling stub")
            addServiceLog("downloadShow('${show.showId}') -> calling downloadV2Service.downloadShow()")
            downloadV2Service.downloadShow(show.show)
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
    
    fun cancelShowDownloads(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: cancelShowDownloads('${show.showId}') -> calling stub")
            addServiceLog("cancelShowDownloads('${show.showId}') -> calling downloadV2Service.cancelShowDownloads()")
            downloadV2Service.cancelShowDownloads(show.show)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: cancelShowDownloads succeeded")
                    addServiceLog("cancelShowDownloads('${show.showId}') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: cancelShowDownloads failed: ${it.message}")
                    addServiceLog("ERROR: cancelShowDownloads('${show.showId}') failed: ${it.message}")
                }
        }
    }
    
    fun getDownloadStatus(show: LibraryV2Show) = downloadV2Service.getDownloadStatus(show.show).also {
        Log.d(TAG, "ViewModel: getDownloadStatus('${show.showId}') -> calling stub")
    }
    
    fun getPinStatus(show: LibraryV2Show) = libraryV2Service.isShowPinned(show.showId).also {
        Log.d(TAG, "ViewModel: getPinStatus('${show.showId}') -> calling stub")
    }
    
    fun shareShow(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: shareShow('${show.showId}')")
            addServiceLog("shareShow('${show.showId}') -> finding best recording")
            
            // Get the best recording to share
            val recording = show.show.recordings.firstOrNull()
            if (recording == null) {
                Log.e(TAG, "ViewModel: shareShow failed - no recording found")
                addServiceLog("ERROR: shareShow('${show.showId}') failed - no recording found")
                return@launch
            }
            
            // Share the show using ShareService
            addServiceLog("shareShow('${show.showId}') -> calling shareService.shareShow()")
            try {
                shareService.shareShow(show.show, recording)
                Log.d(TAG, "ViewModel: shareShow succeeded")
                addServiceLog("shareShow('${show.showId}') succeeded")
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: shareShow failed", e)
                addServiceLog("ERROR: shareShow('${show.showId}') failed: ${e.message}")
            }
        }
    }
    
    fun shareRecordingUrl(recording: Recording) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: shareRecordingUrl('${recording.identifier}')")
            addServiceLog("shareRecordingUrl('${recording.identifier}')")
            
            // We don't have direct access to the ShareService's ability to share just a URL
            // So we'll use the basic Android Intent approach by creating a fake Show
            val fakeShow = Show(
                date = recording.concertDate,
                venue = recording.concertVenue ?: "Unknown Venue",
                location = recording.concertLocation ?: "Unknown Location",
                recordings = listOf(recording),
                isInLibrary = true
            )
            
            try {
                shareService.shareShow(fakeShow, recording)
                Log.d(TAG, "ViewModel: shareRecordingUrl succeeded")
                addServiceLog("shareRecordingUrl('${recording.identifier}') succeeded")
            } catch (e: Exception) {
                Log.e(TAG, "ViewModel: shareRecordingUrl failed", e)
                addServiceLog("ERROR: shareRecordingUrl('${recording.identifier}') failed: ${e.message}")
            }
        }
    }
    
    fun pinShow(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: pinShow('${show.showId}') -> calling stub")
            addServiceLog("pinShow('${show.showId}') -> calling libraryV2Service.pinShow()")
            libraryV2Service.pinShow(show.showId)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: pinShow succeeded")
                    addServiceLog("pinShow('${show.showId}') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: pinShow failed: ${it.message}")
                    addServiceLog("ERROR: pinShow('${show.showId}') failed: ${it.message}")
                }
        }
    }
    
    fun unpinShow(show: LibraryV2Show) {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel: unpinShow('${show.showId}') -> calling stub")
            addServiceLog("unpinShow('${show.showId}') -> calling libraryV2Service.unpinShow()")
            libraryV2Service.unpinShow(show.showId)
                .onSuccess { 
                    Log.d(TAG, "ViewModel: unpinShow succeeded")
                    addServiceLog("unpinShow('${show.showId}') succeeded")
                }
                .onFailure { 
                    Log.e(TAG, "ViewModel: unpinShow failed: ${it.message}")
                    addServiceLog("ERROR: unpinShow('${show.showId}') failed: ${it.message}")
                }
        }
    }
    
    fun isShowPinned(showId: String) = libraryV2Service.isShowPinned(showId).also {
        Log.d(TAG, "ViewModel: isShowPinned('$showId') -> calling stub")
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
    data class Success(val shows: List<LibraryV2Show>) : LibraryV2UiState()
    data class Error(val message: String) : LibraryV2UiState()
}