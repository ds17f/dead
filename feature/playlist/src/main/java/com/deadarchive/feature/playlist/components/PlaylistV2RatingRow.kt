package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deadarchive.feature.playlist.components.InteractiveRatingDisplay
import com.deadarchive.core.design.component.LibraryAction
import com.deadarchive.feature.playlist.model.PlaylistShowViewModel

/**
 * PlaylistV2RatingRow - Action buttons and rating display
 * 
 * Clean V2 implementation of the rating and action row, combining
 * library button, download button, share button, and interactive rating display.
 * Matches V1 layout and functionality with V2 architecture patterns.
 */
@Composable
fun PlaylistV2RatingRow(
    showData: PlaylistShowViewModel,
    onLibraryAction: (LibraryAction) -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onShowReviews: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Action buttons (on the left side)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Library button - simplified for V2 stub implementation
            IconButton(onClick = { 
                if (showData.isInLibrary) {
                    onLibraryAction(LibraryAction.REMOVE_FROM_LIBRARY) 
                } else {
                    onLibraryAction(LibraryAction.ADD_TO_LIBRARY)
                }
            }) {
                Icon(
                    imageVector = if (showData.isInLibrary) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = if (showData.isInLibrary) "Remove from Library" else "Add to Library",
                    tint = if (showData.isInLibrary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Download button with progress states
            IconButton(
                onClick = onDownloadClick
            ) {
                when {
                    showData.downloadProgress == null -> {
                        // Not downloaded
                        Icon(
                            painter = IconResources.Content.FileDownload(),
                            contentDescription = "Download Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    showData.downloadProgress < 1.0f -> {
                        // Downloading - show progress
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { showData.downloadProgress },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                            Icon(
                                painter = IconResources.Content.FileDownload(),
                                contentDescription = "Downloading ${(showData.downloadProgress * 100).toInt()}%",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    else -> {
                        // Downloaded - show completed state
                        Icon(
                            painter = IconResources.Status.CheckCircle(),
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Share button
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share Show",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Interactive rating display (takes remaining space)
        InteractiveRatingDisplay(
            rating = showData.rating,
            reviewCount = showData.reviewCount,
            confidence = 0.8f, // Stub confidence value
            onShowReviews = onShowReviews,
            modifier = Modifier.weight(1f)
        )
    }
}