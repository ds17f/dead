package com.deadly.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.deadly.core.model.Venue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Database entity for storing venue data.
 */
@Entity(
    tableName = "venues",
    indices = [
        Index(value = ["venueId"], unique = true),
        Index(value = ["name"]),
        Index(value = ["normalizedName"]),
        Index(value = ["city"]),
        Index(value = ["state"]),
        Index(value = ["country"]),
        Index(value = ["venueType"]),
        Index(value = ["totalShows"]),
        Index(value = ["isUSVenue"]),
        Index(value = ["isInternational"])
    ]
)
data class VenueEntity(
    @PrimaryKey
    val venueId: String,
    val name: String,
    val aliasesJson: String? = null, // Serialized List<String>
    val normalizedName: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val fullLocation: String?,
    val venueType: String?, // theater, arena, outdoor, etc.
    val capacity: Int?,
    val firstShow: String?, // YYYY-MM-DD format
    val lastShow: String?, // YYYY-MM-DD format
    val totalShows: Int?,
    val notes: String?,
    val isUSVenue: Boolean = false, // Convenience field for querying
    val isInternational: Boolean = false, // Convenience field for querying
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toVenue(): Venue {
        val aliases = aliasesJson?.let { json ->
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
        
        return Venue(
            venueId = venueId,
            name = name,
            aliases = aliases,
            normalizedName = normalizedName,
            city = city,
            state = state,
            country = country,
            fullLocation = fullLocation,
            venueType = venueType,
            capacity = capacity,
            firstShow = firstShow,
            lastShow = lastShow,
            totalShows = totalShows,
            notes = notes
        )
    }
    
    companion object {
        fun fromVenue(venue: Venue): VenueEntity {
            val aliasesJson = if (venue.aliases.isNotEmpty()) {
                Json.encodeToString(venue.aliases)
            } else null
            
            return VenueEntity(
                venueId = venue.venueId,
                name = venue.name,
                aliasesJson = aliasesJson,
                normalizedName = venue.normalizedName,
                city = venue.city,
                state = venue.state,
                country = venue.country,
                fullLocation = venue.fullLocation,
                venueType = venue.venueType,
                capacity = venue.capacity,
                firstShow = venue.firstShow,
                lastShow = venue.lastShow,
                totalShows = venue.totalShows,
                notes = venue.notes,
                isUSVenue = venue.isUSVenue,
                isInternational = venue.isInternational
            )
        }
    }
}