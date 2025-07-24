package com.deadarchive.feature.library.service

import android.util.Log
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadarchive.core.data.api.repository.ShowRepository
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.LibraryItemType
import com.deadarchive.core.model.Show
import com.deadarchive.feature.library.LibraryUiState
import com.deadarchive.feature.library.LibrarySortOption
import com.deadarchive.feature.library.DecadeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for data loading and filtering operations in the Library feature.
 * Handles library data loading, decade filtering, and sorting with reactive state management.
 */
@Singleton
class LibraryDataService @Inject constructor(
    private val showRepository: ShowRepository
) {
    
    companion object {
        private const val TAG = "LibraryDataService"
    }
    
    // Sort option state
    private val _sortOption = MutableStateFlow(LibrarySortOption.ADDED_DESCENDING)
    val sortOption: StateFlow<LibrarySortOption> = _sortOption.asStateFlow()
    
    // Decade filter state
    private val _decadeFilter = MutableStateFlow(DecadeFilter.ALL)
    val decadeFilter: StateFlow<DecadeFilter> = _decadeFilter.asStateFlow()
    
    /**
     * Load library items with reactive filtering and sorting
     */
    fun loadLibraryItems(
        coroutineScope: CoroutineScope,
        onStateChange: (LibraryUiState) -> Unit
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Starting library data loading")
                onStateChange(LibraryUiState.Loading)
                
                // Use simplified approach - just get library shows directly
                combine(
                    showRepository.getLibraryShows(),
                    _sortOption,
                    _decadeFilter
                ) { libraryShows, sortOption, decadeFilter ->
                    Log.d(TAG, "Found ${libraryShows.size} library shows")
                    
                    // Apply decade filtering first
                    val filteredShows = if (decadeFilter.decade != null) {
                        libraryShows.filter { show ->
                            show.date.startsWith(decadeFilter.decade)
                        }
                    } else {
                        libraryShows
                    }
                    
                    // Apply sorting based on sort option
                    val sortedShows = when (sortOption) {
                        LibrarySortOption.DATE_ASCENDING -> {
                            filteredShows.sortedBy { it.date }
                        }
                        LibrarySortOption.DATE_DESCENDING -> {
                            filteredShows.sortedByDescending { it.date }
                        }
                        LibrarySortOption.ADDED_ASCENDING -> {
                            filteredShows.sortedBy { it.addedToLibraryAt ?: 0L }
                        }
                        LibrarySortOption.ADDED_DESCENDING -> {
                            filteredShows.sortedByDescending { it.addedToLibraryAt ?: 0L }
                        }
                    }
                    
                    Log.d(TAG, "Created ${sortedShows.size} shows for display after filtering by ${decadeFilter.displayName} and sorting by ${sortOption.displayName}")
                    
                    // Convert to legacy format for UI compatibility
                    val libraryItems = sortedShows.map { show ->
                        LibraryItem(
                            id = "show_${show.showId}",
                            showId = show.showId,
                            type = LibraryItemType.SHOW,
                            addedTimestamp = show.addedToLibraryAt ?: 0L
                        )
                    }
                    
                    LibraryUiState.Success(libraryItems, sortedShows)
                }
                .catch { exception ->
                    Log.e(TAG, "Error loading library data: ${exception.message}")
                    onStateChange(LibraryUiState.Error(
                        exception.message ?: "Failed to load library items"
                    ))
                }
                .collect { uiState ->
                    onStateChange(uiState)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading library data: ${e.message}")
                onStateChange(LibraryUiState.Error(
                    e.message ?: "Failed to load library items"
                ))
            }
        }
    }
    
    /**
     * Set the sort option and trigger data reload
     */
    fun setSortOption(option: LibrarySortOption) {
        Log.d(TAG, "Setting sort option to: ${option.displayName}")
        _sortOption.value = option
    }
    
    /**
     * Set the decade filter and trigger data reload
     */
    fun setDecadeFilter(filter: DecadeFilter) {
        Log.d(TAG, "Setting decade filter to: ${filter.displayName}")
        _decadeFilter.value = filter
    }
}