package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concerts")
data class ConcertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String?,
    val venue: String?,
    val isFavorite: Boolean = false
)