package com.deadly.core.data.download

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.deadly.core.data.download.worker.DownloadQueueManagerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the download queue processing worker lifecycle and scheduling.
 * This class provides a simple interface for starting, stopping, and triggering
 * the download queue management system.
 */
@Singleton
class DownloadQueueManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DownloadQueueManager"
    }
    
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }
    
    /**
     * Starts the periodic download queue processing.
     * This should be called when the app starts or when downloads are enabled.
     */
    fun startQueueProcessing() {
        try {
            Log.d(TAG, "Starting periodic download queue processing")
            
            val periodicRequest = DownloadQueueManagerWorker.createPeriodicWorkRequest()
            
            workManager.enqueueUniquePeriodicWork(
                DownloadQueueManagerWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already running
                periodicRequest
            )
            
            Log.d(TAG, "Periodic download queue processing started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start queue processing", e)
        }
    }
    
    /**
     * Stops the periodic download queue processing.
     * This should be called when downloads are disabled or app is being shut down.
     */
    fun stopQueueProcessing() {
        try {
            Log.d(TAG, "Stopping download queue processing")
            
            workManager.cancelUniqueWork(DownloadQueueManagerWorker.WORK_NAME)
            
            Log.d(TAG, "Download queue processing stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop queue processing", e)
        }
    }
    
    /**
     * Triggers an immediate queue processing check.
     * This should be called when new downloads are added to the queue.
     */
    fun triggerImmediateProcessing() {
        try {
            Log.d(TAG, "Triggering immediate queue processing")
            
            val immediateRequest = DownloadQueueManagerWorker.createImmediateWorkRequest()
            
            workManager.enqueueUniqueWork(
                "${DownloadQueueManagerWorker.WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE, // Replace any pending immediate work
                immediateRequest
            )
            
            Log.d(TAG, "Immediate queue processing triggered")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate processing", e)
        }
    }
    
    /**
     * Checks if the queue processing is currently active
     */
    suspend fun isQueueProcessingActive(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(DownloadQueueManagerWorker.WORK_NAME).get()
            workInfos.any { !it.state.isFinished }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check queue processing status", e)
            false
        }
    }
    
    /**
     * Gets the current status of the queue processing system
     */
    suspend fun getQueueProcessingStatus(): QueueProcessingStatus {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(DownloadQueueManagerWorker.WORK_NAME).get()
            val latestWork = workInfos.maxByOrNull { it.runAttemptCount }
            
            when {
                latestWork == null -> QueueProcessingStatus.NOT_STARTED
                latestWork.state.isFinished -> {
                    if (latestWork.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        QueueProcessingStatus.COMPLETED
                    } else {
                        QueueProcessingStatus.FAILED
                    }
                }
                else -> QueueProcessingStatus.RUNNING
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get queue processing status", e)
            QueueProcessingStatus.UNKNOWN
        }
    }
}

/**
 * Represents the current status of the download queue processing system
 */
enum class QueueProcessingStatus {
    NOT_STARTED,
    RUNNING,
    COMPLETED,
    FAILED,
    UNKNOWN
}