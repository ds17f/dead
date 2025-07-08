package com.deadarchive.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.deadarchive.core.data.download.DownloadQueueManager
import com.deadarchive.core.data.repository.RatingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DeadArchiveApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject 
    lateinit var downloadQueueManager: DownloadQueueManager
    
    @Inject
    lateinit var ratingsRepository: RatingsRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager with custom configuration
        WorkManager.initialize(this, workManagerConfiguration)
        
        // Initialize ratings database
        applicationScope.launch {
            try {
                ratingsRepository.initializeRatingsIfNeeded()
                android.util.Log.d("DeadArchiveApplication", "✅ Ratings database initialized")
            } catch (e: Exception) {
                android.util.Log.e("DeadArchiveApplication", "❌ Failed to initialize ratings database", e)
            }
        }
        
        // Start download queue processing
        applicationScope.launch {
            try {
                downloadQueueManager.startQueueProcessing()
                android.util.Log.d("DeadArchiveApplication", "✅ Download queue processing started")
            } catch (e: Exception) {
                android.util.Log.e("DeadArchiveApplication", "❌ Failed to start download queue processing", e)
            }
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMaxSchedulerLimit(20)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}