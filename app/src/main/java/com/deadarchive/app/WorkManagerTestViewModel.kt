package com.deadarchive.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.workDataOf
import com.deadarchive.core.data.debug.WorkManagerDebugUtil
import com.deadarchive.core.data.download.worker.AudioDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorkManagerTestViewModel @Inject constructor(
    private val workManagerDebugUtil: WorkManagerDebugUtil
) : ViewModel() {
    
    companion object {
        private const val TAG = "WorkManagerTestViewModel"
    }
    
    private val _uiState = MutableStateFlow(WorkManagerTestUiState())
    val uiState: StateFlow<WorkManagerTestUiState> = _uiState.asStateFlow()
    
    init {
        // Automatically test on initialization
        testWorkManagerInitialization()
    }
    
    fun testWorkManagerInitialization() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing WorkManager initialization")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = ""
                )
                
                val isInitialized = workManagerDebugUtil.verifyWorkManagerInitialization()
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialized = isInitialized,
                    statusMessage = if (isInitialized) {
                        "WorkManager is properly configured and operational"
                    } else {
                        "WorkManager initialization has issues"
                    },
                    lastTestTime = currentTime
                )
                
                // Also refresh the work status
                refreshWorkManagerStatus()
                
            } catch (e: Exception) {
                Log.e(TAG, "WorkManager test failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialized = false,
                    statusMessage = "Test failed with error",
                    errorMessage = e.message ?: "Unknown error occurred",
                    lastTestTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
            }
        }
    }
    
    fun refreshWorkManagerStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing WorkManager status")
                
                // Get WorkManager instance and query all work
                val workManager = WorkManager.getInstance(workManagerDebugUtil.context)
                val allWork = workManager.getWorkInfos(
                    WorkQuery.Builder
                        .fromStates(
                            listOf(
                                WorkInfo.State.ENQUEUED,
                                WorkInfo.State.RUNNING,
                                WorkInfo.State.SUCCEEDED,
                                WorkInfo.State.FAILED,
                                WorkInfo.State.BLOCKED,
                                WorkInfo.State.CANCELLED
                            )
                        )
                        .build()
                ).get()
                
                // Count work by state
                val enqueuedCount = allWork.count { it.state == WorkInfo.State.ENQUEUED }
                val runningCount = allWork.count { it.state == WorkInfo.State.RUNNING }
                val succeededCount = allWork.count { it.state == WorkInfo.State.SUCCEEDED }
                val failedCount = allWork.count { it.state == WorkInfo.State.FAILED }
                val cancelledCount = allWork.count { it.state == WorkInfo.State.CANCELLED }
                val blockedCount = allWork.count { it.state == WorkInfo.State.BLOCKED }
                
                // Create work details
                val workDetails = allWork.map { workInfo ->
                    WorkDetail(
                        id = workInfo.id.toString(),
                        state = workInfo.state,
                        tags = workInfo.tags,
                        runAttemptCount = workInfo.runAttemptCount
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    totalWorkItems = allWork.size,
                    enqueuedWork = enqueuedCount,
                    runningWork = runningCount,
                    succeededWork = succeededCount,
                    failedWork = failedCount,
                    cancelledWork = cancelledCount,
                    blockedWork = blockedCount,
                    workDetails = workDetails
                )
                
                Log.d(TAG, "WorkManager status refreshed: ${allWork.size} total work items")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh WorkManager status", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to refresh status: ${e.message}"
                )
            }
        }
    }
    
    fun getDetailedStatusReport(): String {
        val state = _uiState.value
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        return buildString {
            appendLine("=== WorkManager Status Report ===")
            appendLine("Generated: $timestamp")
            appendLine()
            
            appendLine("## Configuration")
            appendLine("- Max Scheduler Limit: 20")
            appendLine("- Minimum Logging Level: INFO")
            appendLine("- Worker Factory: HiltWorkerFactory")
            appendLine("- Initialization Status: ${if (state.isInitialized) "✅ Ready" else "❌ Failed"}")
            appendLine("- Last Test: ${state.lastTestTime}")
            appendLine()
            
            appendLine("## Work Queue Status")
            appendLine("- Total Work Items: ${state.totalWorkItems}")
            appendLine("- Enqueued: ${state.enqueuedWork}")
            appendLine("- Running: ${state.runningWork}")
            appendLine("- Succeeded: ${state.succeededWork}")
            appendLine("- Failed: ${state.failedWork}")
            appendLine("- Cancelled: ${state.cancelledWork}")
            appendLine("- Blocked: ${state.blockedWork}")
            appendLine()
            
            if (state.workDetails.isNotEmpty()) {
                appendLine("## Work Items Detail")
                state.workDetails.forEach { work ->
                    appendLine("- Work ID: ${work.id.take(8)}...")
                    appendLine("  State: ${work.state}")
                    appendLine("  Tags: ${work.tags.joinToString()}")
                    appendLine("  Run Attempts: ${work.runAttemptCount}")
                    appendLine()
                }
            }
            
            if (state.errorMessage.isNotEmpty()) {
                appendLine("## Error Details")
                appendLine(state.errorMessage)
                appendLine()
            }
            
            appendLine("## System Information")
            appendLine("- Android Version: API ${android.os.Build.VERSION.SDK_INT}")
            appendLine("- Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("- App Process: ${android.os.Process.myPid()}")
            appendLine("- WorkManager Version: 2.9.0")
            appendLine("- Hilt Work Version: 1.1.0")
            appendLine()
            
            appendLine("=== End Report ===")
        }
    }
    
    fun clearCopyConfirmation() {
        _uiState.value = _uiState.value.copy(showCopyConfirmation = false)
    }
    
    /**
     * Test the AudioDownloadWorker with a sample download
     */
    fun testDownloadWorker() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Testing AudioDownloadWorker")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    errorMessage = ""
                )
                
                // Create a test download request for a small Grateful Dead track
                val downloadId = "test_download_${System.currentTimeMillis()}"
                val testRecordingId = "gd1977-05-08.sbd.miller.97245.flac16"
                val testTrackFilename = "gd77-05-08d1t01.flac"
                val testDownloadUrl = "https://archive.org/download/$testRecordingId/$testTrackFilename"
                
                val inputData = workDataOf(
                    AudioDownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    AudioDownloadWorker.KEY_RECORDING_ID to testRecordingId,
                    AudioDownloadWorker.KEY_TRACK_FILENAME to testTrackFilename,
                    AudioDownloadWorker.KEY_DOWNLOAD_URL to testDownloadUrl
                )
                
                val downloadWorkRequest = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                    .setInputData(inputData)
                    .addTag("test_download")
                    .build()
                
                val workManager = WorkManager.getInstance(workManagerDebugUtil.context)
                workManager.enqueue(downloadWorkRequest)
                
                Log.d(TAG, "Download worker enqueued with ID: ${downloadWorkRequest.id}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Test download worker enqueued successfully! Check work queue for progress."
                )
                
                // Refresh status to show the new work item
                refreshWorkManagerStatus()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test download worker", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to test download worker: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cancel all running work items (useful for testing)
     */
    fun cancelAllWork() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cancelling all work")
                val workManager = WorkManager.getInstance(workManagerDebugUtil.context)
                workManager.cancelAllWork()
                
                _uiState.value = _uiState.value.copy(
                    statusMessage = "All work cancelled"
                )
                
                // Refresh status after a delay to show the changes
                kotlinx.coroutines.delay(1000)
                refreshWorkManagerStatus()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel work", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to cancel work: ${e.message}"
                )
            }
        }
    }
}