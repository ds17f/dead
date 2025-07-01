package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = ConcertNewEntity::class,
            parentColumns = ["concertId"],
            childColumns = ["concertId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["concertId"]),
        Index(value = ["source"]),
        Index(value = ["taper"])
    ]
)
data class RecordingEntity(
    @PrimaryKey
    val identifier: String, // Archive.org recording identifier
    
    val title: String,
    
    // Recording-specific metadata
    val source: String?,
    val taper: String?,
    val transferer: String?,
    val lineage: String?,
    val description: String?,
    
    // Archive.org upload metadata
    val uploader: String?,
    val addedDate: String?,
    val publicDate: String?,
    
    // Concert reference
    val concertId: String, // Foreign key to ConcertNewEntity
    val concertDate: String, // YYYY-MM-DD format
    val concertVenue: String?,
    
    // Audio content as JSON
    val tracksJson: String? = null,
    
    // UI state
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    
    // Cache management
    val cachedTimestamp: Long = System.currentTimeMillis()
) {
    fun toRecording(): Recording {
        val tracks = tracksJson?.let { json ->
            try {
                Json.decodeFromString<List<Track>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return Recording(
            identifier = identifier,
            title = title,
            source = source,
            taper = taper,
            transferer = transferer,
            lineage = lineage,
            description = description,
            uploader = uploader,
            addedDate = addedDate,
            publicDate = publicDate,
            concertDate = concertDate,
            concertVenue = concertVenue,
            tracks = tracks,
            audioFiles = emptyList(), // Will be populated from tracks
            isFavorite = isFavorite,
            isDownloaded = isDownloaded
        )
    }
    
    companion object {
        fun fromRecording(recording: Recording, concertId: String): RecordingEntity {
            val tracksJson = if (recording.tracks.isNotEmpty()) {
                Json.encodeToString(recording.tracks)
            } else null
            
            return RecordingEntity(
                identifier = recording.identifier,
                title = recording.title,
                source = recording.source,
                taper = recording.taper,
                transferer = recording.transferer,
                lineage = recording.lineage,
                description = recording.description,
                uploader = recording.uploader,
                addedDate = recording.addedDate,
                publicDate = recording.publicDate,
                concertId = concertId,
                concertDate = recording.concertDate,
                concertVenue = recording.concertVenue,
                tracksJson = tracksJson,
                isFavorite = recording.isFavorite,
                isDownloaded = recording.isDownloaded
            )
        }
    }
}