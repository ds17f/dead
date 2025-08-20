package com.deadly.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadly.core.model.Song
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Database entity for storing song data.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["songId"], unique = true),
        Index(value = ["name"]),
        Index(value = ["canonicalName"]),
        Index(value = ["category"]),
        Index(value = ["originalArtist"]),
        Index(value = ["timesPlayed"]),
        Index(value = ["isOriginal"])
    ]
)
data class SongEntity(
    @PrimaryKey
    val songId: String,
    val name: String,
    val aliasesJson: String? = null, // Serialized List<String>
    val variantsJson: String? = null, // Serialized List<String>
    val canonicalName: String?,
    val category: String?, // original, cover, jam, etc.
    val originalArtist: String?,
    val firstPerformed: String?, // YYYY-MM-DD format
    val lastPerformed: String?, // YYYY-MM-DD format
    val timesPlayed: Int?,
    val notes: String?,
    val isOriginal: Boolean = false, // Convenience field for querying
    val isCover: Boolean = false, // Convenience field for querying
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toSong(): Song {
        val aliases = aliasesJson?.let { json ->
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        val variants = variantsJson?.let { json ->
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return Song(
            songId = songId,
            name = name,
            aliases = aliases,
            variants = variants,
            canonicalName = canonicalName,
            category = category,
            originalArtist = originalArtist,
            firstPerformed = firstPerformed,
            lastPerformed = lastPerformed,
            timesPlayed = timesPlayed,
            notes = notes
        )
    }
    
    companion object {
        fun fromSong(song: Song): SongEntity {
            val aliasesJson = if (song.aliases.isNotEmpty()) {
                Json.encodeToString(song.aliases)
            } else null
            
            val variantsJson = if (song.variants.isNotEmpty()) {
                Json.encodeToString(song.variants)
            } else null
            
            return SongEntity(
                songId = song.songId,
                name = song.name,
                aliasesJson = aliasesJson,
                variantsJson = variantsJson,
                canonicalName = song.canonicalName,
                category = song.category,
                originalArtist = song.originalArtist,
                firstPerformed = song.firstPerformed,
                lastPerformed = song.lastPerformed,
                timesPlayed = song.timesPlayed,
                notes = song.notes,
                isOriginal = song.isOriginal,
                isCover = song.isCover
            )
        }
    }
}