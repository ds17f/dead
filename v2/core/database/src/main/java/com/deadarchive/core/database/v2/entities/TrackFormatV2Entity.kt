package com.deadarchive.v2.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_formats_v2",
    foreignKeys = [
        ForeignKey(
            entity = TrackV2Entity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["track_id"]),
        Index(value = ["format"]),
        Index(value = ["track_id", "format"], unique = true) // Ensure unique track-format combinations
    ]
)
data class TrackFormatV2Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "track_id")
    val trackId: Long, // References TrackV2Entity.id
    
    @ColumnInfo(name = "format")
    val format: String, // "Flac", "VBR MP3", "Ogg Vorbis", etc.
    
    @ColumnInfo(name = "filename")
    val filename: String, // Archive.org filename for this format
    
    @ColumnInfo(name = "bitrate")
    val bitrate: String? = null // For compressed formats (e.g., "192", "256") - null for lossless
)