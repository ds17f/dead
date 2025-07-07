package com.deadarchive.core.data.download.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.deadarchive.core.data.repository.DownloadRepository
import com.deadarchive.core.model.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Periodic worker that manages the download queue by processing queued downloads
 * and enqueuing AudioDownloadWorkers while respecting concurrency limits and priorities.
 */
@HiltWorker
class DownloadQueueManagerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(context, workerParams) {
    
    init {
        Log.d(TAG, "DownloadQueueManagerWorker constructor called - WITH REPOSITORY DEPENDENCY")
        Log.d(TAG, "Context: ${context.javaClass.simpleName}")
        Log.d(TAG, "Repository: ${downloadRepository.javaClass.simpleName}")
    }

    companion object {
        private const val TAG = "DownloadQueueManagerWorker"
        
        // Work constraints and configuration
        const val WORK_NAME = "download_queue_manager"
        private const val MAX_CONCURRENT_DOWNLOADS = 3
        
        // Tags for tracking download workers
        private const val AUDIO_DOWNLOAD_TAG = "audio_download"
        private const val QUEUE_MANAGED_TAG = "queue_managed"
        
        /**
         * Creates a periodic work request for the download queue manager
         */
        fun createPeriodicWorkRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .setRequiresBatteryNotLow(false) // Allow downloads even on low battery
                .build()
            
            return PeriodicWorkRequestBuilder<DownloadQueueManagerWorker>(
                repeatInterval = 15, // Minimum allowed by Android (will be overridden by immediate processing)
                repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.MINUTES,
                flexTimeInterval = 1, // Minimal flex window for faster processing
                flexTimeIntervalUnit = java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, // Start with 10 second backoff
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .build()
        }
        
        /**
         * Creates a one-time work request for immediate queue processing
         */
        fun createImmediateWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            
            return OneTimeWorkRequestBuilder<DownloadQueueManagerWorker>()
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .addTag("immediate")
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 DownloadQueueManagerWorker.doWork() STARTED - NEW IMPLEMENTATION")
            Log.d(TAG, "⚙️ Worker context: ${applicationContext.javaClass.simpleName}")
            Log.d(TAG, "⚙️ Repository injected: ${downloadRepository.javaClass.simpleName}")
            
            // Get queued downloads
            val queuedDownloads = downloadRepository.getDownloadQueue()
            Log.d(TAG, "🔍 Found ${queuedDownloads.size} queued downloads in database")
            
            if (queuedDownloads.isEmpty()) {
                Log.d(TAG, "No downloads in queue, queue processing complete")
                return@withContext Result.success(workDataOf(
                    "status" to "success",
                    "message" to "No downloads in queue"
                ))
            }
            
            // Get current running download count
            val runningCount = getCurrentRunningDownloadCount()
            val availableSlots = MAX_CONCURRENT_DOWNLOADS - runningCount
            
            Log.d(TAG, "Current running downloads: $runningCount, Available slots: $availableSlots")
            
            if (availableSlots <= 0) {
                Log.d(TAG, "Maximum concurrent downloads reached, skipping new downloads")
                return@withContext Result.success(workDataOf(
                    "status" to "success",
                    "message" to "Max concurrent downloads reached"
                ))
            }
            
            // Process the download queue up to available slots
            val startedCount = processDownloadQueue(availableSlots)
            
            Log.d(TAG, "Queue processing complete. Started $startedCount downloads")
            
            // Check if there are still downloads in queue after processing
            val remainingDownloads = downloadRepository.getDownloadQueue()
            if (remainingDownloads.isNotEmpty() && startedCount > 0) {
                // Immediately queue another processing cycle if there are more downloads
                Log.d(TAG, "🔄 ${remainingDownloads.size} downloads still queued, scheduling immediate re-processing")
                enqueueImmediateProcessing()
            }
            
            Result.success(workDataOf(
                "status" to "success",
                "downloads_started" to startedCount,
                "remaining_queue_size" to remainingDownloads.size,
                "message" to "Started $startedCount downloads, ${remainingDownloads.size} remaining"
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Queue processing failed with exception: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            Result.failure(workDataOf(
                "error" to (e.message ?: "Unknown error"),
                "exception_type" to e.javaClass.simpleName
            ))
        }
    }

    /**
     * Gets the current count of running download workers
     */
    private suspend fun getCurrentRunningDownloadCount(): Int {
        return try {
            val workManager = WorkManager.getInstance(applicationContext)
            val runningWork = workManager.getWorkInfosByTag(AUDIO_DOWNLOAD_TAG).get()
                .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            
            runningWork.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get running download count", e)
            MAX_CONCURRENT_DOWNLOADS // Assume max to be safe
        }
    }

    /**
     * Processes the download queue and enqueues new AudioDownloadWorkers
     */
    private suspend fun processDownloadQueue(availableSlots: Int): Int {
        var enqueuedCount = 0
        
        Log.d(TAG, "Processing download queue with $availableSlots available slots")
        
        // Check if we have any queued downloads at all
        try {
            val queuedDownloads = downloadRepository.getDownloadQueue()
            Log.d(TAG, "Total queued downloads in database: ${queuedDownloads.size}")
            if (queuedDownloads.isNotEmpty()) {
                Log.d(TAG, "Queued downloads: ${queuedDownloads.map { "${it.id}:${it.trackFilename}" }}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download queue for debugging", e)
        }
        
        repeat(availableSlots) {
            // Get the next queued download based on priority
            Log.d(TAG, "Fetching next queued download from repository...")
            val queuedDownload = try {
                downloadRepository.getNextQueuedDownload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get next queued download", e)
                return@repeat
            }
            
            if (queuedDownload == null) {
                Log.d(TAG, "No more queued downloads available")
                return@repeat
            }
            
            try {
                // Update status to indicate processing
                downloadRepository.updateDownloadStatus(queuedDownload.id, DownloadStatus.DOWNLOADING)
                
                // Create the download URL
                val downloadUrl = "https://archive.org/download/${queuedDownload.recordingId}/${queuedDownload.trackFilename}"
                
                // Create input data for the AudioDownloadWorker
                val inputData = workDataOf(
                    AudioDownloadWorker.KEY_DOWNLOAD_ID to queuedDownload.id,
                    AudioDownloadWorker.KEY_RECORDING_ID to queuedDownload.recordingId,
                    AudioDownloadWorker.KEY_TRACK_FILENAME to queuedDownload.trackFilename,
                    AudioDownloadWorker.KEY_DOWNLOAD_URL to downloadUrl
                )
                
                // Create the download work request
                val downloadWorkRequest = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                    .setInputData(inputData)
                    .addTag(AUDIO_DOWNLOAD_TAG)
                    .addTag(QUEUE_MANAGED_TAG)
                    .addTag("download_${queuedDownload.id}")
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresStorageNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30, // 30 second initial backoff for download retries
                        java.util.concurrent.TimeUnit.SECONDS
                    )
                    .build()
                
                // Enqueue the download worker
                val workManager = WorkManager.getInstance(applicationContext)
                workManager.enqueue(downloadWorkRequest)
                
                enqueuedCount++
                Log.d(TAG, "Enqueued download worker for: ${queuedDownload.recordingId}/${queuedDownload.trackFilename}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue download for ${queuedDownload.id}", e)
                // Revert status back to queued on failure
                downloadRepository.updateDownloadStatus(queuedDownload.id, DownloadStatus.QUEUED)
            }
        }
        
        return enqueuedCount
    }

    /**
     * Cleans up orphaned workers and handles cancelled downloads
     */
    private suspend fun cleanupWorkers() {
        try {
            val workManager = WorkManager.getInstance(applicationContext)
            
            // Get all queue-managed workers
            val allWorkers = workManager.getWorkInfosByTag(QUEUE_MANAGED_TAG).get()
            
            // Cancel workers for downloads that are no longer active
            for (workInfo in allWorkers) {
                if (workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED) {
                    val downloadTag = workInfo.tags.find { it.startsWith("download_") }
                    if (downloadTag != null) {
                        val downloadId = downloadTag.removePrefix("download_")
                        val download = downloadRepository.getDownloadById(downloadId)
                        
                        // Cancel worker if download is cancelled or doesn't exist
                        if (download == null || download.status == DownloadStatus.CANCELLED) {
                            Log.d(TAG, "Cancelling orphaned worker for download: $downloadId")
                            workManager.cancelWorkById(workInfo.id)
                        }
                    }
                }
            }
            
            // Clean up completed/failed workers periodically (keep last 24 hours)
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
            val oldCompletedWorkers = allWorkers.filter { workInfo ->
                (workInfo.state == WorkInfo.State.SUCCEEDED || 
                 workInfo.state == WorkInfo.State.FAILED ||
                 workInfo.state == WorkInfo.State.CANCELLED) &&
                workInfo.outputData.getLong("completion_time", Long.MAX_VALUE) < cutoffTime
            }
            
            if (oldCompletedWorkers.isNotEmpty()) {
                Log.d(TAG, "Cleaning up ${oldCompletedWorkers.size} old completed workers")
                for (oldWorker in oldCompletedWorkers) {
                    workManager.cancelWorkById(oldWorker.id)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup workers", e)
        }
    }
    
    /**
     * Immediately enqueue another instance of this worker to continue processing
     */
    private fun enqueueImmediateProcessing() {
        try {
            val workManager = WorkManager.getInstance(applicationContext)
            val immediateWorkRequest = createImmediateWorkRequest()
            workManager.enqueue(immediateWorkRequest)
            Log.d(TAG, "🚀 Immediate queue processing work enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue immediate processing", e)
        }
    }
}