package com.deadarchive.core.data.debug

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import androidx.work.WorkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility for testing WorkManager configuration
 */
@Singleton
class WorkManagerDebugUtil @Inject constructor(
    @ApplicationContext val context: Context
) {
    
    companion object {
        private const val TAG = "WorkManagerDebug"
    }
    
    /**
     * Verify WorkManager is properly initialized
     */
    fun verifyWorkManagerInitialization(): Boolean {
        return try {
            val workManager = WorkManager.getInstance(context)
            Log.d(TAG, "✅ WorkManager initialized successfully")
            
            // Check if we can query work info (this would fail if not properly initialized)
            val workInfos = workManager.getWorkInfosByTag("test-tag").get()
            Log.d(TAG, "✅ WorkManager query successful, found ${workInfos.size} work items")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ WorkManager initialization failed", e)
            false
        }
    }
    
    /**
     * Log WorkManager configuration details
     */
    fun logWorkManagerStatus() {
        try {
            val workManager = WorkManager.getInstance(context)
            
            Log.d(TAG, "=== WorkManager Status ===")
            Log.d(TAG, "WorkManager instance: ${workManager.javaClass.simpleName}")
            
            // Get all work info
            val allWork = workManager.getWorkInfos(
                androidx.work.WorkQuery.Builder
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
            
            Log.d(TAG, "Total work items: ${allWork.size}")
            
            allWork.forEach { workInfo ->
                Log.d(TAG, "Work ${workInfo.id}: ${workInfo.state} (tags: ${workInfo.tags})")
            }
            
            Log.d(TAG, "=== End WorkManager Status ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get WorkManager status", e)
        }
    }
}