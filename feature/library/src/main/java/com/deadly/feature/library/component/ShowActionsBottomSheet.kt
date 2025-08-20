package com.deadly.feature.library.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadly.core.design.component.IconResources
import com.deadly.core.model.Show
import com.deadly.core.download.api.DownloadStatus

/**
 * Bottom sheet that displays actions for a specific show.
 * Appears when a user long-presses on a show item.
 * 
 * Features:
 * - Share: Share the show URL via system intent
 * - Remove from Library: Delete the show from user's library
 * - Download: Download the show for offline playback
 * - Pin: Pin the show for quick access (stub for now)
 * - Show QR Code: Display a QR code linking to archive.org
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowActionsBottomSheet(
    show: Show,
    downloadStatus: DownloadStatus,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onShowQRCode: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with show info
            ShowActionHeader(show)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            ActionButton(
                icon = IconResources.Content.Share(),
                label = "Share",
                onClick = onShare
            )
            
            ActionButton(
                icon = IconResources.Content.Delete(),
                label = "Remove from Library",
                onClick = onRemoveFromLibrary
            )
            
            // Dynamic download action based on status
            if (downloadStatus == DownloadStatus.COMPLETED) {
                ActionButton(
                    icon = IconResources.Content.Delete(),
                    label = "Remove Download",
                    onClick = onRemoveDownload
                )
            } else {
                ActionButton(
                    icon = IconResources.Content.ArrowCircleDown(),
                    label = "Download",
                    onClick = onDownload
                )
            }
            
            // Dynamic pin action based on status
            if (isPinned) {
                ActionButton(
                    icon = IconResources.Content.PushPin(),
                    label = "Unpin",
                    onClick = onUnpin
                )
            } else {
                ActionButton(
                    icon = IconResources.Content.PushPin(),
                    label = "Pin",
                    onClick = onPin
                )
            }
            
            ActionButton(
                icon = IconResources.Content.QrCode(),
                label = "Show QR Code",
                onClick = onShowQRCode
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShowActionHeader(show: Show) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album cover placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = IconResources.PlayerControls.AlbumArt(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Show info - left justified like the card
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${show.date} • ${show.displayLocation}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = show.displayVenue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}