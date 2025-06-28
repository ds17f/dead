package com.deadarchive.core.data.mapper

import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.database.FavoriteEntity
import com.deadarchive.core.model.*
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse

/**
 * Comprehensive data mapping utilities for converting between network, database, and domain models.
 * Centralizes all data transformation logic with proper error handling and type safety.
 */
object DataMappers {

    // ============ Network to Domain Mappings ============

    /**
     * Convert Archive search document to Concert domain model with null safety
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
     * Convert Archive metadata response to Concert with full details and tracks
     */
    fun ArchiveMetadataResponse.toConcert(): Concert {
        val meta = metadata
        return Concert(
            identifier = meta?.identifier ?: "",
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
            tracks = files.filter { it.isAudioFile() }.mapIndexed { index, file ->
                file.toTrack(meta?.identifier ?: "", index + 1)
            },
            audioFiles = files.filter { it.isAudioFile() }.map { it.toAudioFile() }
        )
    }

    /**
     * Convert Archive file to Track domain model
     */
    fun ArchiveMetadataResponse.ArchiveFile.toTrack(
        concertId: String,
        trackNumber: Int
    ): Track {
        return Track(
            filename = name,
            title = title ?: name.substringBeforeLast('.'),
            trackNumber = trackNumber.toString(),
            durationSeconds = length?.toString(),
            audioFile = toAudioFile()
        )
    }

    /**
     * Convert Archive file to AudioFile domain model
     */
    fun ArchiveMetadataResponse.ArchiveFile.toAudioFile(): AudioFile {
        return AudioFile(
            filename = name,
            format = format ?: "Unknown",
            sizeBytes = size?.toString(),
            durationSeconds = length?.toString(),
            bitrate = bitrate,
            sampleRate = sampleRate
        )
    }

    // ============ Domain to Database Mappings ============

    /**
     * Convert Concert domain model to ConcertEntity for database storage
     */
    fun Concert.toConcertEntity(isFavorite: Boolean = false): ConcertEntity {
        return ConcertEntity.fromConcert(this, isFavorite)
    }

    /**
     * Convert FavoriteItem to FavoriteEntity for database storage
     */
    fun FavoriteItem.toFavoriteEntity(): FavoriteEntity {
        return FavoriteEntity.fromFavoriteItem(this)
    }

    /**
     * Convert DownloadState to DownloadEntity for database storage
     */
    fun DownloadState.toDownloadEntity(): DownloadEntity {
        return DownloadEntity.fromDownloadState(this)
    }

    // ============ Database to Domain Mappings ============

    /**
     * Convert ConcertEntity to Concert domain model
     */
    fun ConcertEntity.toConcert(): Concert {
        return this.toConcert()
    }

    /**
     * Convert FavoriteEntity to FavoriteItem domain model
     */
    fun FavoriteEntity.toFavoriteItem(): FavoriteItem {
        return this.toFavoriteItem()
    }

    /**
     * Convert DownloadEntity to DownloadState domain model
     */
    fun DownloadEntity.toDownloadState(): DownloadState {
        return this.toDownloadState()
    }

    // ============ Factory Methods for Domain Models ============

    /**
     * Create FavoriteItem from Concert
     */
    fun Concert.toFavoriteItem(notes: String? = null): FavoriteItem {
        return FavoriteItem.fromConcert(this).copy(notes = notes)
    }

    /**
     * Create FavoriteItem from Track
     */
    fun Track.toFavoriteItem(concertId: String, notes: String? = null): FavoriteItem {
        return FavoriteItem.fromTrack(concertId, this).copy(notes = notes)
    }

    /**
     * Create DownloadState from Concert and Track
     */
    fun createDownloadState(
        concert: Concert,
        trackFilename: String,
        status: DownloadStatus = DownloadStatus.QUEUED
    ): DownloadState {
        return DownloadState(
            concertIdentifier = concert.identifier,
            trackFilename = trackFilename,
            status = status
        )
    }

    // ============ Batch Conversion Utilities ============

    /**
     * Convert list of Archive search results to Concert list
     */
    fun List<ArchiveSearchResponse.ArchiveDoc>.toConcerts(): List<Concert> {
        return mapNotNull { doc ->
            try {
                doc.toConcert()
            } catch (e: Exception) {
                // Log error and skip malformed entries
                null
            }
        }
    }

    /**
     * Convert list of FavoriteEntity to FavoriteItem list
     */
    fun List<FavoriteEntity>.toFavoriteItems(): List<FavoriteItem> {
        return map { it.toFavoriteItem() }
    }

    /**
     * Convert list of DownloadEntity to DownloadState list
     */
    fun List<DownloadEntity>.toDownloadStates(): List<DownloadState> {
        return map { it.toDownloadState() }
    }

    /**
     * Convert list of Concert to ConcertEntity list
     */
    fun List<Concert>.toConcertEntities(isFavorite: Boolean = false): List<ConcertEntity> {
        return map { it.toConcertEntity(isFavorite) }
    }

    // ============ Utility Functions ============

    /**
     * Check if archive file is an audio file
     */
    fun ArchiveMetadataResponse.ArchiveFile.isAudioFile(): Boolean {
        val audioFormats = setOf(
            "flac", "mp3", "vbr mp3", "ogg vorbis", "ogg", "wav", "aiff", "ape", "wv", "m4a", "shn"
        )
        return format?.lowercase() in audioFormats
    }

    /**
     * Determine audio quality based on format and bitrate
     */
    fun ArchiveMetadataResponse.ArchiveFile.determineQuality(): AudioQuality {
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
     * Extract year from date string with error handling
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
     * Safe string conversion with null handling
     */
    fun String?.orEmpty(): String = this ?: ""

    /**
     * Safe long conversion with null handling
     */
    fun String?.toLongOrZero(): Long {
        return try {
            this?.toLong() ?: 0L
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * Safe int conversion with null handling
     */
    fun String?.toIntOrZero(): Int {
        return try {
            this?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }
    }

    /**
     * Safe float conversion with null handling
     */
    fun String?.toFloatOrZero(): Float {
        return try {
            this?.toFloat() ?: 0f
        } catch (e: NumberFormatException) {
            0f
        }
    }

    /**
     * Parse duration string to seconds with multiple format support
     */
    fun parseDurationToSeconds(lengthStr: String?): Int {
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
     * Format duration from seconds to readable string
     */
    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%d:%02d", minutes, secs)
        }
    }

    /**
     * Format file size from bytes to human readable format
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }

    /**
     * Validate concert identifier format
     */
    fun isValidConcertId(identifier: String?): Boolean {
        if (identifier.isNullOrBlank()) return false
        
        // Archive.org identifiers typically follow pattern: artist-date.source.info
        val pattern = Regex("^[a-zA-Z0-9._-]+$")
        return identifier.matches(pattern) && identifier.length > 3
    }

    /**
     * Generate safe filename from concert and track info
     */
    fun generateSafeFilename(concertId: String, trackFilename: String): String {
        val safeChars = Regex("[^a-zA-Z0-9._-]")
        val safeConcertId = concertId.replace(safeChars, "_")
        val safeTrackName = trackFilename.replace(safeChars, "_")
        return "${safeConcertId}_${safeTrackName}"
    }
}