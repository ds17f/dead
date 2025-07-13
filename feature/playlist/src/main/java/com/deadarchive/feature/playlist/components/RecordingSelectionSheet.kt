package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deadarchive.core.model.Recording
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.design.component.CompactStarRating

data class RecordingOption(
    val recording: Recording,
    val isRecommended: Boolean = false,
    val matchReason: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSelectionSheet(
    showTitle: String,
    currentRecording: Recording?,
    alternativeRecordings: List<RecordingOption>,
    settings: AppSettings,
    onRecordingSelected: (Recording) -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose Recording",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Recording Preferences"
                    )
                }
            }
            
            Text(
                text = showTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Current Selection Criteria Info
            if (settings.preferredAudioSource != "Any" || settings.minimumRating > 0f) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Selection Preferences",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        if (settings.preferredAudioSource != "Any") {
                            Text(
                                text = "Source: ${settings.preferredAudioSource}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        if (settings.minimumRating > 0f) {
                            Text(
                                text = "Min Rating: ${settings.minimumRating}★",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
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
                            isSelected = true,
                            isRecommended = false,
                            matchReason = "Currently Playing",
                            onClick = { onRecordingSelected(currentRecording) }
                        )
                    }
                }
                
                // Alternative recordings
                items(alternativeRecordings) { option ->
                    RecordingOptionCard(
                        recording = option.recording,
                        isSelected = false,
                        isRecommended = option.isRecommended,
                        matchReason = option.matchReason,
                        onClick = { onRecordingSelected(option.recording) }
                    )
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