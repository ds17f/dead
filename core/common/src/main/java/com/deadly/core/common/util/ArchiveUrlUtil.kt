package com.deadly.core.common.util

import android.util.Log
import com.deadly.core.model.Recording
import com.deadly.core.model.Track

/**
 * Utility class for generating Archive.org URLs for recordings and tracks
 */
object ArchiveUrlUtil {
    
    private const val ARCHIVE_BASE_URL = "https://archive.org/details/"
    
    /**
     * Generate Archive.org URL for a recording
     * @param recording The recording to generate URL for
     * @return URL string for the recording on Archive.org
     */
    fun getRecordingUrl(recording: Recording): String {
        return "$ARCHIVE_BASE_URL${recording.identifier}"
    }
    
    /**
     * Generate Archive.org URL for a specific track within a recording
     * @param recording The recording containing the track
     * @param track The specific track to link to
     * @return URL string for the track on Archive.org with track parameter
     */
    fun getTrackUrl(recording: Recording, track: Track): String {
        val baseUrl = getRecordingUrl(recording)
        // Archive.org uses the actual audio file filename to link to specific tracks
        val rawFilename = track.audioFile?.filename ?: track.filename
        val filename = getArchiveFilename(recording, rawFilename)
        return if (filename.isNotBlank()) {
            "$baseUrl/$filename"
        } else {
            baseUrl
        }
    }
    
    /**
     * Generate Archive.org URL for a track with time offset
     * @param recording The recording containing the track
     * @param track The specific track to link to
     * @param timeOffsetSeconds Optional time offset within the track
     * @return URL string for the track with time parameter
     */
    fun getTrackUrlWithTime(recording: Recording, track: Track, timeOffsetSeconds: Long? = null): String {
        val baseUrl = getRecordingUrl(recording)
        // Archive.org uses the actual audio file filename to link to specific tracks
        val audioFilename = track.audioFile?.filename
        val trackFilename = track.filename
        val rawFilename = audioFilename ?: trackFilename
        
        // Convert filename to match Archive.org's actual file format
        val filename = getArchiveFilename(recording, rawFilename)
        
        Log.d("ArchiveUrlUtil", "DEBUG URL Generation:")
        Log.d("ArchiveUrlUtil", "  Recording ID: ${recording.identifier}")
        Log.d("ArchiveUrlUtil", "  Track.filename: '$trackFilename'")
        Log.d("ArchiveUrlUtil", "  Track.audioFile?.filename: '$audioFilename'")
        Log.d("ArchiveUrlUtil", "  Raw filename: '$rawFilename'")
        Log.d("ArchiveUrlUtil", "  Converted filename: '$filename'")
        Log.d("ArchiveUrlUtil", "  Time offset: $timeOffsetSeconds seconds")
        
        val finalUrl = when {
            timeOffsetSeconds != null && timeOffsetSeconds > 0 && filename.isNotBlank() -> {
                // Archive.org accepts time parameters with the file and #start/seconds format
                "$baseUrl/$filename#start/$timeOffsetSeconds"
            }
            filename.isNotBlank() -> {
                "$baseUrl/$filename"
            }
            else -> baseUrl
        }
        
        Log.d("ArchiveUrlUtil", "  Final URL: '$finalUrl'")
        return finalUrl
    }
    
    /**
     * Convert filename to match Archive.org's actual file format based on recording type
     */
    private fun getArchiveFilename(recording: Recording, filename: String): String {
        if (filename.isBlank()) return filename
        
        // Determine the actual file format from the recording identifier
        val actualExtension = when {
            recording.identifier.contains(".shnf") -> "shn"
            recording.identifier.contains(".flac") -> "flac"
            recording.identifier.contains(".mp3") -> "mp3"
            else -> {
                // Default logic: if streaming shows .mp3 but recording suggests SBD/AUD, likely SHN
                if (recording.source?.contains("sbd", ignoreCase = true) == true ||
                    recording.source?.contains("aud", ignoreCase = true) == true) {
                    "shn"
                } else {
                    "mp3"
                }
            }
        }
        
        // Replace the extension in the filename
        val nameWithoutExtension = filename.substringBeforeLast('.')
        return "$nameWithoutExtension.$actualExtension"
    }
}