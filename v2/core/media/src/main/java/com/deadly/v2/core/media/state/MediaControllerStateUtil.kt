package com.deadly.v2.core.media.state

import android.util.Log
import androidx.media3.common.MediaMetadata
import com.deadly.v2.core.media.repository.MediaControllerRepository
import com.deadly.v2.core.model.CurrentTrackInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared utility for MediaController state combination and transformation
 * 
 * Eliminates duplication across V2 services by providing common MediaController
 * state observation and CurrentTrackInfo creation patterns.
 * 
 * FOUNDATION FIRST: Built on solid Phase 0 MediaController threading foundation.
 * All MediaController operations properly handle main thread requirements.
 */
@Singleton
class MediaControllerStateUtil @Inject constructor(
    private val mediaControllerRepository: MediaControllerRepository
) {
    companion object {
        private const val TAG = "MediaControllerStateUtil"
    }
    
    /**
     * Create a StateFlow of CurrentTrackInfo combining all MediaController state
     * 
     * This is the common pattern used by PlaylistServiceImpl and similar services.
     * Combines MediaController metadata, playback state, position, and duration into
     * a single reactive CurrentTrackInfo stream.
     * 
     * @param coroutineScope The coroutine scope for StateFlow lifecycle
     * @return StateFlow of CurrentTrackInfo that updates when any MediaController state changes
     */
    fun createCurrentTrackInfoStateFlow(
        coroutineScope: CoroutineScope
    ): StateFlow<CurrentTrackInfo?> {
        Log.d(TAG, "Creating CurrentTrackInfo StateFlow with comprehensive MediaController state combination")
        
        return combine(
            mediaControllerRepository.currentTrack,
            mediaControllerRepository.currentRecordingId,
            mediaControllerRepository.currentShowId,
            mediaControllerRepository.isPlaying,
            mediaControllerRepository.currentPosition,
            mediaControllerRepository.duration
        ) { values ->
            val mediaMetadata = values[0] as MediaMetadata?
            val recordingId = values[1] as String?
            val showId = values[2] as String?
            val isCurrentlyPlaying = values[3] as Boolean
            val currentPosition = values[4] as Long
            val duration = values[5] as Long
            
            Log.v(TAG, "MediaController state change: metadata=${mediaMetadata != null}, recordingId=$recordingId, showId=$showId, playing=$isCurrentlyPlaying")
            
            if (mediaMetadata == null || recordingId == null) {
                Log.v(TAG, "CurrentTrackInfo is null - missing metadata or recordingId")
                null
            } else {
                val trackInfo = createCurrentTrackInfo(
                    metadata = mediaMetadata,
                    recordingId = recordingId,
                    showId = showId,
                    isPlaying = isCurrentlyPlaying,
                    position = currentPosition,
                    duration = duration
                )
                
                Log.v(TAG, "Created CurrentTrackInfo: ${trackInfo.songTitle} (${trackInfo.recordingId})")
                trackInfo
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }
    
    /**
     * Create CurrentTrackInfo from MediaMetadata with comprehensive data extraction
     * 
     * Combines the approaches from both PlaylistServiceImpl and MiniPlayerServiceImpl:
     * - Extracts rich metadata from MediaMetadata extras (MiniPlayerServiceImpl approach)
     * - Includes live playback state, position, duration (PlaylistServiceImpl approach)
     * - Handles show info parsing from album metadata (PlaylistServiceImpl approach)
     * 
     * @param metadata MediaMetadata from MediaController
     * @param recordingId Current recording ID from MediaController
     * @param showId Current show ID from MediaController  
     * @param isPlaying Current playback state from MediaController
     * @param position Current playback position from MediaController
     * @param duration Current track duration from MediaController
     * @return CurrentTrackInfo with all available data populated
     */
    fun createCurrentTrackInfo(
        metadata: MediaMetadata,
        recordingId: String?,
        showId: String?,
        isPlaying: Boolean,
        position: Long,
        duration: Long
    ): CurrentTrackInfo {
        Log.d(TAG, "Creating CurrentTrackInfo from MediaMetadata for recording: $recordingId")
        
        // Extract basic track info
        val title = metadata.title?.toString() ?: "Unknown Track"
        val album = metadata.albumTitle?.toString() ?: ""
        val trackNumber = metadata.trackNumber
        
        // Extract rich metadata from extras (MiniPlayerServiceImpl pattern)
        val extras = metadata.extras
        val trackUrl = extras?.getString("trackUrl") ?: "${recordingId}_${title}"
        val filename = extras?.getString("filename") ?: title
        val venue = extras?.getString("venue")
        val location = extras?.getString("location") 
        val showDate = extras?.getString("showDate")
        
        // Parse show info from album if not in extras (PlaylistServiceImpl pattern)
        val (parsedShowDate, parsedVenue, parsedLocation) = if (showDate.isNullOrEmpty()) {
            parseShowInfo(album)
        } else {
            Triple(showDate, venue, location)
        }
        
        val trackInfo = CurrentTrackInfo(
            trackUrl = trackUrl,
            recordingId = recordingId ?: "",
            showId = showId ?: parsedShowDate,
            showDate = parsedShowDate.takeIf { it.isNotEmpty() } ?: showId ?: "",
            venue = parsedVenue,
            location = parsedLocation,
            songTitle = title,
            trackNumber = trackNumber,
            filename = filename,
            isPlaying = isPlaying,
            position = position,
            duration = duration
        )
        
        Log.d(TAG, "CurrentTrackInfo created successfully - Title: ${trackInfo.songTitle}, Recording: ${trackInfo.recordingId}, Playing: ${trackInfo.isPlaying}")
        return trackInfo
    }
    
    /**
     * Parse show information from album metadata
     * 
     * Extracts show date, venue, location from album title format.
     * This is the same logic used in PlaylistServiceImpl.
     * 
     * @param album Album title containing show information
     * @return Triple of (showDate, venue, location) - may contain nulls/empty strings
     */
    private fun parseShowInfo(album: String): Triple<String, String?, String?> {
        Log.v(TAG, "Parsing show info from album: $album")
        
        // TODO: Implement proper parsing based on album format
        // For now, return empty values - this will be populated properly
        // when MediaController provides richer metadata
        
        // This maintains the exact same behavior as PlaylistServiceImpl
        return Triple("", null, null)
    }
    
    /**
     * Get debug information about current MediaController state
     * 
     * Provides detailed state information for troubleshooting state combination issues.
     * Uses the MediaControllerRepository debug capabilities from Phase 0.
     * 
     * @return Debug information string
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== MediaControllerStateUtil Debug Info ===")
            appendLine("MediaControllerRepository Debug Info:")
            appendLine(mediaControllerRepository.getDebugInfo())
            appendLine("=== End MediaControllerStateUtil Debug Info ===")
        }
    }
}