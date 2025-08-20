package com.deadly.feature.player.service

import android.util.Log
import com.deadly.core.data.repository.LibraryRepository
import com.deadly.core.data.api.repository.ShowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerLibraryServiceImpl @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val showRepository: ShowRepository
) : PlayerLibraryService {
    
    companion object {
        private const val TAG = "PlayerLibraryService"
    }
    
    private val _isInLibrary = MutableStateFlow(false)
    override val isInLibrary: StateFlow<Boolean> = _isInLibrary.asStateFlow()
    
    override suspend fun checkLibraryStatus(showId: String) {
        Log.d(TAG, "checkLibraryStatus: Checking library status for show $showId")
        
        try {
            val inLibrary = libraryRepository.isShowInLibrary(showId)
            _isInLibrary.value = inLibrary
            Log.d(TAG, "checkLibraryStatus: Show $showId in library: $inLibrary")
        } catch (e: Exception) {
            Log.e(TAG, "checkLibraryStatus: Error checking library status", e)
            _isInLibrary.value = false
        }
    }
    
    override suspend fun addToLibrary(showId: String) {
        Log.d(TAG, "addToLibrary: Adding show $showId to library")
        
        try {
            // Get the Show object from the repository
            val show = showRepository.getShowById(showId)
            if (show != null) {
                val wasAdded = libraryRepository.addShowToLibrary(show)
                if (wasAdded) {
                    _isInLibrary.value = true
                    Log.d(TAG, "addToLibrary: Successfully added show $showId to library")
                } else {
                    Log.w(TAG, "addToLibrary: Failed to add show $showId to library")
                }
            } else {
                Log.w(TAG, "addToLibrary: Show $showId not found in repository")
                throw IllegalArgumentException("Show $showId not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "addToLibrary: Error adding show to library", e)
            throw e
        }
    }
    
    override suspend fun removeFromLibrary(showId: String) {
        Log.d(TAG, "removeFromLibrary: Removing show $showId from library")
        
        try {
            libraryRepository.removeShowFromLibrary(showId)
            _isInLibrary.value = false
            Log.d(TAG, "removeFromLibrary: Successfully removed show from library")
        } catch (e: Exception) {
            Log.e(TAG, "removeFromLibrary: Error removing show from library", e)
            throw e
        }
    }
}