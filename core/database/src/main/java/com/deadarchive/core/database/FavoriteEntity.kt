package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadarchive.core.model.FavoriteItem
import com.deadarchive.core.model.FavoriteType

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val type: String,
    val concertIdentifier: String,
    val trackFilename: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    fun toFavoriteItem(): FavoriteItem {
        return FavoriteItem(
            id = id,
            type = FavoriteType.valueOf(type),
            concertIdentifier = concertIdentifier,
            trackFilename = trackFilename,
            addedTimestamp = addedTimestamp,
            notes = notes
        )
    }
    
    companion object {
        fun fromFavoriteItem(favoriteItem: FavoriteItem): FavoriteEntity {
            return FavoriteEntity(
                id = favoriteItem.id,
                type = favoriteItem.type.name,
                concertIdentifier = favoriteItem.concertIdentifier,
                trackFilename = favoriteItem.trackFilename,
                addedTimestamp = favoriteItem.addedTimestamp,
                notes = favoriteItem.notes
            )
        }
    }
}