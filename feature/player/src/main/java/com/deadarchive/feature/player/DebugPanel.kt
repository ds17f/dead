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
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.CurrentTrackInfo
import com.deadarchive.core.design.component.IconResources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanel(
    onClose: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentTrackInfo by viewModel.mediaControllerRepository.currentTrackInfo.collectAsState()
    val isPlaying by viewModel.mediaControllerRepository.isPlaying.collectAsState()
    val currentPosition by viewModel.mediaControllerRepository.currentPosition.collectAsState()
    val duration by viewModel.mediaControllerRepository.duration.collectAsState()
    val currentTrackUrl by viewModel.mediaControllerRepository.currentTrackUrl.collectAsState()
    val currentRecordingId by viewModel.mediaControllerRepository.currentRecordingIdFlow.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Media Debug Panel",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Track Info Section
            DebugSection(
                title = "Current Track Info",
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onCopy = {
                    val content = if (currentTrackInfo != null) {
                        buildString {
                            appendLine("=== Current Track Info ===")
                            appendLine("Track URL: ${currentTrackInfo!!.trackUrl}")
                            appendLine("Recording ID: ${currentTrackInfo!!.recordingId}")
                            appendLine("Show ID: ${currentTrackInfo!!.showId}")
                            appendLine("Show Date: ${currentTrackInfo!!.showDate}")
                            appendLine("Venue: ${currentTrackInfo!!.venue ?: "N/A"}")
                            appendLine("Location: ${currentTrackInfo!!.location ?: "N/A"}")
                            appendLine("Song Title: ${currentTrackInfo!!.songTitle}")
                            appendLine("Track Number: ${currentTrackInfo!!.trackNumber?.toString() ?: "N/A"}")
                            appendLine("Filename: ${currentTrackInfo!!.filename}")
                            appendLine("Is Playing: ${currentTrackInfo!!.isPlaying}")
                            appendLine("Position: ${currentTrackInfo!!.position}ms")
                            appendLine("Duration: ${currentTrackInfo!!.duration}ms")
                            appendLine()
                            appendLine("Display Values:")
                            appendLine("Display Title: ${currentTrackInfo!!.displayTitle}")
                            appendLine("Display Subtitle: ${currentTrackInfo!!.displaySubtitle}")
                            appendLine("Display Artist: ${currentTrackInfo!!.displayArtist}")
                            appendLine("Album Title: ${currentTrackInfo!!.albumTitle}")
                        }
                    } else {
                        "=== Current Track Info ===\nNo current track info available"
                    }
                    clipboardManager.setText(AnnotatedString(content))
                }
            ) {
                if (currentTrackInfo != null) {
                    DebugKeyValue("Track URL", currentTrackInfo!!.trackUrl)
                    DebugKeyValue("Recording ID", currentTrackInfo!!.recordingId)
                    DebugKeyValue("Show ID", currentTrackInfo!!.showId)
                    DebugKeyValue("Show Date", currentTrackInfo!!.showDate)
                    DebugKeyValue("Venue", currentTrackInfo!!.venue ?: "N/A")
                    DebugKeyValue("Location", currentTrackInfo!!.location ?: "N/A")
                    DebugKeyValue("Song Title", currentTrackInfo!!.songTitle)
                    DebugKeyValue("Track Number", currentTrackInfo!!.trackNumber?.toString() ?: "N/A")
                    DebugKeyValue("Filename", currentTrackInfo!!.filename)
                    DebugKeyValue("Is Playing", currentTrackInfo!!.isPlaying.toString())
                    DebugKeyValue("Position", "${currentTrackInfo!!.position}ms")
                    DebugKeyValue("Duration", "${currentTrackInfo!!.duration}ms")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Display Values:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    DebugKeyValue("Display Title", currentTrackInfo!!.displayTitle)
                    DebugKeyValue("Display Subtitle", currentTrackInfo!!.displaySubtitle)
                    DebugKeyValue("Display Artist", currentTrackInfo!!.displayArtist)
                    DebugKeyValue("Album Title", currentTrackInfo!!.albumTitle)
                } else {
                    Text(
                        text = "No current track info available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Raw MediaController State
            DebugSection(
                title = "Raw MediaController State",
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                onCopy = {
                    val content = buildString {
                        appendLine("=== Raw MediaController State ===")
                        appendLine("Is Playing: $isPlaying")
                        appendLine("Current Position: ${currentPosition}ms")
                        appendLine("Duration: ${duration}ms")
                        appendLine("Current Track URL: ${currentTrackUrl ?: "N/A"}")
                        appendLine("Current Recording ID: ${currentRecordingId ?: "N/A"}")
                    }
                    clipboardManager.setText(AnnotatedString(content))
                }
            ) {
                DebugKeyValue("Is Playing", isPlaying.toString())
                DebugKeyValue("Current Position", "${currentPosition}ms")
                DebugKeyValue("Duration", "${duration}ms")
                DebugKeyValue("Current Track URL", currentTrackUrl ?: "N/A")
                DebugKeyValue("Current Recording ID", currentRecordingId ?: "N/A")
            }
            
            // Notification Preview
            if (currentTrackInfo != null) {
                DebugSection(
                    title = "Notification Preview",
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onCopy = {
                        val content = buildString {
                            appendLine("=== Notification Preview ===")
                            appendLine("How this will appear in notifications:")
                            appendLine()
                            appendLine("Title: ${currentTrackInfo!!.displayTitle}")
                            appendLine("Artist: ${currentTrackInfo!!.displayArtist}")
                            appendLine("Album: ${currentTrackInfo!!.displaySubtitle}")
                        }
                        clipboardManager.setText(AnnotatedString(content))
                    }
                ) {
                    Text(
                        text = "How this will appear in notifications:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = currentTrackInfo!!.displayTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = currentTrackInfo!!.displayArtist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentTrackInfo!!.displaySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onCopy: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and copy button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = IconResources.Content.FileCopy(),
                        contentDescription = "Copy $title",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
private fun DebugKeyValue(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$key:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(2f)
        )
    }
}