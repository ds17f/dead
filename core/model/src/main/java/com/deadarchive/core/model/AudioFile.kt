package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioFile(
    @SerialName("name")
    val filename: String,
    
    @SerialName("format")
    val format: String, // "Flac", "VBR MP3", "Ogg Vorbis", etc.
    
    @SerialName("size")
    val sizeBytes: String? = null,
    
    @SerialName("length")
    val durationSeconds: String? = null,
    
    @SerialName("md5")
    val md5Hash: String? = null,
    
    @SerialName("sha1")
    val sha1Hash: String? = null,
    
    @SerialName("crc32")
    val crc32Hash: String? = null,
    
    @SerialName("bitrate")
    val bitrate: String? = null,
    
    @SerialName("sample_rate")
    val sampleRate: String? = null,
    
    // Download information
    val downloadUrl: String? = null,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
) {
    val displayFormat: String
        get() = when (format.lowercase()) {
            "flac" -> "FLAC"
            "vbr mp3", "mp3" -> "MP3"
            "ogg vorbis" -> "OGG"
            else -> format.uppercase()
        }
    
    val displaySize: String
        get() = sizeBytes?.toLongOrNull()?.let { bytes ->
            when {
                bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
                bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        } ?: "Unknown"
    
    val displayBitrate: String
        get() = bitrate?.let { "${it} kbps" } ?: ""
    
    val duration: Long
        get() = durationSeconds?.toDoubleOrNull()?.toLong() ?: 0L
    
    val isLossless: Boolean
        get() = format.lowercase() == "flac"
    
    val quality: AudioQuality
        get() = when {
            isLossless -> AudioQuality.LOSSLESS
            bitrate?.toIntOrNull()?.let { it >= 320 } == true -> AudioQuality.HIGH
            bitrate?.toIntOrNull()?.let { it >= 192 } == true -> AudioQuality.MEDIUM
            else -> AudioQuality.LOW
        }
}

enum class AudioQuality {
    LOSSLESS,
    HIGH,
    MEDIUM,
    LOW
}