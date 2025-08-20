package com.deadly.feature.browse.service

import android.util.Log
import com.deadly.core.data.repository.LibraryRepository
import com.deadly.core.model.Show
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for library operations in the Browse feature.
 * Handles adding/removing shows from library. With reactive search results,
 * UI updates automatically when library status changes.
 */
@Singleton
class BrowseLibraryService @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    
    companion object {
        private const val TAG = "BrowseLibraryService"
    }
    
    /**
     * Toggle show's library status. 
     * UI will update automatically via reactive search results.
     */
    suspend fun toggleLibrary(show: Show): Boolean {
        return try {
            Log.d(TAG, "Toggling library for show ${show.showId} - current status: ${show.isInLibrary}")
            
            // Add/remove the show to/from library
            val isInLibrary = libraryRepository.toggleShowLibrary(show)
            
            Log.d(TAG, "Library toggle completed - new status: $isInLibrary")
            isInLibrary
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle library for show ${show.showId}: ${e.message}")
            throw e
        }
    }
}