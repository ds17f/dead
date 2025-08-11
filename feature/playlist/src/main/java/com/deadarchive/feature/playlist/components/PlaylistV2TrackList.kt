package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadarchive.feature.playlist.model.PlaylistTrackViewModel

/**
 * PlaylistV2TrackList - Scrollable track listing component
 * 
 * Clean V2 implementation as LazyListScope extension for integration
 * with main PlaylistV2Screen LazyColumn. Displays track list with
 * section header and individual track items.
 */
fun LazyListScope.PlaylistV2TrackList(
    tracks: List<PlaylistTrackViewModel>,
    onPlayClick: (PlaylistTrackViewModel) -> Unit,
    onDownloadClick: (PlaylistTrackViewModel) -> Unit
) {
    // Section header
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Tracks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (tracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Track items
    if (tracks.isEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "No tracks available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track information will appear here when available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        items(
            items = tracks,
            key = { track -> track.number }
        ) { track ->
            PlaylistV2TrackItem(
                track = track,
                onPlayClick = onPlayClick,
                onDownloadClick = onDownloadClick
            )
            
            // Divider between tracks (except last)
            if (track != tracks.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
    
    // Bottom spacing
    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}