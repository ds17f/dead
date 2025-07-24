package com.deadarchive.feature.browse.service

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.model.Show
import com.deadarchive.feature.browse.BrowseUiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for library operations in the Browse feature.
 * Handles adding/removing shows from library with local UI state updates.
 */
@Singleton
class BrowseLibraryService @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    
    companion object {
        private const val TAG = "BrowseLibraryService"
    }
    
    /**
     * Toggle show's library status and update UI state locally
     */
    fun toggleLibrary(
        show: Show, 
        coroutineScope: CoroutineScope, 
        onStateChange: (BrowseUiState) -> Unit,
        currentState: BrowseUiState
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Toggling library for show ${show.showId} - current status: ${show.isInLibrary}")
                
                // Add/remove the show to/from library
                val isInLibrary = libraryRepository.toggleShowLibrary(show)
                
                Log.d(TAG, "Library toggle completed - new status: $isInLibrary")
                
                // Update the UI state locally instead of refreshing search
                if (currentState is BrowseUiState.Success) {
                    val updatedShows = currentState.shows.map { existingShow ->
                        if (existingShow.showId == show.showId) {
                            existingShow.copy(isInLibrary = isInLibrary)
                        } else {
                            existingShow
                        }
                    }
                    
                    Log.d(TAG, "Updated ${updatedShows.count { it.isInLibrary }} shows in library")
                    onStateChange(BrowseUiState.Success(updatedShows))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle library for show ${show.showId}: ${e.message}")
                // Could add error handling/snackbar here if needed
            }
        }
    }
}