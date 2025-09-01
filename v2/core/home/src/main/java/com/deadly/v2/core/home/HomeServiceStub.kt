package com.deadly.v2.core.home

import android.util.Log
import com.deadly.v2.core.api.home.HomeService
import com.deadly.v2.core.api.home.HomeContent
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive stub implementation of HomeService with realistic Dead show data.
 * 
 * This stub provides rich mock data for immediate UI development:
 * - Recent shows spanning decades of Grateful Dead concerts
 * - Today in History with historical context
 * - Featured collections representing major releases and tours
 * 
 * Enables UI-first development while validating V2 architecture patterns.
 */
@Singleton
class HomeServiceStub @Inject constructor(
    private val showRepository: ShowRepository
) : HomeService {
    
    companion object {
        private const val TAG = "HomeServiceStub"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _homeContent = MutableStateFlow(HomeContent.initial())
    override val homeContent: StateFlow<HomeContent> = _homeContent.asStateFlow()
    
    init {
        Log.d(TAG, "HomeServiceStub initialized - loading dynamic today in history data")
        loadInitialContent()
    }
    
    private fun loadInitialContent() {
        serviceScope.launch {
            try {
                val todayInHistory = loadTodayInHistoryShows()
                
                _homeContent.value = HomeContent(
                    recentShows = generateMockRecentShows(),
                    todayInHistory = todayInHistory,
                    featuredCollections = generateMockCollections(),
                    lastRefresh = System.currentTimeMillis()
                )
                
                Log.d(TAG, "Loaded ${todayInHistory.size} shows for today in history")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load today in history shows", e)
                // Fall back to mock data
                _homeContent.value = HomeContent(
                    recentShows = generateMockRecentShows(),
                    todayInHistory = generateMockHistoryShows(),
                    featuredCollections = generateMockCollections(),
                    lastRefresh = System.currentTimeMillis()
                )
            }
        }
    }
    
    /**
     * Load actual shows for today's date from the database
     */
    private suspend fun loadTodayInHistoryShows(): List<Show> {
        val today = LocalDate.now()
        Log.d(TAG, "Loading shows for ${today.monthValue}/${today.dayOfMonth}")
        
        return showRepository.getShowsForDate(today.monthValue, today.dayOfMonth)
    }
    
    override suspend fun refreshAll(): Result<Unit> {
        Log.d(TAG, "refreshAll() called - reloading today in history")
        
        return try {
            val todayInHistory = loadTodayInHistoryShows()
            
            _homeContent.value = _homeContent.value.copy(
                todayInHistory = todayInHistory,
                lastRefresh = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Refreshed with ${todayInHistory.size} shows for today")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh today in history", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate realistic recent shows data for development
     * Mix of famous shows across different eras
     */
    private fun generateMockRecentShows(): List<Show> {
        return listOf(
            // Cornell 5/8/77 - The legendary show
            Show(
                id = "gd1977-05-08",
                date = "1977-05-08",
                year = 1977,
                band = "Grateful Dead",
                venue = Venue("Barton Hall, Cornell University", "Ithaca", "NY", "USA"),
                location = Location("Ithaca, NY", "Ithaca", "NY"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd1977-05-08.sbd.hicks.4982.sbeok.shnf"),
                bestRecordingId = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.8f,
                totalReviews = 245,
                isInLibrary = true,
                libraryAddedAt = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            
            // Europe '72 Classic
            Show(
                id = "gd1972-05-11",
                date = "1972-05-11",
                year = 1972,
                band = "Grateful Dead",
                venue = Venue("Civic Auditorium", "Albuquerque", "NM", "USA"),
                location = Location("Albuquerque, NM", "Albuquerque", "NM"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd72-05-11.sbd.unknown.30057.sbeok.shnf"),
                bestRecordingId = "gd72-05-11.sbd.unknown.30057.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.6f,
                totalReviews = 189,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            
            // Dick's Picks era
            Show(
                id = "gd1973-06-10",
                date = "1973-06-10",
                year = 1973,
                band = "Grateful Dead",
                venue = Venue("RFK Stadium", "Washington", "DC", "USA"),
                location = Location("Washington, DC", "Washington", "DC"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("dp12"),
                bestRecordingId = "dp12",
                recordingCount = 1,
                averageRating = 4.7f,
                totalReviews = 203,
                isInLibrary = true,
                libraryAddedAt = System.currentTimeMillis() - 172800000 // 2 days ago
            ),
            
            // 1990s era
            Show(
                id = "gd1995-07-09",
                date = "1995-07-09",
                year = 1995,
                band = "Grateful Dead",
                venue = Venue("Soldier Field", "Chicago", "IL", "USA"),
                location = Location("Chicago, IL", "Chicago", "IL"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd95-07-09.sbd.miller.97483.flac1644"),
                bestRecordingId = "gd95-07-09.sbd.miller.97483.flac1644",
                recordingCount = 1,
                averageRating = 4.1f,
                totalReviews = 298,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            
            // Fillmore East classics
            Show(
                id = "gd1970-02-13",
                date = "1970-02-13",
                year = 1970,
                band = "Grateful Dead",
                venue = Venue("Fillmore East", "New York", "NY", "USA"),
                location = Location("New York, NY", "New York", "NY"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd70-02-13.sbd.16332.sbeok.shnf"),
                bestRecordingId = "gd70-02-13.sbd.16332.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.5f,
                totalReviews = 167,
                isInLibrary = true,
                libraryAddedAt = System.currentTimeMillis() - 259200000 // 3 days ago
            ),
            
            // Fillmore West
            Show(
                id = "gd1969-02-27",
                date = "1969-02-27",
                year = 1969,
                band = "Grateful Dead",
                venue = Venue("Fillmore West", "San Francisco", "CA", "USA"),
                location = Location("San Francisco, CA", "San Francisco", "CA"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd69-02-27.sbd.vernon.87915.flac1644"),
                bestRecordingId = "gd69-02-27.sbd.vernon.87915.flac1644",
                recordingCount = 1,
                averageRating = 4.3f,
                totalReviews = 134,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            
            // More 1977 shows
            Show(
                id = "gd1977-05-22",
                date = "1977-05-22",
                year = 1977,
                band = "Grateful Dead",
                venue = Venue("The Sportatorium", "Pembroke Pines", "FL", "USA"),
                location = Location("Pembroke Pines, FL", "Pembroke Pines", "FL"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd77-05-22.sbd.hicks.32928.sbeok.shnf"),
                bestRecordingId = "gd77-05-22.sbd.hicks.32928.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.4f,
                totalReviews = 178,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            
            // Early 1980s
            Show(
                id = "gd1981-05-16",
                date = "1981-05-16", 
                year = 1981,
                band = "Grateful Dead",
                venue = Venue("Barton Hall, Cornell University", "Ithaca", "NY", "USA"),
                location = Location("Ithaca, NY", "Ithaca", "NY"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd81-05-16.sbd.clugston.11112.sbeok.shnf"),
                bestRecordingId = "gd81-05-16.sbd.clugston.11112.sbeok.shnf", 
                recordingCount = 1,
                averageRating = 4.2f,
                totalReviews = 145,
                isInLibrary = true,
                libraryAddedAt = System.currentTimeMillis() - 345600000 // 4 days ago
            )
        )
    }
    
    /**
     * Generate mock "Today In History" shows
     * Shows that happened on this date in Dead history
     */
    private fun generateMockHistoryShows(): List<Show> {
        return listOf(
            Show(
                id = "gd1977-05-08-history",
                date = "1977-05-08",
                year = 1977,
                band = "Grateful Dead",
                venue = Venue("Barton Hall, Cornell University", "Ithaca", "NY", "USA"),
                location = Location("Ithaca, NY", "Ithaca", "NY"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd1977-05-08.sbd.hicks.4982.sbeok.shnf"),
                bestRecordingId = "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
                recordingCount = 1,
                averageRating = 4.8f,
                totalReviews = 245,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            Show(
                id = "gd1970-05-08-history",
                date = "1970-05-08",
                year = 1970,
                band = "Grateful Dead",
                venue = Venue("Kresge Plaza, MIT", "Cambridge", "MA", "USA"),
                location = Location("Cambridge, MA", "Cambridge", "MA"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd70-05-08.aud.unknown.12345.shnf"),
                bestRecordingId = "gd70-05-08.aud.unknown.12345.shnf",
                recordingCount = 1,
                averageRating = 4.0f,
                totalReviews = 87,
                isInLibrary = false,
                libraryAddedAt = null
            ),
            Show(
                id = "gd1972-05-08-history",
                date = "1972-05-08",
                year = 1972,
                band = "Grateful Dead",
                venue = Venue("Civic Arena", "Pittsburgh", "PA", "USA"),
                location = Location("Pittsburgh, PA", "Pittsburgh", "PA"),
                setlist = null,
                lineup = null,
                recordingIds = listOf("gd72-05-08.sbd.unknown.67890.shnf"),
                bestRecordingId = "gd72-05-08.sbd.unknown.67890.shnf",
                recordingCount = 1,
                averageRating = 4.3f,
                totalReviews = 156,
                isInLibrary = true,
                libraryAddedAt = System.currentTimeMillis() - 604800000 // 7 days ago
            )
        )
    }
    
    /**
     * Generate mock collection categories
     * Representing major Dead releases and tours
     */
    private fun generateMockCollections(): List<DeadCollection> {
        return listOf(
            DeadCollection(
                id = "dicks-picks",
                name = "Dick's Picks",
                description = "Dick Latvala's archival series featuring the best soundboard recordings",
                tags = listOf("official", "soundboard", "archival"),
                shows = emptyList()
            ),
            DeadCollection(
                id = "europe-72",
                name = "Europe '72",
                description = "The legendary European tour that produced countless classics",
                tags = listOf("tour", "1972", "europe"),
                shows = emptyList()
            ),
            DeadCollection(
                id = "greatest-shows",
                name = "Greatest Shows",
                description = "The most celebrated concerts in Grateful Dead history",
                tags = listOf("quality", "greatest", "top-rated"),
                shows = emptyList()
            ),
            DeadCollection(
                id = "wall-of-sound",
                name = "Wall of Sound",
                description = "Shows featuring the massive Wall of Sound PA system (1974)",
                tags = listOf("era", "1974", "wall-of-sound"),
                shows = emptyList()
            ),
            DeadCollection(
                id = "rare-recordings",
                name = "Rare Recordings",
                description = "Hard-to-find and limited circulation recordings",
                tags = listOf("rarity", "limited", "rare"),
                shows = emptyList()
            ),
            DeadCollection(
                id = "acoustic-sets",
                name = "Acoustic Sets",
                description = "Intimate acoustic performances and rare unplugged moments",
                tags = listOf("theme", "acoustic", "intimate"),
                shows = emptyList()
            )
        )
    }
}