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
    
    @SerialName("coverage")
    val concertLocation: String? = null, // City, State format
    
    // Audio content  
    val tracks: List<Track> = emptyList(),
    val audioFiles: List<AudioFile> = emptyList(),
    
    // UI state
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false
) {
    val displayTitle: String
        get() = if (concertVenue?.isNotBlank() == true && concertDate.isNotBlank()) {
            "$concertDate - $concertVenue"
        } else if (concertDate.isNotBlank()) {
            concertDate
        } else {
            title
        }
        
    val displaySource: String
        get() = source ?: "Unknown Source"
        
    val displayTaper: String
        get() = taper ?: "Unknown Taper"
        
    val archiveUrl: String
        get() = "https://archive.org/details/$identifier"
        
    val recordingQuality: String
        get() = when (cleanSource?.uppercase()) {
            "SBD" -> "Soundboard"
            "AUD" -> "Audience" 
            "FM" -> "FM Broadcast"
            "MATRIX" -> "Matrix"
            else -> cleanSource ?: "Unknown"
        }
    
    val cleanSource: String?
        get() {
            val clean = source?.replace(Regex("<[^>]*>"), "")?.trim()
            return when {
                clean == null -> null
                clean.contains("Soundboard", ignoreCase = true) -> "SBD"
                clean.startsWith("SBD", ignoreCase = true) -> "SBD"  // Handles "SBD -> Cassette Master"
                clean.contains("Audience", ignoreCase = true) -> "AUD"
                clean.startsWith("AUD", ignoreCase = true) -> "AUD"  // Handles "AUD -> ..."
                clean.contains("Matrix", ignoreCase = true) -> "MATRIX"
                clean.startsWith("MATRIX", ignoreCase = true) -> "MATRIX"
                clean.contains("FM", ignoreCase = true) -> "FM"
                clean.startsWith("FM", ignoreCase = true) -> "FM"
                else -> clean
            }
        }
}