package com.deadarchive.v2.core.model

import kotlinx.serialization.Serializable

/**
 * V2 Show domain model components
 * 
 * Value objects that compose the Show domain model.
 */

@Serializable
data class Venue(
    val name: String,
    val city: String?,
    val state: String?,
    val country: String
) {
    val displayLocation: String
        get() = listOfNotNull(city, state, country.takeIf { it != "USA" })
            .joinToString(", ")
}

@Serializable
data class Location(
    val displayText: String,
    val city: String?,
    val state: String?
) {
    companion object {
        fun fromRaw(raw: String?, city: String?, state: String?): Location {
            val display = raw ?: listOfNotNull(city, state).joinToString(", ").ifEmpty { "Unknown Location" }
            return Location(display, city, state)
        }
    }
}

@Serializable
data class Setlist(
    val status: String,
    val sets: List<SetlistSet>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Setlist? {
            if (json.isNullOrBlank() || status.isNullOrBlank()) return null
            
            // For now, return a simple structure
            // TODO: Implement full JSON parsing when needed
            return Setlist(
                status = status,
                sets = emptyList(),
                raw = json
            )
        }
    }
}

@Serializable
data class SetlistSet(
    val name: String,
    val songs: List<String>
)

@Serializable
data class Lineup(
    val status: String,
    val members: List<LineupMember>,
    val raw: String?
) {
    companion object {
        fun parse(json: String?, status: String?): Lineup? {
            if (json.isNullOrBlank() || status.isNullOrBlank()) return null
            
            // For now, return a simple structure
            // TODO: Implement full JSON parsing when needed
            return Lineup(
                status = status,
                members = emptyList(),
                raw = json
            )
        }
    }
}

@Serializable
data class LineupMember(
    val name: String,
    val instruments: String
)