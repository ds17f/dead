package com.deadly.feature.playlist

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadly.core.design.component.IconResources
import com.deadly.feature.playlist.model.MiniPlayerV2UiState
import kotlinx.coroutines.delay

/**
 * MiniPlayerV2 - Global V2 mini-player matching V1 visual design exactly
 * 
 * V2 implementation that replicates V1's EnrichedMiniPlayer visual layout and behavior.
 * Uses proper V2 UI state and follows V2 architecture patterns.
 */
@Composable
fun MiniPlayerV2(
    uiState: MiniPlayerV2UiState,
    onPlayPause: () -> Unit,
    onTapToExpand: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp) // Matches V1 EnrichedMiniPlayer height
            .clickable { onTapToExpand(uiState.recordingId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Matches V1
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp), // Matches V1
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Matches V1
        )
    ) {
        Column {
            // Progress indicator at the very top - matches V1
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary, // Matches V1
                trackColor = MaterialTheme.colorScheme.surfaceVariant // Matches V1
            )
            
            // Main content row - matches V1 layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Matches V1 padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder - matches V1 exactly
                Box(
                    modifier = Modifier
                        .size(56.dp) // Matches V1 size
                        .clip(RoundedCornerShape(8.dp)) // Matches V1 shape
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Matches V1 color
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.AlbumArt(),
                        contentDescription = "Album Art",
                        modifier = Modifier.size(24.dp), // Matches V1 size
                        tint = MaterialTheme.colorScheme.onSurfaceVariant // Matches V1 color
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp)) // Matches V1 spacing
                
                // Track info - 3 lines like V1 EnrichedMiniPlayer
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween // Matches V1
                ) {
                    // Line 1: Track Name - matches V1
                    ScrollingText(
                        text = uiState.trackInfo?.displayTitle ?: "Unknown Track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Line 2: Show Date - matches V1
                    ScrollingText(
                        text = uiState.trackInfo?.displayDate ?: "Unknown Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Line 3: Venue, City, State - matches V1 exactly
                    ScrollingText(
                        text = buildString {
                            val trackInfo = uiState.trackInfo
                            if (trackInfo != null) {
                                if (!trackInfo.venue.isNullOrBlank()) {
                                    append(trackInfo.venue)
                                    if (!trackInfo.location.isNullOrBlank()) {
                                        append(" • ")
                                        append(trackInfo.location)
                                    }
                                } else {
                                    append(trackInfo.location ?: "Unknown Location")
                                }
                            } else {
                                append("Unknown Location")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp)) // Matches V1 spacing
                
                // Play/Pause button - matches V1 exactly
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(40.dp) // Matches V1 size
                        .clip(CircleShape) // Matches V1 shape
                        .background(MaterialTheme.colorScheme.primary) // Matches V1 color
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), // Matches V1 size
                            color = MaterialTheme.colorScheme.onPrimary, // Matches V1 color
                            strokeWidth = 2.dp // Matches V1
                        )
                    } else {
                        Icon(
                            painter = if (uiState.isPlaying) {
                                IconResources.PlayerControls.Pause()
                            } else {
                                IconResources.PlayerControls.Play()
                            },
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp), // Matches V1 size
                            tint = MaterialTheme.colorScheme.onPrimary // Matches V1 color
                        )
                    }
                }
            }
        }
    }
}

/**
 * MiniPlayerV2Container - Global V2 mini-player container with proper V2 architecture
 * 
 * Clean V2 implementation using MiniPlayerV2ViewModel and MiniPlayerV2Service.
 * Follows established V2 patterns with no V1 dependencies.
 */
@Composable
fun MiniPlayerV2Container(
    onTapToExpand: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerV2ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Only show MiniPlayerV2 if there's current track info and no errors
    if (!uiState.shouldShow) return
    
    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Log error and clear it after a delay
            android.util.Log.e("MiniPlayerV2Container", "Error: $error")
            delay(3000)
            viewModel.clearError()
        }
        return
    }
    
    // Loading state
    if (uiState.isLoading) {
        // Could show a loading indicator here if needed
        return
    }
    
    MiniPlayerV2(
        uiState = uiState,
        onPlayPause = viewModel::onPlayPauseClicked,
        onTapToExpand = { recordingId ->
            viewModel.onTapToExpand(recordingId)
            onTapToExpand(recordingId)
        },
        modifier = modifier
    )
}

/**
 * ScrollingText - Matches V1 scrolling text behavior exactly
 * 
 * Replicates V1's ScrollingText component from MiniPlayer.kt for visual consistency.
 */
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
    
    // Custom easing function that pauses at both ends - matches V1
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
    
    // Animation for scrolling - back and forth motion with pauses at ends - matches V1
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
    
    // Auto-start scrolling after a delay if text is too long - matches V1
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