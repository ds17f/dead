package com.deadarchive.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.deadarchive.core.data.debug.WorkManagerDebugUtil
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
}