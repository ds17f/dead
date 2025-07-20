package com.deadarchive.core.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
import com.deadarchive.core.settings.model.VersionInfo

/**
 * Main settings screen with organized card-based sections
 * Follows established DebugScreen layout patterns and Material3 design system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDebug: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    versionInfo: VersionInfo,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val backupJson by viewModel.backupJson.collectAsState()
    val backupFile by viewModel.backupFile.collectAsState()
    
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
                onUpdateDownloadWifiOnly = viewModel::updateDownloadWifiOnly,
                onUpdateDeletionGracePeriod = viewModel::updateDeletionGracePeriod,
                onUpdateLowStorageThreshold = viewModel::updateLowStorageThreshold,
                onNavigateToDownloads = onNavigateToDownloads
            )
            
            // Backup & Restore Section
            BackupRestoreCard(
                settings = settings,
                backupJson = backupJson,
                backupFile = backupFile,
                onBackupLibrary = viewModel::backupLibrary,
                onRestoreLibrary = viewModel::restoreLibrary
            )
            
            // About Section
            AboutCard(versionInfo = versionInfo)
            
            // Developer Options Section
            DeveloperOptionsCard(
                settings = settings,
                onNavigateToDebug = onNavigateToDebug,
                onResetSettings = viewModel::resetToDefaults,
                onUpdateShowDebugInfo = viewModel::updateShowDebugInfo
            )
        }
    }
    
    // Clear messages after a delay
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            kotlinx.coroutines.delay(5000) // Extended to 5 seconds for better visibility
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun AudioFormatSettingsCard(
    settings: AppSettings,
    onUpdateAudioFormats: (List<String>) -> Unit
) {
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
                text = "Use arrows to reorder priority when multiple formats are available",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Display current format order with up/down controls
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                settings.audioFormatPreferences.forEachIndexed { index, format ->
                    FormatPreferenceItem(
                        format = format,
                        position = index + 1,
                        isFirst = index == 0,
                        isLast = index == settings.audioFormatPreferences.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                val newList = settings.audioFormatPreferences.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index - 1]
                                newList[index - 1] = temp
                                onUpdateAudioFormats(newList)
                            }
                        },
                        onMoveDown = {
                            if (index < settings.audioFormatPreferences.size - 1) {
                                val newList = settings.audioFormatPreferences.toMutableList()
                                val temp = newList[index]
                                newList[index] = newList[index + 1]
                                newList[index + 1] = temp
                                onUpdateAudioFormats(newList)
                            }
                        }
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
    onUpdateDownloadWifiOnly: (Boolean) -> Unit,
    onUpdateDeletionGracePeriod: (Int) -> Unit,
    onUpdateLowStorageThreshold: (Long) -> Unit,
    onNavigateToDownloads: () -> Unit
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
            
            // WiFi Only Setting
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
                    checked = settings.downloadWifiOnly,
                    onCheckedChange = onUpdateDownloadWifiOnly
                )
            }
            
            HorizontalDivider()
            
            // Deletion Grace Period Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Grace period: ${settings.deletionGracePeriodDays} days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "How long to keep removed downloads before cleanup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { 
                            if (settings.deletionGracePeriodDays > 1) {
                                onUpdateDeletionGracePeriod(settings.deletionGracePeriodDays - 1)
                            }
                        },
                        enabled = settings.deletionGracePeriodDays > 1
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = { 
                            onUpdateDeletionGracePeriod(settings.deletionGracePeriodDays + 1)
                        }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            // Storage Threshold Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Storage threshold: ${settings.lowStorageThresholdMB}MB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Trigger cleanup when free space falls below this",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(
                        onClick = { 
                            if (settings.lowStorageThresholdMB > 100L) {
                                onUpdateLowStorageThreshold(settings.lowStorageThresholdMB - 100L)
                            }
                        },
                        enabled = settings.lowStorageThresholdMB > 100L
                    ) {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = { 
                            onUpdateLowStorageThreshold(settings.lowStorageThresholdMB + 100L)
                        }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onNavigateToDownloads,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Downloads")
            }
        }
    }
}


@Composable
private fun DeveloperOptionsCard(
    settings: AppSettings,
    onNavigateToDebug: () -> Unit,
    onResetSettings: () -> Unit,
    onUpdateShowDebugInfo: (Boolean) -> Unit
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
            
            // Debug Info Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Debug Info",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Display debug panels and technical information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showDebugInfo,
                    onCheckedChange = onUpdateShowDebugInfo
                )
            }
            
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
private fun FormatPreferenceItem(
    format: String,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            
            // Arrow controls
            Row {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (!isFirst) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (!isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreCard(
    settings: AppSettings,
    backupJson: String?,
    backupFile: java.io.File?,
    onBackupLibrary: () -> Unit,
    onRestoreLibrary: () -> Unit
) {
    var showRestoreDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Backup & Restore",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Backup your library and settings to restore on other devices",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Backup Button
            OutlinedButton(
                onClick = onBackupLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup Library")
            }
            
            // Restore Button
            OutlinedButton(
                onClick = { showRestoreDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Library")
            }
            
            // Debug section - only show if debug info is enabled
            if (settings.showDebugInfo && backupJson != null) {
                android.util.Log.d("SettingsScreen", "Rendering debug section - JSON length: ${backupJson.length}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Debug Info",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // File information
                android.util.Log.d("SettingsScreen", "Rendering file info - file: ${backupFile?.absolutePath ?: "null"}")
                if (backupFile != null) {
                    Text(
                        text = "File: ${backupFile.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Location: ${backupFile.absolutePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Size: ${backupFile.length()} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "File: Not saved (memory only)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Backup JSON (${backupJson.length} characters):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                android.util.Log.d("SettingsScreen", "About to render DebugBackupJsonDisplay")
                DebugBackupJsonDisplay(backupJson = backupJson)
                android.util.Log.d("SettingsScreen", "DebugBackupJsonDisplay rendered successfully")
            }
        }
    }
    
    // Restore confirmation dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Library") },
            text = { 
                Text("This will restore your library from the most recent backup file in Downloads. This will replace your current library and settings. Are you sure?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        onRestoreLibrary()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DebugBackupJsonDisplay(backupJson: String) {
    android.util.Log.d("DebugBackupJsonDisplay", "Starting to render - JSON length: ${backupJson.length}")
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            android.util.Log.d("DebugBackupJsonDisplay", "Rendering header row")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JSON Content",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                android.util.Log.d("DebugBackupJsonDisplay", "Rendering copy button")
                OutlinedButton(
                    onClick = {
                        android.util.Log.d("DebugBackupJsonDisplay", "Copy button clicked")
                        clipboardManager.setText(AnnotatedString(backupJson))
                        android.util.Log.d("DebugBackupJsonDisplay", "Text copied to clipboard")
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy JSON",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            android.util.Log.d("DebugBackupJsonDisplay", "Creating preview text")
            // Scrollable JSON preview (first 500 characters)
            val preview = if (backupJson.length > 500) {
                "${backupJson.take(500)}..."
            } else {
                backupJson
            }
            
            android.util.Log.d("DebugBackupJsonDisplay", "Rendering preview text - length: ${preview.length}")
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            android.util.Log.d("DebugBackupJsonDisplay", "Preview text rendered successfully")
        }
    }
    android.util.Log.d("DebugBackupJsonDisplay", "Component rendered successfully")
}

@Composable
private fun AboutCard(versionInfo: VersionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "About",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // App Version
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = versionInfo.getFormattedVersion(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Version Code (for debug builds)
            if (versionInfo.isDebugBuild()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version Code",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = versionInfo.versionCode.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Build Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Build Date",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = versionInfo.getFormattedBuildDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Build Type (for debug builds)
            if (versionInfo.isDebugBuild()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Build Type",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = versionInfo.buildType.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

