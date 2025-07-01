package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recording(
    @SerialName("identifier")
    val identifier: String, // Archive.org recording identifier
    
    @SerialName("title")
    val title: String,
    
    // Recording-specific metadata
    @SerialName("source")
    val source: String? = null, // SBD, AUD, FM, etc.
    
    @SerialName("taper")
    val taper: String? = null, // Person who recorded the show
    
    @SerialName("transferer")
    val transferer: String? = null, // Person who transferred the recording
    
    @SerialName("lineage")
    val lineage: String? = null, // Equipment chain used for recording
    
    @SerialName("description")
    val description: String? = null, // Recording-specific description
    
    // Archive.org upload metadata
    @SerialName("uploader")
    val uploader: String? = null,
    
    @SerialName("addeddate")
    val addedDate: String? = null,
    
    @SerialName("publicdate")
    val publicDate: String? = null,
    
    // Concert reference
    val concertDate: String, // YYYY-MM-DD format - links to Concert
    val concertVenue: String? = null, // For grouping recordings by concert
    
    // Audio content
    val tracks: List<Track> = emptyList(),
    val audioFiles: List<AudioFile> = emptyList(),
    
    // UI state
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false
) {
    val displaySource: String
        get() = source ?: "Unknown Source"
        
    val displayTaper: String
        get() = taper ?: "Unknown Taper"
        
    val archiveUrl: String
        get() = "https://archive.org/details/$identifier"
        
    val recordingQuality: String
        get() = when (source?.uppercase()) {
            "SBD" -> "Soundboard"
            "AUD" -> "Audience"
            "FM" -> "FM Broadcast"
            "MATRIX" -> "Matrix"
            else -> source ?: "Unknown"
        }
}