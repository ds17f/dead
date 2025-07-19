package com.deadarchive.core.data.api.repository

import com.deadarchive.core.model.Show
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for library operations.
 * Handles user library management, including adding/removing shows and library queries.
 */
interface LibraryRepository {
    /**
     * Get all shows in the user's library as a Flow for reactive UI updates
     */
    fun getLibraryShows(): Flow<List<Show>>

    /**
     * Add a show to the user's library
     * @param showId The unique identifier for the show
     * @return True if the operation was successful
     */
    suspend fun addShowToLibrary(showId: String): Boolean

    /**
     * Remove a show from the user's library
     * @param showId The unique identifier for the show
     * @return True if the operation was successful
     */
    suspend fun removeShowFromLibrary(showId: String): Boolean

    /**
     * Check if a show is in the user's library
     * @param showId The unique identifier for the show
     * @return True if the show is in the library
     */
    suspend fun isShowInLibrary(showId: String): Boolean

    /**
     * Get the count of shows in the user's library
     * @return Number of shows in library
     */
    suspend fun getLibraryShowCount(): Int
}