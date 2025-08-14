package com.deadarchive.core.database.v2.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shows_v2",
    foreignKeys = [
        ForeignKey(
            entity = VenueV2Entity::class,
            parentColumns = ["venueId"],
            childColumns = ["venueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["venueId"]),
        Index(value = ["date"]),
        Index(value = ["year"]),
        Index(value = ["yearMonth"])
    ]
)
data class ShowV2Entity(
    @PrimaryKey
    val showId: String,           // "1977-05-08-barton-hall-cornell-u-ithaca-ny-usa"
    
    // Date components for flexible searching
    val date: String,             // "1977-05-08" (full date)
    val year: Int,                // 1977 (indexed)
    val month: Int,               // 5 (indexed)
    val yearMonth: String,        // "1977-05" (indexed)
    
    // Show metadata
    val band: String,             // "Grateful Dead"
    val url: String?,             // Jerry Garcia URL
    
    // Location (denormalized for fast search + FK)
    val venueId: String,          // FK to venues_v2
    val city: String?,            // Denormalized for fast search
    val state: String?,           // Denormalized for fast search
    val country: String = "USA",
    val locationRaw: String?,     // "Ithaca, NY" (original)
    
    // Setlist data
    val setlistStatus: String?,   // "found", "not_found", etc.
    val setlistRaw: String?,      // JSON string of full setlist
    val songList: String?,        // "Scarlet Begonias,Fire on the Mountain" (comma-separated for LIKE queries)
    
    // Multiple shows same date/venue (rare but happens)
    val showSequence: Int = 1,    // 1, 2, 3... for multiple shows
    
    // Computed/cached stats (will be populated later)
    val recordingCount: Int = 0,
    val bestRecordingId: String?,
    val averageRating: Float?,
    val totalReviews: Int = 0,
    
    // Library status (will be used by V2 features later)
    val isInLibrary: Boolean = false,
    val libraryAddedAt: Long?,
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)