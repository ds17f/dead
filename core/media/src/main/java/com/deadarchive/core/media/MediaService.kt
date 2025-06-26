package com.deadarchive.core.media

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MediaService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}