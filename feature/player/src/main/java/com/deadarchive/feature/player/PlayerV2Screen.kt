package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import com.deadarchive.core.design.component.IconResources

enum class RepeatMode { NORMAL, REPEAT_ALL, REPEAT_ONE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerV2Screen(
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylist: (String?) -> Unit = {},
    viewModel: PlayerV2ViewModel = hiltViewModel()
) {
    Log.d("PlayerV2Screen", "=== PLAYERV2 SCREEN LOADED === recordingId: $recordingId")
    
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    // Bottom sheet state
    var showTrackActionsBottomSheet by remember { mutableStateOf(false) }
    var showConnectBottomSheet by remember { mutableStateOf(false) }
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    
    // Load recording when recordingId changes
    LaunchedEffect(recordingId) {
        if (recordingId != null) {
            viewModel.loadRecording(recordingId)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Top navigation bar - sticky header
            stickyHeader {
                PlayerV2TopBar(
                    contextText = "Playing from Show", // TODO: Make dynamic
                    onNavigateBack = onNavigateBack,
                    onMoreOptionsClick = { showTrackActionsBottomSheet = true }
                )
            }
            
            // Large cover art section
            item {
                PlayerV2CoverArt(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp) // Fixed height instead of weight
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            
            // Track information with add to playlist button
            item {
                PlayerV2TrackInfoRow(
                    trackTitle = uiState.trackInfo?.trackTitle ?: "Scarlet Begonias",
                    showDate = uiState.trackInfo?.showDate ?: "May 8, 1977",
                    venue = uiState.trackInfo?.venue ?: "Barton Hall, Cornell University, Ithaca, NY",
                    onAddToPlaylist = {
                        // TODO: Show snackbar "Playlists are coming soon"
                    },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            // Progress control section  
            item {
                PlayerV2ProgressControl(
                    currentTime = uiState.progressInfo?.currentTime ?: "2:34",
                    totalTime = uiState.progressInfo?.totalTime ?: "8:15",
                    progress = uiState.progressInfo?.progress ?: 0.31f,
                    onSeek = viewModel::onSeek,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                )
            }
            
            // Enhanced primary controls row
            item {
                PlayerV2EnhancedControls(
                    isPlaying = uiState.isPlaying,
                    shuffleEnabled = false, // TODO: Make dynamic
                    repeatMode = RepeatMode.NORMAL, // TODO: Make dynamic
                    onPlayPause = viewModel::onPlayPauseClicked,
                    onPrevious = viewModel::onPreviousClicked,
                    onNext = viewModel::onNextClicked,
                    onShuffleToggle = { /* TODO */ },
                    onRepeatModeChange = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            
            // Secondary controls row (updated for queue sheet)
            item {
                PlayerV2SecondaryControls(
                    onConnectClick = { showConnectBottomSheet = true },
                    onShareClick = { /* TODO: Share track */ },
                    onQueueClick = { showQueueBottomSheet = true }, // Changed to show bottom sheet
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            
            // Extended content as Material panels
            item {
                PlayerV2MaterialPanels(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Bottom padding for last item
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Bottom Sheets
        if (showTrackActionsBottomSheet) {
            TrackActionsBottomSheet(
                trackTitle = uiState.trackInfo?.trackTitle ?: "Scarlet Begonias",
                showDate = uiState.trackInfo?.showDate ?: "May 8, 1977", 
                venue = uiState.trackInfo?.venue ?: "Barton Hall, Cornell University, Ithaca, NY",
                onDismiss = { showTrackActionsBottomSheet = false },
                onShare = { /* TODO: Share track */ },
                onAddToPlaylist = { /* TODO: Add to playlist */ },
                onDownload = { /* TODO: Download track */ }
            )
        }
        
        if (showConnectBottomSheet) {
            ConnectBottomSheet(
                onDismiss = { showConnectBottomSheet = false }
            )
        }
        
        if (showQueueBottomSheet) {
            QueueBottomSheet(
                onDismiss = { showQueueBottomSheet = false }
            )
        }
    }
}

/**
 * Custom top navigation bar with background for visibility
 */
@Composable
private fun PlayerV2TopBar(
    contextText: String,
    onNavigateBack: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Down chevron
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = IconResources.Navigation.KeyboardArrowDown(),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Context text
            Text(
                text = contextText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // 3-dot menu
            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    painter = IconResources.Navigation.MoreVertical(),
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Large cover art section (~40% of screen height)
 */
@Composable
private fun PlayerV2CoverArt(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = "Album Art",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Track information row with add to playlist button
 */
@Composable
private fun PlayerV2TrackInfoRow(
    trackTitle: String,
    showDate: String,
    venue: String,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Track info (takes most space)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = showDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = venue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Add to playlist button
        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = IconResources.Content.AddCircle(),
                contentDescription = "Add to playlist",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Progress control section with slider and time displays
 */
@Composable
private fun PlayerV2ProgressControl(
    currentTime: String,
    totalTime: String,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = totalTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enhanced primary controls with larger buttons and proper layout
 */
@Composable
private fun PlayerV2EnhancedControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatModeChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle - Far left
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = IconResources.PlayerControls.Shuffle(),
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // Spacer to push center controls
        Spacer(modifier = Modifier.weight(1f))
        
        // Previous - Larger
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = IconResources.PlayerControls.SkipPrevious(),
                contentDescription = "Previous",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Play/Pause - Large circular FAB-style button
        FloatingActionButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                painter = if (isPlaying) {
                    IconResources.PlayerControls.Pause()
                } else {
                    IconResources.PlayerControls.Play()
                },
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Next - Larger
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = IconResources.PlayerControls.SkipNext(),
                contentDescription = "Next",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Spacer to push repeat to far right
        Spacer(modifier = Modifier.weight(1f))
        
        // Repeat - Far right
        IconButton(
            onClick = onRepeatModeChange,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = when (repeatMode) {
                    RepeatMode.REPEAT_ONE -> IconResources.PlayerControls.RepeatOne()
                    else -> IconResources.PlayerControls.Repeat()
                },
                contentDescription = "Repeat mode",
                tint = when (repeatMode) {
                    RepeatMode.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    RepeatMode.REPEAT_ALL, RepeatMode.REPEAT_ONE -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

/**
 * Secondary controls row (connections, share, queue)
 */
@Composable
private fun PlayerV2SecondaryControls(
    onConnectClick: () -> Unit,
    onShareClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connections (left)
        IconButton(
            onClick = onConnectClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = IconResources.Content.Share(), // Using Share as placeholder for Cast
                contentDescription = "Connect",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Right section
        Row {
            // Share
            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = IconResources.Content.Share(),
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Queue
            IconButton(
                onClick = onQueueClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.Queue(),
                    contentDescription = "Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Always-expanded Material3 panels for extended content
 */
@Composable
private fun PlayerV2MaterialPanels(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // About the Venue Panel
        MaterialPanel(
            title = "About the Venue",
            content = "Barton Hall at Cornell University in Ithaca, New York, is legendary among Deadheads for hosting one of the greatest Grateful Dead concerts of all time on May 8, 1977. The show is often cited as the pinnacle of the band's creative peak during their spring 1977 tour."
        )
        
        // Lyrics Panel
        MaterialPanel(
            title = "Lyrics",
            content = "Scarlet begonias tucked into her curls\nI knew right away she was not like other girls\nOther girls\nWell I ain't often right but I've never been wrong\nSeldom turns out the way it does in a song\nOnce in a while you get shown the light\nIn the strangest of places if you look at it right"
        )
        
        // Similar Shows Panel
        MaterialPanel(
            title = "Similar Shows",
            content = "Other standout shows from Spring 1977 include Boston Music Hall (May 7), Buffalo Memorial Auditorium (May 9), and Hartford Civic Center (May 28). This tour is considered the creative peak of the Grateful Dead."
        )
        
        // Credits Panel
        MaterialPanel(
            title = "Credits",
            content = "Jerry Garcia - Lead Guitar, Vocals\nBob Weir - Rhythm Guitar, Vocals\nPhil Lesh - Bass, Vocals\nBill Kreutzmann - Drums\nMickey Hart - Drums\nKeith Godchaux - Piano\nDonna Jean Godchaux - Vocals"
        )
    }
}

/**
 * Beautiful Material3 panel component
 */
@Composable
private fun MaterialPanel(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
        }
    }
}

/**
 * Track Actions Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackActionsBottomSheet(
    trackTitle: String,
    showDate: String,
    venue: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Track card (similar to LibraryV2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Album cover placeholder
                Card(
                    modifier = Modifier.size(60.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = IconResources.PlayerControls.AlbumArt(),
                            contentDescription = "Album Art",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Track info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = trackTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = showDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = venue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Divider()
            
            // Action buttons
            ActionButton(
                text = "Share",
                icon = IconResources.Content.Share(),
                onClick = onShare
            )
            
            ActionButton(
                text = "Add to Playlist",
                icon = IconResources.Content.PlaylistAdd(),
                onClick = onAddToPlaylist
            )
            
            ActionButton(
                text = "Download",
                icon = IconResources.Content.CloudDownload(),
                onClick = onDownload
            )
            
            ActionButton(
                text = "More Options",
                icon = IconResources.Navigation.MoreVertical(),
                onClick = { /* TODO */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Connect Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable  
private fun ConnectBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Connect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Cast and connection features are coming soon!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Queue Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(32.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "3 tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(modifier = Modifier.padding(horizontal = 8.dp))
            
            // Mock queue items
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mockQueueItems) { item ->
                    QueueItem(
                        trackTitle = item.title,
                        isCurrentTrack = item.isPlaying,
                        duration = item.duration
                    )
                }
                
                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Individual queue item
 */
@Composable
private fun QueueItem(
    trackTitle: String,
    isCurrentTrack: Boolean,
    duration: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play indicator for current track
            if (isCurrentTrack) {
                Icon(
                    painter = IconResources.PlayerControls.Play(),
                    contentDescription = "Currently playing",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }
            
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isCurrentTrack) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Text(
            text = duration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Mock data for queue
private data class QueueItemData(
    val title: String,
    val isPlaying: Boolean,
    val duration: String
)

private val mockQueueItems = listOf(
    QueueItemData("Scarlet Begonias", true, "7:32"),
    QueueItemData("Fire on the Mountain", false, "12:05"),
    QueueItemData("Estimated Prophet", false, "9:18")
)

/**
 * Action button for bottom sheets
 */
@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}