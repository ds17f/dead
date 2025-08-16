package com.deadarchive.v2.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "member_search_v2",
    indices = [
        Index(value = ["memberKey"]),
        Index(value = ["memberName"]),
        Index(value = ["showId"]),
        Index(value = ["date"]),
        Index(value = ["venue"]),
        Index(value = ["instruments"])
    ]
)
data class MemberSearchV2Entity(
    @PrimaryKey val id: String, // memberKey + "_" + showId
    val memberKey: String,      // normalized: "jerry-garcia"
    val memberName: String,     // display: "Jerry Garcia"
    val showId: String,
    val date: String,
    val venue: String,
    val location: String,
    val instruments: String,    // comma-separated: "guitar, vocals"
    val rating: Double,
    val totalShows: Int,
    val firstShow: String,
    val lastShow: String,
    val primaryInstruments: String  // comma-separated primary instruments
)