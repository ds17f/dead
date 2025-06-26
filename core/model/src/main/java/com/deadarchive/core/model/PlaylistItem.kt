package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val concertIdentifier: String,
    val track: Track,
    val position: Int,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    val id: String
        get() = "${concertIdentifier}_${track.filename}"
}