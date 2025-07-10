package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadarchive.core.model.Setlist
import com.deadarchive.core.model.SetlistSong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Database entity for storing setlist data.
 */
@Entity(
    tableName = "setlists",
    indices = [
        Index(value = ["showId"], unique = true),
        Index(value = ["date"]),
        Index(value = ["venueId"]),
        Index(value = ["source"]),
        Index(value = ["hasSongs"])
    ]
)
data class SetlistEntity(
    @PrimaryKey
    val showId: String,
    val date: String, // YYYY-MM-DD format
    val venueId: String?,
    val venueLine: String?,
    val source: String, // "cmu" or "gdsets"
    val setsJson: String? = null, // Serialized Map<String, List<String>>
    val songsJson: String? = null, // Serialized List<SetlistSong>
    val rawContent: String?,
    val cmuRawContent: String?,
    val cmuVenueLine: String?,
    val hasSongs: Boolean = false, // Convenience field for querying
    val totalSongs: Int = 0, // Convenience field for querying
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toSetlist(): Setlist {
        val sets = setsJson?.let { json ->
            try {
                Json.decodeFromString<Map<String, List<String>>>(json)
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
        
        val songs = songsJson?.let { json ->
            try {
                Json.decodeFromString<List<SetlistSong>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return Setlist(
            showId = showId,
            date = date,
            venueId = venueId,
            venueLine = venueLine,
            source = source,
            sets = sets,
            songs = songs,
            rawContent = rawContent,
            cmuRawContent = cmuRawContent,
            cmuVenueLine = cmuVenueLine
        )
    }
    
    companion object {
        fun fromSetlist(setlist: Setlist): SetlistEntity {
            val setsJson = if (setlist.sets.isNotEmpty()) {
                Json.encodeToString(setlist.sets)
            } else null
            
            val songsJson = if (setlist.songs.isNotEmpty()) {
                Json.encodeToString(setlist.songs)
            } else null
            
            return SetlistEntity(
                showId = setlist.showId,
                date = setlist.date,
                venueId = setlist.venueId,
                venueLine = setlist.venueLine,
                source = setlist.source,
                setsJson = setsJson,
                songsJson = songsJson,
                rawContent = setlist.rawContent,
                cmuRawContent = setlist.cmuRawContent,
                cmuVenueLine = setlist.cmuVenueLine,
                hasSongs = setlist.hasSongs,
                totalSongs = setlist.totalSongs
            )
        }
    }
}