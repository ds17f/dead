package com.deadarchive.core.model.util

/**
 * Utility functions for venue name normalization.
 * Used to ensure consistent venue naming across the application.
 */
object VenueUtil {
    
    /**
     * Normalize venue name to eliminate duplicates caused by inconsistent venue names.
     * This function ensures that venue names are consistently formatted for use in showIds
     * and database relationships.
     * 
     * Used by:
     * - Show.showId property
     * - ShowEntity.generateShowId()
     * - ShowRepository venue processing
     * - Recording entity creation
     */
    fun normalizeVenue(venue: String?): String {
        if (venue.isNullOrBlank()) return "Unknown"
        
        return venue
            // Remove punctuation that causes issues
            .replace("'", "")      // Veterans' -> Veterans
            .replace("'", "")      // Smart quote
            .replace(".", "")      // U.C.S.B. -> UCSB
            .replace("\"", "")     // Remove quotes
            .replace("(", "_")     // Convert parens to underscores
            .replace(")", "_")
            
            // Normalize separators
            .replace(" - ", "_")   // Common separator
            .replace(" – ", "_")   // Em dash
            .replace(", ", "_")    // Comma separator
            .replace(" & ", "_and_")
            .replace("&", "_and_")
            
            // Standardize common word variations
            .replace("Theatre", "Theater", ignoreCase = true)
            .replace("Center", "Center", ignoreCase = true)  // Keep consistent
            .replace("Coliseum", "Coliseum", ignoreCase = true)
            
            // University abbreviations (most common cases)
            .replace(" University", "_U", ignoreCase = true)
            .replace(" College", "_C", ignoreCase = true)
            .replace(" State", "_St", ignoreCase = true)
            .replace("Memorial", "Mem", ignoreCase = true)
            .replace("Auditorium", "Aud", ignoreCase = true)
            .replace("Stadium", "Stad", ignoreCase = true)
            
            // Remove common filler words
            .replace(" The ", "_", ignoreCase = true)
            .replace("The ", "", ignoreCase = true)
            .replace(" of ", "_", ignoreCase = true)
            .replace(" at ", "_", ignoreCase = true)
            
            // Clean up and normalize
            .replace(Regex("\\s+"), "_")     // Any whitespace to underscore
            .replace(Regex("_+"), "_")       // Multiple underscores to single
            .trim('_')                       // Remove leading/trailing underscores
            .lowercase()                     // Consistent case
    }
}