package com.deadly.core.data.service

import android.util.Log
import com.deadly.core.model.AppConstants
import com.deadly.core.model.Track
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for filtering audio tracks based on user preferences
 * Ensures only the highest-ranked format is shown for each unique song
 */
@Singleton
class AudioFormatFilterService @Inject constructor() {
    
    companion object {
        private const val TAG = "AudioFormatFilter"
    }
    
    /**
     * Filter tracks to show only the highest-ranked format for each unique song
     * @param tracks List of all available tracks
     * @param formatPreferences User's preferred format order (e.g., ["Ogg Vorbis", "VBR MP3", "MP3", "Flac"])
     * @return Filtered list with one track per song using the best available format
     */
    fun filterTracksByPreferredFormat(
        tracks: List<Track>,
        formatPreferences: List<String>
    ): List<Track> {
        if (tracks.isEmpty() || formatPreferences.isEmpty()) {
            return tracks
        }
        
        Log.d(TAG, "Filtering ${tracks.size} tracks with preferences: $formatPreferences")
        
        // Group tracks by song identifier
        val trackGroups = groupTracksBySong(tracks)
        Log.d(TAG, "Grouped into ${trackGroups.size} unique songs")
        
        // Select best format for each song
        val filteredTracks = trackGroups.mapNotNull { (songId, songTracks) ->
            selectBestFormatForSong(songTracks, formatPreferences).also { selectedTrack ->
                if (selectedTrack != null) {
                    Log.d(TAG, "Song '$songId': Selected ${selectedTrack.audioFile?.format} from ${songTracks.size} options")
                }
            }
        }
        
        Log.d(TAG, "Filtered to ${filteredTracks.size} tracks")
        return filteredTracks.sortedBy { it.trackNumber?.toIntOrNull() ?: 0 }
    }
    
    /**
     * Group tracks by song identifier extracted from filename
     * This handles cases where the same song exists in multiple formats
     */
    private fun groupTracksBySong(tracks: List<Track>): Map<String, List<Track>> {
        return tracks.groupBy { track ->
            extractSongIdentifier(track.filename)
        }
    }
    
    /**
     * Extract a song identifier from filename to group different formats of the same song
     * Examples:
     * - "gd77-05-08d1t01.flac" -> "gd77-05-08d1t01"
     * - "gd1977-05-08d1t01.ogg" -> "gd1977-05-08d1t01"
     * - "Dark Star.mp3" -> "dark star"
     */
    private fun extractSongIdentifier(filename: String): String {
        return filename
            .substringBeforeLast(".") // Remove file extension
            .lowercase()
            .replace(Regex("[^a-z0-9\\-_]"), "_") // Normalize special characters
            .replace(Regex("_+"), "_") // Collapse multiple underscores
            .trim('_')
    }
    
    /**
     * Select the best format for a song based on user preferences
     * @param songTracks All tracks for the same song in different formats
     * @param formatPreferences User's preferred format order
     * @return Track with the highest-ranked format, or null if no valid tracks
     */
    private fun selectBestFormatForSong(
        songTracks: List<Track>,
        formatPreferences: List<String>
    ): Track? {
        if (songTracks.isEmpty()) return null
        if (songTracks.size == 1) return songTracks.first()
        
        // Rank tracks by format preference (lower index = higher preference)
        val rankedTracks = songTracks.mapNotNull { track ->
            val format = track.audioFile?.format
            if (format != null) {
                val rank = getFormatRank(format, formatPreferences)
                RankedTrack(track, rank, format)
            } else null
        }
        
        if (rankedTracks.isEmpty()) {
            return songTracks.first() // Fallback to first track if no format info
        }
        
        // Sort by rank (lower is better), then by quality indicators
        val bestTrack = rankedTracks
            .sortedWith(compareBy<RankedTrack> { it.rank }
                .thenByDescending { getQualityScore(it.format) })
            .first()
        
        Log.d(TAG, "Selected format '${bestTrack.format}' (rank ${bestTrack.rank}) from options: ${rankedTracks.map { "${it.format}:${it.rank}" }}")
        
        return bestTrack.track
    }
    
    /**
     * Get format rank from user preferences (lower number = higher preference)
     * @param format Audio format string from track
     * @param formatPreferences User's ordered preferences
     * @return Rank index (0 = most preferred), or high number if not found
     */
    private fun getFormatRank(format: String, formatPreferences: List<String>): Int {
        val normalizedFormat = normalizeFormat(format)
        
        formatPreferences.forEachIndexed { index, preference ->
            if (formatMatches(normalizedFormat, preference)) {
                return index
            }
        }
        
        // If format not found in preferences, assign high rank (low priority)
        return formatPreferences.size + 100
    }
    
    /**
     * Check if an audio format matches a preference
     * Handles variations like "VBR MP3" vs "MP3", "Ogg Vorbis" vs "Ogg"
     */
    private fun formatMatches(format: String, preference: String): Boolean {
        val normalizedFormat = format.lowercase()
        val normalizedPreference = preference.lowercase()
        
        return when {
            normalizedFormat == normalizedPreference -> true
            normalizedPreference == "mp3" && normalizedFormat.contains("mp3") -> true
            normalizedPreference == "vbr mp3" && normalizedFormat.contains("vbr") -> true
            normalizedPreference == "ogg vorbis" && (normalizedFormat.contains("ogg") || normalizedFormat.contains("vorbis")) -> true
            normalizedPreference == "ogg" && normalizedFormat.contains("ogg") -> true
            normalizedPreference == "flac" && normalizedFormat.contains("flac") -> true
            else -> false
        }
    }
    
    /**
     * Normalize format string for consistent comparison
     */
    private fun normalizeFormat(format: String): String {
        return format.lowercase().trim()
    }
    
    /**
     * Get quality score for tie-breaking when formats have same preference rank
     * Higher score = better quality
     */
    private fun getQualityScore(format: String): Int {
        val normalizedFormat = format.lowercase()
        
        return when {
            normalizedFormat.contains("flac") -> 100
            normalizedFormat.contains("vbr") -> 80
            normalizedFormat.contains("ogg") -> 70
            normalizedFormat.contains("mp3") -> {
                // Try to extract bitrate for MP3
                val bitrateMatch = Regex("(\\d+)").find(format)
                val bitrate = bitrateMatch?.value?.toIntOrNull() ?: 192
                when {
                    bitrate >= 320 -> 60
                    bitrate >= 256 -> 50
                    bitrate >= 192 -> 40
                    else -> 30
                }
            }
            else -> 20
        }
    }
    
    /**
     * Data class to hold track with its format ranking
     */
    private data class RankedTrack(
        val track: Track,
        val rank: Int,
        val format: String
    )
}