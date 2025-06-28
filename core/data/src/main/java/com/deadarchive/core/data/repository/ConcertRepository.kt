package com.deadarchive.core.data.repository

import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

interface ConcertRepository {
    fun searchConcerts(query: String): Flow<List<Concert>>
    suspend fun getConcertById(id: String): Concert?
    suspend fun getFavoriteConcerts(): Flow<List<Concert>>
    
    // New methods for streaming URL generation
    suspend fun getConcertMetadata(identifier: String): ArchiveMetadataResponse?
    suspend fun getStreamingUrl(identifier: String, filename: String): String?
    suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>>
    suspend fun getPreferredStreamingUrl(identifier: String): String?
    suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String?
}

@Singleton
class ConcertRepositoryImpl @Inject constructor(
    private val archiveApiService: ArchiveApiService
) : ConcertRepository {
    
    override fun searchConcerts(query: String): Flow<List<Concert>> = flowOf(emptyList())
    override suspend fun getConcertById(id: String): Concert? = null
    override suspend fun getFavoriteConcerts(): Flow<List<Concert>> = flowOf(emptyList())
    
    /**
     * Fetch metadata for a concert from Archive.org
     */
    override suspend fun getConcertMetadata(identifier: String): ArchiveMetadataResponse? {
        return try {
            val response = archiveApiService.getConcertMetadata(identifier)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: HttpException) {
            null
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            // JSON parsing errors, etc.
            null
        }
    }
    
    /**
     * Generate a direct streaming URL for a specific file
     * 
     * @param identifier Archive.org concert identifier
     * @param filename Name of the audio file
     * @return Direct streaming URL or null if metadata unavailable
     */
    override suspend fun getStreamingUrl(identifier: String, filename: String): String? {
        val metadata = getConcertMetadata(identifier) ?: return null
        
        // Get server and directory info from metadata
        val server = metadata.server ?: metadata.workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directory = metadata.directory ?: "0"
        
        // URL encode the filename to handle spaces and special characters
        val encodedFilename = URLEncoder.encode(filename, "UTF-8")
        
        // Construct the direct streaming URL
        // Note: directory already includes the full path like "/1/items/identifier"
        val streamingUrl = "https://$server$directory/$encodedFilename"
        println("DEBUG: Generated URL: server=$server, directory=$directory, filename=$filename -> $streamingUrl")
        return streamingUrl
    }
    
    /**
     * Get streaming URLs for all audio tracks in a concert
     * 
     * @param identifier Archive.org concert identifier
     * @return List of pairs (AudioFile, StreamingURL)
     */
    override suspend fun getTrackStreamingUrls(identifier: String): List<Pair<AudioFile, String>> {
        val metadata = getConcertMetadata(identifier) ?: return emptyList()
        
        // Get server and directory info
        val server = metadata.server ?: metadata.workableServers?.firstOrNull() ?: "ia800000.us.archive.org"
        val directory = metadata.directory ?: "0"
        
        // Filter for audio files and generate URLs
        return metadata.files
            .filter { file -> isAudioFile(file.format) }
            .mapNotNull { file ->
                try {
                    val audioFile = AudioFile(
                        filename = file.name,
                        format = file.format,
                        sizeBytes = file.size,
                        durationSeconds = file.length,
                        bitrate = file.bitrate,
                        sampleRate = file.sampleRate,
                        md5Hash = file.md5,
                        sha1Hash = file.sha1,
                        crc32Hash = file.crc32
                    )
                    
                    val encodedFilename = URLEncoder.encode(file.name, "UTF-8")
                    val streamingUrl = "https://$server$directory/$encodedFilename"
                    
                    audioFile to streamingUrl
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { (audioFile, _) -> 
                // Sort by track number if available, otherwise by filename
                audioFile.filename
            }
    }
    
    /**
     * Get the preferred streaming URL for a concert (usually the best quality MP3)
     */
    override suspend fun getPreferredStreamingUrl(identifier: String): String? {
        val trackUrls = getTrackStreamingUrls(identifier)
        
        // Prefer MP3 files for streaming (better compatibility)
        val mp3Tracks = trackUrls.filter { (audioFile, _) -> 
            audioFile.format.lowercase().contains("mp3")
        }
        
        return if (mp3Tracks.isNotEmpty()) {
            // Get the first MP3 track (usually track 01)
            mp3Tracks.first().second
        } else {
            // Fallback to first available audio file
            trackUrls.firstOrNull()?.second
        }
    }
    
    /**
     * Get streaming URL for a specific track by title or track number
     */
    override suspend fun getTrackStreamingUrl(identifier: String, trackQuery: String): String? {
        val trackUrls = getTrackStreamingUrls(identifier)
        
        // Try to find by filename match
        return trackUrls.find { (audioFile, _) -> 
            audioFile.filename.contains(trackQuery, ignoreCase = true)
        }?.second
    }
    
    private fun isAudioFile(format: String): Boolean {
        val audioFormats = setOf(
            "flac", "mp3", "vbr mp3", "ogg vorbis", "wav", "aiff", "ape", "wv"
        )
        return format.lowercase() in audioFormats
    }
}