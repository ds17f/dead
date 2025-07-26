package com.deadarchive.core.data.service

import android.util.Log
import kotlinx.coroutines.flow.first
import com.deadarchive.core.data.repository.LibraryRepository
import com.deadarchive.core.data.download.DownloadService
import com.deadarchive.core.model.Show
import com.deadarchive.core.design.component.ShowDownloadState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified service for all library operations across the application.
 * Consolidates duplicate library logic from feature-specific services
 * and provides download integration for smart library management.
 * 
 * This service replaces:
 * - BrowseLibraryService
 * - PlayerLibraryService (partial)
 * - LibraryManagementService (enhanced)
 * - Direct LibraryRepository calls in ViewModels
 */
@Singleton
class LibraryService @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadService: DownloadService
) {
    
    companion object {
        private const val TAG = "LibraryService"
    }
    
    /**
     * Toggle show's library status. 
     * Automatically adds shows to library when downloads start (handled by DownloadRepository).
     * UI will update automatically via reactive flows.
     */
    suspend fun toggleLibrary(show: Show): Boolean {
        return try {
            Log.d(TAG, "Toggling library for show ${show.showId} - current status: ${show.isInLibrary}")
            
            val isInLibrary = libraryRepository.toggleShowLibrary(show)
            
            Log.d(TAG, "Library toggle completed - new status: $isInLibrary")
            isInLibrary
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle library for show ${show.showId}: ${e.message}")
            throw e
        }
    }
    
    /**
     * Add show to library explicitly.
     * Used by player and other features that need explicit add operations.
     */
    suspend fun addToLibrary(show: Show): Boolean {
        return try {
            Log.d(TAG, "Adding show ${show.showId} to library")
            
            val wasAdded = libraryRepository.addShowToLibrary(show)
            if (wasAdded) {
                Log.d(TAG, "Successfully added show ${show.showId} to library")
            } else {
                Log.d(TAG, "Show ${show.showId} was already in library")
            }
            
            wasAdded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add show ${show.showId} to library: ${e.message}")
            throw e
        }
    }
    
    /**
     * Remove show from library by showId.
     * Used by player and other features that work with showId strings.
     */
    suspend fun removeFromLibrary(showId: String) {
        try {
            Log.d(TAG, "Removing show $showId from library")
            libraryRepository.removeShowFromLibrary(showId)
            Log.d(TAG, "Successfully removed show $showId from library")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove show $showId from library: ${e.message}")
            throw e
        }
    }
    
    /**
     * Remove show from library with Show object.
     * Convenience method for features that work with Show objects.
     */
    suspend fun removeFromLibrary(show: Show) {
        removeFromLibrary(show.showId)
    }
    
    /**
     * Check if a show is currently in the user's library.
     */
    suspend fun isShowInLibrary(showId: String): Boolean {
        return try {
            libraryRepository.isShowInLibrary(showId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check library status for show $showId: ${e.message}")
            false
        }
    }
    
    /**
     * Get reactive library status for a show.
     * Returns a Flow that emits true/false whenever the show's library status changes.
     * This enables UI components to automatically update when library changes occur.
     */
    fun isShowInLibraryFlow(showId: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return try {
            libraryRepository.isShowInLibraryFlow(showId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create library status flow for show $showId: ${e.message}")
            kotlinx.coroutines.flow.flowOf(false)
        }
    }
    
    /**
     * Check if a show has any downloads (completed, in-progress, or paused).
     * Used for smart library removal with download cleanup.
     */
    suspend fun hasDownloadsForShow(show: Show): Boolean {
        return try {
            val downloadState = downloadService.getShowDownloadState(show)
            val hasDownloads = when (downloadState) {
                is ShowDownloadState.NotDownloaded -> false
                else -> true // Any other state means downloads exist
            }
            
            Log.d(TAG, "Show ${show.showId} has downloads: $hasDownloads (state: $downloadState)")
            hasDownloads
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check downloads for show ${show.showId}: ${e.message}")
            false
        }
    }
    
    /**
     * Get download information for a show to display in confirmation dialogs.
     * Returns user-friendly information about download status and size.
     */
    suspend fun getDownloadInfoForShow(show: Show): LibraryRemovalInfo {
        return try {
            val downloadState = downloadService.getShowDownloadState(show)
            val hasDownloads = hasDownloadsForShow(show)
            
            val downloadInfo = when (downloadState) {
                is ShowDownloadState.NotDownloaded -> "No downloads"
                is ShowDownloadState.Downloading -> "Downloading (${downloadState.completedTracks}/${downloadState.totalTracks} tracks)"
                is ShowDownloadState.Paused -> "Paused (${downloadState.completedTracks}/${downloadState.totalTracks} tracks)"
                is ShowDownloadState.Cancelled -> "Cancelled (${downloadState.completedTracks}/${downloadState.totalTracks} tracks)"
                is ShowDownloadState.Downloaded -> "Downloaded (100% complete)"
                is ShowDownloadState.Failed -> "Failed download"
            }
            
            LibraryRemovalInfo(
                hasDownloads = hasDownloads,
                downloadInfo = downloadInfo,
                downloadState = downloadState
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download info for show ${show.showId}: ${e.message}")
            LibraryRemovalInfo(
                hasDownloads = false,
                downloadInfo = "Unknown",
                downloadState = ShowDownloadState.NotDownloaded
            )
        }
    }
    
    /**
     * Remove show from library and optionally clean up downloads.
     * This is the unified method for library removal with download integration.
     */
    suspend fun removeShowWithDownloadCleanup(
        show: Show,
        alsoRemoveDownloads: Boolean = false
    ) {
        try {
            Log.d(TAG, "Removing show ${show.showId} from library (cleanup downloads: $alsoRemoveDownloads)")
            
            // Remove from library first
            removeFromLibrary(show.showId)
            
            // Clean up downloads if requested
            if (alsoRemoveDownloads && hasDownloadsForShow(show)) {
                Log.d(TAG, "Cleaning up downloads for show ${show.showId}")
                downloadService.clearShowDownloads(show)
            }
            
            Log.d(TAG, "Successfully removed show ${show.showId} from library with cleanup")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove show ${show.showId} with cleanup: ${e.message}")
            throw e
        }
    }
    
    /**
     * Clear all items from the library.
     * Used by LibraryManagementService.
     */
    suspend fun clearLibrary() {
        try {
            Log.d(TAG, "Clearing entire library")
            libraryRepository.clearLibrary()
            Log.d(TAG, "Successfully cleared library")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear library: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get library item count for statistics.
     */
    suspend fun getLibraryItemCount(): Int {
        return try {
            libraryRepository.getLibraryItemCount()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get library item count: ${e.message}")
            0
        }
    }
}

/**
 * Information about a show's download status for library removal confirmation.
 */
data class LibraryRemovalInfo(
    val hasDownloads: Boolean,
    val downloadInfo: String,
    val downloadState: ShowDownloadState
)