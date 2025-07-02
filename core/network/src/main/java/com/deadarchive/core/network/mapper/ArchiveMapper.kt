package com.deadarchive.core.network.mapper

import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.AudioQuality
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.ConcertNew
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse
import java.net.URLEncoder

/**
 * Mapper utilities for converting Archive.org API responses to domain models
 */
object ArchiveMapper {
    
    /**
     * Convert Archive search document to Concert domain model
     */
    fun ArchiveSearchResponse.ArchiveDoc.toConcert(): Concert {
        return Concert(
            identifier = identifier,
            title = title,
            date = date ?: "",
            venue = venue,
            location = coverage,
            year = year,
            source = source,
            taper = taper,
            transferer = transferer,
            lineage = lineage,
            description = description,
            setlistRaw = setlist,
            uploader = uploader,
            addedDate = addedDate,
            publicDate = publicDate
        )
    }
    
    /**
     * Convert Archive metadata to Concert domain model with full details
     */
    fun ArchiveMetadataResponse.toConcert(): Concert {
        val meta = metadata
        val identifier = meta?.identifier ?: ""
        
        // Get server and directory info for URL generation
        val server = server ?: workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directoryPath = directory ?: "/0"
        
        val audioFiles = files.filter { it.isAudioFile() }
        
        return Concert(
            identifier = identifier,
            title = meta?.title ?: "",
            date = meta?.date ?: "",
            venue = meta?.venue,
            location = meta?.coverage,
            year = extractYearFromDate(meta?.date),
            source = meta?.source,
            taper = meta?.taper,
            transferer = meta?.transferer,
            lineage = meta?.lineage,
            description = meta?.description,
            setlistRaw = meta?.setlist,
            uploader = meta?.uploader,
            addedDate = meta?.addedDate,
            publicDate = meta?.publicDate,
            tracks = audioFiles.mapIndexed { index, file ->
                file.toTrack(index + 1, server, directoryPath)
            },
            audioFiles = audioFiles.map { file ->
                file.toAudioFile(server, directoryPath)
            }
        )
    }
    
    /**
     * Convert Archive file to Track domain model
     */
    private fun ArchiveMetadataResponse.ArchiveFile.toTrack(
        trackNumber: Int,
        server: String,
        directoryPath: String
    ): Track {
        return Track(
            filename = name,
            title = title ?: name.substringBeforeLast('.'),
            trackNumber = trackNumber.toString(),
            durationSeconds = length?.toString(),
            audioFile = toAudioFile(server, directoryPath)
        )
    }
    
    /**
     * Convert Archive file to AudioFile domain model
     */
    private fun ArchiveMetadataResponse.ArchiveFile.toAudioFile(
        server: String,
        directoryPath: String
    ): AudioFile {
        // Generate the streaming URL for this audio file
        // Only encode spaces for Archive.org URLs (they don't encode other characters)
        val encodedFilename = name.replace(" ", "%20")
        val downloadUrl = "https://$server$directoryPath/$encodedFilename"
        
        return AudioFile(
            filename = name,
            format = format ?: "Unknown",
            sizeBytes = size?.toString(),
            durationSeconds = length?.toString(),
            bitrate = bitrate,
            sampleRate = sampleRate,
            md5Hash = md5,
            sha1Hash = sha1,
            crc32Hash = crc32,
            downloadUrl = downloadUrl
        )
    }
    
    /**
     * Check if archive file is an audio file
     */
    private fun ArchiveMetadataResponse.ArchiveFile.isAudioFile(): Boolean {
        val audioFormats = setOf(
            "flac", "mp3", "vbr mp3", "ogg vorbis", "ogg", "wav", "aiff", "ape", "wv", "m4a", "shn"
        )
        return format?.lowercase() in audioFormats
    }
    
    /**
     * Determine audio quality based on format and bitrate
     */
    private fun ArchiveMetadataResponse.ArchiveFile.determineQuality(): AudioQuality {
        return when (format?.lowercase()) {
            "flac", "shn" -> AudioQuality.LOSSLESS
            "mp3" -> {
                val bitrateInt = bitrate?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                when {
                    bitrateInt >= 256 -> AudioQuality.HIGH
                    bitrateInt >= 192 -> AudioQuality.MEDIUM
                    else -> AudioQuality.LOW
                }
            }
            "ogg" -> AudioQuality.MEDIUM
            else -> AudioQuality.LOW
        }
    }
    
