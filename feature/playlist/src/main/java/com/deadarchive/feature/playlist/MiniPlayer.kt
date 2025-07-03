package com.deadarchive.feature.playlist

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Recording
import com.deadarchive.feature.player.PlayerUiState
import com.deadarchive.feature.player.PlayerViewModel

@Composable
fun MiniPlayer(
    uiState: PlayerUiState,
    recording: Recording?,
    trackTitle: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: (String?) -> Unit,
    recordingId: String? = null,
    modifier: Modifier = Modifier
) {
    // Only show MiniPlayer if there's a current track
    if (trackTitle == null) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onTapToExpand(recordingId) },
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
                    ScrollingText(
                        text = trackTitle ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    ScrollingText(
                        text = recording?.displayTitle ?: "Unknown Recording",
                        style = MaterialTheme.typography.bodySmall,
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
    onTapToExpand: (String?) -> Unit, // Now accepts recording ID
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
    val currentRecordingId by viewModel.mediaControllerRepository.currentRecordingIdFlow.collectAsState()
    
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
    
    // Create a minimal recording object for display
    val miniPlayerRecording = if (currentArtist != null) {
        Recording(
            identifier = "",
            title = currentArtist,
            concertDate = ""
        )
    } else null
    
    MiniPlayer(
        uiState = miniPlayerUiState,
        recording = miniPlayerRecording,
        trackTitle = currentTrackTitle,
        onPlayPause = viewModel::playPause,
        onTapToExpand = onTapToExpand,
        recordingId = currentRecordingId,
        modifier = modifier
    )
}

@Composable
private fun ScrollingText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var shouldScroll by remember { mutableStateOf(false) }
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    // Custom easing function that pauses at both ends
    val pausingEasing = Easing { fraction ->
        when {
            fraction < 0.15f -> 0f // Pause at start (15% of time at position 0)
            fraction > 0.85f -> 1f // Pause at end (15% of time at position 1)
            else -> {
                // Smooth transition for the middle 70% of the time
                val adjustedFraction = (fraction - 0.15f) / 0.7f
                adjustedFraction
            }
        }
    }
    
    // Animation for scrolling - back and forth motion with pauses at ends
    val animatedOffset by animateFloatAsState(
        targetValue = if (shouldScroll) -(textWidth - containerWidth).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000, // Fixed 8 second duration for consistency
                easing = pausingEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scrolling_text"
    )
    
    // Auto-start scrolling after a delay if text is too long
    LaunchedEffect(shouldScroll, textWidth, containerWidth) {
        if (textWidth > containerWidth && containerWidth > 0) {
            delay(2000) // Wait 2 seconds before starting to scroll
            shouldScroll = true
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width
            }
    ) {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .onGloballyPositioned { coordinates ->
                    textWidth = coordinates.size.width
                }
        )
    }
}