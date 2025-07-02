package com.deadarchive.feature.playlist

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Concert
import com.deadarchive.feature.player.PlayerUiState
import com.deadarchive.feature.player.PlayerViewModel

@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    concert: Concert?,
    trackTitle: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: (String?) -> Unit,
    concertId: String? = null,
    modifier: Modifier = Modifier
) {
    // Only show MiniPlayer if there's a current track
    if (trackTitle == null) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onTapToExpand(concertId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Progress indicator at the very top
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.AlbumArt(),
                        contentDescription = "Album Art",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = trackTitle ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = concert?.displayTitle ?: "Unknown Concert",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Play/Pause button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = if (uiState.isPlaying) IconResources.PlayerControls.Pause() else IconResources.PlayerControls.Play(),
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerContainer(
    onTapToExpand: (String?) -> Unit, // Now accepts concert ID
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    // Get current playback state directly from MediaController
    val currentTrackUrl by viewModel.mediaControllerRepository.currentTrackUrl.collectAsState()
    val queueMetadata by viewModel.mediaControllerRepository.queueMetadata.collectAsState()
    val isPlaying by viewModel.mediaControllerRepository.isPlaying.collectAsState()
    val currentPosition by viewModel.mediaControllerRepository.currentPosition.collectAsState()
    val duration by viewModel.mediaControllerRepository.duration.collectAsState()
    val playbackState by viewModel.mediaControllerRepository.playbackState.collectAsState()
    val currentConcertId by viewModel.mediaControllerRepository.currentConcertIdFlow.collectAsState()
    
    // Get track title from MediaController metadata
    val currentTrackTitle = if (currentTrackUrl != null) {
        queueMetadata.find { it.first == currentTrackUrl }?.second 
            ?: currentTrackUrl?.substringAfterLast("/")?.substringBeforeLast(".")
            ?: "Unknown Track"
    } else {
        null
    }
    
    // Get artist name from MediaController metadata - use concert name from metadata
    val currentArtist = if (currentTrackUrl != null && queueMetadata.isNotEmpty()) {
        "Unknown Artist" // We could enhance this with proper artist metadata
    } else {
        null
    }
    
    // Create a minimal UI state for the mini player based on MediaController data
    val miniPlayerUiState = PlayerUiState(
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        playbackState = playbackState // Already an Int from MediaController
    )
    
    // Create a minimal concert object for display
    val miniPlayerConcert = if (currentArtist != null) {
        Concert(
            identifier = "",
            title = currentArtist,
            date = ""
        )
    } else null
    
    MiniPlayer(
        uiState = miniPlayerUiState,
        concert = miniPlayerConcert,
        trackTitle = currentTrackTitle,
        onPlayPause = viewModel::playPause,
        onTapToExpand = onTapToExpand,
        concertId = currentConcertId,
        modifier = modifier
    )
}