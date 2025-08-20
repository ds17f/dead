package com.deadly.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a venue where the Grateful Dead performed.
 */
@Serializable
data class Venue(
    @SerialName("venue_id")
    val venueId: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("aliases")
    val aliases: List<String> = emptyList(),
    
    @SerialName("normalized_name")
    val normalizedName: String? = null,
    
    @SerialName("city")
    val city: String? = null,
    
    @SerialName("state")
    val state: String? = null,
    
    @SerialName("country")
    val country: String? = null,
    
    @SerialName("full_location")
    val fullLocation: String? = null,
    
    @SerialName("venue_type")
    val venueType: String? = null, // theater, arena, outdoor, etc.
    
    @SerialName("capacity")
    val capacity: Int? = null,
    
    @SerialName("first_show")
    val firstShow: String? = null, // YYYY-MM-DD format
    
    @SerialName("last_show")
    val lastShow: String? = null, // YYYY-MM-DD format
    
    @SerialName("total_shows")
    val totalShows: Int? = null,
    
    @SerialName("notes")
    val notes: String? = null
) {
    /**
     * Get the primary display name for this venue.
     */
    val displayName: String
        get() = normalizedName ?: name
    
    /**
     * Get all known names for this venue (primary name + aliases + normalized).
     */
    val allNames: Set<String>
        get() = setOf(name) + aliases + listOfNotNull(normalizedName)
    
    /**
     * Get formatted location string.
     */
    val displayLocation: String
        get() = when {
            fullLocation != null -> fullLocation
            city != null && state != null && country != null -> {
                if (country == "USA") "$city, $state" else "$city, $state, $country"
            }
            city != null && state != null -> "$city, $state"
            city != null && country != null -> {
                if (country == "USA") city else "$city, $country"
            }
            city != null -> city
            state != null -> state
            country != null -> country
            else -> "Location unknown"
        }
    
    /**
     * Get complete venue description with location.
     */
    val fullDescription: String
        get() = "$displayName - $displayLocation"
    
    /**
     * Get performance period for this venue.
     */
    val performancePeriod: String
        get() = when {
            firstShow != null && lastShow != null -> {
                if (firstShow == lastShow) {
                    "Performed: ${formatDate(firstShow)}"
                } else {
                    "${formatDate(firstShow)} - ${formatDate(lastShow)}"
                }
            }
            firstShow != null -> "First show: ${formatDate(firstShow)}"
            lastShow != null -> "Last show: ${formatDate(lastShow)}"
            else -> "Performance dates unknown"
        }
    
    /**
     * Get show frequency description.
     */
    val showFrequency: String
        get() = when (totalShows) {
            null -> "Unknown show count"
            0 -> "No shows recorded"
            1 -> "1 show"
            in 2..5 -> "Few shows ($totalShows)"
            in 6..15 -> "Several shows ($totalShows)"
            in 16..30 -> "Many shows ($totalShows)"
            else -> "Frequent venue ($totalShows shows)"
        }
    
    /**
     * Check if this is a US venue.
     */
    val isUSVenue: Boolean
        get() = country == "USA" || country == null && state != null
    
    /**
     * Check if this is an international venue.
     */
    val isInternational: Boolean
        get() = country != null && country != "USA"
    
    /**
     * Get venue category based on type and capacity.
     */
    val venueCategory: String
        get() = when {
            venueType != null -> venueType.lowercase()
            capacity != null -> when {
                capacity < 1000 -> "small venue"
                capacity < 5000 -> "medium venue"
                capacity < 15000 -> "large venue"
                else -> "arena"
            }
            else -> "venue"
        }
    
    /**
     * Check if this venue matches a given search term.
     */
    fun matchesSearch(searchTerm: String): Boolean {
        val term = searchTerm.lowercase()
        return allNames.any { it.lowercase().contains(term) } ||
               city?.lowercase()?.contains(term) == true ||
               state?.lowercase()?.contains(term) == true ||
               country?.lowercase()?.contains(term) == true ||
               fullLocation?.lowercase()?.contains(term) == true
    }
    
    /**
     * Format date for display.
     */
    private fun formatDate(date: String): String {
        return try {
            val parts = date.split("-")
            "${parts[1]}/${parts[2]}/${parts[0]}"
        } catch (e: Exception) {
            date
        }
    }
}