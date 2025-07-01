package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.Track
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    concertId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    Log.d("PlayerScreen", "PlayerScreen: Composing with concertId: $concertId")
    
    Log.d("PlayerScreen", "PlayerScreen: Getting states")
    val uiState by viewModel.uiState.collectAsState()
    val currentConcert by viewModel.currentConcert.collectAsState()
    
    // Load concert data when concertId is provided, but only if not already loaded
    LaunchedEffect(concertId, currentConcert) {
        Log.d("PlayerScreen", "LaunchedEffect: concertId = $concertId, currentConcert.identifier = ${currentConcert?.identifier}")
        if (!concertId.isNullOrBlank() && currentConcert?.identifier != concertId) {
            Log.d("PlayerScreen", "LaunchedEffect: Loading new concert $concertId")
            try {
                viewModel.loadConcert(concertId)
            } catch (e: Exception) {
                Log.e("PlayerScreen", "LaunchedEffect: Exception in loadConcert", e)
            }
        } else if (!concertId.isNullOrBlank() && currentConcert?.identifier == concertId) {
            Log.d("PlayerScreen", "LaunchedEffect: Concert $concertId already loaded, skipping reload")
        } else {
            Log.w("PlayerScreen", "LaunchedEffect: concertId is null or blank")
        }
    }
    
    // Debug current state
    LaunchedEffect(uiState, currentConcert) {
        Log.d("PlayerScreen", "State Update - isLoading: ${uiState.isLoading}, " +
                "tracks.size: ${uiState.tracks.size}, " +
                "error: ${uiState.error}, " +
                "concert: ${currentConcert?.title}")
    }
    
    // Position updates are handled automatically by MediaControllerRepository
    // No manual position updates needed - service provides real-time position via StateFlow
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Spotify-style Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = currentConcert?.displayTitle ?: "Player",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        // TODO: Navigate to album/playlist view
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(painter = IconResources.Navigation.KeyboardArrowDown(), contentDescription = "Back")
                }
            },
            actions = {
                var showDropdownMenu by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(painter = IconResources.Navigation.MoreVertical(), contentDescription = "More options")
                    }
                    
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Queue") },
                            onClick = {
                                showDropdownMenu = false
                                onNavigateToQueue()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = IconResources.PlayerControls.Queue(),
                                    contentDescription = "Queue"
                                )
                            }
                        )
                    }
                }
            }
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = IconResources.Status.Error(),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { concertId?.let { viewModel.loadConcert(it) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.tracks.isEmpty() -> {
                // Show full-screen player UI even without tracks
                NowPlayingSection(
                    uiState = uiState,
                    concert = currentConcert,
                    onPlayPause = viewModel::playPause,
                    onNext = viewModel::skipToNext,
                    onPrevious = viewModel::skipToPrevious,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                // Full-screen player content
                NowPlayingSection(
                    uiState = uiState,
                    concert = currentConcert,
                    onPlayPause = viewModel::playPause,
                    onNext = viewModel::skipToNext,
                    onPrevious = viewModel::skipToPrevious,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NowPlayingSection(
    uiState: PlayerUiState,
    concert: Concert?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Large cover art - takes up most of the screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = "Album Art",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Track info section with scrolling title and add button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.currentTrack?.displayTitle ?: "No track selected",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = concert?.displayTitle ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Add to playlist button
                IconButton(
                    onClick = { 
                        // TODO: Add to playlist functionality
                    }
                ) {
                    Icon(
                        painter = IconResources.Navigation.Add(),
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Progress bar section
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Slider(
                value = uiState.progress,
                onValueChange = { progress ->
                    val newPosition = (progress * uiState.duration).toLong()
                    onSeek(newPosition)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                    activeTrackColor = MaterialTheme.colorScheme.onSurface,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(uiState.currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(uiState.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Media controls - clean Material Design
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = onPrevious,
                enabled = uiState.hasPreviousTrack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.SkipPrevious(),
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp),
                    tint = if (uiState.hasPreviousTrack) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )
            }
            
            // Play/Pause button - large and prominent
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface)
            ) {
                if (uiState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        painter = if (uiState.isPlaying) IconResources.PlayerControls.Pause() else IconResources.PlayerControls.Play(),
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
            
            // Next button
            IconButton(
                onClick = onNext,
                enabled = uiState.hasNextTrack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.SkipNext(),
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp),
                    tint = if (uiState.hasNextTrack) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )
            }
        }
        
        // Expandable bottom section placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Pull indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}


private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}