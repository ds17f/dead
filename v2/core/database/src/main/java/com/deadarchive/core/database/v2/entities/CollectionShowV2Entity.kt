package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_show_v2",
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["showId"]),
        Index(value = ["collectionId", "showId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CollectionV2Entity::class,
            parentColumns = ["collectionId"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionShowV2Entity(
    @PrimaryKey val id: String,        // collectionId + "_" + showId
    val collectionId: String,          // "acid-tests"
    val showId: String,                // "1966-01-08-fillmore-auditorium-san-francisco-ca-usa-early-show"
    val orderIndex: Int = 0            // Order within collection (for chronological display)
)