package com.deadly.v2.core.model

import kotlinx.serialization.Serializable

/**
 * Playlist-specific data models for the V2 playlist interface
 */

/**
 * Actions available for library button interactions
 */
enum class LibraryAction {
    ADD_TO_LIBRARY,
    REMOVE_FROM_LIBRARY,
    REMOVE_WITH_DOWNLOADS
}

/**
 * PlaylistShowViewModel - UI representation of show data
 * 
 * Clean architecture ViewModel representing show information as needed
 * by the playlist UI components. This isolates UI concerns from domain models.
 */
@Serializable
data class PlaylistShowViewModel(
    val date: String,
    val displayDate: String,
    val venue: String,
    val location: String,
    val rating: Float,
    val reviewCount: Int,
    val currentRecordingId: String?,
    val trackCount: Int,
    val hasNextShow: Boolean,
    val hasPreviousShow: Boolean,
    val isInLibrary: Boolean = false,
    val downloadProgress: Float? = null // null = not downloaded, 0.0-1.0 = downloading, 1.0 = complete
)

/**
 * PlaylistTrackViewModel - UI representation of track data
 * 
 * Clean architecture ViewModel representing track information as needed
 * by the playlist UI components. This isolates UI concerns from domain models.
 */
@Serializable
data class PlaylistTrackViewModel(
    val number: Int,
    val title: String,
    val duration: String,
    val format: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float? = null, // null = not downloaded, 0.0-1.0 = downloading
    val isCurrentTrack: Boolean = false,
    val isPlaying: Boolean = false
)

/**
 * Review data for V2 architecture
 */
@Serializable
data class PlaylistReview(
    val username: String,
    val rating: Int,
    val stars: Double,
    val reviewText: String,
    val reviewDate: String
)

/**
 * RecordingOptionViewModel - UI representation of recording option
 * V2 View Model (UI-specific) without V1 domain model dependencies
 */
@Serializable
data class RecordingOptionViewModel(
    val identifier: String,
    val source: String,
    val title: String,
    val rating: Float?,
    val reviewCount: Int?,
    val isSelected: Boolean,
    val isRecommended: Boolean,
    val matchReason: String?
)

/**
 * RecordingSelectionState - UI state for recording selection modal
 */
@Serializable
data class RecordingSelectionState(
    val isVisible: Boolean = false,
    val showTitle: String = "",
    val currentRecording: RecordingOptionViewModel? = null,
    val alternativeRecordings: List<RecordingOptionViewModel> = emptyList(),
    val hasRecommended: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * RecordingOptionsResult - Result from service containing recording options
 */
@Serializable
data class RecordingOptionsResult(
    val currentRecording: RecordingOptionViewModel?,
    val alternativeRecordings: List<RecordingOptionViewModel>,
    val hasRecommended: Boolean
)

/**
 * UI State for Playlist components
 * 
 * Comprehensive state model for all playlist UI components.
 */
data class PlaylistUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showData: PlaylistShowViewModel? = null,
    val trackData: List<PlaylistTrackViewModel> = emptyList(),
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isInLibrary: Boolean = false,
    // Progressive loading: spinner over track section only
    val isTrackListLoading: Boolean = false,
    // Review details modal state
    val showReviewDetails: Boolean = false,
    val reviewsLoading: Boolean = false,
    val reviews: List<PlaylistReview> = emptyList(),
    val ratingDistribution: Map<Int, Int> = emptyMap(),
    val reviewsError: String? = null,
    // Menu state
    val showMenu: Boolean = false,
    // Recording selection modal state
    val recordingSelection: RecordingSelectionState = RecordingSelectionState()
)

// === Archive Domain Models ===

/**
 * Track domain model for V2 Archive integration
 */
@Serializable
data class Track(
    val name: String,
    val title: String? = null,
    val trackNumber: Int? = null,
    val duration: String? = null,
    val format: String,
    val size: String? = null,
    val bitrate: String? = null,
    val sampleRate: String? = null,
    val isAudio: Boolean = true
)

/**
 * Review domain model for V2 Archive integration
 */
@Serializable
data class Review(
    val reviewer: String?,
    val title: String? = null,
    val body: String? = null,
    val rating: Int? = null,
    val reviewDate: String? = null
)

/**
 * Recording metadata domain model for V2 Archive integration
 */
@Serializable
data class RecordingMetadata(
    val identifier: String,
    val title: String,
    val date: String? = null,
    val venue: String? = null,
    val description: String? = null,
    val setlist: String? = null,
    val source: String? = null,
    val taper: String? = null,
    val transferer: String? = null,
    val lineage: String? = null,
    val totalTracks: Int = 0,
    val totalReviews: Int = 0
)