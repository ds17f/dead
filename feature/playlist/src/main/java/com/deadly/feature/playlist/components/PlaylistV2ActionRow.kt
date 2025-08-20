package com.deadly.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deadly.core.design.component.IconResources
import com.deadly.core.design.component.LibraryAction
import com.deadly.feature.playlist.model.PlaylistShowViewModel

/**
 * PlaylistV2ActionRow - V2 implementation of action buttons row
 * 
 * Copies V1 action buttons layout exactly but integrates with V2 architecture.
 * Left side: Grouped action buttons (Library, Download, Setlist, Menu)
 * Right side: Large play/pause button
 */
@Composable
fun PlaylistV2ActionRow(
    showData: PlaylistShowViewModel,
    isPlaying: Boolean,
    onLibraryAction: (LibraryAction) -> Unit,
    onDownload: () -> Unit,
    onShowSetlist: () -> Unit,
    onShowMenu: () -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Grouped action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Library button
            IconButton(
                onClick = { 
                    if (showData.isInLibrary) {
                        onLibraryAction(LibraryAction.REMOVE_FROM_LIBRARY)
                    } else {
                        onLibraryAction(LibraryAction.ADD_TO_LIBRARY)
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = if (showData.isInLibrary) {
                        IconResources.Content.LibraryAddCheck()
                    } else {
                        IconResources.Content.LibraryAdd()
                    },
                    contentDescription = if (showData.isInLibrary) "Remove from Library" else "Add to Library",
                    tint = if (showData.isInLibrary) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Download button
            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(40.dp)
            ) {
                when {
                    showData.downloadProgress == null -> {
                        // Not downloaded
                        Icon(
                            painter = IconResources.Content.FileDownload(),
                            contentDescription = "Download Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Setlist button
            IconButton(
                onClick = onShowSetlist,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = IconResources.Content.FormatListBulleted(),
                    contentDescription = "Show Setlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Menu button
            IconButton(
                onClick = onShowMenu,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = IconResources.Navigation.MoreVertical(),
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Right side: Play/Pause button (large)
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = if (isPlaying) {
                    IconResources.PlayerControls.PauseCircleFilled()
                } else {
                    IconResources.PlayerControls.PlayCircleFilled()
                },
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}