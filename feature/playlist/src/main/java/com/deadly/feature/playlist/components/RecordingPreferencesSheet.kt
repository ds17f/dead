package com.deadly.feature.playlist.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadly.core.model.AppConstants
import com.deadly.core.settings.api.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingPreferencesSheet(
    settings: AppSettings,
    onUpdatePreferredAudioSource: (String) -> Unit,
    onUpdateMinimumRating: (Float) -> Unit,
    onUpdatePreferHigherRated: (Boolean) -> Unit,
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
                    text = "Recording Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preferred Audio Source
            Text(
                text = "Preferred Audio Source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val audioSources = listOf("Any", "Soundboard", "Audience")
            
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .padding(bottom = 24.dp)
            ) {
                audioSources.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = settings.preferredAudioSource == source,
                                onClick = { onUpdatePreferredAudioSource(source) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.preferredAudioSource == source,
                            onClick = null
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            val description = when (source) {
                                "Any" -> "No preference - select best available"
                                "Soundboard" -> "Prefer direct board recordings (SBD)"
                                "Audience" -> "Prefer audience recordings (AUD)"
                                else -> ""
                            }
                            
                            if (description.isNotEmpty()) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Minimum Rating Filter
            Text(
                text = "Minimum Rating Filter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Only show recordings with at least ${settings.minimumRating}★",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Slider(
                value = settings.minimumRating,
                onValueChange = onUpdateMinimumRating,
                valueRange = 0f..5f,
                steps = 9, // 0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Higher Rated Preference
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Prefer Higher Rated",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "When multiple recordings match criteria, prefer higher rated ones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = settings.preferHigherRated,
                    onCheckedChange = onUpdatePreferHigherRated
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}