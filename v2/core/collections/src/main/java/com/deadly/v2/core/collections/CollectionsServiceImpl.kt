package com.deadly.v2.core.collections

import android.util.Log
import com.deadly.v2.core.api.collections.Collection
import com.deadly.v2.core.api.collections.CollectionCategory
import com.deadly.v2.core.api.collections.CollectionDetails
import com.deadly.v2.core.api.collections.CollectionsService
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.Show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CollectionsServiceImpl - Production implementation of CollectionsService
 * 
 * Provides curated Grateful Dead show collections with real data integration.
 * Collections are defined statically but can reference real shows from the database.
 */
@Singleton
class CollectionsServiceImpl @Inject constructor(
    private val showRepository: ShowRepository
) : CollectionsService {
    
    companion object {
        private const val TAG = "CollectionsServiceImpl"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _featuredCollections = MutableStateFlow<List<Collection>>(emptyList())
    override val featuredCollections: StateFlow<List<Collection>> = _featuredCollections.asStateFlow()
    
    // Static collection definitions
    private val allCollections = createAllCollections()
    
    init {
        Log.d(TAG, "CollectionsServiceImpl initialized with ${allCollections.size} collections")
        loadFeaturedCollections()
    }
    
    private fun loadFeaturedCollections() {
        serviceScope.launch {
            try {
                // Get featured collections (highest priority, limit to 6 for home screen)
                val featured = allCollections
                    .sortedByDescending { it.priority }
                    .take(6)
                
                _featuredCollections.value = featured
                Log.d(TAG, "Loaded ${featured.size} featured collections")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load featured collections", e)
            }
        }
    }
    
    override suspend fun getAllCollections(): Result<List<Collection>> {
        return try {
            Result.success(allCollections.sortedBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all collections", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCollectionShows(collectionId: String): Result<List<Show>> {
        return try {
            val collection = allCollections.find { it.id == collectionId }
                ?: return Result.failure(IllegalArgumentException("Collection not found: $collectionId"))
            
            // For now, return sample shows - in real implementation would query database
            // based on collection criteria (dates, venues, etc.)
            val shows = when (collectionId) {
                "dicks-picks" -> getDicksPicksShows()
                "europe-72" -> getEurope72Shows()
                "greatest-shows" -> getGreatestShows()
                "wall-of-sound" -> getWallOfSoundShows()
                "rare-recordings" -> getRareRecordingsShows()
                "acoustic-sets" -> getAcousticSetsShows()
                else -> emptyList()
            }
            
            Result.success(shows)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get shows for collection $collectionId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCollectionDetails(collectionId: String): Result<CollectionDetails> {
        return try {
            val collection = allCollections.find { it.id == collectionId }
                ?: return Result.failure(IllegalArgumentException("Collection not found: $collectionId"))
            
            val shows = getCollectionShows(collectionId).getOrElse { emptyList() }
            
            val details = when (collectionId) {
                "dicks-picks" -> CollectionDetails(
                    collection = collection,
                    shows = shows,
                    curator = "Dick Latvala",
                    releaseInfo = "Released 1993-2005, 36 volumes",
                    description = "Dick Latvala's archival series featuring the best soundboard recordings from the Grateful Dead vault. Each volume represents Dick's personal selection of outstanding performances.",
                    tags = listOf("soundboard", "official", "archival", "dick-latvala")
                )
                "europe-72" -> CollectionDetails(
                    collection = collection,
                    shows = shows,
                    curator = "Grateful Dead",
                    releaseInfo = "Tour: April-May 1972",
                    description = "The legendary European tour that revitalized the band and produced countless classics. Features the final performances with Pigpen.",
                    tags = listOf("tour", "1972", "europe", "pigpen", "classic")
                )
                else -> CollectionDetails(
                    collection = collection,
                    shows = shows
                )
            }
            
            Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get collection details for $collectionId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchCollections(query: String): Result<List<Collection>> {
        return try {
            val results = allCollections.filter { collection ->
                collection.name.contains(query, ignoreCase = true) ||
                collection.description.contains(query, ignoreCase = true)
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search collections for '$query'", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create all available collections with comprehensive metadata
     */
    private fun createAllCollections(): List<Collection> {
        return listOf(
            Collection(
                id = "dicks-picks",
                name = "Dick's Picks",
                description = "Dick Latvala's archival series featuring the best soundboard recordings",
                showCount = 36,
                category = CollectionCategory.OFFICIAL_RELEASE,
                priority = 10
            ),
            Collection(
                id = "europe-72",
                name = "Europe '72",
                description = "The legendary European tour that produced countless classics",
                showCount = 22,
                category = CollectionCategory.TOUR,
                priority = 9
            ),
            Collection(
                id = "greatest-shows",
                name = "Greatest Shows",
                description = "The most celebrated concerts in Grateful Dead history",
                showCount = 50,
                category = CollectionCategory.QUALITY,
                priority = 8
            ),
            Collection(
                id = "wall-of-sound",
                name = "Wall of Sound",
                description = "Shows featuring the massive Wall of Sound PA system (1974)",
                showCount = 40,
                category = CollectionCategory.ERA,
                priority = 7
            ),
            Collection(
                id = "rare-recordings",
                name = "Rare Recordings",
                description = "Hard-to-find and limited circulation recordings",
                showCount = 125,
                category = CollectionCategory.RARITY,
                priority = 6
            ),
            Collection(
                id = "acoustic-sets",
                name = "Acoustic Sets", 
                description = "Intimate acoustic performances and rare unplugged moments",
                showCount = 18,
                category = CollectionCategory.THEME,
                priority = 5
            ),
            Collection(
                id = "fillmore-west",
                name = "Fillmore West",
                description = "Classic performances at Bill Graham's legendary venue",
                showCount = 28,
                category = CollectionCategory.VENUE,
                priority = 4
            ),
            Collection(
                id = "egypt-78",
                name = "Egypt '78",
                description = "The mystical concerts at the Great Pyramid of Giza",
                showCount = 3,
                category = CollectionCategory.TOUR,
                priority = 3
            ),
            Collection(
                id = "long-jams",
                name = "Epic Jams",
                description = "Extended improvisational journeys and marathon songs",
                showCount = 45,
                category = CollectionCategory.THEME,
                priority = 2
            ),
            Collection(
                id = "new-years",
                name = "New Year's Shows",
                description = "Celebration concerts welcoming each new year",
                showCount = 15,
                category = CollectionCategory.THEME,
                priority = 1
            )
        )
    }
    
    // Sample show data for collections - in real implementation would query database
    
    private suspend fun getDicksPicksShows(): List<Show> {
        // Would query database for Dick's Picks shows
        // For now, get some shows from common Dick's Picks dates
        return listOf("1973-06-10", "1977-05-08", "1972-05-11")
            .mapNotNull { date -> 
                try {
                    showRepository.getShowsByYearMonth(date.substring(0, 7))
                        .find { it.date == date }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not find Dick's Picks show for $date", e)
                    null
                }
            }
    }
    
    private suspend fun getEurope72Shows(): List<Show> {
        // Shows from April-May 1972 Europe tour
        return try {
            val april72 = showRepository.getShowsByYearMonth("1972-04")
            val may72 = showRepository.getShowsByYearMonth("1972-05")
            (april72 + may72).sortedBy { it.date }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load Europe '72 shows", e)
            emptyList()
        }
    }
    
    private suspend fun getGreatestShows(): List<Show> {
        // Top-rated shows 
        return try {
            showRepository.getTopRatedShows(25)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load greatest shows", e)
            emptyList()
        }
    }
    
    private suspend fun getWallOfSoundShows(): List<Show> {
        // 1974 shows when Wall of Sound was active
        return try {
            showRepository.getShowsByYear(1974)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load Wall of Sound shows", e)
            emptyList()
        }
    }
    
    private suspend fun getRareRecordingsShows(): List<Show> {
        // Shows with fewer recordings (rare/limited circulation)
        return try {
            showRepository.getAllShows()
                .filter { it.recordingCount <= 2 }
                .sortedByDescending { it.averageRating ?: 0f }
                .take(25)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load rare recordings", e)
            emptyList()
        }
    }
    
    private suspend fun getAcousticSetsShows(): List<Show> {
        // Shows with acoustic sets - would need setlist data to identify properly
        // For now return a small curated list
        return listOf("1970-09-20", "1980-10-31", "1985-03-28")
            .mapNotNull { date -> 
                try {
                    showRepository.getShowsByYearMonth(date.substring(0, 7))
                        .find { it.date == date }
                } catch (e: Exception) {
                    null
                }
            }
    }
}