package com.deadarchive.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.deadarchive.core.design.component.DebugMultilineText
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.media.player.QueueStateManager
import com.deadarchive.core.media.player.QueueItem

/**
 * Debug panel for displaying real-time queue state information
 * Provides visibility into current queue position, navigation state, and queue contents
 */
@Composable
fun QueueDebugPanel(
    queueStateManager: QueueStateManager,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    initiallyExpanded: Boolean = false
) {
    if (!isVisible) return
    
    val queueUrls by queueStateManager.queueUrls.collectAsState(initial = emptyList())
    val queueMetadata by queueStateManager.queueMetadata.collectAsState(initial = emptyList())
    val currentIndex by queueStateManager.queueIndex.collectAsState(initial = 0)
    val hasNext by queueStateManager.hasNext.collectAsState(initial = false)
    val hasPrevious by queueStateManager.hasPrevious.collectAsState(initial = false)
    val currentRecording by queueStateManager.currentRecording.collectAsState(initial = null)
    
    val clipboardManager = LocalClipboardManager.current
    
    DebugPanel(
        title = "Queue State",
        modifier = modifier,
        isVisible = isVisible,
        initiallyExpanded = initiallyExpanded
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navigation State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                            text = "Navigation State",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val content = buildString {
                                    appendLine("=== Queue Navigation State ===")
                                    appendLine("Current Index: $currentIndex")
                                    appendLine("Has Next: $hasNext")
                                    appendLine("Has Previous: $hasPrevious")
                                    appendLine("Queue Size: ${queueUrls.size}")
                                }
                                clipboardManager.setText(AnnotatedString(content))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = IconResources.Content.FileCopy(),
                                contentDescription = "Copy navigation state",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DebugText("Current Index", currentIndex.toString())
                    DebugText("Has Next", hasNext.toString())
                    DebugText("Has Previous", hasPrevious.toString())
                    DebugText("Queue Size", queueUrls.size.toString())
                }
            }
            
            // Current Track
            if (currentIndex >= 0 && currentIndex < queueMetadata.size) {
                val currentTrack = queueMetadata[currentIndex]
                val currentUrl = if (currentIndex < queueUrls.size) queueUrls[currentIndex] else "N/A"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                                text = "Current Track",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val content = buildString {
                                        appendLine("=== Current Queue Track ===")
                                        appendLine("Track URL: $currentUrl")
                                        appendLine("Song Title: ${currentTrack.second}")
                                        appendLine("Media ID: ${currentTrack.first}")
                                        appendLine("Index: $currentIndex")
                                        currentRecording?.let { recording ->
                                            appendLine("Recording: ${recording.title}")
                                            appendLine("Concert Date: ${recording.concertDate}")
                                            appendLine("Venue: ${recording.concertVenue ?: "N/A"}")
                                            appendLine("Location: ${recording.concertLocation ?: "N/A"}")
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(content))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = IconResources.Content.FileCopy(),
                                    contentDescription = "Copy current track",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        DebugText("Track URL", currentUrl)
                        DebugText("Song Title", currentTrack.second)
                        DebugText("Media ID", currentTrack.first)
                        DebugText("Index", currentIndex.toString())
                        
                        currentRecording?.let { recording ->
                            DebugDivider()
                            DebugText("Recording", recording.title)
                            DebugText("Concert Date", recording.concertDate)
                            DebugText("Venue", recording.concertVenue ?: "N/A")
                            DebugText("Location", recording.concertLocation ?: "N/A")
                        }
                    }
                }
            }
            
            // Queue Contents Summary
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
                            text = "Queue Contents",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val content = buildString {
                                    appendLine("=== Complete Queue Contents ===")
                                    appendLine("Total Items: ${queueMetadata.size}")
                                    appendLine("Current Index: $currentIndex")
                                    appendLine()
                                    queueMetadata.forEachIndexed { index, (mediaId, title) ->
                                        val marker = if (index == currentIndex) "► " else "  "
                                        appendLine("${marker}[$index] $title")
                                        appendLine("    Media ID: $mediaId")
                                        if (index < queueUrls.size) {
                                            appendLine("    URL: ${queueUrls[index]}")
                                        }
                                        appendLine()
                                    }
                                }
                                clipboardManager.setText(AnnotatedString(content))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = IconResources.Content.FileCopy(),
                                contentDescription = "Copy queue contents",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (queueMetadata.isEmpty()) {
                        Text(
                            text = "Queue is empty",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Show condensed queue overview
                        val queueSummary = buildString {
                            val maxItems = 5
                            val startIndex = maxOf(0, currentIndex - 2)
                            val endIndex = minOf(queueMetadata.size, startIndex + maxItems)
                            
                            for (i in startIndex until endIndex) {
                                val (mediaId, title) = queueMetadata[i]
                                val marker = if (i == currentIndex) "► " else "  "
                                appendLine("${marker}[$i] $title")
                            }
                            
                            if (endIndex < queueMetadata.size) {
                                appendLine("  ... and ${queueMetadata.size - endIndex} more")
                            }
                        }
                        
                        DebugMultilineText(
                            label = "Queue Overview",
                            value = queueSummary.trim(),
                            maxLines = 8
                        )
                    }
                }
            }
        }
    }
}