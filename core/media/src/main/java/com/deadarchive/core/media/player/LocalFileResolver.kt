package com.deadarchive.core.media.player

import android.net.Uri
import android.util.Log
import com.deadarchive.core.data.repository.DownloadRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves local file paths for downloaded tracks to enable offline playback.
 * 
 * This class provides the mechanism to check if a track URL has a corresponding
 * local downloaded file and returns the appropriate file:// URI for offline playback.
 */
@Singleton
class LocalFileResolver @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    
    companion object {
        private const val TAG = "LocalFileResolver"
    }
    
    /**
     * Resolve a track URL to a local file URI if the track is downloaded,
     * otherwise return null to fall back to streaming.
     * 
     * @param originalUrl The original Archive.org streaming URL
     * @param recordingId The recording identifier (optional, will be extracted if not provided)
     * @return file:// URI string if local file exists, null otherwise
     */
    suspend fun resolveLocalFile(originalUrl: String, recordingId: String? = null): String? {
        return try {
            Log.d(TAG, "Resolving local file for URL: $originalUrl")
            
            // Extract recording ID and filename from the URL if not provided
            val (extractedRecordingId, filename) = extractRecordingInfoFromUrl(originalUrl, recordingId)
            
            if (extractedRecordingId == null || filename == null) {
                Log.d(TAG, "Could not extract recording info from URL: $originalUrl")
                return null
            }
            
            Log.d(TAG, "Extracted - Recording ID: $extractedRecordingId, Filename: $filename")
            
            // Check if the track is downloaded
            val isDownloaded = downloadRepository.isTrackDownloaded(extractedRecordingId, filename)
            if (!isDownloaded) {
                Log.d(TAG, "Track not downloaded: ${extractedRecordingId}_$filename")
                return null
            }
            
            // Get the local file path
            val localPath = downloadRepository.getLocalFilePath(extractedRecordingId, filename)
            if (localPath == null) {
                Log.w(TAG, "Local path is null for downloaded track: ${extractedRecordingId}_$filename")
                return null
            }
            
            // Verify the file actually exists
            val localFile = File(localPath)
            if (!localFile.exists() || !localFile.canRead()) {
                Log.w(TAG, "Local file does not exist or is not readable: $localPath")
                return null
            }
            
            // Convert to file:// URI
            val fileUri = Uri.fromFile(localFile).toString()
            Log.i(TAG, "✅ Resolved local file: $originalUrl -> $fileUri")
            return fileUri
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local file for URL: $originalUrl", e)
            null
        }
    }
    
    /**
     * Extract recording ID and filename from Archive.org URL.
     * 
     * Archive.org URLs typically have the format:
     * https://archive.org/download/{recording_id}/{filename}
     * 
     * @param url The Archive.org URL
     * @param providedRecordingId Optional recording ID if already known
     * @return Pair of (recordingId, filename) or (null, null) if extraction fails
     */
    private fun extractRecordingInfoFromUrl(url: String, providedRecordingId: String?): Pair<String?, String?> {
        return try {
            if (providedRecordingId != null) {
                // If recording ID is provided, just extract filename
                val filename = url.substringAfterLast("/")
                if (filename.isNotBlank() && filename.contains(".")) {
                    Pair(providedRecordingId, filename)
                } else {
                    Pair(null, null)
                }
            } else {
                // Extract both recording ID and filename from URL
                // Expected format: https://archive.org/download/{recording_id}/{filename}
                val uri = Uri.parse(url)
                val pathSegments = uri.pathSegments
                
                if (pathSegments.size >= 3 && pathSegments[0] == "download") {
                    val recordingId = pathSegments[1]
                    val filename = pathSegments[2]
                    
                    if (recordingId.isNotBlank() && filename.isNotBlank() && filename.contains(".")) {
                        Pair(recordingId, filename)
                    } else {
                        Pair(null, null)
                    }
                } else {
                    // Try alternative extraction from last two segments
                    if (pathSegments.size >= 2) {
                        val recordingId = pathSegments[pathSegments.size - 2]
                        val filename = pathSegments[pathSegments.size - 1]
                        
                        if (recordingId.isNotBlank() && filename.isNotBlank() && filename.contains(".")) {
                            Pair(recordingId, filename)
                        } else {
                            Pair(null, null)
                        }
                    } else {
                        Pair(null, null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recording info from URL: $url", e)
            Pair(null, null)
        }
    }
    
    /**
     * Check if a track is available locally without resolving the full path.
     * Useful for UI indicators.
     */
    suspend fun isAvailableLocally(originalUrl: String, recordingId: String? = null): Boolean {
        return try {
            val (extractedRecordingId, filename) = extractRecordingInfoFromUrl(originalUrl, recordingId)
            
            if (extractedRecordingId == null || filename == null) {
                return false
            }
            
            downloadRepository.isTrackDownloaded(extractedRecordingId, filename)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking local availability for URL: $originalUrl", e)
            false
        }
    }
    
    /**
     * Get the local file path directly if available.
     * Returns null if not downloaded or if file doesn't exist.
     */
    suspend fun getLocalFilePath(originalUrl: String, recordingId: String? = null): String? {
        return try {
            val (extractedRecordingId, filename) = extractRecordingInfoFromUrl(originalUrl, recordingId)
            
            if (extractedRecordingId == null || filename == null) {
                return null
            }
            
            val localPath = downloadRepository.getLocalFilePath(extractedRecordingId, filename)
            
            // Verify file exists
            if (localPath != null) {
                val file = File(localPath)
                if (file.exists() && file.canRead()) {
                    localPath
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local file path for URL: $originalUrl", e)
            null
        }
    }
}