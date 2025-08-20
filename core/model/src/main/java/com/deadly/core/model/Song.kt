package com.deadly.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a song in the Grateful Dead catalog.
 */
@Serializable
data class Song(
    @SerialName("song_id")
    val songId: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("aliases")
    val aliases: List<String> = emptyList(),
    
    @SerialName("variants")
    val variants: List<String> = emptyList(),
    
    @SerialName("canonical_name")
    val canonicalName: String? = null,
    
    @SerialName("category")
    val category: String? = null, // original, cover, jam, etc.
    
    @SerialName("original_artist")
    val originalArtist: String? = null,
    
    @SerialName("first_performed")
    val firstPerformed: String? = null, // YYYY-MM-DD format
    
    @SerialName("last_performed")
    val lastPerformed: String? = null, // YYYY-MM-DD format
    
    @SerialName("times_played")
    val timesPlayed: Int? = null,
    
    @SerialName("notes")
    val notes: String? = null
) {
    /**
     * Get the primary display name for this song.
     */
    val displayName: String
        get() = canonicalName ?: name
    
    /**
     * Get all known names for this song (primary name + aliases + variants).
     */
    val allNames: Set<String>
        get() = setOf(name) + aliases + variants + listOfNotNull(canonicalName)
    
    /**
     * Check if this song is an original Grateful Dead composition.
     */
    val isOriginal: Boolean
        get() = category == "original" || originalArtist == null
    
    /**
     * Check if this song is a cover version.
     */
    val isCover: Boolean
        get() = category == "cover" || originalArtist != null
    
    /**
     * Get formatted performance period.
     */
    val performancePeriod: String
        get() = when {
            firstPerformed != null && lastPerformed != null -> {
                if (firstPerformed == lastPerformed) {
                    "Performed: ${formatDate(firstPerformed)}"
                } else {
                    "${formatDate(firstPerformed)} - ${formatDate(lastPerformed)}"
                }
            }
            firstPerformed != null -> "First: ${formatDate(firstPerformed)}"
            lastPerformed != null -> "Last: ${formatDate(lastPerformed)}"
            else -> "Performance dates unknown"
        }
    
    /**
     * Get performance frequency description.
     */
    val performanceFrequency: String
        get() = when (timesPlayed) {
            null -> "Unknown frequency"
            0 -> "Never performed"
            1 -> "Performed once"
            in 2..5 -> "Rarely performed ($timesPlayed times)"
            in 6..20 -> "Occasionally performed ($timesPlayed times)"
            in 21..50 -> "Regularly performed ($timesPlayed times)"
            in 51..100 -> "Frequently performed ($timesPlayed times)"
            else -> "Very frequently performed ($timesPlayed times)"
        }
    
    /**
     * Check if this song matches a given search term.
     */
    fun matchesSearch(searchTerm: String): Boolean {
        val term = searchTerm.lowercase()
        return allNames.any { it.lowercase().contains(term) } ||
               originalArtist?.lowercase()?.contains(term) == true
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