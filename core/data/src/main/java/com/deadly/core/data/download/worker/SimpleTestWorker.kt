package com.deadly.core.data.download.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SimpleTestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val TAG = "SimpleTestWorker"
    }

    init {
        Log.d(TAG, "SimpleTestWorker constructor - NO CONSTRAINTS TEST")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SimpleTestWorker.doWork() - EXECUTING IMMEDIATELY")
        Log.d(TAG, "This proves WorkManager + Hilt integration works perfectly!")
        
        return Result.success()
    }
}