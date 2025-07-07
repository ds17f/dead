package com.deadarchive.core.data.download.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.model.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * WorkManager worker that handles individual audio track downloads.
 * Integrates with existing DownloadRepository and OkHttpClient infrastructure.
 */
@HiltWorker
class AudioDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository,
    private val okHttpClient: OkHttpClient,
    private val downloadQueueManager: com.deadarchive.core.data.download.DownloadQueueManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AudioDownloadWorker"
        
        // Input data keys
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_TRACK_FILENAME = "track_filename"
        const val KEY_DOWNLOAD_URL = "download_url"
        
        // Output data keys
        const val KEY_RESULT_SUCCESS = "result_success"
        const val KEY_RESULT_ERROR = "result_error"
        const val KEY_RESULT_BYTES_DOWNLOADED = "result_bytes_downloaded"
        
        // Progress data keys
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_PROGRESS_BYTES = "progress_bytes"
        const val KEY_PROGRESS_TOTAL_BYTES = "progress_total_bytes"
        
        // Constants
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL = 256 * 1024 // Update every 256KB for smoother progress
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return@withContext Result.failure()
        val recordingId = inputData.getString(KEY_RECORDING_ID) ?: return@withContext Result.failure()
        val trackFilename = inputData.getString(KEY_TRACK_FILENAME) ?: return@withContext Result.failure()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@withContext Result.failure()
        
        Log.d(TAG, "Starting download: $downloadId")
        Log.d(TAG, "URL: $downloadUrl")
        
        try {
            // Update status to downloading
            downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            
            // Get download directory and create target file
            val downloadDir = downloadRepository.getDownloadDirectory()
            val recordingDir = File(downloadDir, recordingId).apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(recordingDir, trackFilename)
            
            // Check if file already exists and is complete
            if (targetFile.exists()) {
                val existingDownload = downloadRepository.getDownloadById(downloadId)
                if (existingDownload?.isCompleted == true) {
                    Log.d(TAG, "File already downloaded: ${targetFile.absolutePath}")
                    return@withContext Result.success(createSuccessOutput(targetFile.length()))
                }
                // Delete incomplete file
                targetFile.delete()
            }
            
            // Start the download
            val result = performDownload(downloadId, downloadUrl, targetFile)
            
            when {
                result.isSuccess -> {
                    val fileSize = targetFile.length()
                    
                    // Update download progress and local path
                    downloadRepository.updateDownloadProgress(downloadId, 1.0f, fileSize)
                    downloadRepository.updateDownloadLocalPath(downloadId, targetFile.absolutePath)
                    
                    // Update download status to completed
                    downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                    
                    Log.d(TAG, "Download completed successfully: $downloadId")
                    Log.d(TAG, "File saved to: ${targetFile.absolutePath}")
                    
                    // Trigger immediate queue processing to start next download
                    downloadQueueManager.triggerImmediateProcessing()
                    
                    Result.success(createSuccessOutput(fileSize))
                }
                result.shouldRetry -> {
                    val currentAttempt = runAttemptCount
                    if (currentAttempt < MAX_RETRY_ATTEMPTS) {
                        Log.w(TAG, "Download failed, retrying (attempt $currentAttempt/$MAX_RETRY_ATTEMPTS): ${result.errorMessage}")
                        downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
                        Result.retry()
                    } else {
                        Log.e(TAG, "Download failed after $MAX_RETRY_ATTEMPTS attempts: ${result.errorMessage}")
                        downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.FAILED, result.errorMessage)
                        Result.failure(createErrorOutput(result.errorMessage ?: "Max retry attempts exceeded"))
                    }
                }
                else -> {
                    Log.e(TAG, "Download failed permanently: ${result.errorMessage}")
                    downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.FAILED, result.errorMessage)
                    Result.failure(createErrorOutput(result.errorMessage ?: "Download failed"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in download worker", e)
            downloadRepository.updateDownloadStatus(downloadId, DownloadStatus.FAILED, e.message)
            Result.failure(createErrorOutput(e.message ?: "Unexpected error"))
        }
    }

    /**
     * Performs the actual file download with progress updates
     */
    private suspend fun performDownload(
        downloadId: String,
        downloadUrl: String,
        targetFile: File
    ): DownloadResult = withContext(Dispatchers.IO) {
        
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "DeadArchive/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorMsg = "HTTP ${response.code}: ${response.message}"
                Log.e(TAG, "HTTP error: $errorMsg")
                return@withContext DownloadResult.failure(errorMsg, shouldRetry = response.code in 500..599)
            }

            val responseBody = response.body
            if (responseBody == null) {
                Log.e(TAG, "Response body is null")
                return@withContext DownloadResult.failure("Empty response body", shouldRetry = false)
            }

            val contentLength = responseBody.contentLength()
            if (contentLength <= 0) {
                Log.w(TAG, "Content-Length not available or zero")
            }

            Log.d(TAG, "Starting download: ${targetFile.name}, size: ${formatFileSize(contentLength)}")

            var totalBytesRead = 0L
            var lastProgressUpdate = 0L
            
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        // Check if work is cancelled
                        if (!isActive) {
                            Log.d(TAG, "Download cancelled: $downloadId")
                            targetFile.delete()
                            return@withContext DownloadResult.failure("Download cancelled", shouldRetry = false)
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress periodically
                        if (totalBytesRead - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL || contentLength > 0) {
                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f // Indeterminate progress
                            }

                            // Update progress in repository
                            downloadRepository.updateDownloadProgress(downloadId, progress, totalBytesRead)
                            
                            // Update WorkManager progress
                            setProgressAsync(createProgressData(progress, totalBytesRead, contentLength))
                            
                            lastProgressUpdate = totalBytesRead
                            
                            Log.v(TAG, "Download progress: ${(progress * 100).toInt()}%, ${formatFileSize(totalBytesRead)} / ${formatFileSize(contentLength)}")
                        }
                    }
                    
                    outputStream.flush()
                }
            }

            // Final progress update
            val finalProgress = if (contentLength > 0) 1.0f else -1f
            downloadRepository.updateDownloadProgress(downloadId, finalProgress, totalBytesRead)
            setProgressAsync(createProgressData(finalProgress, totalBytesRead, contentLength))

            Log.d(TAG, "Download completed: ${targetFile.name}, total size: ${formatFileSize(totalBytesRead)}")
            DownloadResult.success()

        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error (no internet connection)", e)
            DownloadResult.failure("No internet connection", shouldRetry = true)
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Network timeout", e)
            DownloadResult.failure("Connection timeout", shouldRetry = true)
        } catch (e: IOException) {
            Log.e(TAG, "IO error during download", e)
            DownloadResult.failure("Download failed: ${e.message}", shouldRetry = true)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during download", e)
            DownloadResult.failure("Unexpected error: ${e.message}", shouldRetry = false)
        }
    }

    /**
     * Creates progress data for WorkManager
     */
    private fun createProgressData(progress: Float, bytesDownloaded: Long, totalBytes: Long): Data {
        return workDataOf(
            KEY_PROGRESS_PERCENT to if (progress >= 0) (progress * 100).toInt() else -1,
            KEY_PROGRESS_BYTES to bytesDownloaded,
            KEY_PROGRESS_TOTAL_BYTES to totalBytes
        )
    }

    /**
     * Creates success output data
     */
    private fun createSuccessOutput(bytesDownloaded: Long): Data {
        return workDataOf(
            KEY_RESULT_SUCCESS to true,
            KEY_RESULT_BYTES_DOWNLOADED to bytesDownloaded
        )
    }

    /**
     * Creates error output data
     */
    private fun createErrorOutput(errorMessage: String): Data {
        return workDataOf(
            KEY_RESULT_SUCCESS to false,
            KEY_RESULT_ERROR to errorMessage
        )
    }

    /**
     * Formats file size for logging
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }

    /**
     * Result wrapper for download operations
     */
    private data class DownloadResult(
        val isSuccess: Boolean,
        val shouldRetry: Boolean,
        val errorMessage: String?
    ) {
        companion object {
            fun success() = DownloadResult(true, false, null)
            fun failure(errorMessage: String, shouldRetry: Boolean) = 
                DownloadResult(false, shouldRetry, errorMessage)
        }
    }
}