package com.deadly.feature.browse.service

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadly.feature.browse.domain.SearchShowsUseCase
import com.deadly.feature.browse.BrowseUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for data loading operations in the Browse feature.
 * Handles loading popular shows, recent shows, and other data operations.
 */
@Singleton
class BrowseDataService @Inject constructor(
    private val searchShowsUseCase: SearchShowsUseCase
) {
    
    companion object {
        private const val TAG = "BrowseDataService"
    }
    
    // Track current data loading job to cancel previous operations
    private var currentDataJob: Job? = null
    
    /**
     * Load popular shows for the home screen
     */
    fun loadPopularShows(
        coroutineScope: CoroutineScope,
        onStateChange: (BrowseUiState) -> Unit,
        onSearchingStateChange: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Loading popular shows")
        
        // Cancel any previous data loading operation
        currentDataJob?.cancel()
        
        currentDataJob = coroutineScope.launch {
            onSearchingStateChange(true)
            onStateChange(BrowseUiState.Loading)
            
            try {
                val startTime = System.currentTimeMillis()
                searchShowsUseCase.getPopularShows()
                    .catch { exception ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Popular shows error after ${loadTime}ms: ${exception.message}")
                        onStateChange(BrowseUiState.Error(
                            exception.message ?: "Failed to load popular concerts"
                        ))
                        onSearchingStateChange(false)
                    }
                    .collect { shows ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Popular shows success after ${loadTime}ms, got ${shows.size} shows")
                        onStateChange(BrowseUiState.Success(shows))
                        onSearchingStateChange(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Popular shows exception: ${e.message}")
                onStateChange(BrowseUiState.Error(
                    e.message ?: "Failed to load popular concerts"
                ))
                onSearchingStateChange(false)
            }
        }
    }
    
    /**
     * Load recent shows
     */
    fun loadRecentShows(
        coroutineScope: CoroutineScope,
        onStateChange: (BrowseUiState) -> Unit,
        onSearchingStateChange: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Loading recent shows")
        
        // Cancel any previous data loading operation
        currentDataJob?.cancel()
        
        currentDataJob = coroutineScope.launch {
            onSearchingStateChange(true)
            onStateChange(BrowseUiState.Loading)
            
            try {
                val startTime = System.currentTimeMillis()
                searchShowsUseCase.getRecentShows()
                    .catch { exception ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Recent shows error after ${loadTime}ms: ${exception.message}")
                        onStateChange(BrowseUiState.Error(
                            exception.message ?: "Failed to load recent concerts"
                        ))
                        onSearchingStateChange(false)
                    }
                    .collect { shows ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Recent shows success after ${loadTime}ms, got ${shows.size} shows")
                        onStateChange(BrowseUiState.Success(shows))
                        onSearchingStateChange(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Recent shows exception: ${e.message}")
                onStateChange(BrowseUiState.Error(
                    e.message ?: "Failed to load recent concerts"
                ))
                onSearchingStateChange(false)
            }
        }
    }
    
    /**
     * Load initial data for the browse screen (popular shows from 1977)
     */
    fun loadInitialData(
        coroutineScope: CoroutineScope,
        onStateChange: (BrowseUiState) -> Unit,
        onSearchingStateChange: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Loading initial data (1977 shows)")
        
        // Cancel any previous data loading operation
        currentDataJob?.cancel()
        
        currentDataJob = coroutineScope.launch {
            onSearchingStateChange(true)
            onStateChange(BrowseUiState.Loading)
            
            try {
                val startTime = System.currentTimeMillis()
                searchShowsUseCase("grateful dead 1977")
                    .catch { exception ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Initial data error after ${loadTime}ms: ${exception.message}")
                        onStateChange(BrowseUiState.Error(
                            exception.message ?: "Failed to load initial data"
                        ))
                        onSearchingStateChange(false)
                    }
                    .collect { shows ->
                        val loadTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Initial data success after ${loadTime}ms, got ${shows.size} shows")
                        shows.take(3).forEachIndexed { index, show ->
                            Log.d(TAG, "[$index] ${show.showId} - ${show.displayTitle} (${show.date}) - ${show.recordingCount} recordings")
                        }
                        onStateChange(BrowseUiState.Success(shows))
                        onSearchingStateChange(false)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Initial data exception: ${e.message}")
                onStateChange(BrowseUiState.Error(
                    e.message ?: "Failed to load initial data"
                ))
                onSearchingStateChange(false)
            }
        }
    }
    
    /**
     * Cancel any current data loading operation
     */
    fun cancelCurrentDataOperation() {
        Log.d(TAG, "Cancelling current data operation")
        currentDataJob?.cancel()
    }
}