package com.deadarchive.feature.library.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.Show

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
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onDownload: () -> Unit,
    onPin: () -> Unit,
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
            
            ActionButton(
                icon = IconResources.Content.CloudDownload(),
                label = "Download",
                onClick = onDownload
            )
            
            ActionButton(
                icon = IconResources.Content.PushPin(),
                label = "Pin",
                onClick = onPin
            )
            
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = show.date,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = show.displayVenue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (show.displayLocation != "Unknown Location") {
            Text(
                text = show.displayLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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