package com.deadarchive.core.library.service

import android.util.Log
import com.deadarchive.core.library.api.LibraryV2Service
import com.deadarchive.core.library.api.LibraryStats
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.ConcertSet
import com.deadarchive.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced stub implementation of LibraryV2Service with realistic test data.
 * 
 * This stub provides:
 * - In-memory library management with realistic Show objects
 * - Test data spanning multiple decades for hierarchical filtering
 * - Proper addedToLibraryAt timestamps for sorting
 * - Sample recordings and concert data for UI testing
 * 
 * Enables immediate UI development with realistic data while validating
 * the architecture and integration patterns.
 */
@Singleton
class LibraryV2ServiceStub @Inject constructor() : LibraryV2Service {
    
    companion object {
        private const val TAG = "LibraryV2ServiceStub"
    }
    
    // In-memory library state with reactive updates
    private val _libraryShows = MutableStateFlow<List<Show>>(emptyList())
    
    // In-memory pinned shows (just store IDs for simplicity)
    private val _pinnedShowIds = MutableStateFlow<Set<String>>(emptySet())
    
    override fun getLibraryShows(): Flow<List<Show>> {
        Log.d(TAG, "STUB: getLibraryShows() called - returning ${_libraryShows.value.size} shows")
        return _libraryShows
    }
    
    override suspend fun addShowToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: addShowToLibrary(showId='$showId') called")
        
