package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.feature.playlist.model.PlaylistTrackViewModel

/**
 * PlaylistV2TrackItem - Individual track row with play/pause, download states
 * 
 * Clean V2 implementation of track list item with playback controls, 
 * download indicators, and current track highlighting. Matches V1 functionality
 * with V2 architecture patterns.
 */
@Composable
fun PlaylistV2TrackItem(
    track: PlaylistTrackViewModel,
    onPlayClick: (PlaylistTrackViewModel) -> Unit,
    onDownloadClick: (PlaylistTrackViewModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick(track) }
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .then(
                // Highlight current track with subtle background
                if (track.isCurrentTrack) {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ).padding(8.dp)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // Track number
        Text(
            text = track.number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (track.isCurrentTrack) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(24.dp)
        )
        
        // Play/Pause button
        IconButton(
            onClick = { onPlayClick(track) },
            modifier = Modifier.size(40.dp)
        ) {
            if (track.isCurrentTrack && track.isPlaying) {
                Icon(
                    painter = IconResources.PlayerControls.Pause(),
                    contentDescription = "Pause ${track.title}",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${track.title}",
                    tint = if (track.isCurrentTrack) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        
        // Track title and details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (track.isCurrentTrack) FontWeight.Medium else FontWeight.Normal,
                color = if (track.isCurrentTrack) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = track.format,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Download indicator
                if (track.isDownloaded) {
                    Icon(
                        painter = IconResources.Status.CheckCircle(),
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        
        // Download button/progress
        when {
            track.downloadProgress == null && !track.isDownloaded -> {
                // Not downloaded - show download button
                IconButton(
                    onClick = { onDownloadClick(track) }
                ) {
                    Icon(
                        painter = IconResources.Content.FileDownload(),
                        contentDescription = "Download ${track.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            track.downloadProgress != null && track.downloadProgress < 1.0f -> {
                // Downloading - show progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { track.downloadProgress },
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "${(track.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            track.isDownloaded -> {
                // Downloaded - show completed state
                Icon(
                    painter = IconResources.Status.CheckCircle(),
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}