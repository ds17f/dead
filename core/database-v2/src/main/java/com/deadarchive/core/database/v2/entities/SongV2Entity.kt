package com.deadarchive.core.database.v2.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs_v2",
    indices = [
        Index(value = ["song_key"], unique = true),
        Index(value = ["song_name"]),
        Index(value = ["song_name", "song_key"])
    ]
)
data class SongV2Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "song_name")
    val songName: String, // Display name: "Dark Star", "Scarlet Begonias"
    
    @ColumnInfo(name = "song_key")
    val songKey: String, // Normalized key: "dark-star", "scarlet-begonias"
    
    @ColumnInfo(name = "song_url")
    val songUrl: String? = null // Optional jerrygarcia.com URL
)