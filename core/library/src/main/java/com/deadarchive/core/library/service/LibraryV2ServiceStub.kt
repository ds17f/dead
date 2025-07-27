package com.deadarchive.core.library.service

import android.util.Log
import com.deadarchive.core.library.api.LibraryV2Service
import com.deadarchive.core.library.api.LibraryStats
import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal logging-only stub implementation of LibraryV2Service.
 * 
 * This stub provides the simplest possible implementation that:
 * - Logs all method calls clearly
 * - Returns safe default values
 * - Establishes the architecture and integration patterns
 * 
 * This is the foundation for the stub-first development approach, allowing UI development
 * to proceed immediately while the architecture and integration patterns are validated.
 */
@Singleton
class LibraryV2ServiceStub @Inject constructor() : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceStub"
    }
    
    override fun getLibraryShows(): Flow<List<Show>> {
        Log.d(TAG, "STUB: getLibraryShows() called")
        // Return empty list, just log the call
        return flowOf(emptyList())
    }
    
    override suspend fun addShowToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: addShowToLibrary(showId='$showId') called")
        // Just log and return success
        return Result.success(Unit)
    }
    
    override suspend fun removeShowFromLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: removeShowFromLibrary(showId='$showId') called")
        // Just log and return success
        return Result.success(Unit)
    }
    
    override suspend fun clearLibrary(): Result<Unit> {
        Log.d(TAG, "STUB: clearLibrary() called")
        // Just log and return success
        return Result.success(Unit)
    }
    
    override fun isShowInLibrary(showId: String): Flow<Boolean> {
        Log.d(TAG, "STUB: isShowInLibrary(showId='$showId') called")
        // Always return false, just log
        return flowOf(false)
    }
    
    override suspend fun getLibraryStats(): LibraryStats {
        Log.d(TAG, "STUB: getLibraryStats() called")
        // Return empty stats, just log
        return LibraryStats(
            totalShows = 0,
            totalDownloaded = 0,
            totalStorageUsed = 0L
        )
    }
}