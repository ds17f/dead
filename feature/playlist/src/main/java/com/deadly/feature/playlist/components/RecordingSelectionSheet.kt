package com.deadly.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import com.deadly.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadly.core.model.Recording
import com.deadly.core.model.RecordingOption
import com.deadly.core.settings.api.model.AppSettings
import com.deadly.core.design.component.CompactStarRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet(
    showTitle: String,
    currentRecording: Recording?,
    alternativeRecordings: List<RecordingOption>,
    settings: AppSettings,
    onRecordingSelected: (Recording) -> Unit,
    onSetAsDefault: (String) -> Unit,
    onResetToRecommended: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRecording by remember { mutableStateOf(currentRecording) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Choose Recording",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = showTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            
            // Recording Options
            LazyColumn(
                modifier = Modifier
                    .selectableGroup()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current recording first
                if (currentRecording != null) {
                    item {
                        RecordingOptionCard(
                            recording = currentRecording,
                            isSelected = selectedRecording?.identifier == currentRecording.identifier,
                            isRecommended = false,
                            matchReason = "Currently Playing",
                            onClick = { 
                                selectedRecording = currentRecording
                                onRecordingSelected(currentRecording) 
                            }
                        )
                    }
                }
                
                // Alternative recordings
                items(alternativeRecordings) { option ->
                    RecordingOptionCard(
                        recording = option.recording,
                        isSelected = selectedRecording?.identifier == option.recording.identifier,
                        isRecommended = option.isRecommended,
                        matchReason = option.matchReason,
                        onClick = { 
                            selectedRecording = option.recording
                            onRecordingSelected(option.recording) 
                        }
                    )
                }
            }
            
            // Action buttons
            val hasRecommendedRecording = alternativeRecordings.any { it.isRecommended && it.matchReason == "Recommended" }
            val recommendedRecording = alternativeRecordings.find { it.isRecommended && it.matchReason == "Recommended" }
            val currentIsRecommended = recommendedRecording?.recording?.identifier == currentRecording?.identifier
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reset to Recommended button (show if there's a recommended recording and current isn't it)
            if (hasRecommendedRecording && !currentIsRecommended && onResetToRecommended != null) {
                OutlinedButton(
                    onClick = { 
                        onResetToRecommended()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = IconResources.Content.Star(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Recommended")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Set as Default button (only show if different recording selected)
            if (selectedRecording != null && selectedRecording?.identifier != currentRecording?.identifier) {
                Button(
                    onClick = { 
                        onSetAsDefault(selectedRecording!!.identifier)
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
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordingOptionCard(
    recording: Recording,
    isSelected: Boolean,
    isRecommended: Boolean,
    matchReason: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isRecommended -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
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
                // Source and Date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recording.source ?: "Unknown Source",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    recording.rawRating?.let { rating ->
                        CompactStarRating(
                            rating = rating,
                            starSize = 12.dp
                        )
                    }
                }
                
                // Quality and Format
                if (!recording.title.isNullOrBlank()) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Match reason or recommendation
                matchReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            isRecommended -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (isSelected) {
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