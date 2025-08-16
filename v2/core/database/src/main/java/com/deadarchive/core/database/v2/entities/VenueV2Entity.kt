package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues_v2")
data class VenueV2Entity(
    @PrimaryKey
    val venueId: String,          // Generated deterministic key
    
    // Venue identity
    val name: String,             // "Barton Hall, Cornell University"
    val normalizedName: String,   // "barton hall, cornell university" (for search)
    
    // Location hierarchy
    val city: String?,            // "Ithaca"
    val state: String?,           // "NY"
    val country: String = "USA",
    
    // Statistics (will be computed later)
    val showCount: Int = 0,
    val firstShowDate: String?,   // "1977-05-08"
    val lastShowDate: String?,    // "1995-07-09"
    
    // Timestamps
    val createdAt: Long,
    val updatedAt: Long
)