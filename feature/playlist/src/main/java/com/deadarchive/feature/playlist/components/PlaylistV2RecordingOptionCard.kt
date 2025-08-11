package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.CompactStarRating
import com.deadarchive.feature.playlist.RecordingOptionV2ViewModel

/**
 * PlaylistV2RecordingOptionCard - Individual recording option card for V2 architecture
 * 
 * Based on V1 RecordingOptionCard but uses V2 View Models and follows V2 patterns.
 * Displays recording information with selection state and recommendation status.
 */
@Composable
fun PlaylistV2RecordingOptionCard(
    recordingOption: RecordingOptionV2ViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                recordingOption.isSelected -> MaterialTheme.colorScheme.primaryContainer
                recordingOption.isRecommended -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (recordingOption.isSelected) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Source and Rating row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recordingOption.source,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    recordingOption.rating?.let { rating ->
                        CompactStarRating(
                            rating = rating,
                            starSize = 12.dp
                        )
                    }
                }
                
                // Recording title/quality
                if (recordingOption.title.isNotBlank()) {
                    Text(
                        text = recordingOption.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Match reason or recommendation
                recordingOption.matchReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            recordingOption.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            recordingOption.isRecommended -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (recordingOption.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}