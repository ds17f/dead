package com.deadly.v2.feature.playlist.screens.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadly.v2.core.model.PlaylistShowViewModel

/**
 * PlaylistV2ShowInfo - Show information with navigation
 * 
 * Clean V2 implementation displaying concert date, venue, and location
 * with previous/next navigation buttons. Matches V1 layout and styling.
 */
@Composable
fun PlaylistShowInfo(
    showData: PlaylistShowViewModel,
    isNavigationLoading: Boolean = false,
    onPreviousShow: () -> Unit,
    onNextShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Show info
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            // Show Date
            Text(
                text = showData.displayDate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Venue and Location
            val venueLine = "${showData.venue}, ${showData.location}"
            
            Text(
                text = venueLine,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Right side: Navigation buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous show button
            IconButton(
                onClick = onPreviousShow,
                enabled = !isNavigationLoading && showData.hasPreviousShow,
                modifier = Modifier.size(40.dp)
            ) {
                if (isNavigationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous Show",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Next show button
            IconButton(
                onClick = onNextShow,
                enabled = !isNavigationLoading && showData.hasNextShow,
                modifier = Modifier.size(40.dp)
            ) {
                if (isNavigationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next Show",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}