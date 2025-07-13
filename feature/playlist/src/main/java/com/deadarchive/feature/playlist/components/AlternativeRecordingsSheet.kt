package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.CompactStarRating
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.Recording

/**
 * Modal bottom sheet that shows alternative recordings for a show
 * and allows user to select their preferred recording
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeRecordingsSheet(
    showTitle: String,
    recordings: List<Recording>,
    currentRecordingId: String,
    preferredRecordingId: String?,
    onRecordingSelected: (String) -> Unit,
    onSetAsPreferred: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRecordingId by remember { mutableStateOf(preferredRecordingId ?: currentRecordingId) }
    val showSetDefaultButton = selectedRecordingId != (preferredRecordingId ?: currentRecordingId)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Alternative Recordings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = showTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${recordings.size} recordings available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Recording list
            items(recordings) { recording ->
                RecordingSelectionItem(
                    recording = recording,
                    isSelected = recording.identifier == selectedRecordingId,
                    isCurrent = recording.identifier == currentRecordingId,
                    isPreferred = recording.identifier == preferredRecordingId,
                    onSelect = { 
                        selectedRecordingId = recording.identifier
                        onRecordingSelected(recording.identifier)
                    }
                )
            }
            
            // Set as default button
            if (showSetDefaultButton) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            onSetAsPreferred(selectedRecordingId)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = IconResources.Content.Star(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Default Recording")
                    }
                }
            }
            
            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecordingSelectionItem(
    recording: Recording,
    isSelected: Boolean,
    isCurrent: Boolean,
    isPreferred: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isCurrent -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Recording info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source type badge
                    SourceTypeBadge(sourceType = recording.cleanSource)
                    
                    // Status indicators
                    if (isCurrent) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (isPreferred && !isCurrent) {
                        Text(
                            text = "PREFERRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Rating
                if (recording.hasRawRating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactStarRating(
                            rating = recording.rawRating,
                            confidence = recording.ratingConfidence,
                            starSize = IconResources.Size.SMALL
                        )
                        Text(
                            text = String.format("%.1f", recording.rawRating ?: 0f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        recording.reviewCount?.let { count ->
                            if (count > 0) {
                                Text(
                                    text = "($count reviews)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No rating available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Additional info
                if (!recording.source.isNullOrBlank() && recording.source != recording.cleanSource) {
                    Text(
                        text = "Source: ${recording.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTypeBadge(
    sourceType: String?
) {
    val (backgroundColor, textColor) = when (sourceType?.uppercase()) {
        "SBD" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "MATRIX" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary  
        "FM" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "AUD" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = sourceType ?: "Unknown",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}