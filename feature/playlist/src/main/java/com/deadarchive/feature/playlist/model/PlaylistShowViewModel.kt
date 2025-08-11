package com.deadarchive.feature.playlist.model

/**
 * PlaylistShowViewModel - UI representation of show data
 * 
 * Clean architecture ViewModel representing show information as needed
 * by the playlist UI components. This isolates UI concerns from domain models.
 */
data class PlaylistShowViewModel(
    val date: String,
    val displayDate: String,
    val venue: String,
    val location: String,
    val rating: Float,
    val reviewCount: Int,
    val trackCount: Int,
    val hasNextShow: Boolean,
    val hasPreviousShow: Boolean,
    val isInLibrary: Boolean = false,
    val downloadProgress: Float? = null // null = not downloaded, 0.0-1.0 = downloading, 1.0 = complete
)