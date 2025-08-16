package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "venue_search_v2",
    indices = [
        Index(value = ["venueKey"]),
        Index(value = ["venueName"]),
        Index(value = ["city"]),
        Index(value = ["state"]),
        Index(value = ["country"]),
        Index(value = ["showId"]),
        Index(value = ["date"])
    ]
)
data class VenueSearchV2Entity(
    @PrimaryKey val id: String, // venueKey + "_" + showId
    val venueKey: String,       // normalized: "fillmore-auditorium"
    val venueName: String,      // display: "Fillmore Auditorium"
    val location: String,       // full: "San Francisco, CA, USA"
    val city: String,
    val state: String,
    val country: String,
    val showId: String,
    val date: String,
    val rating: Double,
    val rawRating: Double,
    val recordingCount: Int,
    val totalShows: Int,
    val firstShow: String,
    val lastShow: String
)