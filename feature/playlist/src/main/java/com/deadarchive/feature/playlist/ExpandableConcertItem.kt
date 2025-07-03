package com.deadarchive.feature.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording

@Composable
fun ExpandableConcertItem(
    show: Show,
    onShowClick: (Show) -> Unit,
    onRecordingClick: (Recording) -> Unit,
    onFavoriteClick: (Show) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main show header
            ShowHeader(
                show = show,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded },
                onShowClick = onShowClick,
                onFavoriteClick = onFavoriteClick
            )
            
            // Expandable recordings section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RecordingsSection(
                    recordings = show.recordings,
                    onRecordingClick = onRecordingClick
                )
            }
        }
    }
}

@Composable
private fun ShowHeader(
    show: Show,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onShowClick: (Show) -> Unit,
    onFavoriteClick: (Show) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onShowClick(show) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = IconResources.PlayerControls.AlbumArt(),
                contentDescription = "Concert Art",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Concert information
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Date (most prominent)
            Text(
                text = show.displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Venue name
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // City, State
            Text(
                text = show.displayLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Recording count and sources
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${show.recordingCount} recordings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (show.availableSources.isNotEmpty()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = show.availableSources.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Favorite button
            IconButton(
                onClick = { onFavoriteClick(show) }
            ) {
                Icon(
                    imageVector = if (show.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (show.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Expand/collapse button
            IconButton(onClick = onExpandClick) {
                Icon(
                    painter = if (isExpanded) IconResources.Navigation.ExpandLess() else IconResources.Navigation.ExpandMore(),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordingsSection(
    recordings: List<Recording>,
    onRecordingClick: (Recording) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Section header
        Text(
            text = "Available Recordings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Recordings list - sorted by quality priority (SBD > MATRIX > FM > AUD > others)
        recordings.sortedBy { recording ->
            when (recording.cleanSource?.uppercase()) {
                "SBD" -> 1
                "MATRIX" -> 2
                "FM" -> 3
                "AUD" -> 4
                else -> 5
            }
        }.forEach { recording ->
            RecordingItem(
                recording = recording,
                onRecordingClick = onRecordingClick
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun RecordingItem(
    recording: Recording,
    onRecordingClick: (Recording) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onRecordingClick(recording) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (recording.cleanSource?.uppercase()) {
                            "SBD" -> Color(0xFFD32F2F)      // Bright Red - Highest priority
                            "MATRIX" -> Color(0xFFFF9800)   // Orange - High priority  
                            "FM" -> Color(0xFF4CAF50)       // Green - Medium priority
                            "AUD" -> Color(0xFF2196F3)      // Blue - Lower priority
                            else -> MaterialTheme.colorScheme.outline // Grey - Unknown
                        }.also { color ->
                            // Debug logging to see what's happening
                            println("DEBUG: Recording ${recording.identifier} - source='${recording.source}' cleanSource='${recording.cleanSource}' color=$color")
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = recording.cleanSource?.take(3)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (recording.cleanSource?.uppercase()) {
                        "SBD" -> Color.White       // White text on red background
                        "MATRIX" -> Color.White    // White text on orange background
                        "FM" -> Color.White        // White text on green background
                        "AUD" -> Color.White       // White text on blue background
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Recording details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recording.recordingQuality,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                val details = buildList {
                    recording.taper?.let { add("Taper: $it") }
                    if (recording.tracks.isNotEmpty()) {
                        add("${recording.tracks.size} tracks")
                    }
                }.joinToString(" • ")
                
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Play button
            IconButton(
                onClick = { onRecordingClick(recording) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = IconResources.PlayerControls.Play(),
                    contentDescription = "Play Recording",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}