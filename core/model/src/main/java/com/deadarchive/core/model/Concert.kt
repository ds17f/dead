package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Concert(
    @SerialName("identifier")
    val identifier: String,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("date")
    val date: String, // YYYY-MM-DD format
    
    @SerialName("venue")
    val venue: String? = null,
    
    @SerialName("coverage")
    val location: String? = null, // City, State format
    
    @SerialName("year")
    val year: String? = null,
    
    @SerialName("source")
    val source: String? = null, // SBD, AUD, FM, etc.
    
    @SerialName("taper")
    val taper: String? = null,
    
    @SerialName("transferer")
    val transferer: String? = null,
    
    @SerialName("lineage")
    val lineage: String? = null,
    
    @SerialName("description")
    val description: String? = null,
    
    @SerialName("setlist")
    val setlistRaw: String? = null,
    
    @SerialName("uploader")
    val uploader: String? = null,
    
    @SerialName("addeddate")
    val addedDate: String? = null,
    
    @SerialName("publicdate")
    val publicDate: String? = null,
    
    // Derived properties
    val sets: List<ConcertSet> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val audioFiles: List<AudioFile> = emptyList(),
    
    // UI state
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false
) {
    val displayTitle: String
        get() = title.takeIf { it.isNotBlank() } ?: "$venue - $date"
    
    val displayLocation: String
        get() = location ?: "Unknown Location"
    
    val displayVenue: String
        get() = venue ?: "Unknown Venue"
    
    val displaySource: String
        get() = source ?: "Unknown"
        
    val archiveUrl: String
        get() = "https://archive.org/details/$identifier"
}