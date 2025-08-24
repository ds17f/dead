package com.deadly.v2.core.playlist.service

import android.util.Log
import com.deadly.v2.core.api.playlist.PlaylistService
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.*
import com.deadly.v2.core.network.archive.service.ArchiveService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * PlaylistServiceImpl - Real implementation using V2 domain architecture
 * 
 * Integrates with ShowRepository for real database operations, efficient
 * chronological navigation, and ArchiveService for track/review data.
 * 
 * Phase 1 Implementation:
 * ✅ Real show loading from database
 * ✅ Real chronological navigation 
 * ✅ Domain model → ViewModel conversion
 * ✅ Real track lists from Archive.org API
 * ✅ Real reviews from Archive.org API
 * 🔲 Stubbed: Media operations, library/download integrations (marked with TODOs)
 */
@Singleton
class PlaylistServiceImpl @Inject constructor(
    private val showRepository: ShowRepository,
    private val archiveService: ArchiveService,
    @Named("PlaylistApplicationScope") private val coroutineScope: CoroutineScope
) : PlaylistService {
    
    
    private var currentShow: Show? = null
    private var currentRecordingId: String? = null

    // Internal prefetch cache for transparent optimization
    private val trackCache = ConcurrentHashMap<String, List<Track>>()
    private val prefetchJobs = ConcurrentHashMap<String, Job>()

    companion object {
        private const val TAG = "PlaylistServiceImpl"
        
        // Default show ID (Cornell '77) for fallback
        private const val DEFAULT_SHOW_ID = "1977-05-08"
        
        // Prefetch priorities
        const val PREFETCH_NEXT = "next"
        const val PREFETCH_PREVIOUS = "previous"
    }
    
    
    // === PHASE 1: REAL IMPLEMENTATIONS ===
    
    override suspend fun loadShow(showId: String?) {
        Log.d(TAG, "Loading show: $showId")
        
        currentShow = if (showId != null) {
            showRepository.getShowById(showId)
        } else {
            // Default to Cornell '77 if no showId provided
            showRepository.getShowById(DEFAULT_SHOW_ID)
        }
        
        if (currentShow != null) {
            Log.d(TAG, "Loaded show: ${currentShow!!.displayTitle}")
            // Set default recording to best recording for this show
            currentRecordingId = currentShow!!.bestRecordingId
            Log.d(TAG, "Set current recording to best: $currentRecordingId")
        } else {
            Log.w(TAG, "Failed to load show with ID: $showId")
        }
    }
    
    override suspend fun getCurrentShowInfo(): PlaylistShowViewModel? {
        return currentShow?.let { show ->
            convertShowToViewModel(show)
        }
    }
    
    override suspend fun navigateToNextShow() {
        currentShow?.let { current ->
            val nextShow = showRepository.getNextShowByDate(current.date)
            if (nextShow != null) {
                currentShow = nextShow
                // Update recording ID to best recording for new show
                currentRecordingId = nextShow.bestRecordingId
                Log.d(TAG, "Navigated to next show: ${nextShow.displayTitle}")
                Log.d(TAG, "Set recording to best: ${nextShow.bestRecordingId}")
            } else {
                Log.d(TAG, "No next show available after ${current.date}")
            }
        } ?: Log.w(TAG, "Cannot navigate: no current show loaded")
    }
    
    override suspend fun navigateToPreviousShow() {
        currentShow?.let { current ->
            val previousShow = showRepository.getPreviousShowByDate(current.date)
            if (previousShow != null) {
                currentShow = previousShow
                // Update recording ID to best recording for new show
                currentRecordingId = previousShow.bestRecordingId
                Log.d(TAG, "Navigated to previous show: ${previousShow.displayTitle}")
                Log.d(TAG, "Set recording to best: ${previousShow.bestRecordingId}")
            } else {
                Log.d(TAG, "No previous show available before ${current.date}")
            }
        } ?: Log.w(TAG, "Cannot navigate: no current show loaded")
    }
    
    // === DOMAIN MODEL CONVERSION ===
    
    private suspend fun convertShowToViewModel(show: Show): PlaylistShowViewModel {
        // Calculate navigation availability using database queries
        val hasNext = showRepository.getNextShowByDate(show.date) != null
        val hasPrevious = showRepository.getPreviousShowByDate(show.date) != null
        
        return PlaylistShowViewModel(
            date = show.date,
            displayDate = formatDisplayDate(show.date),
            venue = show.venue.name,
            location = show.location.displayText,
            rating = show.averageRating ?: 0.0f,
            reviewCount = show.totalReviews,
            currentRecordingId = show.bestRecordingId, // TODO: Enhance this with user preference // TODO: Does this even really belong here?
            trackCount = show.recordingCount, // Use recording count as proxy for now
            hasNextShow = hasNext,
            hasPreviousShow = hasPrevious,
            isInLibrary = show.isInLibrary,
            downloadProgress = null // TODO: Integrate with V2 download service
        )
    }
    
    private fun formatDisplayDate(date: String): String {
        // Convert "1977-05-08" to "May 8, 1977"
        return try {
            val parts = date.split("-")
            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            val monthNames = arrayOf(
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            
            "${monthNames[month]} $day, $year"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to format date: $date", e)
            date // Fallback to original format
        }
    }
    
    // === PHASE 1: STUBBED IMPLEMENTATIONS (TODOs) ===
    
    override suspend fun getTrackList(): List<PlaylistTrackViewModel> {
        Log.d(TAG, "getTrackList() - Cache-first implementation with prefetch")
        
        val recordingId = currentRecordingId
        if (recordingId == null) {
            Log.w(TAG, "No current recording ID set")
            return emptyList()
        }
        
        // 1. Check internal cache first
        val cachedTracks = trackCache[recordingId]
        if (cachedTracks != null) {
            Log.d(TAG, "Cache HIT for recording: $recordingId (${cachedTracks.size} tracks)")
            val filteredTracks = filterPreferredAudioTracks(cachedTracks)
            // Start background prefetch for adjacent shows after cache hit
            startAdjacentPrefetch()
            return convertTracksToViewModels(filteredTracks)
        }
        
        // 2. Cache miss - load from Archive.org API
        return try {
            Log.d(TAG, "Cache MISS - Fetching tracks for recording: $recordingId")
            val result = archiveService.getRecordingTracks(recordingId)
            
            if (result.isSuccess) {
                val allTracks = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Got ${allTracks.size} tracks from ArchiveService")
                
                // Store in cache for future requests
                trackCache[recordingId] = allTracks
                
                // Apply smart audio format filtering (business logic)
                val filteredTracks = filterPreferredAudioTracks(allTracks)
                Log.d(TAG, "Filtered to ${filteredTracks.size} preferred audio tracks")
                
                // Start background prefetch for adjacent shows
                startAdjacentPrefetch()
                
                // Convert to ViewModels
                convertTracksToViewModels(filteredTracks)
            } else {
                Log.e(TAG, "Error fetching tracks: ${result.exceptionOrNull()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getTrackList", e)
            emptyList()
        }
    }
    
    override suspend fun playTrack(trackIndex: Int) {
        Log.d(TAG, "playTrack($trackIndex) - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service when available
    }
    
    override suspend fun addToLibrary() {
        currentShow?.let { show ->
            Log.d(TAG, "addToLibrary() for ${show.displayTitle} - TODO: Integrate with V2 library service")
        }
        // TODO: Integrate with V2 library service when available
        // TODO: Update show.isInLibrary and persist to database
    }
    
    override suspend fun downloadShow() {
        currentShow?.let { show ->
            Log.d(TAG, "downloadShow() for ${show.displayTitle} - TODO: Integrate with V2 download service")
        }
        // TODO: Integrate with V2 download service when available
        // TODO: Update downloadProgress in ViewModel
    }
    
    override suspend fun shareShow() {
        currentShow?.let { show ->
            Log.d(TAG, "shareShow() for ${show.displayTitle} - TODO: Implement sharing functionality")
        }
        // TODO: Implement sharing functionality (generate share URL, etc.)
    }
    
    override suspend fun loadSetlist() {
        currentShow?.let { show ->
            Log.d(TAG, "loadSetlist() for ${show.displayTitle} - TODO: Use setlist data from Show domain model")
            if (show.setlist != null) {
                Log.d(TAG, "Setlist available with status: ${show.setlist!!.status}")
            } else {
                Log.d(TAG, "No setlist data available for this show")
            }
        }
        // TODO: Use setlist data from Show domain model
        // TODO: Parse and format setlist for UI display
    }
    
    override suspend fun pause() {
        Log.d(TAG, "pause() - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service
    }
    
    override suspend fun resume() {
        Log.d(TAG, "resume() - TODO: Integrate with V2 media service")
        // TODO: Integrate with V2 media service
    }
    
    override suspend fun getCurrentReviews(): List<PlaylistReview> {
        Log.d(TAG, "getCurrentReviews() - Real implementation using ArchiveService")
        
        val recordingId = currentRecordingId
        if (recordingId == null) {
            Log.w(TAG, "No current recording ID set")
            return emptyList()
        }
        
        return try {
            Log.d(TAG, "Fetching reviews for recording: $recordingId")
            val result = archiveService.getRecordingReviews(recordingId)
            
            if (result.isSuccess) {
                val reviews = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Got ${reviews.size} reviews from ArchiveService")
                
                // Convert Review domain models to PlaylistReview ViewModels
                reviews.map { review ->
                    PlaylistReview(
                        username = review.reviewer ?: "Anonymous",
                        rating = review.rating ?: 0,
                        stars = (review.rating ?: 0).toDouble(),
                        reviewText = review.body ?: "",
                        reviewDate = review.reviewDate ?: ""
                    )
                }
            } else {
                Log.e(TAG, "Error fetching reviews: ${result.exceptionOrNull()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getCurrentReviews", e)
            emptyList()
        }
    }
    
    override suspend fun getRatingDistribution(): Map<Int, Int> {
        currentShow?.let { show ->
            Log.d(TAG, "getRatingDistribution() for ${show.displayTitle} - TODO: Calculate from recording ratings")
            Log.d(TAG, "Show average rating: ${show.averageRating}")
        }
        // TODO: Calculate from recording ratings when available
        // TODO: Aggregate rating distribution from all recordings for this show
        return emptyMap()
    }
    
    override suspend fun getRecordingOptions(): RecordingOptionsResult {
        currentShow?.let { show ->
            Log.d(TAG, "getRecordingOptions() for ${show.displayTitle} - TODO: Load from Recording domain models")
            Log.d(TAG, "Show has ${show.recordingIds.size} recordings: ${show.recordingIds}")
            Log.d(TAG, "Best recording ID: ${show.bestRecordingId}")
        }
        // TODO: Convert Recording domain models to RecordingOptionViewModel
        // TODO: Load recordings using showRepository.getRecordingsForShow()
        // TODO: Determine current, recommended, and alternative recordings
        return RecordingOptionsResult(
            currentRecording = null,
            alternativeRecordings = emptyList(),
            hasRecommended = false
        )
    }
    
    override suspend fun selectRecording(recordingId: String) {
        currentShow?.let { show ->
            Log.d(TAG, "selectRecording($recordingId) for ${show.displayTitle} - Real implementation")
            
            // Update current recording
            if (show.recordingIds.contains(recordingId)) {
                currentRecordingId = recordingId
                Log.d(TAG, "Selected recording: $recordingId")
                // TODO: Notify media service of recording change
            } else {
                Log.w(TAG, "Recording $recordingId not found in show recording list")
            }
        } ?: Log.w(TAG, "Cannot select recording: no current show loaded")
    }
    
    override suspend fun setRecordingAsDefault(recordingId: String) {
        currentShow?.let { show ->
            Log.d(TAG, "setRecordingAsDefault($recordingId) for ${show.displayTitle} - TODO: Implement user preferences")
        }
        // TODO: Implement user preference storage
        // TODO: Save recording preference for this show to database/preferences
    }
    
    override suspend fun resetToRecommended() {
        currentShow?.let { show ->
            Log.d(TAG, "resetToRecommended() for ${show.displayTitle} - TODO: Implement recommendation logic")
            Log.d(TAG, "Would reset to best recording: ${show.bestRecordingId}")
        }
        // TODO: Implement recommendation logic
        // TODO: Clear user preferences and use bestRecordingId from Show
        // TODO: Update UI to reflect recommended recording selection
    }
    
    override fun cancelTrackLoading() {
        Log.d(TAG, "cancelTrackLoading() - Clearing prefetch jobs and cache")
        // Cancel all prefetch jobs and clear cache when explicitly requested
        prefetchJobs.values.forEach { it.cancel() }
        prefetchJobs.clear()
        trackCache.clear()
    }

    // === PRIVATE HELPER METHODS ===
    
    /**
     * Convert Track domain models to PlaylistTrackViewModel display models
     */
    private fun convertTracksToViewModels(tracks: List<Track>): List<PlaylistTrackViewModel> {
        return tracks.mapIndexed { index, track ->
            PlaylistTrackViewModel(
                number = index + 1,
                title = track.title ?: track.name,
                duration = track.duration ?: "",
                format = track.format,
                isDownloaded = false,     // TODO: Integrate with download service
                downloadProgress = null,  // TODO: Integrate with download service
                isCurrentTrack = false,   // TODO: Integrate with media service to determine current track
                isPlaying = false         // TODO: Integrate with media service for playback state
            )
        }
    }
    
    /**
     * Start background prefetch for adjacent shows (next/previous 2 shows each)
     */
    private fun startAdjacentPrefetch() {
        currentShow?.let { current ->
            coroutineScope.launch {
                try {
                    Log.d(TAG, "Starting adjacent prefetch for current show: ${current.displayTitle}")
                    
                    // Prefetch next 2 shows
                    val nextShows = mutableListOf<Show>()
                    var currentNextDate = current.date
                    repeat(2) { index ->
                        val nextShow = showRepository.getNextShowByDate(currentNextDate)
                        if (nextShow != null) {
                            nextShows.add(nextShow)
                            currentNextDate = nextShow.date
                            
                            val recordingId = nextShow.bestRecordingId
                            if (recordingId != null && !trackCache.containsKey(recordingId)) {
                                startPrefetchInternal(nextShow, recordingId, "next+${index + 1}")
                            } else {
                                Log.d(TAG, "Skipping next+${index + 1} show prefetch: recordingId=$recordingId, cached=${trackCache.containsKey(recordingId ?: "")}")
                            }
                        }
                    }
                    
                    // Prefetch previous 2 shows
                    val previousShows = mutableListOf<Show>()
                    var currentPrevDate = current.date
                    repeat(2) { index ->
                        val previousShow = showRepository.getPreviousShowByDate(currentPrevDate)
                        if (previousShow != null) {
                            previousShows.add(previousShow)
                            currentPrevDate = previousShow.date
                            
                            val recordingId = previousShow.bestRecordingId
                            if (recordingId != null && !trackCache.containsKey(recordingId)) {
                                startPrefetchInternal(previousShow, recordingId, "previous+${index + 1}")
                            } else {
                                Log.d(TAG, "Skipping previous+${index + 1} show prefetch: recordingId=$recordingId, cached=${trackCache.containsKey(recordingId ?: "")}")
                            }
                        }
                    }
                    
                    Log.d(TAG, "Extended prefetch initiated - Next 2: ${nextShows.map { it.displayTitle }}, Previous 2: ${previousShows.map { it.displayTitle }}")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in startAdjacentPrefetch", e)
                }
            }
        } ?: Log.d(TAG, "No current show set, skipping adjacent prefetch")
    }
    
    /**
     * Smart audio format filtering with priority-based selection
     * 
     * Applies business logic to filter tracks to the most suitable audio formats
     * for streaming and playback. Prefers MP3 formats for compatibility.
     */
    private fun filterPreferredAudioTracks(tracks: List<Track>): List<Track> {
        // Audio format priority (most to least preferred for streaming)
        val formatPriority = listOf(
            "VBR MP3",      // Variable bitrate MP3 - most common on Archive.org
            "MP3",          // Standard MP3 
            "128Kbps MP3",  // Fixed bitrate MP3
            "256Kbps MP3",  // Higher quality MP3
            "64Kbps MP3"    // Lower quality MP3
        )
        
        // Secondary audio formats (fallback if no MP3 available)
        val fallbackFormats = listOf(
            "FLAC",         // Lossless audio
            "OGG Vorbis",   // Open source compressed audio  
            "M4A",          // Apple audio format
            "AAC",          // Advanced audio codec
            "WAV"           // Uncompressed audio
        )
        
        Log.d(TAG, "Filtering ${tracks.size} tracks by audio format priority")
        
        // First, try to get preferred MP3 formats
        val preferredTracks = tracks.filter { track ->
            track.format in formatPriority
        }
        
        if (preferredTracks.isNotEmpty()) {
            Log.d(TAG, "Found ${preferredTracks.size} preferred format tracks")
            return preferredTracks.sortedBy { track ->
                formatPriority.indexOf(track.format).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
        }
        
        // Fallback to other audio formats if no MP3 available
        val fallbackTracks = tracks.filter { track ->
            track.format in fallbackFormats
        }
        
        if (fallbackTracks.isNotEmpty()) {
            Log.d(TAG, "Using ${fallbackTracks.size} fallback format tracks")
            return fallbackTracks.sortedBy { track ->
                fallbackFormats.indexOf(track.format).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
        }
        
        // Last resort: return all tracks marked as audio
        val audioTracks = tracks.filter { it.isAudio }
        Log.d(TAG, "Using ${audioTracks.size} generic audio tracks as last resort")
        return audioTracks
    }
    
    // === PREFETCH MANAGEMENT ===
    
    /**
     * Internal prefetch method using service's coroutine scope
     */
    private fun startPrefetchInternal(show: Show, recordingId: String, priority: String) {
        // Don't prefetch if already in progress
        if (prefetchJobs[recordingId]?.isActive == true) {
            Log.d(TAG, "Prefetch already active for recording: $recordingId")
            return
        }
        
        Log.d(TAG, "Starting $priority prefetch for ${show.displayTitle} (recording: $recordingId)")
        
        val job = coroutineScope.launch {
            try {
                val result = archiveService.getRecordingTracks(recordingId)
                
                if (result.isSuccess) {
                    val allTracks = result.getOrNull() ?: emptyList()
                    val filteredTracks = filterPreferredAudioTracks(allTracks)
                    
                    // Store in cache - THIS WAS MISSING
                    trackCache[recordingId] = allTracks
                    
                    Log.d(TAG, "Prefetch completed for ${show.displayTitle}: ${filteredTracks.size} tracks cached")
                } else {
                    Log.w(TAG, "Prefetch failed for ${show.displayTitle}: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prefetch error for ${show.displayTitle}", e)
            } finally {
                // Remove from active prefetches when complete
                prefetchJobs.remove(recordingId)
            }
        }
        
        prefetchJobs[recordingId] = job
    }
    
    /**
     * Check if we have an active prefetch for a specific show
     */
    fun hasPrefetchFor(showId: String): Boolean {
        return prefetchJobs[showId]?.isActive == true
    }
    
    /**
     * Start prefetching tracks for a show in background
     */
    fun startPrefetch(show: Show, priority: String, coroutineScope: CoroutineScope): Job? {
        val showId = show.date
        
        // Don't prefetch if already in progress
        if (hasPrefetchFor(showId)) {
            Log.d(TAG, "Prefetch already active for show: $showId")
            return prefetchJobs[showId]
        }
        
        Log.d(TAG, "Starting $priority prefetch for show: ${show.displayTitle}")
        
        val job = coroutineScope.launch {
            try {
                val result = archiveService.getRecordingTracks(show.bestRecordingId ?: "")
                
                if (result.isSuccess) {
                    val tracks = result.getOrNull() ?: emptyList()
                    val filteredTracks = filterPreferredAudioTracks(tracks)
                    
                    // Store in cache - FIX: This was missing before
                    trackCache[show.bestRecordingId ?: ""] = tracks
                    
                    Log.d(TAG, "Prefetch completed for ${show.displayTitle}: ${filteredTracks.size} tracks cached")
                } else {
                    Log.w(TAG, "Prefetch failed for ${show.displayTitle}: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prefetch error for ${show.displayTitle}", e)
            } finally {
                // Remove from active prefetches when complete
                prefetchJobs.remove(showId)
            }
        }
        
        prefetchJobs[showId] = job
        return job
    }
    
    /**
     * Promote a prefetch request to current priority (don't cancel it)
     */
    fun promotePrefetch(showId: String): Job? {
        val existingJob = prefetchJobs[showId]
        if (existingJob?.isActive == true) {
            Log.d(TAG, "Promoting prefetch for show: $showId to current priority")
            return existingJob
        }
        return null
    }
    
    /**
     * Cancel prefetch for a specific show
     */
    fun cancelPrefetch(showId: String) {
        prefetchJobs[showId]?.let { job ->
            Log.d(TAG, "Cancelling prefetch for show: $showId")
            job.cancel()
            prefetchJobs.remove(showId)
        }
    }
    
    /**
     * Get next and previous shows for prefetching
     */
    suspend fun getAdjacentShows(): Pair<Show?, Show?> {
        return currentShow?.let { current ->
            val nextShow = showRepository.getNextShowByDate(current.date)
            val previousShow = showRepository.getPreviousShowByDate(current.date)
            Pair(nextShow, previousShow)
        } ?: Pair(null, null)
    }
}