        // Find show in available shows and add to library
        val availableShow = createAvailableShows().find { it.showId == showId }
        if (availableShow != null) {
            val libraryShow = availableShow.copy(
                addedToLibraryAt = System.currentTimeMillis()
            )
            
            val currentShows = _libraryShows.value.toMutableList()
            if (!currentShows.any { it.showId == showId }) {
                currentShows.add(libraryShow)
                _libraryShows.value = currentShows
                Log.d(TAG, "STUB: Added show '$showId' to library")
            } else {
                Log.d(TAG, "STUB: Show '$showId' already in library")
            }
        } else {
            Log.w(TAG, "STUB: Show '$showId' not found in available shows")
            return Result.failure(Exception("Show not found"))
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun removeShowFromLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: removeShowFromLibrary(showId='$showId') called")
        
        val currentShows = _libraryShows.value.toMutableList()
        val removed = currentShows.removeAll { it.showId == showId }
        
        if (removed) {
            _libraryShows.value = currentShows
            Log.d(TAG, "STUB: Removed show '$showId' from library")
        } else {
            Log.d(TAG, "STUB: Show '$showId' not found in library")
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun clearLibrary(): Result<Unit> {
        Log.d(TAG, "STUB: clearLibrary() called")
        _libraryShows.value = emptyList()
        return Result.success(Unit)
    }
    
    override fun isShowInLibrary(showId: String): Flow<Boolean> {
        Log.d(TAG, "STUB: isShowInLibrary(showId='$showId') called")
        return _libraryShows.map { shows ->
            shows.any { it.showId == showId }
        }
    }
    
    override suspend fun pinShow(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: pinShow(showId='$showId') called")
        
        // Check if the show exists in the library
        if (!_libraryShows.value.any { it.showId == showId }) {
            Log.w(TAG, "STUB: Cannot pin show '$showId', not in library")
            return Result.failure(Exception("Show not in library"))
        }
        
        // Add to pinned set
        val currentPinned = _pinnedShowIds.value.toMutableSet()
        currentPinned.add(showId)
        _pinnedShowIds.value = currentPinned
        
        Log.d(TAG, "STUB: Show '$showId' pinned successfully")
        return Result.success(Unit)
    }
    
    override suspend fun unpinShow(showId: String): Result<Unit> {
        Log.d(TAG, "STUB: unpinShow(showId='$showId') called")
        
        // Remove from pinned set
        val currentPinned = _pinnedShowIds.value.toMutableSet()
        val removed = currentPinned.remove(showId)
        
        if (removed) {
            _pinnedShowIds.value = currentPinned
            Log.d(TAG, "STUB: Show '$showId' unpinned successfully")
        } else {
            Log.d(TAG, "STUB: Show '$showId' was not pinned")
        }
        
        return Result.success(Unit)
    }
    
    override fun isShowPinned(showId: String): Flow<Boolean> {
        Log.d(TAG, "STUB: isShowPinned(showId='$showId') called")
        return _pinnedShowIds.map { pinnedIds ->
            pinnedIds.contains(showId)
        }
    }
    
    override suspend fun getLibraryStats(): LibraryStats {
        Log.d(TAG, "STUB: getLibraryStats() called")
        val showCount = _libraryShows.value.size
        val pinnedCount = _pinnedShowIds.value.size
        
        return LibraryStats(
            totalShows = showCount,
            totalDownloaded = showCount / 2, // Simulate half downloaded
            totalStorageUsed = showCount * 250L * 1024 * 1024, // ~250MB per show
            totalPinned = pinnedCount
        )
    }
    
    /**
     * Populate library with realistic test data for UI development.
     * Call this from UI to immediately see library functionality.
     */
    override suspend fun populateTestData(): Result<Unit> {
        Log.d(TAG, "STUB: populateTestData() called")
        _libraryShows.value = createTestLibraryShows()
        return Result.success(Unit)
    }
    
    /**
     * Create realistic test library shows spanning multiple decades
     */
    private fun createTestLibraryShows(): List<Show> {
        
        return listOf(
            // 1970s - Classic early shows
            createShow(
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                location = "Ithaca, NY",
                year = "1977",
                addedDaysAgo = 30,
                rating = 4.8f,
                setlist = "Set I: Promised Land > Bertha > Good Lovin' > Loser > El Paso > They Love Each Other > Jack Straw > Deal"
            ),
            
            createShow(
                date = "1972-05-04",
                venue = "Olympia Theatre", 
                location = "Paris, France",
                year = "1972",
                addedDaysAgo = 7,
                rating = 4.2f,
                setlist = "Set I: Truckin' > Drums > The Other One > Me and My Uncle > Next Time You See Me"
            ),
            
            createShow(
                date = "1976-06-09",
                venue = "Boston Music Hall",
                location = "Boston, MA", 
                year = "1976",
                addedDaysAgo = 45,
                rating = 4.1f,
                setlist = "Set I: Help On The Way > Slipknot! > Franklin's Tower > Mama Tried > Row Jimmy"
            ),
            
            // 1980s - Brent era
            createShow(
                date = "1981-05-16",
                venue = "Cornell University",
                location = "Ithaca, NY",
                year = "1981", 
                addedDaysAgo = 14,
                rating = 3.9f,
                setlist = "Set I: Alabama Getaway > Promised Land > Sugaree > El Paso > Tennessee Jed"
            ),
            
            createShow(
                date = "1989-07-04",
                venue = "Rich Stadium",
                location = "Orchard Park, NY",
                year = "1989",
                addedDaysAgo = 3,
                rating = 4.3f,
                setlist = "Set I: Hell in a Bucket > Sugaree > Row Jimmy > Estimated Prophet > Eyes of the World"
            ),
            
            // 1990s - Later period
            createShow(
                date = "1994-06-25",
                venue = "Sam Boyd Silver Bowl",
                location = "Las Vegas, NV", 
                year = "1994",
                addedDaysAgo = 60,
                rating = 3.7f,
                setlist = "Set I: Shakedown Street > Little Red Rooster > Dire Wolf > Row Jimmy > Deal"
            ),
            
            createShow(
                date = "1990-09-20",
                venue = "Madison Square Garden",
                location = "New York, NY",
                year = "1990",
                addedDaysAgo = 21,
                rating = 4.0f,
                setlist = "Set I: Feel Like a Stranger > Franklin's Tower > Candyman > Me and My Uncle"
            ),
            
            // 1960s - Early psychedelic era
            createShow(
                date = "1969-02-27",
                venue = "Fillmore East",
                location = "New York, NY",
                year = "1969",
                addedDaysAgo = 90,
                rating = 4.4f,
                setlist = "Set I: Good Morning Little Schoolgirl > Doin' That Rag > I'm a King Bee > Turn On Your Love Light"
            ),
            
            // Additional seasonal test shows for better filtering coverage
            createShow(
                date = "1977-04-22", // Spring show
                venue = "The Spectrum",
                location = "Philadelphia, PA",
                year = "1977",
                addedDaysAgo = 12,
                rating = 4.5f,
                setlist = "Set I: Promised Land > Deal > Cassidy > Candyman > Music Never Stopped"
            ),
            
            createShow(
                date = "1989-07-15", // Summer show
                venue = "Soldier Field",
                location = "Chicago, IL",
                year = "1989",
                addedDaysAgo = 40,
                rating = 4.2f,
                setlist = "Set I: Touch of Grey > Hell in a Bucket > West L.A. Fadeaway > Estimated Prophet"
            ),
            
            createShow(
                date = "1992-12-31", // Winter show (New Year's Eve)
                venue = "Oakland-Alameda County Coliseum",
                location = "Oakland, CA",
                year = "1992",
                addedDaysAgo = 85,
                rating = 4.6f,
                setlist = "Set I: Jack Straw > Sugaree > Row Jimmy > Promised Land > Deal"
            ),
            
            createShow(
                date = "1985-10-31", // Fall show (Halloween)
                venue = "Berkeley Community Theatre",
                location = "Berkeley, CA", 
                year = "1985",
                addedDaysAgo = 55,
                rating = 4.1f,
                setlist = "Set I: Dancing in the Street > Franklin's Tower > Estimated Prophet > Eyes of the World"
            )
        )
    }
    
    /**
     * Create available shows for adding to library (simulate browse results)
     */
    private fun createAvailableShows(): List<Show> {
        return createTestLibraryShows().map { it.copy(addedToLibraryAt = null) } + 
            listOf(
                createShow(
                    date = "1978-05-08",
                    venue = "Field House, Rensselaer Polytechnic Institute",
                    location = "Troy, NY",
                    year = "1978",
                    addedDaysAgo = null,
                    rating = 4.1f
                ),
                createShow(
                    date = "1983-09-02",
                    venue = "Boise State University Pavilion",
                    location = "Boise, ID",
                    year = "1983", 
                    addedDaysAgo = null,
                    rating = 3.8f
                )
            )
    }
    
    /**
     * Helper to create realistic Show objects
     */
    private fun createShow(
        date: String,
        venue: String,
        location: String,
        year: String,
        addedDaysAgo: Int?,
        rating: Float,
        setlist: String = ""
    ): Show {
        val addedToLibraryAt = addedDaysAgo?.let { 
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong())
        }
        
        // Create sample recordings
        val recordings = listOf(
            Recording(
                identifier = "${date.replace("-", "")}.sbd.${venue.take(3).lowercase()}",
                title = "$date $venue SBD",
                source = "SBD",
                taper = "Unknown",
                concertDate = date,
                concertVenue = venue,
                concertLocation = location,
                rawRating = rating,
                reviewCount = (10..50).random(),
                tracks = createSampleTracks()
            )
        )
        
        // Create sample sets from setlist
        val sets = if (setlist.isNotEmpty()) {
            listOf(
                ConcertSet(
                    setNumber = 1,
                    setName = "Set I",
                    songs = setlist.removePrefix("Set I: ").split(" > "),
                    totalDuration = 3600L // 1 hour
                )
            )
        } else emptyList()
        
        return Show(
            date = date,
            venue = venue,
            location = location,
            year = year,
            recordings = recordings,
            sets = sets,
            setlistRaw = setlist,
            addedToLibraryAt = addedToLibraryAt,
            rating = rating,
            rawRating = rating
        )
    }
    
    /**
     * Create sample tracks for recordings
     */
    private fun createSampleTracks(): List<Track> {
        return listOf(
            Track(
                filename = "t01.mp3",
                title = "Good Lovin'",
                trackNumber = "1",
                durationSeconds = "360"
            ),
            Track(
                filename = "t02.mp3",
                title = "Deal",
                trackNumber = "2",
                durationSeconds = "280"
            )
        )
    }
}