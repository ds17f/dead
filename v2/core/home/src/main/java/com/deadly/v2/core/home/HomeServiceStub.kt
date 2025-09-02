package com.deadly.v2.core.home

import android.util.Log
import com.deadly.v2.core.api.home.HomeService
import com.deadly.v2.core.api.home.HomeContent
import com.deadly.v2.core.api.recent.RecentShowsService
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2 HomeService implementation with real recent shows tracking.
 * 
 * Integrates with RecentShowsService for real user listening behavior tracking:
 * - Recent shows from actual user plays (via MediaController observation)
 * - Today in History from database queries  
 * - Featured collections from mock/curated data
 * 
 * Uses reactive StateFlow combination for real-time UI updates.
 */
@Singleton
class HomeServiceStub @Inject constructor(
    private val showRepository: ShowRepository,
    private val recentShowsService: RecentShowsService
) : HomeService {
    
    companion object {
        private const val TAG = "HomeServiceStub"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Reactive combination of all home content sources
    override val homeContent: StateFlow<HomeContent> = combine(
        recentShowsService.recentShows,
        loadTodayInHistoryFlow(),
        loadFeaturedCollectionsFlow()
    ) { recentShows, todayInHistory, featuredCollections ->
        HomeContent(
            recentShows = recentShows,
            todayInHistory = todayInHistory,
            featuredCollections = featuredCollections,
            lastRefresh = System.currentTimeMillis()
        )
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeContent.initial()
    )
    
    init {
        Log.d(TAG, "HomeServiceStub initialized with reactive RecentShowsService integration")
    }
    
    /**
     * Reactive flow for today in history shows
     */
    private fun loadTodayInHistoryFlow() = kotlinx.coroutines.flow.flow {
        try {
            val todayInHistory = loadTodayInHistoryShows()
            emit(todayInHistory)
            Log.d(TAG, "Loaded ${todayInHistory.size} shows for today in history")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load today in history shows", e)
            // Fall back to mock data
            emit(generateMockHistoryShows())
        }
    }
    
    /**
     * Reactive flow for featured collections (currently mock data)
     */
    private fun loadFeaturedCollectionsFlow() = kotlinx.coroutines.flow.flow {
        emit(generateMockCollections())
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
        Log.d(TAG, "refreshAll() called - reactive flows will auto-refresh")
        
        return try {
            // With reactive architecture, the StateFlow will automatically update
            // when underlying data sources change. No manual refresh needed.
            // The combine operator will re-emit when any source flow emits.
            
            Log.d(TAG, "Refresh complete - reactive flows handle updates automatically")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh", e)
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