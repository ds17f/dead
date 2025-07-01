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
     * Group multiple recordings into ConcertNew objects by date and venue
     */
    fun List<Recording>.groupByConcert(): List<ConcertNew> {
        return this.groupBy { recording ->
            // Group by date + venue combination
            "${recording.concertDate}_${recording.concertVenue ?: "Unknown"}"
        }.map { (_, recordings) ->
            // Use the first recording to get concert-level metadata
            val firstRecording = recordings.first()
            
            ConcertNew(
                date = firstRecording.concertDate,
                venue = firstRecording.concertVenue,
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
        return this.map { concert ->
            // Convert each Concert to a Recording first
            val recording = Recording(
                identifier = concert.identifier,
                title = concert.title,
                source = concert.source,
                taper = concert.taper,
                transferer = concert.transferer,
                lineage = concert.lineage,
                description = concert.description,
                uploader = concert.uploader,
                addedDate = concert.addedDate,
                publicDate = concert.publicDate,
                concertDate = concert.date,
                concertVenue = concert.venue,
                tracks = concert.tracks,
                audioFiles = concert.audioFiles,
                isFavorite = concert.isFavorite,
                isDownloaded = concert.isDownloaded
            )
            
            // Then create ConcertNew with single recording
            ConcertNew(
                date = concert.date,
                venue = concert.venue,
                location = concert.location,
                year = concert.year,
                setlistRaw = concert.setlistRaw,
                sets = concert.sets,
                recordings = listOf(recording),
                isFavorite = concert.isFavorite
            )
        }
        // Group any concerts that share the same date/venue using existing groupByConcert extension
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
}