package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    val id: String,
    val type: FavoriteType,
    val concertIdentifier: String,
    val trackFilename: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    companion object {
        fun fromConcert(concert: Concert): FavoriteItem {
            return FavoriteItem(
                id = "concert_${concert.identifier}",
                type = FavoriteType.CONCERT,
                concertIdentifier = concert.identifier
            )
        }
        
        fun fromTrack(concertIdentifier: String, track: Track): FavoriteItem {
            return FavoriteItem(
                id = "track_${concertIdentifier}_${track.filename}",
                type = FavoriteType.TRACK,
                concertIdentifier = concertIdentifier,
                trackFilename = track.filename
            )
        }
    }
}

enum class FavoriteType {
    CONCERT,
    TRACK
}