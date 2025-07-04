package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.LibraryItemType

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val recordingId: String,
    val trackFilename: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    fun toLibraryItem(): LibraryItem {
        return LibraryItem(
            id = id,
            type = LibraryItemType.valueOf(type),
            recordingId = recordingId,
            trackFilename = trackFilename,
            addedTimestamp = addedTimestamp,
            notes = notes
        )
    }
    
    companion object {
        fun fromLibraryItem(libraryItem: LibraryItem): LibraryEntity {
            return LibraryEntity(
                id = libraryItem.id,
                type = libraryItem.type.name,
                recordingId = libraryItem.recordingId,
                trackFilename = libraryItem.trackFilename,
                addedTimestamp = libraryItem.addedTimestamp,
                notes = libraryItem.notes
            )
        }
    }
}