    /**
     * Parse duration string to seconds
     */
    private fun parseDuration(lengthStr: String?): Int {
        if (lengthStr.isNullOrBlank()) return 0
        
        return try {
            // Handle formats like "4:32", "1:23:45", or "123.45" seconds
            when {
                lengthStr.contains(':') -> {
                    val parts = lengthStr.split(':').map { it.toDouble() }
                    when (parts.size) {
                        2 -> (parts[0] * 60 + parts[1]).toInt() // mm:ss
                        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]).toInt() // hh:mm:ss
                        else -> 0
                    }
                }
                else -> lengthStr.toDouble().toInt() // seconds
            }
        } catch (e: NumberFormatException) {
            0
        }
    }
    
    /**
     * Parse file size string to bytes
     */
    private fun parseFileSize(sizeStr: String?): Long {
        if (sizeStr.isNullOrBlank()) return 0L
        
        return try {
            sizeStr.toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }
    
    /**
     * Extract year from date string
     */
    private fun extractYearFromDate(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        
        return try {
            // Extract year from formats like "1977-05-08" or "1977"
            dateStr.substring(0, 4).takeIf { it.all { char -> char.isDigit() } }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Normalize date string to YYYY-MM-DD format for consistent grouping
     */
    private fun normalizeDateString(dateString: String): String {
        val normalized = when {
            // Handle ISO format: 1995-07-09T00:00:00Z -> 1995-07-09
            dateString.contains("T") -> dateString.substringBefore("T")
            // Handle YYYY-MM-DD format (already correct)
            dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> dateString
            // Default: return as-is
            else -> dateString
        }
        
        if (normalized != dateString) {
            println("📅 DATE NORMALIZE: '$dateString' -> '$normalized'")
        }
        
        return normalized
    }
    
    /**
     * Extract standardized source type from source description
     */
    private fun extractSourceType(sourceDescription: String?): String? {
        if (sourceDescription.isNullOrBlank()) return null
        
        val source = sourceDescription.uppercase()
        return when {
            source.contains("SBD") || source.contains("SOUNDBOARD") || source.contains("DSBD") -> "SBD"
            source.contains("MATRIX") -> "MATRIX"
            source.contains("FM") -> "FM"
            source.contains("SCHOEPS") -> "AUD" // High-quality audience
            source.contains("AUD") || source.contains("AUDIENCE") -> "AUD"
            else -> "AUD" // Default to audience if unclear
        }
    }
    
    /**
     * Calculate similarity between two venue names using multiple fuzzy matching techniques
     * Returns similarity percentage (0-100)
     */
    private fun calculateVenueSimilarity(venue1: String?, venue2: String?): Int {
        if (venue1.isNullOrBlank() && venue2.isNullOrBlank()) return 100
        if (venue1.isNullOrBlank() || venue2.isNullOrBlank()) return 0
        if (venue1 == venue2) return 100
        
        val v1 = venue1.trim().lowercase()
        val v2 = venue2.trim().lowercase()
        
        debugLog("🔍 SIMILARITY: Comparing '$v1' (${v1.length}) vs '$v2' (${v2.length})")
        
        // Check for common venue abbreviations first
        val abbreviationSimilarity = checkVenueAbbreviations(v1, v2)
        if (abbreviationSimilarity > 0) {
            debugLog("🔍 SIMILARITY: Abbreviation match detected! Result: ${abbreviationSimilarity}%")
            return abbreviationSimilarity
        }
        
        // Check if one is a prefix of the other (handles "Sam Boyd Silver Bowl" vs "Sam Boyd Silver Bowl, U.N.L.V.")
        val shorter = if (v1.length < v2.length) v1 else v2
        val longer = if (v1.length < v2.length) v2 else v1
        
        if (longer.startsWith(shorter)) {
            val prefixRatio = (shorter.length.toDouble() / longer.length * 100).toInt()
            debugLog("🔍 SIMILARITY: Prefix match detected! Ratio: ${prefixRatio}%")
            // For venue names, if one is a clear prefix of the other, it's likely the same venue
            // with additional institutional info. Boost similarity for matches ≥60%
            val result = if (prefixRatio >= 60) 85 else prefixRatio
            debugLog("🔍 SIMILARITY: Final result: ${result}%")
            return result
        }
        
        // Levenshtein distance for general similarity
        val distance = levenshteinDistance(v1, v2)
        val maxLength = maxOf(v1.length, v2.length)
        val similarity = ((maxLength - distance).toDouble() / maxLength * 100).toInt()
        
        debugLog("🔍 SIMILARITY: Levenshtein distance: $distance, similarity: ${similarity}%")
        return similarity
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
                debugLog("🔍 ABBREVIATION: Direct match '$venue1' -> '$venue2'")
                return 90
            }
        }
        
        abbreviationMap[venue2]?.let { possibleMatches ->
            if (venue1 in possibleMatches) {
                debugLog("🔍 ABBREVIATION: Direct match '$venue2' -> '$venue1'")
                return 90
            }
        }
        
        // Check if venues contain abbreviation patterns
        for ((full, abbrevs) in abbreviationMap) {
            if (venue1.contains(full) && abbrevs.any { venue2.contains(it) }) {
                debugLog("🔍 ABBREVIATION: Partial match '$venue1' contains '$full', '$venue2' contains abbreviation")
                return 85
            }
            if (venue2.contains(full) && abbrevs.any { venue1.contains(it) }) {
                debugLog("🔍 ABBREVIATION: Partial match '$venue2' contains '$full', '$venue1' contains abbreviation")
                return 85
            }
        }
        
        return 0
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    
    /**
     * Filter tracks to only include preferred audio formats (MP3)
     */
    private fun filterPreferredAudioTracks(tracks: List<Track>): List<Track> {
        val mp3Tracks = tracks.filter { track ->
            track.audioFile?.format?.lowercase()?.contains("mp3") == true
        }
        
        // Return MP3 tracks if available, otherwise return all tracks
        return if (mp3Tracks.isNotEmpty()) mp3Tracks else tracks
    }
    
    /**
     * Filter audio files to only include preferred formats (MP3)
     */
    private fun filterPreferredAudioFiles(audioFiles: List<AudioFile>): List<AudioFile> {
        val mp3Files = audioFiles.filter { file ->
            file.format.lowercase().contains("mp3")
        }
        
        // Return MP3 files if available, otherwise return all files
        return if (mp3Files.isNotEmpty()) mp3Files else audioFiles
    }
    
    // ==================== NEW RECORDING/CONCERT MAPPING ====================
    
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
            tracks = emptyList(), // Will be populated from detailed metadata
            audioFiles = emptyList(),
            isFavorite = false,
            isDownloaded = false
        )
    }
    
    /**
     * Convert Archive metadata to Recording domain model with full track information
     */
    fun ArchiveMetadataResponse.toRecording(): Recording {
        val meta = metadata
        val identifier = meta?.identifier ?: ""
        
        // Get server and directory info for URL generation
        val server = server ?: workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directoryPath = directory ?: "/0"
        
        val audioFiles = files.filter { it.isAudioFile() }
        
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
            tracks = audioFiles.mapIndexed { index, file ->
                file.toTrackNew(index + 1, server, directoryPath)
            },
            audioFiles = audioFiles.map { file ->
                file.toAudioFile(server, directoryPath)
            },
            isFavorite = false,
            isDownloaded = false
        )
    }
    
    /**
     * Group multiple recordings into ConcertNew objects by date and venue using fuzzy matching
     */
    fun List<Recording>.groupByConcert(): List<ConcertNew> {
        debugLog("🔄 GROUPING: Starting with ${this.size} recordings")
        
        // First group by date
        val dateGroups = this.groupBy { it.concertDate }
        val finalGroups = mutableMapOf<String, MutableList<Recording>>()
        
        dateGroups.forEach { (date, recordings) ->
            debugLog("🔄 GROUPING: Processing date '$date' with ${recordings.size} recordings")
            
            // For recordings on the same date, use fuzzy venue matching
            recordings.forEach { recording ->
                val rawVenue = recording.concertVenue?.trim()
                // Normalize empty/null venues to a consistent value
                val venue = when {
                    rawVenue.isNullOrBlank() -> "Unknown"
                    rawVenue.equals("null", ignoreCase = true) -> "Unknown"  // Handle string "null" 
                    else -> rawVenue
                }
                
                debugLog("🔄 GROUPING: Recording ${recording.identifier}")
                debugLog("🔄 GROUPING:   Venue: '$venue'")
                
                // Find existing group with similar venue name
                val matchingGroupKey = finalGroups.keys.find { existingKey ->
                    if (existingKey.startsWith("${date}_")) {
                        val existingVenue = existingKey.substringAfter("${date}_")
                        val similarity = calculateVenueSimilarity(venue, existingVenue)
                        debugLog("🔄 GROUPING:   Comparing '$venue' vs '$existingVenue' = ${similarity}%")
                        similarity >= 75 // 75% similarity threshold for venue matching
                    } else false
                }
                
                val groupKey = matchingGroupKey ?: "${date}_${venue}"
                debugLog("🔄 GROUPING:   Group Key: '$groupKey'")
                
                finalGroups.getOrPut(groupKey) { mutableListOf() }.add(recording)
            }
        }
        
        val groupedMap = finalGroups.mapValues { it.value.toList() }
        
        debugLog("🔄 GROUPING: Created ${groupedMap.keys.size} groups:")
        groupedMap.keys.forEach { key ->
            debugLog("🔄 GROUPING:   Group '$key' has ${groupedMap[key]?.size} recordings")
        }
        
        return groupedMap.map { (groupKey, recordings) ->
            // Use the first recording to get concert-level metadata
            val firstRecording = recordings.first()
            
            debugLog("🔄 GROUPING: Creating ConcertNew for group '$groupKey' with ${recordings.size} recordings")
            
            // Extract normalized venue from group key (format: "date_venue")
            val normalizedVenue = groupKey.substringAfter("_")
            
            ConcertNew(
                date = firstRecording.concertDate,
                venue = normalizedVenue,
                location = extractLocationFromRecordings(recordings),
                year = extractYearFromDate(firstRecording.concertDate),
                setlistRaw = extractSetlistFromRecordings(recordings),
                sets = emptyList(), // Will be parsed from setlistRaw later
                recordings = recordings.sortedWith(recordingComparator),
                isFavorite = false
            )
        }.sortedByDescending { it.date }
    }
    
    /**
     * Convert existing Concert objects to ConcertNew objects (for migration)
     */
    fun List<Concert>.migrateToConcertNew(): List<ConcertNew> {
        val recordings = this.map { concert ->
            // Convert each Concert to a Recording first
            // Normalize the date format for proper grouping
            val normalizedDate = normalizeDateString(concert.date)
            
            Recording(
                identifier = concert.identifier,
                title = concert.title,
                source = extractSourceType(concert.source),
                taper = concert.taper,
                transferer = concert.transferer,
                lineage = concert.lineage,
                description = concert.description,
                uploader = concert.uploader,
                addedDate = concert.addedDate,
                publicDate = concert.publicDate,
                concertDate = normalizedDate,
                concertVenue = concert.venue,
                tracks = filterPreferredAudioTracks(concert.tracks),
                audioFiles = filterPreferredAudioFiles(concert.audioFiles),
                isFavorite = concert.isFavorite,
                isDownloaded = concert.isDownloaded
            )
        }
        
        // Group recordings by concert date/venue using existing groupByConcert extension
        return recordings.groupByConcert()
    }
    
    /**
     * Convert Archive file to Track domain model for new Recording
     */
    private fun ArchiveMetadataResponse.ArchiveFile.toTrackNew(
        trackNumber: Int,
        server: String,
        directoryPath: String
    ): Track {
        return Track(
            filename = name,
            title = extractTrackTitle(name),
            trackNumber = trackNumber.toString(),
            durationSeconds = length,
            audioFile = toAudioFile(server, directoryPath)
        )
    }
    
    // Helper functions for new Recording/Concert mapping
    
    private fun extractLocationFromRecordings(recordings: List<Recording>): String? {
        // Try to find location from recording titles or descriptions
        return recordings.firstNotNullOfOrNull { recording ->
            // This could be enhanced to parse location from title/description
            // For now, return null - location might need to be looked up separately
            null
        }
    }
    
    private fun extractSetlistFromRecordings(recordings: List<Recording>): String? {
        // Try to find setlist information from descriptions
        return recordings.firstNotNullOfOrNull { recording ->
            recording.description?.let { desc ->
                if (desc.contains("setlist", ignoreCase = true) || 
                    desc.contains("set list", ignoreCase = true) ||
                    desc.contains("set 1", ignoreCase = true)) {
                    desc
                } else null
            }
        }
    }
    
    private fun extractTrackTitle(filename: String): String {
        // Remove file extension and track numbers
        return filename
            .substringBeforeLast(".")
            .replace(Regex("^\\d+[.-]\\s*"), "") // Remove leading track numbers
            .trim()
    }
    
    // Comparator for sorting recordings within a concert (best quality first)
    private val recordingComparator = compareBy<Recording> { recording ->
        when (recording.source?.uppercase()) {
            "SBD" -> 1
            "MATRIX" -> 2
            "FM" -> 3
            "AUD" -> 4
            else -> 5
        }
    }.thenBy { it.identifier }
    
    /**
     * Debug logging that works in both Android and unit tests
     */
    private fun debugLog(message: String) {
        try {
            // Try Android logging for app runtime
            android.util.Log.d("ConcertGrouping", message)
        } catch (e: RuntimeException) {
            // Fall back to println for unit tests
            println("[ConcertGrouping] $message")
        }
    }
}