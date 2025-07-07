package com.deadarchive.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deadarchive.core.design.R

@Composable
fun DownloadsScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with stats
        DownloadsHeader(
            totalDownloads = uiState.totalDownloads,
            completedDownloads = uiState.completedDownloads,
            activeDownloads = uiState.activeDownloads,
            queuedDownloads = uiState.queuedDownloads,
            onClearCompleted = { viewModel.clearCompleted() },
            onPauseAll = { viewModel.pauseAll() },
            onResumeAll = { viewModel.resumeAll() },
            onTriggerQueueProcessing = { viewModel.triggerQueueProcessing() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Debug info section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Debug Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Queued: ${uiState.queuedDownloads} | Active: ${uiState.activeDownloads} | Completed: ${uiState.completedDownloads}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "If downloads are stuck, try 'Process Queue' button above",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Downloads list
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.downloads.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_download),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No downloads yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Downloads will appear here when you start downloading shows",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active downloads section
                    if (uiState.activeDownloads > 0) {
                        item {
                            SectionHeader(
                                title = "Currently Downloading (${uiState.activeDownloads})",
                                color = Color(0xFFFFA726) // Orange
                            )
                        }
                        items(
                            items = uiState.downloads.filter { it.isActive },
                            key = { it.id }
                        ) { download ->
                            DownloadItem(
                                download = download,
                                onCancel = { viewModel.cancelDownload(download.id) },
                                onRetry = { viewModel.retryDownload(download.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // Queued downloads section
                    if (uiState.queuedDownloads > 0) {
                        item {
                            SectionHeader(
                                title = "Queued (${uiState.queuedDownloads})",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(
                            items = uiState.downloads.filter { it.isQueued },
                            key = { it.id }
                        ) { download ->
                            DownloadItem(
                                download = download,
                                onCancel = { viewModel.cancelDownload(download.id) },
                                onRetry = { viewModel.retryDownload(download.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // Completed downloads section
                    if (uiState.completedDownloads > 0) {
                        item {
                            SectionHeader(
                                title = "Completed (${uiState.completedDownloads})",
                                color = Color(0xFF4CAF50) // Green
                            )
                        }
                        items(
                            items = uiState.downloads.filter { it.isCompleted },
                            key = { it.id }
                        ) { download ->
                            DownloadItem(
                                download = download,
                                onCancel = { viewModel.cancelDownload(download.id) },
                                onRetry = { viewModel.retryDownload(download.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    
                    // Failed downloads section
                    val failedDownloads = uiState.downloads.filter { it.isFailed }
                    if (failedDownloads.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Failed (${failedDownloads.size})",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        items(
                            items = failedDownloads,
                            key = { it.id }
                        ) { download ->
                            DownloadItem(
                                download = download,
                                onCancel = { viewModel.cancelDownload(download.id) },
                                onRetry = { viewModel.retryDownload(download.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsHeader(
    totalDownloads: Int,
    completedDownloads: Int,
    activeDownloads: Int,
    queuedDownloads: Int,
    onClearCompleted: () -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onTriggerQueueProcessing: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Downloads Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    label = "Total",
                    value = totalDownloads.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    label = "Active",
                    value = activeDownloads.toString(),
                    color = Color(0xFFFFA726) // Orange
                )
                StatCard(
                    label = "Queued",
                    value = queuedDownloads.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatCard(
                    label = "Done",
                    value = completedDownloads.toString(),
                    color = Color(0xFF4CAF50) // Green
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeDownloads > 0) {
                    OutlinedButton(
                        onClick = onPauseAll,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_library_add), // Using available icon
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause All")
                    }
                }
                
                if (queuedDownloads > 0) {
                    Button(
                        onClick = onResumeAll,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_file_download),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume All")
                    }
                }
                
                if (completedDownloads > 0) {
                    OutlinedButton(
                        onClick = onClearCompleted,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Done")
                    }
                }
                
                Button(
                    onClick = onTriggerQueueProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Process Queue")
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun DownloadItem(
    download: DownloadUiModel,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                download.isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                download.isFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                download.isActive -> Color(0xFFFFA726).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Recording info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                download.isCompleted -> Color(0xFF4CAF50)
                                download.isFailed -> MaterialTheme.colorScheme.error
                                download.isActive -> Color(0xFFFFA726)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.trackFilename,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.recordingId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status text
                Text(
                    text = download.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        download.isCompleted -> Color(0xFF4CAF50)
                        download.isFailed -> MaterialTheme.colorScheme.error
                        download.isActive -> Color(0xFFFFA726)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Progress bar for active downloads
            if (download.isActive && download.progress >= 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(download.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFA726)
                        )
                        if (download.bytesDownloaded > 0) {
                            Text(
                                text = formatBytes(download.bytesDownloaded),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFA726)
                    )
                }
            }
            
            // Action buttons
            if (download.isFailed || download.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (download.isFailed) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            if (download.isActive) "Cancel" else "Remove",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}