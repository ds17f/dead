package com.deadly.core.library.api

import com.deadly.core.model.LibraryV2Show
import kotlinx.coroutines.flow.Flow

/**
 * Clean API interface for Library V2 operations.
 * Defines the contract that both stub and real implementations must follow.
 * 
 * This interface provides the foundation for the stub-first development approach,
 * allowing UI development to proceed with minimal logging stubs before real implementation.
 */
interface LibraryV2Service {
    
    /**
     * Get all shows in the user's library as a reactive Flow with library metadata
     */
    fun getLibraryV2Shows(): Flow<List<LibraryV2Show>>
    
    /**
     * Add a show to the user's library
     * @param showId The unique identifier for the show
     * @return Result indicating success or failure with error details
     */
    suspend fun addShowToLibrary(showId: String): Result<Unit>
    
    /**
     * Remove a show from the user's library
     * @param showId The unique identifier for the show
     * @return Result indicating success or failure with error details
     */
    suspend fun removeShowFromLibrary(showId: String): Result<Unit>
    
    /**
     * Clear all shows from the user's library
     * @return Result indicating success or failure with error details
     */
    suspend fun clearLibrary(): Result<Unit>
    
    /**
     * Check if a show is in the user's library (reactive)
     * @param showId The unique identifier for the show
     * @return Flow of boolean indicating library membership
     */
    fun isShowInLibrary(showId: String): Flow<Boolean>
    
    /**
     * Pin a show for quick access
     * @param showId The unique identifier for the show to pin
     * @return Result indicating success or failure with error details
     */
    suspend fun pinShow(showId: String): Result<Unit>
    
    /**
     * Unpin a previously pinned show
     * @param showId The unique identifier for the show to unpin
     * @return Result indicating success or failure with error details
     */
    suspend fun unpinShow(showId: String): Result<Unit>
    
    /**
     * Check if a show is pinned (reactive)
     * @param showId The unique identifier for the show
     * @return Flow of boolean indicating if the show is pinned
     */
    fun isShowPinned(showId: String): Flow<Boolean>
    
    /**
     * Get library statistics and metadata
     * @return LibraryStats containing counts and storage information
     */
    suspend fun getLibraryStats(): LibraryStats
    
    /**
     * Populate library with test data for UI development.
     * Only implemented in stub - no-op in real implementations.
     * @return Result indicating success or failure
     */
    suspend fun populateTestData(): Result<Unit> {
        // Default no-op implementation for real services
        return Result.success(Unit)
    }
}

/**
 * Data class containing library statistics
 */
data class LibraryStats(
    val totalShows: Int,
    val totalDownloaded: Int,
    val totalStorageUsed: Long,
    val totalPinned: Int = 0
)