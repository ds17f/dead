package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    val id: String,
    val type: FavoriteType,
    val recordingId: String,
    val trackFilename: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    companion object {
        fun fromRecording(recording: Recording): FavoriteItem {
            return FavoriteItem(
                id = "recording_${recording.identifier}",
                type = FavoriteType.RECORDING,
                recordingId = recording.identifier
            )
        }
        
        fun fromTrack(recordingId: String, track: Track): FavoriteItem {
            return FavoriteItem(
                id = "track_${recordingId}_${track.filename}",
                type = FavoriteType.TRACK,
                recordingId = recordingId,
                trackFilename = track.filename
            )
        }
    }
}

enum class FavoriteType {
    CONCERT,
    RECORDING,
    TRACK
}