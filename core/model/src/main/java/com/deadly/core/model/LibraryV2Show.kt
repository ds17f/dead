package com.deadly.core.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a Show within the Library context.
 * Combines core concert data with library-specific metadata.
 * 
 * This model provides a clean separation between core Show data (immutable concert information)
 * and library-specific state (pin status, download status, library membership).
 */
@Serializable
data class LibraryV2Show(
    val show: Show,                                                      // Core concert data (immutable)
    val addedToLibraryAt: Long,                                         // When added to library
    val isPinned: Boolean = false,                                      // Library-specific pin status
    val downloadStatus: DownloadStatus = DownloadStatus.QUEUED  // Download state
) {
    // Delegate Show properties for convenient access
    val showId: String get() = show.showId
    val date: String get() = show.date
    val venue: String? get() = show.venue
    val location: String? get() = show.location
    val year: String? get() = show.year
    val displayTitle: String get() = show.displayTitle
    val displayLocation: String get() = show.displayLocation
    val displayVenue: String get() = show.displayVenue
    val displayDate: String get() = show.displayDate
    val recordings: List<Recording> get() = show.recordings
    val sets: List<ConcertSet> get() = show.sets
    val setlistRaw: String? get() = show.setlistRaw
    val rating: Float? get() = show.rating
    val rawRating: Float? get() = show.rawRating
    val bestRecording: Recording? get() = show.bestRecording
    val hasMultipleRecordings: Boolean get() = show.hasMultipleRecordings
    val recordingCount: Int get() = show.recordingCount
    val availableSources: List<String> get() = show.availableSources
    
    // Library-specific computed properties
    val isPinnedAndDownloaded: Boolean get() = isPinned && downloadStatus == DownloadStatus.COMPLETED
    val libraryAge: Long get() = System.currentTimeMillis() - addedToLibraryAt
    val isDownloaded: Boolean get() = downloadStatus == DownloadStatus.COMPLETED
    val isDownloading: Boolean get() = downloadStatus == DownloadStatus.DOWNLOADING
    
    // Library-specific display properties
    val libraryStatusDescription: String get() = when {
        isPinned && isDownloaded -> "Pinned & Downloaded"
        isPinned -> "Pinned"
        isDownloaded -> "Downloaded"
        isDownloading -> "Downloading..."
        else -> "In Library"
    }
    
    // Sorting helpers
    val sortableAddedDate: Long get() = addedToLibraryAt
    val sortableShowDate: String get() = show.date
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1 // 0 = pinned first in ascending sort
}