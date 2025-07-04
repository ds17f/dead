package com.deadarchive.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LibraryItem(
    val id: String,
    val type: LibraryItemType,
    val showId: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    companion object {
        fun fromShow(show: Show): LibraryItem {
            return LibraryItem(
                id = "show_${show.showId}",
                type = LibraryItemType.SHOW,
                showId = show.showId
            )
        }
    }
}

enum class LibraryItemType {
    SHOW
}