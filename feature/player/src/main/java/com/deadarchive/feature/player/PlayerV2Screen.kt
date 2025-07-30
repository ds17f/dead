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
import com.deadarchive.core.design.component.IconResources

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
            // Top App Bar - similar to current player
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PlayerV2 🚀",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UI-First Development",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(onClick = onNavigateToQueue) {
                        Icon(
                            painter = IconResources.PlayerControls.Queue(),
                            contentDescription = "Queue"
                        )
                    }
                }
            )
            
            // Main content area - placeholder for now
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Placeholder album art
                    Card(
                        modifier = Modifier.size(200.dp),
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
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Track information display
                    val trackInfo = uiState.trackInfo
                    if (trackInfo != null) {
                        PlayerV2TrackInfo(
                            trackTitle = trackInfo.trackTitle,
                            recordingName = trackInfo.recordingName,
                            showDate = trackInfo.showDate,
                            venue = trackInfo.venue
                        )
                    } else {
                        PlayerV2TrackInfo(
                            trackTitle = "Loading...",
                            recordingName = "Preparing playback",
                            showDate = "",
                            venue = ""
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress display
                    val progressInfo = uiState.progressInfo
                    if (progressInfo != null) {
                        PlayerV2ProgressBar(
                            currentTime = progressInfo.currentTime,
                            totalTime = progressInfo.totalTime,
                            progress = progressInfo.progress,
                            onSeek = viewModel::onSeek
                        )
                    } else {
                        PlayerV2ProgressBar(
                            currentTime = "0:00",
                            totalTime = "0:00",
                            progress = 0f,
                            onSeek = viewModel::onSeek
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Playback controls
                    PlayerV2Controls(
                        isPlaying = uiState.isPlaying,
                        onPlayPause = viewModel::onPlayPauseClicked,
                        onPrevious = viewModel::onPreviousClicked,
                        onNext = viewModel::onNextClicked
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Development info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚀 PlayerV2 Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "UI-first development complete! Components connected to service layer.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "RecordingId: ${recordingId ?: "default"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "State: ${if (uiState.isLoading) "Loading" else "Ready"} | Playing: ${uiState.isPlaying}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Next: Integrate with V1 services for real playback",
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

/**
 * UI Component: Track information display
 * 
 * Through building this component, we discover we need domain models that provide:
 * - Track title and metadata
 * - Recording name and identification
 * - Show date and venue information
 * - Proper formatting and display logic
 */
@Composable
private fun PlayerV2TrackInfo(
    trackTitle: String,
    recordingName: String,
    showDate: String,
    venue: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = trackTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$recordingName • $showDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = venue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * UI Component: Progress bar with time displays
 * 
 * Through building this component, we discover we need domain models that provide:
 * - Current playback position and total duration
 * - Formatted time strings
 * - Progress percentage calculation
 * - Seek functionality
 */
@Composable
private fun PlayerV2ProgressBar(
    currentTime: String,
    totalTime: String,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
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
 * UI Component: Playback control buttons
 * 
 * Through building this component, we discover we need domain models that provide:
 * - Play/pause state management
 * - Track navigation capabilities
 * - Control availability (can skip, can play, etc.)
 */
@Composable
private fun PlayerV2Controls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
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
                modifier = Modifier.size(40.dp)
            )
        }
        
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
    }
}