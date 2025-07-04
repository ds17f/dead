package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryItem(
    val id: String,
    val type: LibraryItemType,
    val recordingId: String,
    val trackFilename: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    companion object {
        fun fromRecording(recording: Recording): LibraryItem {
            return LibraryItem(
                id = "recording_${recording.identifier}",
                type = LibraryItemType.RECORDING,
                recordingId = recording.identifier
            )
        }
        
        fun fromTrack(recordingId: String, track: Track): LibraryItem {
            return LibraryItem(
                id = "track_${recordingId}_${track.filename}",
                type = LibraryItemType.TRACK,
                recordingId = recordingId,
                trackFilename = track.filename
            )
        }
    }
}

enum class LibraryItemType {
    CONCERT,
    RECORDING,
    TRACK
}