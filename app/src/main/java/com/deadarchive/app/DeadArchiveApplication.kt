package com.deadarchive.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.deadarchive.core.data.download.DownloadQueueManager
import com.deadarchive.core.data.repository.RatingsRepository
import com.deadarchive.core.media.player.PlaybackResumeService
import com.deadarchive.core.media.player.LastPlayedTrackService
import com.deadarchive.core.media.player.LastPlayedTrackMonitor
import com.deadarchive.core.settings.api.SettingsRepository
import com.deadarchive.core.data.service.UpdateService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.firstOrNull
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
    
    @Inject
    lateinit var playbackResumeService: PlaybackResumeService
    
    @Inject
    lateinit var lastPlayedTrackService: LastPlayedTrackService
    
    @Inject
    lateinit var lastPlayedTrackMonitor: LastPlayedTrackMonitor
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var updateService: UpdateService
    
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
        
        // Restore last played track (simple Spotify-like approach)
        applicationScope.launch {
            try {
                android.util.Log.d("DeadArchiveApplication", "Attempting to restore last played track")
                lastPlayedTrackService.restoreLastPlayedTrack()
                android.util.Log.d("DeadArchiveApplication", "✅ Last played track restoration completed")
            } catch (e: Exception) {
                android.util.Log.e("DeadArchiveApplication", "❌ Failed to restore last played track", e)
            }
        }
        
        // Ensure the monitor is started (it should auto-start via DI, but just to be safe)
        lastPlayedTrackMonitor.startMonitoring()
        
        // Check for app updates on startup if enabled
        applicationScope.launch {
            try {
                val settings = settingsRepository.getSettings().firstOrNull()
                if (settings?.autoUpdateCheckEnabled == true) {
                    android.util.Log.d("DeadArchiveApplication", "Checking for app updates on startup")
                    updateService.checkForUpdates()
                    android.util.Log.d("DeadArchiveApplication", "✅ Startup update check completed")
                }
            } catch (e: Exception) {
                android.util.Log.e("DeadArchiveApplication", "❌ Failed to check for updates on startup", e)
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