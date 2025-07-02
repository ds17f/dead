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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    concertId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    Log.d("DEBUG_PLAYER_NEW_CODE", "=== NEW CODE IS RUNNING === concertId: $concertId")
    // Load concert data if concertId is provided
    LaunchedEffect(concertId) {
        Log.d("PlayerScreen", "LaunchedEffect: concertId = '$concertId'")
        if (!concertId.isNullOrBlank()) {
            Log.d("PlayerScreen", "Calling viewModel.loadConcert('$concertId')")
            viewModel.loadConcert(concertId)
        } else {
            Log.d("PlayerScreen", "concertId is null or blank, not loading concert data")
        }
    }
    
    // PlayerScreen is a pure view - gets all data from MediaController via viewModel
    val uiState by viewModel.uiState.collectAsState()
    val currentConcert by viewModel.currentConcert.collectAsState()
    
    // Get current track info from MediaController through ViewModel
    val currentTrackUrl by viewModel.mediaControllerRepository.currentTrackUrl.collectAsState()
    val queueMetadata by viewModel.mediaControllerRepository.queueMetadata.collectAsState()
    val queueUrls by viewModel.mediaControllerRepository.queueUrls.collectAsState()
    val queueIndex by viewModel.mediaControllerRepository.queueIndex.collectAsState()
    
    // Get track title from MediaController metadata (avoiding direct MediaItem access)
    val currentTrackTitle = if (currentTrackUrl != null) {
        queueMetadata.find { it.first == currentTrackUrl }?.second 
            ?: currentTrackUrl?.substringAfterLast("/")?.substringBeforeLast(".")
            ?: "Unknown Track"
    } else {
        "No track selected"
    }
    
    val currentArtist = currentConcert?.displayTitle ?: "Unknown Artist"
    
    // Determine skip button states from MediaController queue
    val hasNextTrack = queueUrls.isNotEmpty() && queueIndex < queueUrls.size - 1
    val hasPreviousTrack = queueUrls.isNotEmpty() && queueIndex > 0
    
    // Debug current state
    LaunchedEffect(uiState, currentConcert) {
        Log.d("PlayerScreen", "State Update - isLoading: ${uiState.isLoading}, " +
                "tracks.size: ${uiState.tracks.size}, " +
                "error: ${uiState.error}, " +
                "concert: ${currentConcert?.let { "${it.title} (${it.date}) - ${it.venue}" } ?: "null"}")
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
                PlayerTopBarTitle(
                    concert = currentConcert,
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
                        // PlayerScreen is a pure view - no retry functionality needed
                        Text(
                            text = "Please return to the playlist to retry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            uiState.tracks.isEmpty() -> {
                // Show full-screen player UI even without tracks
                NowPlayingSection(
                    uiState = uiState,
                    concert = currentConcert,
                    currentTrackTitle = currentTrackTitle,
                    currentArtist = currentArtist,
                    hasNextTrack = hasNextTrack,
                    hasPreviousTrack = hasPreviousTrack,
                    onPlayPause = viewModel::playPause,
                    onNext = { viewModel.mediaControllerRepository.skipToNext() },
                    onPrevious = { viewModel.mediaControllerRepository.skipToPrevious() },
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            else -> {
                // Full-screen player content
                NowPlayingSection(
                    uiState = uiState,
                    concert = currentConcert,
                    currentTrackTitle = currentTrackTitle,
                    currentArtist = currentArtist,
                    hasNextTrack = hasNextTrack,
                    hasPreviousTrack = hasPreviousTrack,
                    onPlayPause = viewModel::playPause,
                    onNext = { viewModel.mediaControllerRepository.skipToNext() },
                    onPrevious = { viewModel.mediaControllerRepository.skipToPrevious() },
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
    currentTrackTitle: String,
    currentArtist: String,
    hasNextTrack: Boolean,
    hasPreviousTrack: Boolean,
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
                        text = currentTrackTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = currentArtist,
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
                enabled = hasPreviousTrack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.SkipPrevious(),
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp),
                    tint = if (hasPreviousTrack) {
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
                enabled = hasNextTrack,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.SkipNext(),
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp),
                    tint = if (hasNextTrack) {
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

@Composable
private fun PlayerTopBarTitle(
    concert: Concert?,
    modifier: Modifier = Modifier
) {
    Log.d("PlayerTopBarTitle", "Concert data: ${concert?.let { "${it.title} (${it.date}) - ${it.venue}" } ?: "null"}")
    
    if (concert == null) {
        Log.d("PlayerTopBarTitle", "Concert is null, showing 'Player'")
        Text(
            text = "Player",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
        return
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First line: Artist - Date
        val firstLine = buildString {
            append("Grateful Dead")
            if (concert.date.isNotBlank()) {
                append(" - ")
                append(formatConcertDate(concert.date))
            }
        }
        
        Text(
            text = firstLine,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Second line: Venue - City, State
        val secondLine = buildString {
            if (!concert.venue.isNullOrBlank()) {
                append(concert.venue)
            }
            if (!concert.location.isNullOrBlank()) {
                if (!concert.venue.isNullOrBlank()) {
                    append(" - ")
                }
                append(concert.location)
            }
        }
        
        if (secondLine.isNotBlank()) {
            Text(
                text = secondLine,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


private fun formatConcertDate(dateString: String): String {
    return try {
        // Convert from YYYY-MM-DD to more readable format
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            
            "${monthNames[month - 1]} $day, $year"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}