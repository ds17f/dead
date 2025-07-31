package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.core.settings.SettingsViewModel
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import com.deadarchive.core.design.component.IconResources

enum class RepeatMode { NORMAL, REPEAT_ALL, REPEAT_ONE }

/**
 * UI Color Generation Utilities for Recording-Based Gradients
 * 
 * These utilities generate consistent visual identities per recording
 * by hashing recordingId to select from the Grateful Dead color palette.
 */

// Grateful Dead inspired color palette for gradients (from Theme.kt)
private val DeadRed = Color(0xFFDC143C)      // Crimson red
private val DeadGold = Color(0xFFFFD700)     // Golden yellow  
private val DeadGreen = Color(0xFF228B22)    // Forest green
private val DeadBlue = Color(0xFF4169E1)     // Royal blue
private val DeadPurple = Color(0xFF8A2BE2)   // Blue violet

private val GradientColors = listOf(DeadGreen, DeadGold, DeadRed, DeadBlue, DeadPurple)

/**
 * Convert recordingId to a consistent base color using hash function
 */
private fun recordingIdToColor(recordingId: String?): Color {
    if (recordingId.isNullOrEmpty()) return DeadRed
    
    val hash = recordingId.hashCode()
    val index = kotlin.math.abs(hash) % GradientColors.size
    return GradientColors[index]
}

/**
 * Create a beautiful vertical gradient brush for the given recordingId
 * Uses alpha transparency to maintain readability and Material3 compatibility
 */
