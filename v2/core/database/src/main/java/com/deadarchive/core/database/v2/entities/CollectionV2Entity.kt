package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.deadarchive.v2.core.database.ConvertersV2

@Entity(
    tableName = "collection_v2",
    indices = [
        Index(value = ["collectionId"], unique = true),
        Index(value = ["name"]),
        Index(value = ["totalShows"])
    ]
)
@TypeConverters(ConvertersV2::class)
data class CollectionV2Entity(
    @PrimaryKey val collectionId: String,  // "acid-tests", "pigpen-years"
    val name: String,                      // "The Acid Tests", "The Pigpen Years"
    val description: String,               // Full description text
    val tags: List<String>,                // ["era", "early-dead", "psychedelic"]
    val totalShows: Int,                   // Number of shows in collection
    val aliases: List<String> = emptyList() // Alternative names/aliases
)