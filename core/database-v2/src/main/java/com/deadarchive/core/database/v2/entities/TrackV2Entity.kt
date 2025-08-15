package com.deadarchive.core.database.v2.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks_v2",
    foreignKeys = [
        ForeignKey(
            entity = RecordingV2Entity::class,
            parentColumns = ["identifier"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recording_id"]),
        Index(value = ["recording_id", "track_number"]),
        Index(value = ["title"]),
        Index(value = ["duration"])
    ]
)
data class TrackV2Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "recording_id")
    val recordingId: String, // References RecordingV2Entity.identifier
    
    @ColumnInfo(name = "track_number")
    val trackNumber: String, // "01", "02", etc. (String to handle edge cases like "01a")
    
    @ColumnInfo(name = "title")
    val title: String, // Song/track title (e.g., "Dark Star", "Tuning")
    
    @ColumnInfo(name = "duration")
    val duration: Double? = null // Duration in seconds (nullable for unknown durations)
)