package com.deadly.v2.core.api.home

import com.deadly.v2.core.model.Show
import kotlinx.coroutines.flow.StateFlow

/**
 * HomeService - Service interface for home screen content
 * 
 * Orchestrates data from multiple sources to provide unified home experience:
 * - Recent shows from user activity
 * - Today in Grateful Dead History from date-based queries  
 * - Featured collections from curated data
 * 
 * Follows V2 architecture patterns with StateFlow for reactive UI updates.
 */
interface HomeService {
    
    /**
     * Reactive home content state for UI consumption
     * Combines data from recent shows, history, and collections services
     */
    val homeContent: StateFlow<HomeContent>
    
    /**
     * Refresh all home content from underlying services
     * @return Result indicating success or failure with error details
     */
    suspend fun refreshAll(): Result<Unit>
}

/**
 * Comprehensive home content data model
 * 
 * Contains all data needed for home screen UI components
 */
data class HomeContent(
    val recentShows: List<Show>,
    val todayInHistory: List<Show>,
    val featuredCollections: List<Collection>,
    val lastRefresh: Long
) {
    companion object {
        fun initial() = HomeContent(
            recentShows = emptyList(),
            todayInHistory = emptyList(),
            featuredCollections = emptyList(),
            lastRefresh = 0L
        )
    }
}

/**
 * Collection data model for curated show groupings
 * 
 * Represents curated collections like Dick's Picks, Europe '72, etc.
 */
data class Collection(
    val id: String,
    val name: String,
    val description: String,
    val showCount: Int,
    val imageUrl: String? = null
)