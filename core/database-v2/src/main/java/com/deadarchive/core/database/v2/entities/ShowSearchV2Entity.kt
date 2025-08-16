package com.deadarchive.core.database.v2.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.deadarchive.core.database.v2.ConvertersV2

@Entity(
    tableName = "show_search_v2",
    indices = [
        Index(value = ["showId"]),
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["city"]),
        Index(value = ["state"]),
        Index(value = ["country"]),
        Index(value = ["year"]),
        Index(value = ["month"]),
        Index(value = ["rating"]),
        Index(value = ["searchText"])
    ]
)
@TypeConverters(ConvertersV2::class)
data class ShowSearchV2Entity(
    @PrimaryKey val showId: String,
    val date: String,
    val venue: String,
    val location: String,
    val city: String,
    val state: String,
    val country: String,
    val band: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val rating: Double,
    val rawRating: Double,
    val recordingCount: Int,
    val songCount: Int,
    val hasSetlist: Boolean,
    val collections: List<String>,  // Will use Converters for List<String>
    val searchText: String          // Pre-computed search text for full-text search
)