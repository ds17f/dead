package com.deadarchive.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.CurrentTrackInfo
import com.deadarchive.feature.player.PlayerUiState
import com.deadarchive.feature.player.PlayerViewModel

/**
 * MiniPlayerV2 - Global V2 mini-player with recording-based visual identity
 * 
 * V2 implementation providing consistent visual theming across all screens.
 * Uses recording-based color system and enhanced Material3 design patterns.
 */
@Composable
fun MiniPlayerV2(
    uiState: PlayerUiState,
    trackInfo: CurrentTrackInfo,
    recordingId: String?,
    onPlayPause: () -> Unit,
    onTapToExpand: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use recording-based color system from PlayerV2
    val colors = getRecordingColorStack(recordingId)
    val backgroundColor = colors[1] // Medium color (alpha 0.4f)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Slightly taller for enhanced content
            .clickable { onTapToExpand(recordingId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column {
            // Progress indicator at the very top
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            // Main content row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art with recording color theming
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = IconResources.PlayerControls.AlbumArt(),
                        contentDescription = "Album Art",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Track info with V2 enhanced content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Track title
                    Text(
                        text = trackInfo.displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Show date and venue info
                    Text(
                        text = buildString {
                            append(trackInfo.displayDate)
                            if (!trackInfo.venue.isNullOrBlank()) {
                                append(" • ")
                                append(trackInfo.venue)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Enhanced play/pause button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = if (uiState.isPlaying) {
                                IconResources.PlayerControls.Pause()
                            } else {
                                IconResources.PlayerControls.Play()
                            },
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * MiniPlayerV2Container - Global V2 mini-player container with state management
 * 
 * Provides V2 service integration and state management for the global mini-player.
 * Handles data fetching, error states, and navigation coordination.
 */
@Composable
fun MiniPlayerV2Container(
    onTapToExpand: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    // Get current playback state from MediaController (V1 for now, will transition to V2 services)
    val isPlaying by viewModel.mediaControllerRepository.isPlaying.collectAsState()
    val currentPosition by viewModel.mediaControllerRepository.currentPosition.collectAsState()
    val duration by viewModel.mediaControllerRepository.duration.collectAsState()
    val playbackState by viewModel.mediaControllerRepository.playbackState.collectAsState()
    val currentRecordingId by viewModel.mediaControllerRepository.currentRecordingId.collectAsState()
    
    // Get enriched track info from MediaController
    val currentTrackInfo by viewModel.mediaControllerRepository.currentTrackInfo.collectAsState()
    
    // Only show MiniPlayerV2 if there's current track info
    if (currentTrackInfo == null) return
    
    // Create V2-style UI state
    val miniPlayerUiState = PlayerUiState(
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        playbackState = playbackState
    )
    
    MiniPlayerV2(
        uiState = miniPlayerUiState,
        trackInfo = currentTrackInfo!!,
        recordingId = currentRecordingId,
        onPlayPause = viewModel::playPause,
        onTapToExpand = onTapToExpand,
        modifier = modifier
    )
}

/**
 * Recording-based color system for V2 visual identity
 * 
 * Generates consistent color stacks based on recording ID hash.
 * Same logic as PlayerV2 for visual consistency.
 */
private fun getRecordingColorStack(recordingId: String?): List<Color> {
    val baseColor = recordingIdToColor(recordingId)
    
    return listOf(
        baseColor.copy(alpha = 0.8f), // Strong
        baseColor.copy(alpha = 0.4f), // Medium  
        baseColor.copy(alpha = 0.2f), // Light
        baseColor.copy(alpha = 0.1f)  // Very Light
    )
}

private val GradientColors = listOf(
    Color(0xFF1DB954), // Spotify Green (DeadGreen equivalent)
    Color(0xFFFFD700), // Gold
    Color(0xFFDC143C), // Crimson Red
    Color(0xFF4169E1), // Royal Blue
    Color(0xFF9370DB)  // Medium Purple
)

private fun recordingIdToColor(recordingId: String?): Color {
    if (recordingId == null) return GradientColors[0]
    
    val hash = recordingId.hashCode()
    val index = kotlin.math.abs(hash) % GradientColors.size
    return GradientColors[index]
}