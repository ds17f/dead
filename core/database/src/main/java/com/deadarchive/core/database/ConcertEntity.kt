package com.deadarchive.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.Track

@Entity(tableName = "concerts")
data class ConcertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String?,
    val venue: String?,
    val location: String?,
    val year: String?,
    val source: String?,
    val taper: String?,
    val transferer: String?,
    val lineage: String?,
    val description: String?,
    val setlistRaw: String?,
    val uploader: String?,
    val addedDate: String?,
    val publicDate: String?,
    val tracksJson: String? = null,
    val isFavorite: Boolean = false,
    val cachedTimestamp: Long = System.currentTimeMillis()
) {
    fun toConcert(): Concert {
        // Parse tracks from JSON
        val tracks = try {
            tracksJson?.let { 
                kotlinx.serialization.json.Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Track.serializer()), it) 
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        return Concert(
            identifier = id,
            title = title,
            date = date ?: "",
            venue = venue,
            location = location,
            year = year,
            source = source,
            taper = taper,
            transferer = transferer,
            lineage = lineage,
            description = description,
            setlistRaw = setlistRaw,
            uploader = uploader,
            addedDate = addedDate,
            publicDate = publicDate,
            tracks = tracks,
            isFavorite = isFavorite
        )
    }
    
    companion object {
        fun fromConcert(concert: Concert, isFavorite: Boolean = false): ConcertEntity {
            // Serialize tracks to JSON
            val tracksJson = try {
                if (concert.tracks.isNotEmpty()) {
                    kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Track.serializer()), concert.tracks)
                } else null
            } catch (e: Exception) {
                null
            }
            
            return ConcertEntity(
                id = concert.identifier,
                title = concert.title,
                date = concert.date.takeIf { it.isNotBlank() },
                venue = concert.venue,
                location = concert.location,
                year = concert.year,
                source = concert.source,
                taper = concert.taper,
                transferer = concert.transferer,
                lineage = concert.lineage,
                description = concert.description,
                setlistRaw = concert.setlistRaw,
                uploader = concert.uploader,
                addedDate = concert.addedDate,
                publicDate = concert.publicDate,
                tracksJson = tracksJson,
                isFavorite = isFavorite
            )
        }
    }
}