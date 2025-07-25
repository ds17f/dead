package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.data.download.EnrichedDownloadState

@Composable
fun EnrichedDownloadQueueItem(
    enrichedDownload: EnrichedDownloadState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onForceStart: () -> Unit,
    onNavigateToShow: () -> Unit
) {
    val download = enrichedDownload.downloadState
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (download.status) {
                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with enriched show info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show and track info (enriched)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = enrichedDownload.displayShowName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${enrichedDownload.displayTrackNumber}. ${enrichedDownload.displayTrackTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = enrichedDownload.downloadUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status badge
                StatusBadge(status = download.status)
            }
            
            // Progress bar (if in progress)
            if (download.status == DownloadStatus.DOWNLOADING && download.progress > 0f) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${download.progressPercentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (download.totalBytes > 0) {
                            Text(
                                text = "${formatBytes(download.bytesDownloaded)} / ${formatBytes(download.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    LinearProgressIndicator(
                        progress = download.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.QUEUED -> {
                        TextButton(
                            onClick = onForceStart
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Force start",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Now")
                        }
                    }
                    
                    DownloadStatus.DOWNLOADING -> {
                        TextButton(
                            onClick = onCancel
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    
                    DownloadStatus.FAILED -> {
                        TextButton(
                            onClick = onRetry
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                    
                    DownloadStatus.COMPLETED -> {
                        TextButton(
                            onClick = onNavigateToShow
                        ) {
                            Text("View Show")
                        }
                    }
                    
                    else -> {
                        TextButton(
                            onClick = onCancel
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }
            
            // Error message (if failed)
            download.errorMessage?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DownloadQueueItem(
    download: DownloadState,
    onCancel: (DownloadState) -> Unit,
    onRetry: (DownloadState) -> Unit,
    onForceStart: (DownloadState) -> Unit,
    onNavigateToShow: (DownloadState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (download.status) {
                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with show info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show and track info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formatShowName(download.recordingId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTrackInfo(download.trackFilename),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status badge
                StatusBadge(status = download.status)
            }
            
            // Progress bar (if in progress)
            if (download.status == DownloadStatus.DOWNLOADING && download.progress > 0f) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${download.progressPercentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (download.totalBytes > 0) {
                            Text(
                                text = "${formatBytes(download.bytesDownloaded)} / ${formatBytes(download.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    LinearProgressIndicator(
                        progress = download.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.QUEUED -> {
                        TextButton(
                            onClick = { onForceStart(download) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Force start",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Now")
                        }
                    }
                    
                    DownloadStatus.DOWNLOADING -> {
                        TextButton(
                            onClick = { onCancel(download) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                    
                    DownloadStatus.FAILED -> {
                        TextButton(
                            onClick = { onRetry(download) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                    
                    DownloadStatus.COMPLETED -> {
                        TextButton(
                            onClick = { onNavigateToShow(download) }
                        ) {
                            Text("View Show")
                        }
                    }
                    
                    else -> {
                        TextButton(
                            onClick = { onCancel(download) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }
            
            // Error message (if failed)
            download.errorMessage?.let { error ->
                Text(
                    text = "Error: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.outline
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Helper functions for formatting
fun formatShowName(recordingId: String): String {
    // Try to extract date and venue from recording ID
    // Example: "gd1977-05-08.sbd.hicks.4982.sbeok.shnf" -> "1977-05-08"
    val dateRegex = Regex("""(\d{4}-\d{2}-\d{2})""")
    val dateMatch = dateRegex.find(recordingId)
    return if (dateMatch != null) {
        "Show: ${dateMatch.value}"
    } else {
        "Recording: ${recordingId.take(20)}..."
    }
}

fun formatTrackInfo(trackFilename: String): String {
    // Try to extract track number and title from filename
    // Example: "gd77-05-08d1t01.shn" -> "Track 1"
    val trackRegex = Regex("""t(\d+)""")
    val trackMatch = trackRegex.find(trackFilename)
    return if (trackMatch != null) {
        val trackNum = trackMatch.groupValues[1].toIntOrNull() ?: 0
        "Track $trackNum: ${trackFilename.substringBeforeLast('.')}"
    } else {
        trackFilename.substringBeforeLast('.')
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024
    if (kb < 1024) return "${kb}KB"
    val mb = kb / 1024
    if (mb < 1024) return "${mb}MB"
    val gb = mb / 1024
    return "${gb}GB"
}