@Composable
private fun createRecordingGradient(recordingId: String?): Brush {
    val baseColor = recordingIdToColor(recordingId)
    
    return Brush.verticalGradient(
        0f to baseColor.copy(alpha = 0.8f),               // Strong color at top
        0.3f to baseColor.copy(alpha = 0.4f),             // Medium color at 30%
        0.6f to baseColor.copy(alpha = 0.1f),             // Faint color at 60%
        0.8f to MaterialTheme.colorScheme.background,     // Background at 80%
        1f to MaterialTheme.colorScheme.background        // Full background at bottom
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerV2Screen(
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylist: (String?) -> Unit = {},
    viewModel: PlayerV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    Log.d("PlayerV2Screen", "=== PLAYERV2 SCREEN LOADED === recordingId: $recordingId")
    
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Scroll state for mini player detection
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Bottom sheet state
    var showTrackActionsBottomSheet by remember { mutableStateOf(false) }
    var showConnectBottomSheet by remember { mutableStateOf(false) }
    var showQueueBottomSheet by remember { mutableStateOf(false) }
    
    // Debug panel state - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectPlayerV2DebugData(uiState, recordingId)
    } else {
        null
    }
    
    // Mini player visibility based on scroll position
    // Show mini player when the user scrolls past the media controls (approximately item 0 with large offset)
    val showMiniPlayer by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || 
            (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset > 800)
        }
    }
    
    // Load recording when recordingId changes
    LaunchedEffect(recordingId) {
        if (recordingId != null) {
            viewModel.loadRecording(recordingId)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content with gradient as part of the scrolling items
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Gradient section containing top navigation, cover art, track info, progress, and controls
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(createRecordingGradient(recordingId))
                ) {
                    Column {
                        // Top navigation bar
                        PlayerV2TopBar(
                            contextText = "Playing from Show", // TODO: Make dynamic
                            onNavigateBack = onNavigateBack,
                            onMoreOptionsClick = { showTrackActionsBottomSheet = true },
                            recordingId = recordingId,
                            // modifier = Modifier.padding(vertical = 24.dp)
                            // modifier = Modifier.padding(top = 24.dp)
                        )
                        
                        // Large cover art section with generous vertical padding
                        PlayerV2CoverArt(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(450.dp) // Increased height for the row
                                //.background(Color.Blue.copy(alpha = 0.2f)) // Debug: Show padding area
                                //.padding(/*vertical = 48.dp,*/ top = 24.dp)
                                .padding(horizontal = 24.dp)
                                //.padding(vertical = 24.dp, horizontal = 24.dp)
                        )
                        
                        // Track information with add to playlist button
                        PlayerV2TrackInfoRow(
                            trackTitle = uiState.trackInfo?.trackTitle ?: "Scarlet Begonias",
                            showDate = uiState.trackInfo?.showDate ?: "May 8, 1977",
                            venue = uiState.trackInfo?.venue ?: "Barton Hall, Cornell University, Ithaca, NY",
                            onAddToPlaylist = {
                                // TODO: Show snackbar "Playlists are coming soon"
                            },
                            modifier = Modifier.padding(horizontal = 24.dp, /*vertical = 16.dp */)
                        )
                        
                        // Progress control section  
                        PlayerV2ProgressControl(
                            currentTime = uiState.progressInfo?.currentTime ?: "2:34",
                            totalTime = uiState.progressInfo?.totalTime ?: "8:15",
                            progress = uiState.progressInfo?.progress ?: 0.31f,
                            onSeek = viewModel::onSeek,
                            modifier = Modifier.padding(horizontal = 24.dp) // Reduced from 24dp
                            //modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp) // Reduced from 24dp
                        )
                        
                        // Enhanced primary controls row
                        PlayerV2EnhancedControls(
                            isPlaying = uiState.isPlaying,
                            shuffleEnabled = false, // TODO: Make dynamic
                            repeatMode = RepeatMode.NORMAL, // TODO: Make dynamic
                            onPlayPause = viewModel::onPlayPauseClicked,
                            onPrevious = viewModel::onPreviousClicked,
                            onNext = viewModel::onNextClicked,
                            onShuffleToggle = { /* TODO */ },
                            onRepeatModeChange = { /* TODO */ },
                            //modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp) // Added minimal vertical padding
                            modifier = Modifier.padding(horizontal = 24.dp) // Added minimal vertical padding
                        )
                    }
                }
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
            
            // Extended content as Material panels - let gradient show through
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
        
        // Mini Player overlay
        if (showMiniPlayer && uiState.trackInfo != null) {
            PlayerV2MiniPlayer(
                uiState = uiState,
                recordingId = recordingId,
                onPlayPause = viewModel::onPlayPauseClicked,
                onTapToExpand = {
                    // Use a coroutine scope to handle the scroll
                    coroutineScope.launch {
                        scrollState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        
        // Debug Activator (only when debug mode is enabled)
        if (settings.showDebugInfo && debugData != null) {
            DebugActivator(
                isVisible = true,
                onClick = { showDebugPanel = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    } // Close Box
    
    // Debug Bottom Sheet
    debugData?.let { data ->
        DebugBottomSheet(
            debugData = data,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}

/**
 * Custom top navigation bar with transparent background (gradient applied by parent)
 */
@Composable
private fun PlayerV2TopBar(
    contextText: String,
    onNavigateBack: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    recordingId: String?,
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
                    modifier = Modifier.size(34.dp),
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
                    modifier = Modifier.size(28.dp),
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
                .fillMaxHeight() // Fill available height
                .aspectRatio(1f) // Maintain square aspect ratio
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
                    modifier = Modifier.size(160.dp), // Scaled up for larger card
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
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = IconResources.Content.AddCircle(),
                contentDescription = "Add to playlist",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Compact progress control section with smaller drag handle and minimal spacing
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        verticalArrangement = Arrangement.spacedBy(2.dp) // Reduced from 8dp
    ) {
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth()
                               .padding(vertical = 2.dp)
            ,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
            ),
            // thumb = {
            //     SliderDefaults.Thumb(
            //         interactionSource = remember { MutableInteractionSource() },
            //         thumbSize = DpSize(width = 8.dp, height = 8.dp)
            //     )
            // }
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
                painter = IconResources.Content.Cast(),
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

/**
 * Minimal debug data for PlayerV2Screen - ready for future population
 */
@Composable
private fun collectPlayerV2DebugData(
    uiState: PlayerV2UiState,
    recordingId: String?
): com.deadarchive.core.design.component.DebugData {
    return com.deadarchive.core.design.component.DebugData(
        screenName = "PlayerV2Screen",
        sections = listOf(
            com.deadarchive.core.design.component.DebugSection(
                title = "PlayerV2 Debug",
                items = listOf(
                    com.deadarchive.core.design.component.DebugItem.KeyValue("Status", "Debug panel ready - add items as needed")
                )
            )
        )
    )
}

/**
 * Mini Player component that appears when scrolling past media controls
 * Shows current track, play/pause button, and progress bar
 */
@Composable
private fun PlayerV2MiniPlayer(
    uiState: PlayerV2UiState,
    recordingId: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get darker gradient color for background
    val baseColor = recordingIdToColor(recordingId)
    val backgroundColor = baseColor.copy(alpha = 0.9f)
    
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onTapToExpand() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column {
            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track info (clickable area for expand)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTapToExpand() }
                ) {
                    Text(
                        text = uiState.trackInfo?.trackTitle ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.trackInfo?.showDate ?: "Unknown Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Play/Pause button (NOT clickable for expansion)
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = if (uiState.isPlaying) {
                            IconResources.PlayerControls.Pause()
                        } else {
                            IconResources.PlayerControls.Play()
                        },
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Progress bar at bottom (without thumb)
            LinearProgressIndicator(
                progress = uiState.progressInfo?.progress ?: 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}
