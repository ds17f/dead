package com.deadarchive.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchiveMetadata(
    @SerialName("created")
    val created: Long? = null,
    
    @SerialName("d1")
    val startDate: String? = null,
    
    @SerialName("d2")
    val endDate: String? = null,
    
    @SerialName("dir")
    val directory: String? = null,
    
    @SerialName("files")
    val files: List<AudioFile> = emptyList(),
    
    @SerialName("files_count")
    val filesCount: Int = 0,
    
    @SerialName("item_size")
    val totalSize: Long = 0,
    
    @SerialName("metadata")
    val metadata: Concert
) {
    val audioFiles: List<AudioFile>
        get() = files.filter { file ->
            val format = file.format.lowercase()
            format in listOf("flac", "mp3", "vbr mp3", "ogg vorbis", "wav")
        }
    
    val preferredAudioFiles: List<AudioFile>
        get() = audioFiles.sortedWith(compareBy<AudioFile> { 
            when (it.format.lowercase()) {
                "flac" -> 0
                "mp3", "vbr mp3" -> 1
                "ogg vorbis" -> 2
                else -> 3
            }
        }.thenByDescending { 
            it.bitrate?.toIntOrNull() ?: 0 
        })
    
    val displaySize: String
        get() = when {
            totalSize >= 1_073_741_824 -> String.format("%.1f GB", totalSize / 1_073_741_824.0)
            totalSize >= 1_048_576 -> String.format("%.1f MB", totalSize / 1_048_576.0)
            totalSize >= 1024 -> String.format("%.1f KB", totalSize / 1024.0)
            else -> "$totalSize B"
        }
}