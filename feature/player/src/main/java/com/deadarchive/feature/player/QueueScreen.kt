package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.deadarchive.core.model.PlaylistItem
import com.deadarchive.core.media.player.MediaControllerRepository
import javax.inject.Inject

// Simple ViewModel for MediaControllerRepository access
@HiltViewModel
class QueueViewModel @Inject constructor(
    val mediaControllerRepository: MediaControllerRepository
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    queueViewModel: QueueViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    Log.d("QueueScreen", "QueueScreen: Composing")
    
    // MediaControllerRepository is the single source of truth
    val currentQueueIndex by queueViewModel.mediaControllerRepository.queueIndex.collectAsState()
    val queueUrls by queueViewModel.mediaControllerRepository.queueUrls.collectAsState()
    val queueMetadata by queueViewModel.mediaControllerRepository.queueMetadata.collectAsState()
    
    // Only use PlayerViewModel for playlist title and actions, not for queue state
    val playlistTitle by playerViewModel.playlistTitle.collectAsState()
    
    // Debug logging
    LaunchedEffect(queueUrls, currentQueueIndex, queueMetadata) {
        Log.d("QueueScreen", "QueueScreen: queueUrls.size = ${queueUrls.size}")
        Log.d("QueueScreen", "QueueScreen: currentQueueIndex = $currentQueueIndex")
        Log.d("QueueScreen", "QueueScreen: queueMetadata.size = ${queueMetadata.size}")
        Log.d("QueueScreen", "QueueScreen: playlistTitle = $playlistTitle")
        queueUrls.forEachIndexed { index, url ->
            Log.d("QueueScreen", "QueueScreen: queueUrls[$index] = $url")
        }
        queueMetadata.forEachIndexed { index, (_, title) ->
            Log.d("QueueScreen", "QueueScreen: metadata[$index] = $title")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Queue",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = IconResources.Navigation.KeyboardArrowDown(), 
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Clear queue button
                    if (queueUrls.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                playerViewModel.clearPlaylist()
                                Log.d("QueueScreen", "Queue cleared")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear queue"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                queueUrls.isEmpty() -> {
                    // Empty queue state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = IconResources.PlayerControls.Queue(),
                                contentDescription = "Empty queue",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Queue is empty",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Add tracks to your queue to start listening",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Queue header
                        item {
                            QueueHeader(
                                title = playlistTitle ?: "Current Queue",
                                itemCount = queueUrls.size,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Queue items - build from MediaController data
                        itemsIndexed(queueUrls) { index, url ->
                            val trackTitle = queueMetadata.find { it.first == url }?.second 
                                ?: url.substringAfterLast("/").substringBeforeLast(".")
                            
                            QueueItemFromUrl(
                                url = url,
                                title = trackTitle,
                                position = index + 1,
                                isCurrentTrack = index == currentQueueIndex,
                                onPlayClick = { 
                                    playerViewModel.navigateToTrack(index)
                                    Log.d("QueueScreen", "Playing track at queue position $index")
                                },
                                onRemoveClick = { 
                                    playerViewModel.removeFromPlaylist(index)
                                    Log.d("QueueScreen", "Removed track at queue position $index")
                                }
                            )
                        }
                        
                        // Bottom padding for last item
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    title: String,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$itemCount track${if (itemCount != 1) "s" else ""} in queue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QueueItem(
    playlistItem: PlaylistItem,
    position: Int,
    isCurrentTrack: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playlistItem.track
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue position indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Currently playing",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration if available
                    if (track.duration > 0) {
                        Text(
                            text = formatDuration(track.duration.toDouble()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // File size if available
                    track.audioFile?.sizeBytes?.toLongOrNull()?.let { size ->
                        Text(
                            text = "• ${formatFileSize(size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Remove button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Format duration in MM:SS format
 */
private fun formatDuration(durationInSeconds: Double): String {
    val minutes = (durationInSeconds / 60).toInt()
    val seconds = (durationInSeconds % 60).toInt()
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Format file size in MB
 */
private fun formatFileSize(sizeInBytes: Long): String {
    val sizeInMB = sizeInBytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", sizeInMB)
}

@Composable
private fun QueueItemFromUrl(
    url: String,
    title: String,
    position: Int,
    isCurrentTrack: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue position indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack) {
                    Icon(
                        painter = IconResources.PlayerControls.Play(),
                        contentDescription = "Currently playing",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // URL-based file info (simplified)
                val filename = url.substringAfterLast("/")
                val format = filename.substringAfterLast(".").uppercase()
                Text(
                    text = format,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Remove button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}