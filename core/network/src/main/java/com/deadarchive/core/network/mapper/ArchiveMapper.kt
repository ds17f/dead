package com.deadarchive.core.network.mapper

import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.AudioQuality
import com.deadarchive.core.model.Concert
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
}