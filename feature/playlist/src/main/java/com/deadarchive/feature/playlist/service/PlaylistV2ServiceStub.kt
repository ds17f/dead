package com.deadarchive.feature.playlist.service

import android.util.Log
import com.deadarchive.feature.playlist.model.PlaylistShowViewModel
import com.deadarchive.feature.playlist.model.PlaylistTrackViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaylistV2ServiceStub - Mock implementation for UI development
 * 
 * Provides realistic Dead shows from May 1977 era as ViewModel types,
 * following clean architecture principles. Real implementation will map
 * domain models to these ViewModels.
 */
@Singleton
class PlaylistV2ServiceStub @Inject constructor() : PlaylistV2Service {
    
    companion object {
        private const val TAG = "PlaylistV2ServiceStub"
        
        // Mock show data around Cornell '77 - legendary spring tour (as ViewModels)
        private val MOCK_SHOWS = mapOf(
            "1977-05-05" to PlaylistShowViewModel(
                date = "1977-05-05",
                displayDate = "May 5, 1977", 
                venue = "New Haven Coliseum",
                location = "New Haven, CT",
                rating = 4.6f,
                reviewCount = 89,
                trackCount = 16,
                hasNextShow = true,
                hasPreviousShow = false,
                isInLibrary = false
            ),
            "1977-05-07" to PlaylistShowViewModel(
                date = "1977-05-07",
                displayDate = "May 7, 1977",
                venue = "Boston Garden", 
                location = "Boston, MA",
                rating = 4.5f,
                reviewCount = 102,
                trackCount = 17,
                hasNextShow = true,
                hasPreviousShow = true,
                isInLibrary = true
            ),
            "1977-05-08" to PlaylistShowViewModel(
                date = "1977-05-08",
                displayDate = "May 8, 1977",
                venue = "Barton Hall",
                location = "Cornell University, Ithaca, NY", 
                rating = 4.8f,
                reviewCount = 156,
                trackCount = 18,
                hasNextShow = true,
                hasPreviousShow = true,
                isInLibrary = true,
                downloadProgress = 1.0f // Fully downloaded
            ),
            "1977-05-09" to PlaylistShowViewModel(
                date = "1977-05-09",
                displayDate = "May 9, 1977",
                venue = "Buffalo Memorial Auditorium",
                location = "Buffalo, NY",
                rating = 4.4f,
                reviewCount = 67,
                trackCount = 15,
                hasNextShow = true,
                hasPreviousShow = true,
                isInLibrary = false,
                downloadProgress = 0.7f // Downloading
            ),
            "1977-05-11" to PlaylistShowViewModel(
                date = "1977-05-11",
                displayDate = "May 11, 1977",
                venue = "St. Paul Civic Center",
                location = "St. Paul, MN",
                rating = 4.3f,
                reviewCount = 54,
                trackCount = 17,
                hasNextShow = false,
                hasPreviousShow = true,
                isInLibrary = false
            )
        )
        
        private val SHOW_ORDER = listOf("1977-05-05", "1977-05-07", "1977-05-08", "1977-05-09", "1977-05-11")
    }
    
    private var currentShowDate = "1977-05-08" // Start with Cornell
    
    override suspend fun loadShow(showId: String?) {
        Log.d(TAG, "Loading show data for showId: $showId")
        // Simulate network delay
        delay(500)
        
        // Only use showId if it's one of our known mock shows, otherwise default to Cornell
        currentShowDate = if (showId != null && MOCK_SHOWS.containsKey(showId)) {
            showId
        } else {
            "1977-05-08" // Default to Cornell
        }
        Log.d(TAG, "Show data loaded for: $currentShowDate")
    }
    
    override suspend fun playTrack(trackIndex: Int) {
        Log.d(TAG, "Playing track at index: $trackIndex")
        delay(100)
    }
    
    override suspend fun navigateToNextShow() {
        delay(300)
        val currentIndex = SHOW_ORDER.indexOf(currentShowDate)
        Log.d(TAG, "Next navigation: currentShowDate=$currentShowDate, currentIndex=$currentIndex, maxIndex=${SHOW_ORDER.size - 1}")
        if (currentIndex >= 0 && currentIndex < SHOW_ORDER.size - 1) {
            currentShowDate = SHOW_ORDER[currentIndex + 1]
            Log.d(TAG, "Navigated to next show: $currentShowDate")
        } else {
            Log.d(TAG, "Already at last show")
        }
    }
    
    override suspend fun navigateToPreviousShow() {
        delay(300)
        val currentIndex = SHOW_ORDER.indexOf(currentShowDate)
        Log.d(TAG, "Previous navigation: currentShowDate=$currentShowDate, currentIndex=$currentIndex")
        if (currentIndex > 0) {
            currentShowDate = SHOW_ORDER[currentIndex - 1]
            Log.d(TAG, "Navigated to previous show: $currentShowDate")
        } else {
            Log.d(TAG, "Already at first show")
        }
    }
    
    override suspend fun addToLibrary() {
        Log.d(TAG, "Adding Cornell '77 to library")
        delay(200)
    }
    
    override suspend fun downloadShow() {
        Log.d(TAG, "Starting download for Cornell '77")
        delay(100)
    }
    
    override suspend fun shareShow() {
        Log.d(TAG, "Sharing Cornell '77")
        delay(100)
    }
    
    override suspend fun getCurrentShowInfo(): PlaylistShowViewModel? {
        return MOCK_SHOWS[currentShowDate] ?: MOCK_SHOWS["1977-05-08"]
    }
    
    override suspend fun getTrackList(): List<PlaylistTrackViewModel> {
        return listOf(
            PlaylistTrackViewModel(1, "The Music Never Stopped", "7:05", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(2, "Sugaree", "7:12", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(3, "El Paso", "4:36", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(4, "They Love Each Other", "7:23", "FLAC", isDownloaded = false, downloadProgress = 0.8f),
            PlaylistTrackViewModel(5, "Jack Straw", "4:58", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(6, "Deal", "5:19", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(7, "Lazy Lightning", "3:15", "FLAC", isDownloaded = false),
            PlaylistTrackViewModel(8, "Supplication", "4:43", "FLAC", isDownloaded = false),
            PlaylistTrackViewModel(9, "Brown Eyed Women", "4:35", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(10, "Mama Tried", "2:35", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(11, "Row Jimmy", "9:13", "FLAC", isDownloaded = true, isCurrentTrack = true, isPlaying = false),
            PlaylistTrackViewModel(12, "Dancin' In The Streets", "14:43", "FLAC", isDownloaded = false),
            PlaylistTrackViewModel(13, "Scarlet Begonias", "8:26", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(14, "Fire On The Mountain", "14:58", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(15, "Estimated Prophet", "9:33", "FLAC", isDownloaded = false, downloadProgress = 0.3f),
            PlaylistTrackViewModel(16, "St. Stephen", "6:24", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(17, "Not Fade Away", "8:04", "FLAC", isDownloaded = true),
            PlaylistTrackViewModel(18, "Morning Dew", "13:05", "FLAC", isDownloaded = true)
        )
    }
    
    override suspend fun loadSetlist() {
        Log.d(TAG, "Loading setlist for show: $currentShowDate")
        // In real implementation, would load setlist data
        // Stub just logs the action
    }
    
    override suspend fun pause() {
        Log.d(TAG, "Pausing playback")
        // In real implementation, would pause media player
        // Stub just logs the action
    }
    
    override suspend fun resume() {
        Log.d(TAG, "Resuming playback")
        // In real implementation, would resume media player
        // Stub just logs the action
    }
}