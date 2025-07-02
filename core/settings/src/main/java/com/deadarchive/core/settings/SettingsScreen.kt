package com.deadarchive.core.settings

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.model.ThemeMode

/**
 * Main settings screen with organized card-based sections
 * Follows established DebugScreen layout patterns and Material3 design system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDebug: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Show loading indicator if needed
            if (uiState.isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Updating settings...")
                    }
                }
            }
            
            // Show error message if present
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Show success message if present
            uiState.successMessage?.let { successMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Audio Format Preferences Section
            AudioFormatSettingsCard(
                settings = settings,
                onUpdateAudioFormats = viewModel::updateAudioFormatPreference
            )
            
            // Appearance Settings Section
            AppearanceSettingsCard(
                settings = settings,
                onUpdateThemeMode = viewModel::updateThemeMode
            )
            
            // Download Settings Section
            DownloadSettingsCard(
                settings = settings,
                onUpdateDownloadWifiOnly = viewModel::updateDownloadWifiOnly
            )
            
            
            // Developer Options Section
            DeveloperOptionsCard(
                onNavigateToDebug = onNavigateToDebug,
                onResetSettings = viewModel::resetToDefaults
            )
        }
    }
    
    // Clear messages after a delay
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun AudioFormatSettingsCard(
    settings: AppSettings,
    onUpdateAudioFormats: (List<String>) -> Unit
) {
    var draggedItem by remember { mutableStateOf<String?>(null) }
    var draggedOverItem by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Audio Format Preferences",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Drag to reorder priority when multiple formats are available",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Display current format order with drag-and-drop
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settings.audioFormatPreference.forEachIndexed { index, format ->
                    DraggableFormatItem(
                        format = format,
                        position = index + 1,
                        isDragged = draggedItem == format,
                        isDraggedOver = draggedOverItem == format,
                        onDragStart = { draggedItem = format },
                        onDragEnd = { 
                            draggedItem?.let { draggedFormat ->
                                draggedOverItem?.let { targetFormat ->
                                    val fromIndex = settings.audioFormatPreference.indexOf(draggedFormat)
                                    val toIndex = settings.audioFormatPreference.indexOf(targetFormat)
                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                        val newList = settings.audioFormatPreference.toMutableList()
                                        val item = newList.removeAt(fromIndex)
                                        newList.add(toIndex, item)
                                        onUpdateAudioFormats(newList)
                                    }
                                }
                            }
                            draggedItem = null
                            draggedOverItem = null
                        },
                        onDragOver = { draggedOverItem = format }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsCard(
    settings: AppSettings,
    onUpdateThemeMode: (ThemeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Appearance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { themeMode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == themeMode,
                            onClick = { onUpdateThemeMode(themeMode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = themeMode.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadSettingsCard(
    settings: AppSettings,
    onUpdateDownloadWifiOnly: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Downloads",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Download on WiFi only",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Restrict downloads to WiFi connections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.downloadOnWifiOnly,
                    onCheckedChange = onUpdateDownloadWifiOnly
                )
            }
        }
    }
}


@Composable
private fun DeveloperOptionsCard(
    onNavigateToDebug: () -> Unit,
    onResetSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Developer Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedButton(
                onClick = onNavigateToDebug,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Debug & Testing")
            }
            
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset All Settings")
            }
        }
    }
}

@Composable
private fun DraggableFormatItem(
    format: String,
    position: Int,
    isDragged: Boolean,
    isDraggedOver: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragOver: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(format) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() }
                ) { _, _ ->
                    onDragOver()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragged -> MaterialTheme.colorScheme.primaryContainer
                isDraggedOver -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragged) 8.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Priority indicator
                Card(
                    modifier = Modifier.size(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (position) {
                            1 -> MaterialTheme.colorScheme.primary
                            2 -> MaterialTheme.colorScheme.secondary
                            3 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = position.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (position) {
                                1 -> MaterialTheme.colorScheme.onPrimary
                                2 -> MaterialTheme.colorScheme.onSecondary
                                3 -> MaterialTheme.colorScheme.onTertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = format,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (position) {
                            1 -> "Most preferred"
                            2 -> "Second choice"
                            3 -> "Third choice"
                            else -> "Alternative"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Drag handle
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}