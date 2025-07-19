package com.deadarchive.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.DebugPanel
import com.deadarchive.core.design.component.DebugText
import com.deadarchive.core.design.component.DebugDivider
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.media.player.PlaybackEventTracker
import androidx.media3.common.Player
import java.text.SimpleDateFormat
import java.util.*

/**
 * Debug panel for displaying real-time Media3 playback events from PlaybackEventTracker
 * Provides visibility into the exact sequence and timing of Media3 events that will
 * be used for playback history tracking.
 * 
 * Key features:
 * - Real-time event stream with timestamps
 * - Session state monitoring
 * - Event statistics and filtering
 * - Export functionality for debugging
 * - Event type highlighting for quick identification
 */
@Composable
fun PlaybackEventsDebugPanel(
    playbackEventTracker: PlaybackEventTracker,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    initiallyExpanded: Boolean = false
) {
    if (!isVisible) return
    
    val events by playbackEventTracker.events.collectAsState()
    val currentEvent by playbackEventTracker.currentEvent.collectAsState()
    val isSessionActive by playbackEventTracker.isSessionActive.collectAsState()
    val sessionStartTime by playbackEventTracker.sessionStartTime.collectAsState()
    val totalEventsTracked by playbackEventTracker.totalEventsTracked.collectAsState()
    val transitionEventsTracked by playbackEventTracker.transitionEventsTracked.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    
    DebugPanel(
        title = "Media3 Playback Events",
        modifier = modifier,
        isVisible = isVisible,
        initiallyExpanded = initiallyExpanded
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Session Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSessionActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Session Status",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val content = buildString {
                                    appendLine("=== Media3 Event Session Status ===")
                                    appendLine("Session Active: $isSessionActive")
                                    appendLine("Session Start: ${sessionStartTime?.let { dateFormatter.format(Date(it)) } ?: "N/A"}")
                                    appendLine("Total Events: $totalEventsTracked")
                                    appendLine("Transition Events: $transitionEventsTracked")
                                    sessionStartTime?.let {
                                        val duration = System.currentTimeMillis() - it
                                        appendLine("Session Duration: ${duration}ms")
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(content))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = IconResources.Content.FileCopy(),
                                contentDescription = "Copy session status",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DebugText("Session Active", isSessionActive.toString())
                    sessionStartTime?.let { startTime ->
                        DebugText("Session Started", dateFormatter.format(Date(startTime)))
                        val duration = System.currentTimeMillis() - startTime
                        DebugText("Session Duration", "${duration / 1000}s")
                    }
                    DebugText("Total Events", totalEventsTracked.toString())
                    DebugText("Transition Events", transitionEventsTracked.toString())
                }
            }
            
            // Current Event Card
            currentEvent?.let { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = getEventTypeColor(event.eventType).copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latest Event",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val content = buildString {
                                        appendLine("=== Latest Media3 Event ===")
                                        appendLine("Event Type: ${event.eventType}")
                                        appendLine("Timestamp: ${dateFormatter.format(Date(event.timestamp))}")
                                        appendLine("Track URL: ${event.trackUrl ?: "N/A"}")
                                        appendLine("Track Title: ${event.trackTitle ?: "N/A"}")
                                        appendLine("Is Playing: ${event.isPlaying}")
                                        appendLine("Playback State: ${event.playbackState}")
                                        appendLine("Position: ${event.position}ms")
                                        event.reason?.let { appendLine("Reason: ${event.reasonName} ($it)") }
                                        appendLine("Session Context: ${event.sessionContext}")
                                    }
                                    clipboardManager.setText(AnnotatedString(content))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = IconResources.Content.FileCopy(),
                                    contentDescription = "Copy latest event",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        DebugText("Event Type", event.eventType.toString())
                        DebugText("Timestamp", dateFormatter.format(Date(event.timestamp)))
                        event.trackTitle?.let { DebugText("Track", it) }
                        event.trackUrl?.let { DebugText("URL", it) }
                        DebugText("Playing", event.isPlaying.toString())
                        DebugText("State", getPlaybackStateName(event.playbackState))
                        DebugText("Position", "${event.position}ms")
                        event.reasonName?.let { DebugText("Reason", it) }
                    }
                }
            }
            
            // Event History
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Events (${events.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            TextButton(
                                onClick = { playbackEventTracker.clearEvents() }
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = {
                                    val content = buildString {
                                        appendLine("=== Complete Media3 Event History ===")
                                        appendLine("Total Events: ${events.size}")
                                        appendLine()
                                        events.forEach { event ->
                                            appendLine("${dateFormatter.format(Date(event.timestamp))} - ${event.eventType}")
                                            event.trackTitle?.let { appendLine("  Track: $it") }
                                            event.reasonName?.let { appendLine("  Reason: $it") }
                                            appendLine("  Playing: ${event.isPlaying}, State: ${getPlaybackStateName(event.playbackState)}")
                                            appendLine("  Position: ${event.position}ms")
                                            appendLine()
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(content))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = IconResources.Content.FileCopy(),
                                    contentDescription = "Copy event history",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (events.isEmpty()) {
                        Text(
                            text = "No events tracked yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Show recent events in a scrollable list
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = maxOf(0, events.size - 1)
                        )
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(events.takeLast(20)) { event ->
                                EventItemCard(
                                    event = event,
                                    dateFormatter = dateFormatter
                                )
                            }
                        }
                        
                        if (events.size > 20) {
                            Text(
                                text = "Showing last 20 of ${events.size} events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItemCard(
    event: PlaybackEventTracker.PlaybackEvent,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getEventTypeColor(event.eventType).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.eventType.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateFormatter.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            event.trackTitle?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            if (event.eventType == PlaybackEventTracker.EventType.MEDIA_ITEM_TRANSITION) {
                event.reasonName?.let { reason ->
                    Text(
                        text = "Reason: $reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun getEventTypeColor(eventType: PlaybackEventTracker.EventType): androidx.compose.ui.graphics.Color {
    return when (eventType) {
        PlaybackEventTracker.EventType.MEDIA_ITEM_TRANSITION -> MaterialTheme.colorScheme.primary
        PlaybackEventTracker.EventType.IS_PLAYING_CHANGED -> MaterialTheme.colorScheme.secondary
        PlaybackEventTracker.EventType.PLAYBACK_STATE_CHANGED -> MaterialTheme.colorScheme.tertiary
        PlaybackEventTracker.EventType.SESSION_START -> MaterialTheme.colorScheme.primaryContainer
        PlaybackEventTracker.EventType.SESSION_END -> MaterialTheme.colorScheme.errorContainer
        PlaybackEventTracker.EventType.PLAYER_ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun getPlaybackStateName(state: Int): String {
    return when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }
}