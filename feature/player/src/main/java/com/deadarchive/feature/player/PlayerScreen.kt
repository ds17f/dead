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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    concertId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    Log.d("PlayerScreen", "PlayerScreen: Composing with concertId: $concertId")
    
    Log.d("PlayerScreen", "PlayerScreen: Getting states")
    val uiState by viewModel.uiState.collectAsState()
    val currentConcert by viewModel.currentConcert.collectAsState()
    
    // Load concert data when concertId is provided
    LaunchedEffect(concertId) {
        Log.d("PlayerScreen", "LaunchedEffect: concertId = $concertId")
        if (!concertId.isNullOrBlank()) {
            Log.d("PlayerScreen", "LaunchedEffect: Calling viewModel.loadConcert($concertId)")
            try {
                viewModel.loadConcert(concertId)
            } catch (e: Exception) {
                Log.e("PlayerScreen", "LaunchedEffect: Exception in loadConcert", e)
            }
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
    
    // Update position periodically when playing
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            viewModel.updatePosition()
            delay(1000) // Update every second
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = currentConcert?.displayTitle ?: "Player",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Back")
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
                            Icons.Default.Warning,
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
                // Show basic player UI even without tracks
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Now Playing section - shows loading or no tracks message
                    NowPlayingSection(
                        uiState = uiState,
                        concert = currentConcert,
                        onPlayPause = viewModel::playPause,
                        onNext = viewModel::skipToNext,
                        onPrevious = viewModel::skipToPrevious,
                        onSeek = viewModel::seekTo,
                        modifier = Modifier.weight(1f)
                    )
                    
                    HorizontalDivider()
                    
                    // Empty track list message
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (currentConcert == null) "Loading concert..." else "No tracks available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (concertId != null && !uiState.isLoading) {
                                Button(onClick = { viewModel.loadConcert(concertId) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
            
            else -> {
                // Main player content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Now Playing section (takes 1/3 of screen)
                    NowPlayingSection(
                        uiState = uiState,
                        concert = currentConcert,
                        onPlayPause = viewModel::playPause,
                        onNext = viewModel::skipToNext,
                        onPrevious = viewModel::skipToPrevious,
                        onSeek = viewModel::seekTo,
                        modifier = Modifier.weight(1f)
                    )
                    
                    HorizontalDivider()
                    
                    // Track list (takes 2/3 of screen)
                    TrackList(
                        tracks = uiState.tracks,
                        currentTrackIndex = uiState.currentTrackIndex,
                        onTrackClick = viewModel::playTrack,
                        modifier = Modifier.weight(2f)
                    )
                }
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
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Album Art",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Track info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = uiState.currentTrack?.displayTitle ?: "No track selected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = concert?.displayTitle ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Progress bar
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = uiState.progress,
                onValueChange = { progress ->
                    val newPosition = (progress * uiState.duration).toLong()
                    onSeek(newPosition)
                },
                modifier = Modifier.fillMaxWidth()
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
        
        // Media controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = uiState.hasPreviousTrack
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            IconButton(
                onClick = onNext,
                enabled = uiState.hasNextTrack
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<Track>,
    currentTrackIndex: Int,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(tracks) { index, track ->
            TrackItem(
                track = track,
                isCurrentTrack = index == currentTrackIndex,
                onClick = { onTrackClick(index) }
            )
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number
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
                        Icons.Default.PlayArrow,
                        contentDescription = "Currently playing",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = track.displayTrackNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentTrack) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                track.audioFile?.let { audioFile ->
                    Text(
                        text = "${audioFile.displayFormat} • ${track.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrentTrack) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}