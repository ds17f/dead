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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import com.deadarchive.core.design.component.IconResources

enum class RepeatMode { NORMAL, REPEAT_ALL, REPEAT_ONE }

@OptIn(ExperimentalMaterial3Api::class)
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom transparent top navigation bar
            PlayerV2TopBar(
                contextText = "Playing from Show", // TODO: Make dynamic
                onNavigateBack = onNavigateBack,
                onMoreOptionsClick = { showTrackActionsBottomSheet = true }
            )
            
            // Large cover art section (~40% of screen height)
            PlayerV2CoverArt(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(horizontal = 24.dp)
            )
            
            // Track information with add to playlist button
            PlayerV2TrackInfoRow(
                trackTitle = uiState.trackInfo?.trackTitle ?: "Scarlet Begonias",
                showDate = uiState.trackInfo?.showDate ?: "May 8, 1977",
                venue = uiState.trackInfo?.venue ?: "Barton Hall, Cornell University, Ithaca, NY",
                onAddToPlaylist = {
                    // TODO: Show snackbar "Playlists are coming soon"
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress control section
            PlayerV2ProgressControl(
                currentTime = uiState.progressInfo?.currentTime ?: "2:34",
                totalTime = uiState.progressInfo?.totalTime ?: "8:15",
                progress = uiState.progressInfo?.progress ?: 0.31f,
                onSeek = viewModel::onSeek,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Primary controls row
            PlayerV2PrimaryControls(
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary controls row
            PlayerV2SecondaryControls(
                onConnectClick = { showConnectBottomSheet = true },
                onShareClick = { /* TODO: Share track */ },
                onQueueClick = onNavigateToQueue,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Extended content sections (scrollable)
            PlayerV2ExtendedContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(horizontal = 24.dp)
            )
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
    }
}

/**
 * Custom transparent top navigation bar
 */
@Composable
private fun PlayerV2TopBar(
    contextText: String,
    onNavigateBack: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
 * Primary controls row (shuffle, prev, play/pause, next, repeat)
 */
@Composable
private fun PlayerV2PrimaryControls(
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
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
        
        // Previous
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = IconResources.PlayerControls.SkipPrevious(),
                contentDescription = "Previous",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Play/Pause (larger)
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = if (isPlaying) {
                    IconResources.PlayerControls.Pause()
                } else {
                    IconResources.PlayerControls.Play()
                },
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Next
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = IconResources.PlayerControls.SkipNext(),
                contentDescription = "Next",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Repeat
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
 * Extended content sections (scrollable)
 */
@Composable
private fun PlayerV2ExtendedContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ExtendedContentSection(
                title = "About the Venue",
                content = "Barton Hall at Cornell University in Ithaca, New York, is legendary among Deadheads for hosting one of the greatest Grateful Dead concerts of all time on May 8, 1977. The show is often cited as the pinnacle of the band's creative peak during their spring 1977 tour."
            )
        }
        
        item {
            ExtendedContentSection(
                title = "Lyrics",
                content = "Scarlet begonias tucked into her curls\nI knew right away she was not like other girls\nOther girls\nWell I ain't often right but I've never been wrong\nSeldom turns out the way it does in a song\nOnce in a while you get shown the light\nIn the strangest of places if you look at it right"
            )
        }
        
        item {
            ExtendedContentSection(
                title = "Similar Shows",
                content = "Other standout shows from Spring 1977 include Boston Music Hall (May 7), Buffalo Memorial Auditorium (May 9), and Hartford Civic Center (May 28). This tour is considered the creative peak of the Grateful Dead."
            )
        }
        
        item {
            ExtendedContentSection(
                title = "Credits",
                content = "Jerry Garcia - Lead Guitar, Vocals\nBob Weir - Rhythm Guitar, Vocals\nPhil Lesh - Bass, Vocals\nBill Kreutzmann - Drums\nMickey Hart - Drums\nKeith Godchaux - Piano\nDonna Jean Godchaux - Vocals"
            )
        }
    }
}

/**
 * Expandable content section
 */
@Composable
private fun ExtendedContentSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                painter = if (expanded) {
                    IconResources.Navigation.ExpandLess()
                } else {
                    IconResources.Navigation.ExpandMore()
                },
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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