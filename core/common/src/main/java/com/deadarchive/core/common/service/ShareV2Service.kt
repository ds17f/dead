package com.deadarchive.core.common.service

import android.content.Context
import android.content.Intent
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Track
import com.deadarchive.core.common.util.ArchiveUrlUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * V2 Service for sharing show and recording information via system share intents
 * Following V2 architecture patterns while maintaining V1 functionality
 */
@Singleton
class ShareV2Service @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Share a show with recording information
     * @param show The show to share
     * @param recording The current recording being played
     */
    fun shareShow(show: Show, recording: Recording) {
        val message = buildShowShareMessage(show, recording)
        val shareIntent = createShareIntent("Check out this Grateful Dead show!", message)
        context.startActivity(shareIntent)
    }
    
    /**
     * Share a specific track from a recording
     * @param show The show containing the track
     * @param recording The recording containing the track
     * @param track The specific track to share
     * @param currentPosition Optional current playback position in seconds
     */
    fun shareTrack(show: Show, recording: Recording, track: Track, currentPosition: Long? = null) {
        val message = buildTrackShareMessage(show, recording, track, currentPosition)
        val shareIntent = createShareIntent("Check out this Grateful Dead track!", message)
        context.startActivity(shareIntent)
    }
    
    /**
     * Build a formatted message for sharing a show
     */
    private fun buildShowShareMessage(show: Show, recording: Recording): String {
        val url = ArchiveUrlUtil.getRecordingUrl(recording)
        
        return buildString {
            appendLine("🎵 Grateful Dead - ${show.displayDate}")
            appendLine()
            appendLine("📍 ${show.displayVenue}")
            if (!show.displayLocation.isNullOrBlank() && show.displayLocation != "Unknown Location") {
                appendLine("🌎 ${show.displayLocation}")
            }
            appendLine()
            
            // Add recording info
            recording.source?.let { source ->
                appendLine("🎧 Source: $source")
            }
            recording.taper?.let { taper ->
                appendLine("📼 Taper: $taper")
            }
            
            if (show.hasRawRating) {
                appendLine("⭐ Rating: ${show.displayRating}")
            }
            
            appendLine()
            appendLine("🔗 Listen on Archive.org:")
            append(url)
        }
    }
    
    /**
     * Build a formatted message for sharing a track
     */
    private fun buildTrackShareMessage(
        show: Show, 
        recording: Recording, 
        track: Track, 
        currentPosition: Long?
    ): String {
        val url = if (currentPosition != null && currentPosition > 0) {
            ArchiveUrlUtil.getTrackUrlWithTime(recording, track, currentPosition)
        } else {
            ArchiveUrlUtil.getTrackUrl(recording, track)
        }
        
        return buildString {
            appendLine("🎵 ${track.displayTitle}")
            appendLine("🎭 Grateful Dead - ${show.displayDate}")
            appendLine()
            appendLine("📍 ${show.displayVenue}")
            if (!show.displayLocation.isNullOrBlank() && show.displayLocation != "Unknown Location") {
                appendLine("🌎 ${show.displayLocation}")
            }
            
            // Track info
            if (track.trackNumber != null) {
                appendLine("🔢 Track ${track.trackNumber}")
            }
            if (track.formattedDuration.isNotBlank()) {
                appendLine("⏱️ Duration: ${track.formattedDuration}")
            }
            
            appendLine()
            
            // Add recording info
            recording.source?.let { source ->
                appendLine("🎧 Source: $source")
            }
            recording.taper?.let { taper ->
                appendLine("📼 Taper: $taper")
            }
            
            if (show.hasRawRating) {
                appendLine("⭐ Rating: ${show.displayRating}")
            }
            
            if (currentPosition != null && currentPosition > 0) {
                val minutes = currentPosition / 60
                val seconds = currentPosition % 60
                appendLine("▶️ Starting at: ${minutes}:${seconds.toString().padStart(2, '0')}")
            }
            
            appendLine()
            appendLine("🔗 Listen on Archive.org:")
            append(url)
        }
    }
    
    /**
     * Create a system share intent
     */
    private fun createShareIntent(subject: String, message: String): Intent {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        return Intent.createChooser(shareIntent, "Share via").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}