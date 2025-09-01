package com.deadly.v2.core.api.collections

import com.deadly.v2.core.model.Show
import kotlinx.coroutines.flow.StateFlow

/**
 * CollectionsService - Service interface for curated show collections
 * 
 * Manages curated collections of Grateful Dead shows including:
 * - Dick's Picks series (36 official releases)
 * - Europe '72 tour shows  
 * - Greatest Shows compilations
 * - Wall of Sound era (1974)
 * - Rare and limited circulation recordings
 * - Acoustic sets and unplugged performances
 * 
 * Collections provide organized discovery paths for users beyond
 * simple date/venue searches.
 */
interface CollectionsService {
    
    /**
     * Reactive featured collections for home screen
     * Provides curated subset of most popular/interesting collections
     */
    val featuredCollections: StateFlow<List<Collection>>
    
    /**
     * Get all available collections
     * @return Result with complete list of collections or error
     */
    suspend fun getAllCollections(): Result<List<Collection>>
    
    /**
     * Get shows for a specific collection
     * @param collectionId Unique identifier for the collection
     * @return Result with shows in the collection or error
     */
    suspend fun getCollectionShows(collectionId: String): Result<List<Show>>
    
    /**
     * Get detailed information about a collection
     * @param collectionId Unique identifier for the collection  
     * @return Result with collection details or error
     */
    suspend fun getCollectionDetails(collectionId: String): Result<CollectionDetails>
    
    /**
     * Search collections by name or description
     * @param query Search term
     * @return Result with matching collections or error
     */
    suspend fun searchCollections(query: String): Result<List<Collection>>
}

/**
 * Collection data model for curated show groupings
 */
data class Collection(
    val id: String,                    // "dicks-picks", "europe-72"
    val name: String,                  // "Dick's Picks"  
    val description: String,           // "Dick Latvala's archival series..."
    val showCount: Int,                // 36
    val category: CollectionCategory,  // OFFICIAL_RELEASE
    val imageUrl: String? = null,      // Collection artwork
    val priority: Int = 0              // For ordering featured collections
)

/**
 * Detailed collection information with metadata
 */
data class CollectionDetails(
    val collection: Collection,
    val shows: List<Show>,
    val curator: String? = null,       // "Dick Latvala"
    val releaseInfo: String? = null,   // "Released 1993-2005"
    val description: String? = null,   // Extended description
    val tags: List<String> = emptyList() // ["soundboard", "official", "archival"]
)

/**
 * Collection categories for organization
 */
enum class CollectionCategory {
    OFFICIAL_RELEASE,    // Dick's Picks, Europe '72 album
    TOUR,                // Europe '72 tour, Egypt '78
    ERA,                 // Wall of Sound, Acoustic Era
    VENUE,               // Fillmore series, Winterland
    QUALITY,             // Greatest Shows, Best Soundboards  
    RARITY,              // Rare recordings, Limited circulation
    THEME                // Acoustic sets, Long jams
}