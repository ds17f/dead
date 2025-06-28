package com.deadarchive.core.media.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.deadarchive.core.media.player.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class DeadArchivePlaybackService : MediaSessionService() {
    
    @Inject
    lateinit var player: ExoPlayer
    
    @Inject
    lateinit var notificationManager: PlayerNotificationManager
    
    private var mediaSession: MediaSession? = null
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure audio attributes for music playback
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        
        player.setAudioAttributes(audioAttributes, true)
        
        // Create MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(createSessionActivityPendingIntent())
            .build()
        
        // Initialize notification manager
        notificationManager.initialize(this, mediaSession!!)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        notificationManager.release()
        super.onDestroy()
    }
    
    private fun createSessionActivityPendingIntent(): PendingIntent {
        // This should point to your main activity
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}