package com.deadarchive.core.media.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.deadarchive.core.media.R
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlayerNotificationManager @Inject constructor() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "dead_archive_playback"
        private const val CHANNEL_NAME = "Dead Archive Playback"
    }
    
    private var notificationManager: NotificationManager? = null
    private var mediaSession: MediaSession? = null
    
    fun initialize(context: Context, mediaSession: MediaSession) {
        this.mediaSession = mediaSession
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls"
            setShowBadge(false)
        }
        
        notificationManager?.createNotificationChannel(channel)
    }
    
    fun createNotification(context: Context): Notification {
        val mediaSession = this.mediaSession ?: throw IllegalStateException("MediaSession not initialized")
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Dead Archive")
            .setContentText("Playing Grateful Dead concert")
            .setSmallIcon(R.drawable.ic_music_note) // We'll need to create this icon
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .build()
    }
    
    fun showNotification(context: Context) {
        val notification = createNotification(context)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    fun hideNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }
    
    fun release() {
        hideNotification()
        notificationManager = null
        mediaSession = null
    }
}