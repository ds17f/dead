package com.deadarchive.feature.player.service

import com.deadarchive.core.model.PlayerV2State
import kotlinx.coroutines.flow.Flow

/**
 * PlayerV2Service - Service interface discovered through UI-first development
 * 
 * This interface was created after building the UI components and discovering
 * what data and operations we need from the domain layer. Following V2 architecture,
 * the UI drives the service interface requirements.
 * 
 * Domain Requirements Discovered from UI Components:
 * 
 * From PlayerV2TrackInfo component:
 * - Need current track title with proper display formatting
 * - Need recording name and identification
 * - Need show date and venue information formatted for display
 * 
 * From PlayerV2ProgressBar component:
 * - Need current playback position and total duration
 * - Need formatted time strings (MM:SS format)
 * - Need progress percentage calculation
 * - Need seek/scrub functionality
 * 
 * From PlayerV2Controls component:
 * - Need play/pause state management
 * - Need track navigation (previous/next)
 * - Need control availability states (can skip, can play, etc.)
 * 
 * Architecture Pattern:
 * - Service provides reactive Flow-based state
 * - UI components subscribe to state changes
 * - Commands are sent via service methods
 * - Service coordinates with V1 services behind the scenes
 */
interface PlayerV2Service {
    
    /**
     * Reactive state flow for the entire player
     * UI components observe this to update their display
     */
    val playerState: Flow<PlayerV2State>
    
    // Track Information Operations (discovered from PlayerV2TrackInfo needs)
    
    /**
     * Load a recording and prepare for playback
     * Sets up the current track and recording metadata
     */
    suspend fun loadRecording(recordingId: String)
    
    /**
     * Get formatted track information for display
     * Returns display-ready strings discovered through UI component needs
     */
    suspend fun getCurrentTrackInfo(): TrackDisplayInfo?
    
    // Playback Control Operations (discovered from PlayerV2Controls needs)
    
    /**
     * Toggle play/pause state
     */
    suspend fun togglePlayPause()
    
    /**
     * Skip to previous track
     */
    suspend fun skipToPrevious()
    
    /**
     * Skip to next track  
     */
    suspend fun skipToNext()
    
    // Progress Operations (discovered from PlayerV2ProgressBar needs)
    
    /**
     * Seek to specific position (0.0 to 1.0)
     */
    suspend fun seekToPosition(position: Float)
    
    /**
     * Get formatted progress information for display
     */
    suspend fun getProgressInfo(): ProgressDisplayInfo?
    
    // Professional UI Data Operations (discovered through UI design)
    
    /**
     * Get playing context information for top navigation
     */
    suspend fun getPlayingContext(): PlayingContextInfo?
    
    /**
     * Get extended track information for professional display
     */
    suspend fun getExtendedTrackInfo(): ExtendedTrackInfo?
    
    /**
     * Get control states for all player buttons
     */
    suspend fun getControlState(): PlayerV2ControlState
    
    /**
     * Toggle shuffle mode
     */
    suspend fun toggleShuffle()
    
    /**
     * Cycle through repeat modes
     */
    suspend fun toggleRepeatMode()
    
    /**
     * Get venue information for extended content
     */
    suspend fun getVenueInfo(): VenueInfo?
    
    /**
     * Get track lyrics (placeholder for future)
     */
    suspend fun getLyrics(): String?
    
    /**
     * Get credits information
     */
    suspend fun getCreditsInfo(): CreditsInfo?
    
    // State Management
    
    /**
     * Check if service is ready for playback operations
     */
    fun isReady(): Boolean
    
    /**
     * Clean up resources when leaving player
     */
    suspend fun cleanup()
}

/**
 * Data class for track display information
 * Discovered through building PlayerV2TrackInfo component
 */
data class TrackDisplayInfo(
    val trackTitle: String,
    val recordingName: String,
    val showDate: String,
    val venue: String
)

/**
 * Data class for progress display information  
 * Discovered through building PlayerV2ProgressBar component
 */
data class ProgressDisplayInfo(
    val currentTime: String,        // Formatted as "MM:SS"
    val totalTime: String,          // Formatted as "MM:SS"
    val progress: Float             // 0.0 to 1.0
)

/**
 * Enhanced data classes discovered through professional UI design
 */

/**
 * Playing context information for top navigation
 */
data class PlayingContextInfo(
    val context: String,            // "Show", "Playlist", "Queue", "Library"
    val contextDetails: String      // Additional context like playlist name
)

/**
 * Control states for player buttons
 */
data class PlayerV2ControlState(
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode
)

enum class RepeatMode {
    NORMAL,
    REPEAT_ALL, 
    REPEAT_ONE
}

/**
 * Extended track information for professional display
 */
data class ExtendedTrackInfo(
    val title: String,
    val showDate: String,
    val venue: String,
    val city: String,
    val state: String,
    val fullLocation: String        // Combined city, state
)

/**
 * Venue information for extended content
 */
data class VenueInfo(
    val name: String,
    val description: String,
    val capacity: String,
    val notableShows: List<String>
)

/**
 * Credits information
 */
data class CreditsInfo(
    val performers: List<String>,
    val recordingDetails: String,
    val source: String,
    val transferredBy: String
)