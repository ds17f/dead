package com.deadarchive.v2.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "setlists_v2",
    foreignKeys = [
        ForeignKey(
            entity = ShowV2Entity::class,
            parentColumns = ["showId"],
            childColumns = ["show_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["show_id"]),
        Index(value = ["show_id", "set_order"])
    ]
)
data class SetlistV2Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "show_id")
    val showId: String,
    
    @ColumnInfo(name = "set_name")
    val setName: String, // "Set 1", "Set 2", "Encore"
    
    @ColumnInfo(name = "set_order")
    val setOrder: Int // Order of sets within show (0-based)
)