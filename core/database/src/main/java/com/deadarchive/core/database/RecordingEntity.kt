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
            entity = ShowEntity::class,
            parentColumns = ["showId"],
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
    val concertId: String, // Foreign key to ShowEntity
    val concertDate: String, // YYYY-MM-DD format
    val concertVenue: String?,
    val concertLocation: String?, // City, State format
    
    // Audio content as JSON
    val tracksJson: String? = null,
    
    // UI state
    val isInLibrary: Boolean = false,
    val isDownloaded: Boolean = false,
    
    // Soft delete fields (recording-level)
    val isMarkedForDeletion: Boolean = false,
    val deletionTimestamp: Long? = null,
    
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
            concertLocation = concertLocation,
            tracks = tracks,
            audioFiles = emptyList(), // Will be populated from tracks
            isInLibrary = isInLibrary,
            isDownloaded = isDownloaded,
            isMarkedForDeletion = isMarkedForDeletion,
            deletionTimestamp = deletionTimestamp
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
                concertLocation = recording.concertLocation,
                tracksJson = tracksJson,
                isInLibrary = recording.isInLibrary,
                isDownloaded = recording.isDownloaded,
                isMarkedForDeletion = recording.isMarkedForDeletion,
                deletionTimestamp = recording.deletionTimestamp
            )
        }
    }
}

/**
 * Extension function to convert Recording to RecordingEntity
 */
fun Recording.toRecordingEntity(): RecordingEntity {
    // Calculate showId the same way Show class does, using normalized dates
    val normalizedDate = if (concertDate.contains("T")) {
        concertDate.substringBefore("T")
    } else {
        concertDate
    }
    val showId = "${normalizedDate}_${concertVenue?.replace(" ", "_")?.replace(",", "")?.replace("&", "and") ?: "Unknown"}"
    return RecordingEntity.fromRecording(this, showId)
}