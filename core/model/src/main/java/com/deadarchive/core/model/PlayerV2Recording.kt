package com.deadarchive.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Domain model representing a Recording within Player context.
 * Combines core recording data with player-specific metadata and state.
 * 
 * Key Design Principles:
 * - Composition over inheritance (contains Recording, doesn't extend it)
 * - Rich domain model with computed properties
 * - Single source of truth for player recording state
 * - Cross-feature integration (library, downloads, setlist)
 */
data class PlayerV2Recording(
    val recording: Recording,                              // Core recording data
    val loadedAt: Long = System.currentTimeMillis(),     // When loaded into player
    val isCurrentlyLoaded: Boolean = false,              // Active in player state
    val currentTrackIndex: Int = 0,                      // Currently selected track
    val playbackHistory: List<PlaybackHistoryEntry> = emptyList(), // Track play history
    val playerTracks: List<PlayerV2Track> = emptyList(), // Tracks with player context
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED, // Overall download state
    val isInLibrary: Boolean = false,                    // Library integration
    val setlistData: Setlist? = null,                    // Setlist integration
    val audioFormatPreferences: List<String> = emptyList() // User format preferences
) {
    // Delegate Recording properties for convenient access
    val recordingId: String get() = recording.identifier
    val title: String? get() = recording.title
    val concertDate: String get() = recording.concertDate
    val concertVenue: String? get() = recording.concertVenue
    val concertLocation: String? get() = recording.concertLocation
    val displayTitle: String get() = recording.displayTitle
    val tracks: List<Track> get() = recording.tracks
    
    // Player-specific computed properties
    val currentTrack: PlayerV2Track? 
        get() = playerTracks.getOrNull(currentTrackIndex)
    
    val currentCoreTrack: Track?
        get() = tracks.getOrNull(currentTrackIndex)
    
    val hasNextTrack: Boolean 
        get() = currentTrackIndex < playerTracks.size - 1
    
    val hasPreviousTrack: Boolean 
        get() = currentTrackIndex > 0
    
    val totalDuration: Duration
        get() = playerTracks.fold(Duration.ZERO) { acc, track -> acc + track.duration }
    
    val totalPlayedTime: Duration
        get() = playerTracks.fold(Duration.ZERO) { acc, track -> 
            acc + if (track.isCompleted) track.duration else track.playbackPosition
        }
    
    val overallProgress: Float
        get() = if (totalDuration > Duration.ZERO) {
            (totalPlayedTime / totalDuration).toFloat().coerceIn(0f, 1f)
        } else 0f
    
    val tracksCompleted: Int 
        get() = playerTracks.count { it.isCompleted }
    
    val isFullyDownloaded: Boolean 
        get() = downloadStatus == DownloadStatus.COMPLETED
    
    val hasSetlist: Boolean 
        get() = setlistData?.hasSongs == true
    
    val canPlayNext: Boolean 
        get() = hasNextTrack && playerTracks.getOrNull(currentTrackIndex + 1)?.canPlay == true
    
    val canPlayPrevious: Boolean 
        get() = hasPreviousTrack && playerTracks.getOrNull(currentTrackIndex - 1)?.canPlay == true
    
    val displayStatus: String
        get() = when {
            isCurrentlyLoaded && currentTrack?.isCurrentlyPlaying == true -> "Playing"
            isCurrentlyLoaded && currentTrack?.isLoading == true -> "Loading..."
            isCurrentlyLoaded -> "Loaded"
            else -> "Available"
        }
    
    // Concert display properties
    val concertDisplayDate: String
        get() = formatConcertDate(concertDate)
    
    val concertDisplayLocation: String
        get() = buildString {
            if (!concertVenue.isNullOrBlank()) {
                append(concertVenue)
            }
            if (!concertLocation.isNullOrBlank()) {
                if (!concertVenue.isNullOrBlank()) {
                    append(", ")
                }
                append(concertLocation)
            }
        }
    
    // Navigation properties
    val showId: String
        get() = generateShowId()
    
    val nextShowCandidate: String?
        get() = null // Would be populated by service
    
    val previousShowCandidate: String?
        get() = null // Would be populated by service
    
    // Functional update methods
    fun updateCurrentTrackIndex(index: Int): PlayerV2Recording {
        val validIndex = index.coerceIn(0, playerTracks.size - 1)
        return copy(currentTrackIndex = validIndex)
    }
    
    fun updatePlayerTracks(tracks: List<PlayerV2Track>): PlayerV2Recording =
        copy(playerTracks = tracks)
    
    fun updateTrackPlaybackPosition(trackIndex: Int, position: Duration): PlayerV2Recording {
        if (trackIndex !in playerTracks.indices) return this
        
        val updatedTracks = playerTracks.toMutableList()
        updatedTracks[trackIndex] = updatedTracks[trackIndex].updatePlaybackPosition(position)
        return copy(playerTracks = updatedTracks)
    }
    
    fun markTrackAsPlaying(trackIndex: Int): PlayerV2Recording {
        if (trackIndex !in playerTracks.indices) return this
        
        val updatedTracks = playerTracks.mapIndexed { index, track ->
            if (index == trackIndex) {
                track.markAsPlaying()
            } else {
                track.markAsNotPlaying()
            }
        }
        return copy(
            playerTracks = updatedTracks,
            currentTrackIndex = trackIndex
        )
    }
    
    fun markAsLoaded(): PlayerV2Recording =
        copy(isCurrentlyLoaded = true, loadedAt = System.currentTimeMillis())
    
    fun markAsUnloaded(): PlayerV2Recording =
        copy(isCurrentlyLoaded = false)
    
    fun updateDownloadStatus(status: DownloadStatus): PlayerV2Recording =
        copy(downloadStatus = status)
    
    fun updateLibraryStatus(inLibrary: Boolean): PlayerV2Recording =
        copy(isInLibrary = inLibrary)
    
    fun updateSetlist(setlist: Setlist?): PlayerV2Recording =
        copy(setlistData = setlist)
    
    fun addToPlaybackHistory(entry: PlaybackHistoryEntry): PlayerV2Recording =
        copy(playbackHistory = playbackHistory + entry)
    
    private fun generateShowId(): String {
        // Generate consistent show ID from recording data
        return "${concertDate}_${concertVenue?.replace(Regex("\\W"), "_") ?: "unknown"}"
    }
    
    private fun formatConcertDate(dateString: String): String {
        return try {
            // Convert from YYYY-MM-DD to more readable format
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                
                val monthNames = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                
                "${monthNames[month - 1]} $day, $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    companion object {
        /**
         * Create PlayerV2Recording from core Recording
         */
        fun fromRecording(
            recording: Recording,
            audioFormatPreferences: List<String> = emptyList()
        ): PlayerV2Recording {
            // Create PlayerV2Track instances from core tracks
            val playerTracks = recording.tracks.mapIndexed { index, track ->
                PlayerV2Track.fromTrack(track, queuePosition = index)
            }
            
            return PlayerV2Recording(
                recording = recording,
                playerTracks = playerTracks,
                audioFormatPreferences = audioFormatPreferences
            )
        }
        
        /**
         * Create PlayerV2Recording with specific player state
         */
        fun create(
            recording: Recording,
            currentTrackIndex: Int = 0,
            isCurrentlyLoaded: Boolean = false,
            downloadStatus: DownloadStatus = DownloadStatus.QUEUED,
            isInLibrary: Boolean = false
        ): PlayerV2Recording {
            val playerTracks = recording.tracks.mapIndexed { index, track ->
                PlayerV2Track.create(
                    track = track,
                    queuePosition = index,
                    downloadStatus = downloadStatus
                )
            }
            
            return PlayerV2Recording(
                recording = recording,
                currentTrackIndex = currentTrackIndex,
                isCurrentlyLoaded = isCurrentlyLoaded,
                playerTracks = playerTracks,
                downloadStatus = downloadStatus,
                isInLibrary = isInLibrary
            )
        }
    }
}

/**
 * Represents an entry in the playback history
 */
data class PlaybackHistoryEntry(
    val trackIndex: Int,
    val playedAt: Long,
    val playedDuration: Duration,
    val completedPlayback: Boolean
)