package com.deadarchive.core.database.v2.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "setlist_songs_v2",
    foreignKeys = [
        ForeignKey(
            entity = SetlistV2Entity::class,
            parentColumns = ["id"],
            childColumns = ["setlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongV2Entity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["setlist_id"]),
        Index(value = ["song_id"]),
        Index(value = ["setlist_id", "position"]),
        Index(value = ["song_id", "setlist_id"]) // For song-in-show queries
    ]
)
data class SetlistSongV2Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "setlist_id")
    val setlistId: Long,
    
    @ColumnInfo(name = "song_id")
    val songId: Long,
    
    @ColumnInfo(name = "position")
    val position: Int, // 1-based position within set
    
    @ColumnInfo(name = "segue_into_next")
    val segueIntoNext: Boolean = false // Critical for Dead shows!
)