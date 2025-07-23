package com.deadarchive.feature.player.service

import kotlinx.coroutines.flow.StateFlow

/**
 * Service responsible for library integration and status tracking.
 * Handles checking library status and managing library state for the player.
 */
interface PlayerLibraryService {
    
    /**
     * Current library status for the active show
     */
    val isInLibrary: StateFlow<Boolean>
    
    /**
     * Check if a show is in the user's library
     * @param showId The show ID to check
     */
    suspend fun checkLibraryStatus(showId: String)
    
    /**
     * Add current show to library
     * @param showId The show ID to add
     */
    suspend fun addToLibrary(showId: String)
    
    /**
     * Remove current show from library
     * @param showId The show ID to remove
     */
    suspend fun removeFromLibrary(showId: String)
}