package com.deadly.v2.core.library.service

import android.util.Log
import com.deadly.v2.core.api.library.LibraryService
import com.deadly.v2.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * V2 LibraryServiceStub - Comprehensive stub implementation for development
 * 
 * Provides realistic test data spanning multiple decades with proper V2 domain models.
 * Enables immediate UI development while following V2 service architecture patterns.
 */
@Singleton
class LibraryServiceStub @Inject constructor(
    @Named("LibraryApplicationScope") private val coroutineScope: CoroutineScope
) : LibraryService {
    
    companion object {
        private const val TAG = "LibraryServiceStub"
    }
    
    private val _libraryShows = MutableStateFlow<List<Show>>(emptyList())
    private val _pinnedShowIds = MutableStateFlow<Set<String>>(emptySet())
    private val _downloadStatuses = MutableStateFlow<Map<String, LibraryDownloadStatus>>(emptyMap())
    
    private val _currentShows = MutableStateFlow<List<LibraryShow>>(emptyList())
    private val _libraryStats = MutableStateFlow(LibraryStats(0, 0, 0, 0))
    
    init {
        Log.d(TAG, "V2 STUB: Initializing library service with test data")
        
        // Auto-populate with test data and set up reactive flow
        coroutineScope.launch {
            // Populate initial test data
            val testShows = createTestLibraryShows()
            _libraryShows.value = testShows
            
            // Set up a few pinned shows
            val pinnedIds = setOfNotNull(
                testShows.firstOrNull()?.id, // Cornell '77
                testShows.getOrNull(3)?.id   // Another show
            )
            _pinnedShowIds.value = pinnedIds
            
            // Set up some download statuses
            val downloadStatuses = testShows.take(2).associate { show ->
                show.id to LibraryDownloadStatus.COMPLETED
            }
            _downloadStatuses.value = downloadStatuses
            
            Log.d(TAG, "V2 STUB: Initial test data populated - ${testShows.size} shows")
            
            // Set up reactive flow composition (non-blocking)
            combine(
                _libraryShows,
                _pinnedShowIds,
                _downloadStatuses
            ) { shows, pinnedIds, downloadStatuses ->
                shows.map { show ->
                    LibraryShow(
                        show = show,
                        addedToLibraryAt = show.libraryAddedAt ?: System.currentTimeMillis(),
                        isPinned = pinnedIds.contains(show.id),
                        downloadStatus = downloadStatuses[show.id] ?: LibraryDownloadStatus.NOT_DOWNLOADED
                    )
                }
            }.collect { libraryShows ->
                _currentShows.value = libraryShows
                _libraryStats.value = LibraryStats(
                    totalShows = libraryShows.size,
                    totalDownloaded = libraryShows.count { it.isDownloaded },
                    totalStorageUsed = libraryShows.size * 250L * 1024 * 1024,
                    totalPinned = libraryShows.count { it.isPinned }
                )
                Log.v(TAG, "V2 STUB: Updated reactive state - ${libraryShows.size} shows")
            }
        }
    }
    
    override suspend fun loadLibraryShows(): Result<Unit> {
        Log.d(TAG, "V2 STUB: loadLibraryShows() called - data already loaded in init")
        return Result.success(Unit)
    }
    
    override fun getCurrentShows(): StateFlow<List<LibraryShow>> {
        Log.d(TAG, "V2 STUB: getCurrentShows() - returning ${_currentShows.value.size} shows")
        return _currentShows.asStateFlow()
    }
    
    override fun getLibraryStats(): StateFlow<LibraryStats> {
        Log.d(TAG, "V2 STUB: getLibraryStats() called")
        return _libraryStats.asStateFlow()
    }
    
    override suspend fun addToLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: addToLibrary('$showId')")
        
        val availableShow = createAvailableShows().find { it.id == showId }
        if (availableShow != null) {
            val libraryShow = availableShow.copy(
                libraryAddedAt = System.currentTimeMillis(),
                isInLibrary = true
            )
            
            val currentShows = _libraryShows.value.toMutableList()
            if (!currentShows.any { it.id == showId }) {
                currentShows.add(libraryShow)
                _libraryShows.value = currentShows
                Log.d(TAG, "V2 STUB: Added show '$showId' to library")
            }
        } else {
            return Result.failure(Exception("Show not found"))
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun removeFromLibrary(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: removeFromLibrary('$showId')")
        
        val currentShows = _libraryShows.value.toMutableList()
        val removed = currentShows.removeAll { it.id == showId }
        
        if (removed) {
            _libraryShows.value = currentShows
            
            val currentPinned = _pinnedShowIds.value.toMutableSet()
            currentPinned.remove(showId)
            _pinnedShowIds.value = currentPinned
            
            val currentDownloads = _downloadStatuses.value.toMutableMap()
            currentDownloads.remove(showId)
            _downloadStatuses.value = currentDownloads
            
            Log.d(TAG, "V2 STUB: Removed show '$showId' from library")
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun clearLibrary(): Result<Unit> {
        Log.d(TAG, "V2 STUB: clearLibrary()")
        _libraryShows.value = emptyList()
        _pinnedShowIds.value = emptySet()
        _downloadStatuses.value = emptyMap()
        return Result.success(Unit)
    }
    
    override fun isShowInLibrary(showId: String): StateFlow<Boolean> {
        Log.d(TAG, "V2 STUB: isShowInLibrary('$showId')")
        return _libraryShows.map { shows ->
            shows.any { it.id == showId }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    }
    
    override suspend fun pinShow(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: pinShow('$showId')")
        
        if (!_libraryShows.value.any { it.id == showId }) {
            return Result.failure(Exception("Show not in library"))
        }
        
        val currentPinned = _pinnedShowIds.value.toMutableSet()
        currentPinned.add(showId)
        _pinnedShowIds.value = currentPinned
        
        return Result.success(Unit)
    }
    
    override suspend fun unpinShow(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: unpinShow('$showId')")
        
        val currentPinned = _pinnedShowIds.value.toMutableSet()
        currentPinned.remove(showId)
        _pinnedShowIds.value = currentPinned
        
        return Result.success(Unit)
    }
    
    override fun isShowPinned(showId: String): StateFlow<Boolean> {
        Log.d(TAG, "V2 STUB: isShowPinned('$showId')")
        return _pinnedShowIds.map { pinnedIds ->
            pinnedIds.contains(showId)
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    }
    
    override suspend fun downloadShow(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: downloadShow('$showId')")
        
        val currentDownloads = _downloadStatuses.value.toMutableMap()
        currentDownloads[showId] = LibraryDownloadStatus.DOWNLOADING
        _downloadStatuses.value = currentDownloads
        
        return Result.success(Unit)
    }
    
    override suspend fun cancelShowDownloads(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: cancelShowDownloads('$showId')")
        
        val currentDownloads = _downloadStatuses.value.toMutableMap()
        currentDownloads[showId] = LibraryDownloadStatus.CANCELLED
        _downloadStatuses.value = currentDownloads
        
        return Result.success(Unit)
    }
    
    override fun getDownloadStatus(showId: String): StateFlow<LibraryDownloadStatus> {
        Log.d(TAG, "V2 STUB: getDownloadStatus('$showId')")
        return _downloadStatuses.map { statuses ->
            statuses[showId] ?: LibraryDownloadStatus.NOT_DOWNLOADED
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryDownloadStatus.NOT_DOWNLOADED
        )
    }
    
    override suspend fun shareShow(showId: String): Result<Unit> {
        Log.d(TAG, "V2 STUB: shareShow('$showId')")
        return Result.success(Unit)
    }
    
    override suspend fun navigateToPreviousShow(currentShowId: String): Result<String?> {
        Log.d(TAG, "V2 STUB: navigateToPreviousShow('$currentShowId')")
        
        val shows = _libraryShows.value.sortedBy { it.date }
        val currentIndex = shows.indexOfFirst { it.id == currentShowId }
        
        return if (currentIndex > 0) {
            Result.success(shows[currentIndex - 1].id)
        } else {
            Result.success(null)
        }
    }
    
    override suspend fun navigateToNextShow(currentShowId: String): Result<String?> {
        Log.d(TAG, "V2 STUB: navigateToNextShow('$currentShowId')")
        
        val shows = _libraryShows.value.sortedBy { it.date }
        val currentIndex = shows.indexOfFirst { it.id == currentShowId }
        
        return if (currentIndex >= 0 && currentIndex < shows.size - 1) {
            Result.success(shows[currentIndex + 1].id)
        } else {
            Result.success(null)
        }
    }
    
    override suspend fun getCurrentShowInfo(showId: String): LibraryShow? {
        Log.d(TAG, "V2 STUB: getCurrentShowInfo('$showId')")
        return _currentShows.value.find { it.showId == showId }
    }
    
    override suspend fun populateTestData(): Result<Unit> {
        Log.d(TAG, "V2 STUB: populateTestData() - creating comprehensive test data")
        
        val testShows = createTestLibraryShows()
        _libraryShows.value = testShows
        
        val pinnedIds = setOfNotNull(
            testShows.firstOrNull()?.id, // Cornell '77
            testShows.getOrNull(3)?.id   // Another show for testing
        )
        _pinnedShowIds.value = pinnedIds
        
        val downloadStatuses = testShows.take(3).associate { show ->
            show.id to LibraryDownloadStatus.COMPLETED
        }
        _downloadStatuses.value = downloadStatuses
        
        Log.d(TAG, "V2 STUB: Test data populated - ${testShows.size} shows, ${pinnedIds.size} pinned, ${downloadStatuses.size} downloaded")
        
        return Result.success(Unit)
    }
    
    private fun createTestLibraryShows(): List<Show> {
        return listOf(
            createShow(
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                location = "Ithaca, NY",
                year = "1977",
                addedDaysAgo = 30
            ),
            createShow(
                date = "1972-05-04",
                venue = "Olympia Theatre",
                location = "Paris, France",
                year = "1972",
                addedDaysAgo = 7
            ),
            createShow(
                date = "1976-06-09",
                venue = "Boston Music Hall",
                location = "Boston, MA",
                year = "1976",
                addedDaysAgo = 45
            ),
            createShow(
                date = "1981-05-16",
                venue = "Cornell University",
                location = "Ithaca, NY",
                year = "1981",
                addedDaysAgo = 14
            ),
            createShow(
                date = "1989-07-04",
                venue = "Rich Stadium",
                location = "Orchard Park, NY",
                year = "1989",
                addedDaysAgo = 3
            ),
            createShow(
                date = "1994-06-25",
                venue = "Sam Boyd Silver Bowl",
                location = "Las Vegas, NV",
                year = "1994",
                addedDaysAgo = 60
            ),
            createShow(
                date = "1990-09-20",
                venue = "Madison Square Garden",
                location = "New York, NY",
                year = "1990",
                addedDaysAgo = 21
            ),
            createShow(
                date = "1969-02-27",
                venue = "Fillmore East",
                location = "New York, NY",
                year = "1969",
                addedDaysAgo = 90
            ),
            createShow(
                date = "1977-04-22",
                venue = "The Spectrum",
                location = "Philadelphia, PA",
                year = "1977",
                addedDaysAgo = 12
            ),
            createShow(
                date = "1989-07-15",
                venue = "Soldier Field",
                location = "Chicago, IL",
                year = "1989",
                addedDaysAgo = 40
            ),
            createShow(
                date = "1992-12-31",
                venue = "Oakland-Alameda County Coliseum",
                location = "Oakland, CA",
                year = "1992",
                addedDaysAgo = 85
            ),
            createShow(
                date = "1985-10-31",
                venue = "Berkeley Community Theatre",
                location = "Berkeley, CA",
                year = "1985",
                addedDaysAgo = 55
            )
        )
    }
    
    private fun createAvailableShows(): List<Show> {
        return createTestLibraryShows().map { it.copy(libraryAddedAt = null, isInLibrary = false) } +
            listOf(
                createShow(
                    date = "1978-05-08",
                    venue = "Field House, Rensselaer Polytechnic Institute",
                    location = "Troy, NY",
                    year = "1978",
                    addedDaysAgo = null
                ),
                createShow(
                    date = "1983-09-02",
                    venue = "Boise State University Pavilion",
                    location = "Boise, ID",
                    year = "1983",
                    addedDaysAgo = null
                )
            )
    }
    
    private fun createShow(
        date: String,
        venue: String,
        location: String,
        year: String,
        addedDaysAgo: Int?
    ): Show {
        val addedTimestamp = addedDaysAgo?.let {
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong())
        }
        
        val locationParts = location.split(", ")
        val city = locationParts.firstOrNull()
        val state = locationParts.getOrNull(1)
        
        return Show(
            id = date,
            date = date,
            year = year.toIntOrNull() ?: 1970,
            band = "Grateful Dead",
            venue = Venue(
                name = venue, 
                city = city, 
                state = state,
                country = "USA"
            ),
            location = Location(
                displayText = location,
                city = city,
                state = state
            ),
            setlist = null,
            lineup = null,
            recordingIds = listOf("${date.replace("-", "")}.sbd.${venue.take(3).lowercase()}"),
            bestRecordingId = "${date.replace("-", "")}.sbd.${venue.take(3).lowercase()}",
            recordingCount = 1,
            averageRating = (3.5f + Math.random().toFloat() * 1.5f).toFloat(),
            totalReviews = (10..50).random(),
            isInLibrary = addedTimestamp != null,
            libraryAddedAt = addedTimestamp
        )
    }
}