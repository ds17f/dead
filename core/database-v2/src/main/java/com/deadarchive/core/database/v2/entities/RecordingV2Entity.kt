package com.deadarchive.core.database.v2.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings_v2",
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
        Index(value = ["source_type"]),
        Index(value = ["rating"]),
        Index(value = ["date"]),
        Index(value = ["show_id", "rating"]) // For quality-based filtering per show
    ]
)
data class RecordingV2Entity(
    @PrimaryKey
    val identifier: String, // Archive.org unique identifier (e.g., "gd1977-05-08.sbd.miller.97375.sbeok.flac16")
    
    @ColumnInfo(name = "show_id")
    val showId: String,
    
    @ColumnInfo(name = "title")
    val title: String? = null, // Archive.org title
    
    @ColumnInfo(name = "source_type")
    val sourceType: String? = null, // "SBD", "AUD", "FM", "MATRIX", "REMASTER", "UNKNOWN"
    
    @ColumnInfo(name = "lineage")
    val lineage: String? = null, // Recording chain information
    
    @ColumnInfo(name = "taper")
    val taper: String? = null, // Person who recorded/transferred
    
    @ColumnInfo(name = "description")
    val description: String? = null, // Archive.org description
    
    @ColumnInfo(name = "date")
    val date: String, // Recording date (YYYY-MM-DD format)
    
    @ColumnInfo(name = "venue")
    val venue: String, // Venue name
    
    @ColumnInfo(name = "location")
    val location: String? = null, // City, State, Country
    
    // Quality metrics from Archive.org reviews
    @ColumnInfo(name = "rating")
    val rating: Double = 0.0, // Weighted rating for internal ranking (0.0-5.0)
    
    @ColumnInfo(name = "raw_rating")
    val rawRating: Double = 0.0, // Simple average for display (0.0-5.0)
    
    @ColumnInfo(name = "review_count")
    val reviewCount: Int = 0, // Number of reviews
    
    @ColumnInfo(name = "confidence")
    val confidence: Double = 0.0, // Rating confidence (0.0-1.0)
    
    @ColumnInfo(name = "high_ratings")
    val highRatings: Int = 0, // Count of 4-5★ reviews
    
    @ColumnInfo(name = "low_ratings")
    val lowRatings: Int = 0, // Count of 1-2★ reviews
    
    @ColumnInfo(name = "collection_timestamp")
    val collectionTimestamp: Long = System.currentTimeMillis()
)