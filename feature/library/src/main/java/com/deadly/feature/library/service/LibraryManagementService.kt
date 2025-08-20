package com.deadly.feature.library.service

import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.deadly.core.data.repository.LibraryRepository
import com.deadly.core.model.LibraryItem
import com.deadly.core.model.LibraryItemType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for library management operations in the Library feature.
 * Handles adding/removing items from library and clearing operations.
 */
@Singleton
class LibraryManagementService @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    
    companion object {
        private const val TAG = "LibraryManagementService"
    }
    
    /**
     * Remove an item from the library
     */
    fun removeFromLibrary(
        libraryItem: LibraryItem,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Removing library item: ${libraryItem.id} (type: ${libraryItem.type})")
                when (libraryItem.type) {
                    LibraryItemType.SHOW -> {
                        libraryRepository.removeShowFromLibrary(libraryItem.showId)
                        Log.d(TAG, "Successfully removed show ${libraryItem.showId} from library")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove library item ${libraryItem.id}: ${e.message}")
                // TODO: Handle error appropriately with callback if needed
            }
        }
    }
    
    /**
     * Remove a show from the library by show ID
     */
    fun removeShowFromLibrary(
        showId: String,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Removing show from library: $showId")
                libraryRepository.removeShowFromLibrary(showId)
                Log.d(TAG, "Successfully removed show $showId from library")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove show $showId from library: ${e.message}")
                // TODO: Handle error appropriately with callback if needed
            }
        }
    }
    
    /**
     * Clear all items from the library
     */
    fun clearLibrary(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Clearing entire library")
                libraryRepository.clearLibrary()
                Log.d(TAG, "Successfully cleared library")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear library: ${e.message}")
                // TODO: Handle error appropriately with callback if needed
            }
        }
    }
}