package com.deadly.core.data.download.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple test worker to verify Hilt injection is working
 */
@HiltWorker
class TestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParam: WorkerParameters
) : CoroutineWorker(context, workerParam) {

    companion object {
        private const val TAG = "TestWorker"
    }

    init {
        Log.d(TAG, "TestWorker constructor called - Hilt injection working!")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "TestWorker.doWork() called - Worker execution working!")
            Result.success(workDataOf("test" to "success"))
        } catch (e: Exception) {
            Log.e(TAG, "TestWorker failed", e)
            Result.failure(workDataOf("error" to e.message))
        }
    }
}