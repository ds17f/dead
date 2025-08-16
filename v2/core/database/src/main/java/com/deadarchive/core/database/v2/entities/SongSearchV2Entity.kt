package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_search_v2",
    indices = [
        Index(value = ["songKey"]),
        Index(value = ["songName"]),
        Index(value = ["showId"]),
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["rating"])
    ]
)
data class SongSearchV2Entity(
    @PrimaryKey val id: String, // songKey + "_" + showId
    val songKey: String,        // normalized: "dark-star"
    val songName: String,       // display: "Dark Star"
    val showId: String,
    val date: String,
    val venue: String,
    val location: String,
    val setName: String?,
    val position: Int?,
    val segueIntoNext: Boolean,
    val rating: Double,
    val rawRating: Double
)