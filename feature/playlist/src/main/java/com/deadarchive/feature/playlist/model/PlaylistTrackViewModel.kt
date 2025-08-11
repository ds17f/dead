package com.deadarchive.feature.playlist.model

/**
 * PlaylistTrackViewModel - UI representation of track data
 * 
 * Clean architecture ViewModel representing track information as needed
 * by the playlist UI components. This isolates UI concerns from domain models.
 */
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