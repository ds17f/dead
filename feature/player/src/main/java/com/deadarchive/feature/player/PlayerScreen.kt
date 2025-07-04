package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Track
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylist: (String?) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    Log.d("DEBUG_PLAYER_NEW_CODE", "=== NEW CODE IS RUNNING === recordingId: $recordingId")
    // Load recording data if recordingId is provided
    LaunchedEffect(recordingId) {
        Log.d("PlayerScreen", "LaunchedEffect: recordingId = '$recordingId'")
        if (!recordingId.isNullOrBlank()) {
            Log.d("PlayerScreen", "Calling viewModel.loadRecording('$recordingId')")
            viewModel.loadRecording(recordingId)
        } else {
            Log.d("PlayerScreen", "recordingId is null or blank, not loading recording data")
        }
    }
    
    // PlayerScreen is a pure view - gets all data from MediaController via viewModel
    val uiState by viewModel.uiState.collectAsState()
    val currentRecording by viewModel.currentRecording.collectAsState()
    
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
    
    val currentArtist = currentRecording?.displayTitle ?: "Unknown Artist"
    
    // Determine skip button states from MediaController queue
    val hasNextTrack = queueUrls.isNotEmpty() && queueIndex < queueUrls.size - 1
    val hasPreviousTrack = queueUrls.isNotEmpty() && queueIndex > 0
    
    // Debug current state
    LaunchedEffect(uiState, currentRecording) {
        Log.d("PlayerScreen", "State Update - isLoading: ${uiState.isLoading}, " +
                "tracks.size: ${uiState.tracks.size}, " +
                "error: ${uiState.error}, " +
                "recording: ${currentRecording?.let { "${it.title} (${it.concertDate}) - ${it.concertVenue}" } ?: "null"}")
    }
    
    // Animation state for swipe gestures (moved to main scope for track change detection)
    var isNavigatingPlayer by remember { mutableStateOf(false) }
    var navigationDirectionPlayer by remember { mutableIntStateOf(0) }
    
    // Don't reset animation state during navigation - let the finishedListener handle it
    
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
                    recording = currentRecording,
                    modifier = Modifier.clickable {
                        Log.d("PlayerScreen", "Title tapped! Navigating to playlist with recordingId: $recordingId")
                        onNavigateToPlaylist(recordingId)
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
                    recording = currentRecording,
                    currentTrackTitle = currentTrackTitle,
                    currentArtist = currentArtist,
                    hasNextTrack = hasNextTrack,
                    hasPreviousTrack = hasPreviousTrack,
                    queueIndex = queueIndex,
                    queueSize = queueUrls.size,
                    isNavigating = isNavigatingPlayer,
                    navigationDirection = navigationDirectionPlayer,
                    onNavigationStateChange = { navigating, direction ->
                        isNavigatingPlayer = navigating
                        navigationDirectionPlayer = direction
                    },
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
                    recording = currentRecording,
                    currentTrackTitle = currentTrackTitle,
                    currentArtist = currentArtist,
                    hasNextTrack = hasNextTrack,
                    hasPreviousTrack = hasPreviousTrack,
                    queueIndex = queueIndex,
                    queueSize = queueUrls.size,
                    isNavigating = isNavigatingPlayer,
                    navigationDirection = navigationDirectionPlayer,
                    onNavigationStateChange = { navigating, direction ->
                        isNavigatingPlayer = navigating
                        navigationDirectionPlayer = direction
                    },
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
    recording: Recording?,
    currentTrackTitle: String,
    currentArtist: String,
    hasNextTrack: Boolean,
    hasPreviousTrack: Boolean,
    queueIndex: Int,
    queueSize: Int,
    isNavigating: Boolean,
    navigationDirection: Int,
    onNavigationStateChange: (Boolean, Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple carousel state - just track drag offset
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Get screen width for full slide animations
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // Track the current carousel position (which content set we're showing)
    var carouselPosition by remember { mutableIntStateOf(0) }
    
    // Reset carousel position when track changes externally (not from our navigation)
    LaunchedEffect(queueIndex) {
        if (!isNavigating) {
            Log.d("PlayerCarousel", "External track change detected - resetting carousel position")
            carouselPosition = 0
        }
    }
    
    // Debug: Log current state
    Log.d("PlayerCarousel", "State: dragOffset=$dragOffset, isDragging=$isDragging, isNavigating=$isNavigating, queueIndex=$queueIndex, carouselPosition=$carouselPosition")
    
    // Simple animation - just follow drag or navigate to target
    val animatedOffset by animateFloatAsState(
        targetValue = when {
            isDragging -> {
                Log.d("PlayerCarousel", "Animation target: dragging -> $dragOffset")
                dragOffset
            }
            isNavigating -> {
                // Navigate completely off screen
                val target = if (navigationDirection > 0) screenWidth else -screenWidth
                Log.d("PlayerCarousel", "Animation target: navigating -> $target")
                target
            }
            else -> {
                Log.d("PlayerCarousel", "Animation target: at rest -> 0")
                0f
            }
        },
        animationSpec = if (isNavigating) {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        finishedListener = { finalValue ->
            if (isNavigating) {
                Log.d("PlayerCarousel", "Navigation animation finished - rotating carousel content")
                // Update carousel position (this will change the content)
                carouselPosition = if (navigationDirection > 0) carouselPosition + 1 else carouselPosition - 1
                Log.d("PlayerCarousel", "New carousel position: $carouselPosition")
                // Reset navigation state - this will cause animation to target 0 again
                onNavigationStateChange(false, 0)
            }
        },
        label = "carousel_animation"
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Carousel cover art - independent containers for proper slide transitions
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Calculate which tracks to show based on current queue position
            // carouselPosition represents how far we've "slid" the carousel view
            // When carouselPosition = 0: show tracks around queueIndex
            // When carouselPosition = -1: show tracks around queueIndex+1 (because we slid left)
            val centerTrackIndex = queueIndex - carouselPosition
            val previousTrackIndex = centerTrackIndex - 1
            val nextTrackIndex = centerTrackIndex + 1
            
            // Left container (previous track)
            val showPrevious = previousTrackIndex >= 0
            if (showPrevious) {
                CarouselContainer(
                    offset = -screenWidth + animatedOffset,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    label = "Track $previousTrackIndex",
                    isVisible = true
                )
            }
            
            // Center container (current track) - draggable
            CarouselContainer(
                offset = animatedOffset,
                color = MaterialTheme.colorScheme.surfaceVariant,
                label = "Track $centerTrackIndex",
                isVisible = true,
                modifier = Modifier.pointerInput(hasNextTrack, hasPreviousTrack) {
                    val swipeThreshold = 150f // Minimum swipe distance to trigger navigation
                    val maxDragDistance = size.width * 0.4f // Max drag distance (40% of width)
                    
                    detectHorizontalDragGestures(
                        onDragStart = {
                            Log.d("PlayerCarousel", "Drag started")
                            isDragging = true
                            dragOffset = 0f
                        },
                        onDragEnd = { 
                            Log.d("PlayerCarousel", "Drag ended: final dragOffset=$dragOffset")
                            isDragging = false
                            
                            // Check if we should navigate to next/previous track
                            if (abs(dragOffset) > swipeThreshold) {
                                if (dragOffset > 0) {
                                    // Swiped right - go to previous track
                                    if (hasPreviousTrack) {
                                        Log.d("PlayerCarousel", "Swipe right detected - going to previous track")
                                        onNavigationStateChange(true, 1)
                                        onPrevious()
                                    }
                                } else {
                                    // Swiped left - go to next track  
                                    if (hasNextTrack) {
                                        Log.d("PlayerCarousel", "Swipe left detected - going to next track")
                                        onNavigationStateChange(true, -1)
                                        onNext()
                                    }
                                }
                            } else {
                                Log.d("PlayerCarousel", "Swipe too short - resetting to center")
                                dragOffset = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        // Update drag offset with limits
                        val newOffset = dragOffset + dragAmount
                        dragOffset = when {
                            newOffset > maxDragDistance -> maxDragDistance
                            newOffset < -maxDragDistance -> -maxDragDistance
                            else -> newOffset
                        }
                    }
                }
            )
            
            // Right container (next track)
            val showNext = nextTrackIndex < queueSize
            if (showNext) {
                CarouselContainer(
                    offset = screenWidth + animatedOffset,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    label = "Track $nextTrackIndex",
                    isVisible = true
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
private fun CarouselContainer(
    offset: Float,
    color: Color,
    label: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .graphicsLayer {
                    translationX = offset
                }
                .clip(RoundedCornerShape(16.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = IconResources.PlayerControls.AlbumArt(),
                    contentDescription = "$label Track Art",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBarTitle(
    recording: Recording?,
    modifier: Modifier = Modifier
) {
    Log.d("PlayerTopBarTitle", "Recording data: ${recording?.let { "${it.title} (${it.concertDate}) - ${it.concertVenue}" } ?: "null"}")
    
    if (recording == null) {
        Log.d("PlayerTopBarTitle", "Recording is null, showing 'Player'")
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
            if (recording.concertDate.isNotBlank()) {
                append(" - ")
                append(formatConcertDate(recording.concertDate))
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
            if (!recording.concertVenue.isNullOrBlank()) {
                append(recording.concertVenue)
            }
            if (!recording.concertLocation.isNullOrBlank()) {
                if (!recording.concertVenue.isNullOrBlank()) {
                    append(" - ")
                }
                append(recording.concertLocation)
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