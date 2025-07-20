package com.deadarchive.core.network.mapper

import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.AudioQuality
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Track
import com.deadarchive.core.model.util.VenueUtil
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse
import java.net.URLEncoder

/**
 * Mapper utilities for converting Archive.org API responses to domain models
 */
object ArchiveMapper {
    
    /**
     * Convert Archive search document to Recording domain model
     */
    fun ArchiveSearchResponse.ArchiveDoc.toRecording(): Recording {
        return Recording(
            identifier = identifier,
            title = title,
            source = source,
            taper = taper,
            transferer = transferer,
            lineage = lineage,
            description = description,
            uploader = uploader,
            addedDate = addedDate,
            publicDate = publicDate,
            concertDate = date ?: "",
            concertVenue = venue,
            concertLocation = coverage,
            isInLibrary = false,
            isDownloaded = false
        )
    }
    
    /**
     * Convert Archive metadata to Recording domain model with full details
     */
    fun ArchiveMetadataResponse.toRecording(): Recording {
        val meta = metadata
        val identifier = meta?.identifier ?: ""
        
        // Filter audio files and create tracks
        val audioFormats = setOf("flac", "mp3", "vbr mp3", "ogg vorbis", "wav")
        val audioFiles = files.filter { file ->
            file.format.lowercase() in audioFormats
        }
        
        // Create Track objects from audio files
        val tracks = audioFiles.mapIndexed { index, file ->
            val audioFile = AudioFile(
                filename = file.name,
                format = file.format,
                downloadUrl = buildDownloadUrl(this@toRecording.server, this@toRecording.directory, file.name),
                sizeBytes = file.size ?: "0",
                bitrate = file.bitrate,
                sampleRate = file.sampleRate
            )
            
            Track(
                filename = file.name,
                title = file.title ?: extractTitleFromFilename(file.name),
                trackNumber = file.track ?: (index + 1).toString(),
                durationSeconds = file.length ?: "0",
                audioFile = audioFile
            )
        }
        
        // Create list of AudioFiles (for backwards compatibility)
        val allAudioFiles = tracks.map { it.audioFile }.filterNotNull()
        
        return Recording(
            identifier = identifier,
            title = meta?.title ?: "",
            source = meta?.source,
            taper = meta?.taper,
            transferer = meta?.transferer,
            lineage = meta?.lineage,
            description = meta?.description,
            uploader = meta?.uploader,
            addedDate = meta?.addedDate,
            publicDate = meta?.publicDate,
            concertDate = meta?.date ?: "",
            concertVenue = meta?.venue,
            concertLocation = meta?.coverage,
            tracks = tracks,
            audioFiles = allAudioFiles,
            isInLibrary = false,
            isDownloaded = false
        )
    }
    
    /**
     * Build download URL from server, directory and filename
     */
    private fun buildDownloadUrl(server: String?, directory: String?, filename: String): String {
        val baseUrl = if (server != null) {
            // Server from API doesn't include protocol, add https://
            if (server.startsWith("http")) server else "https://$server"
        } else {
            "https://archive.org"
        }
        val dir = directory ?: ""
        val encodedFilename = URLEncoder.encode(filename, "UTF-8").replace("+", "%20")
        return "$baseUrl$dir/$encodedFilename"
    }
    
    /**
     * Extract track title from filename (fallback when title field is missing)
     */
    private fun extractTitleFromFilename(filename: String): String {
        // Remove file extension
        val nameWithoutExtension = filename.substringBeforeLast(".")
        
        // Try to extract title from common patterns like "gd1977-05-08d1t01" -> "Track 01"
        // or use the filename as-is for display
        return when {
            nameWithoutExtension.matches(Regex(".*t\\d+$")) -> {
                val trackNumber = nameWithoutExtension.substringAfterLast("t")
                "Track $trackNumber"
            }
            else -> nameWithoutExtension.replace("_", " ").replace("-", " ")
        }
    }
    
    /**
     * Group recordings by show using fuzzy venue name matching (using old proven pattern)
     */
    private fun List<Recording>.groupByShowWithFuzzyMatching(): List<List<Recording>> {
        // First group by date
        val dateGroups = this.groupBy { it.concertDate }
        val finalGroups = mutableMapOf<String, MutableList<Recording>>()
        
        dateGroups.forEach { (date, recordings) ->
            // For recordings on the same date, use fuzzy venue matching
            recordings.forEach { recording ->
                val rawVenue = recording.concertVenue?.trim()
                // Normalize venue using the canonical venue normalization
                val venue = VenueUtil.normalizeVenue(rawVenue)
                
                // Find existing group with similar venue name
                val matchingGroupKey = finalGroups.keys.find { existingKey ->
                    if (existingKey.startsWith("${date}_")) {
                        val existingVenue = existingKey.substringAfter("${date}_")
                        val similarity = calculateVenueSimilarity(venue, existingVenue)
                        // Debug logging for 1993-05-16
                        if (date == "1993-05-16") {
                            android.util.Log.d("FuzzyMatching", "🔍 1993-05-16: Comparing '$venue' vs '$existingVenue' = ${similarity}%")
                        }
                        similarity >= 75 // 75% similarity threshold for venue matching
                    } else false
                }
                
                val groupKey = matchingGroupKey ?: "${date}_${venue}"
                // Debug logging for 1993-05-16
                if (date == "1993-05-16") {
                    android.util.Log.d("FuzzyMatching", "🔍 1993-05-16: Using group key '$groupKey' for venue '$venue'")
                }
                finalGroups.getOrPut(groupKey) { mutableListOf() }.add(recording)
            }
        }
        
        return finalGroups.values.map { it.toList() }
    }
    
    /**
     * Calculate similarity between two venue names using fuzzy matching
     * Returns similarity percentage (0-100)
     */
    private fun calculateVenueSimilarity(venue1: String?, venue2: String?): Int {
        if (venue1.isNullOrBlank() && venue2.isNullOrBlank()) return 100
        if (venue1.isNullOrBlank() || venue2.isNullOrBlank()) return 0
        if (venue1 == venue2) return 100
        
        val v1 = venue1.trim().lowercase()
        val v2 = venue2.trim().lowercase()
        
        // Check for common venue abbreviations first
        val abbreviationSimilarity = checkVenueAbbreviations(v1, v2)
        if (abbreviationSimilarity > 0) {
            return abbreviationSimilarity
        }
        
        // Check if one is a prefix of the other
        val shorter = if (v1.length < v2.length) v1 else v2
        val longer = if (v1.length < v2.length) v2 else v1
        
        if (longer.startsWith(shorter)) {
            val prefixRatio = (shorter.length.toDouble() / longer.length * 100).toInt()
            // For venue names, if one is a clear prefix of the other, boost similarity
            return if (prefixRatio >= 60) 85 else prefixRatio
        }
        
        // Levenshtein distance for general similarity
        val distance = levenshteinDistance(v1, v2)
        val maxLength = maxOf(v1.length, v2.length)
        return ((maxLength - distance).toDouble() / maxLength * 100).toInt()
    }
    
    /**
     * Check for common venue abbreviations and name variations
     * Returns similarity percentage if match found, 0 if no match
     */
    private fun checkVenueAbbreviations(venue1: String, venue2: String): Int {
        // Define common abbreviation mappings
        val abbreviationMap = mapOf(
            // Madison Square Garden variations
            "msg" to listOf("madison square garden", "madison sq garden", "madison square gdn"),
            "madison square garden" to listOf("msg", "madison sq garden", "madison square gdn"),
            "madison sq garden" to listOf("msg", "madison square garden", "madison square gdn"),
            
            // Theater/Theatre variations  
            "theater" to listOf("theatre"),
            "theatre" to listOf("theater"),
            
            // Amphitheater variations
            "amphitheater" to listOf("amphitheatre", "ampitheater"),
            "amphitheatre" to listOf("amphitheater", "ampitheater"), 
            "ampitheater" to listOf("amphitheater", "amphitheatre"),
            
            // Common venue abbreviations
            "civic center" to listOf("civic ctr", "cc"),
            "civic ctr" to listOf("civic center", "cc"),
            "cc" to listOf("civic center", "civic ctr"),
            
            "auditorium" to listOf("aud", "auditorium"),
            "aud" to listOf("auditorium"),
            
            "university" to listOf("univ", "u"),
            "univ" to listOf("university", "u"),
            
            // Stadium variations
            "stadium" to listOf("stad", "fieldhouse", "field house"),
            "stad" to listOf("stadium"),
            "fieldhouse" to listOf("field house", "stadium"),
            "field house" to listOf("fieldhouse", "stadium")
        )
        
        // Check direct abbreviation matches
        abbreviationMap[venue1]?.let { possibleMatches ->
            if (venue2 in possibleMatches) {
                return 90
            }
        }
        
        abbreviationMap[venue2]?.let { possibleMatches ->
            if (venue1 in possibleMatches) {
                return 90
            }
        }
        
        // Check if venues contain abbreviation patterns
        for ((full, abbrevs) in abbreviationMap) {
            if (venue1.contains(full) && abbrevs.any { venue2.contains(it) }) {
                return 85
            }
            if (venue2.contains(full) && abbrevs.any { venue1.contains(it) }) {
                return 85
            }
        }
        
        return 0
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion  
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return matrix[len1][len2]
    }
    
    /**
     * Convert list of search results to Shows (grouped by date and venue using fuzzy matching)
     */
    fun List<Recording>.toShows(): List<Show> {
        return this.groupByShowWithFuzzyMatching()
            .map { recordings ->
                val firstRecording = recordings.first()
                Show(
                    date = firstRecording.concertDate,
                    venue = firstRecording.concertVenue,
                    location = firstRecording.concertLocation,
                    recordings = recordings,
                    isInLibrary = recordings.any { it.isInLibrary }
                )
            }
            .sortedByDescending { it.date }
    }
}