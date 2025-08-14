package com.deadarchive.core.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.api.model.ThemeMode
import com.deadarchive.core.settings.model.VersionInfo
import com.deadarchive.core.design.component.UpdateAvailableDialog

/**
 * Main settings screen with organized card-based sections
 * Follows Material3 design system with organized card-based sections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionInfo: VersionInfo,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val currentUpdate by viewModel.currentUpdate.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val installationStatus by viewModel.installationStatus.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Settings")
                    }
                },
                windowInsets = WindowInsets(0.dp)
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
            
            // Update Settings Section
            UpdateSettingsCard(
                settings = settings,
                updateStatus = updateStatus,
                onCheckForUpdates = viewModel::checkForUpdates,
                onSetAutoUpdateEnabled = viewModel::setAutoUpdateCheckEnabled
            )
            
            // About Section
            AboutCard(versionInfo = versionInfo)
            
            // Developer Options Section
            DeveloperOptionsCard(
                settings = settings,
                onUpdateShowDebugInfo = viewModel::updateShowDebugInfo,
                onUpdateUseLibraryV2 = viewModel::updateUseLibraryV2,
                onUpdateUsePlayerV2 = viewModel::updateUsePlayerV2,
                onUpdateUseSearchV2 = viewModel::updateUseSearchV2,
                onUpdateUseHomeV2 = viewModel::updateUseHomeV2,
                onUpdateUsePlaylistV2 = viewModel::updateUsePlaylistV2,
                onUpdateUseMiniPlayerV2 = viewModel::updateUseMiniPlayerV2,
                onUpdateUseSplashV2 = viewModel::updateUseSplashV2,
                onResetSettings = viewModel::resetToDefaults,
                onClearV2Database = viewModel::clearV2Database
            )
        }
    }
    
    // Show update dialog if update is available
    currentUpdate?.let { update ->
        UpdateAvailableDialog(
            update = update,
            downloadState = downloadState,
            installationStatus = installationStatus,
            onDownload = viewModel::downloadUpdate,
            onSkip = viewModel::skipUpdate,
            onInstall = viewModel::installUpdate,
            onDismiss = viewModel::clearUpdateState
        )
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
private fun DeveloperOptionsCard(
    settings: AppSettings,
    onUpdateShowDebugInfo: (Boolean) -> Unit,
    onUpdateUseLibraryV2: (Boolean) -> Unit,
    onUpdateUsePlayerV2: (Boolean) -> Unit,
    onUpdateUseSearchV2: (Boolean) -> Unit,
    onUpdateUseHomeV2: (Boolean) -> Unit,
    onUpdateUsePlaylistV2: (Boolean) -> Unit,
    onUpdateUseMiniPlayerV2: (Boolean) -> Unit,
    onUpdateUseSplashV2: (Boolean) -> Unit,
    onResetSettings: () -> Unit,
    onClearV2Database: () -> Unit
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
            
            // Debug Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Debug Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Show debug panels and diagnostic information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showDebugInfo,
                    onCheckedChange = onUpdateShowDebugInfo
                )
            }
            
            HorizontalDivider()
            
            // Library V2 Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Use Library V2 (Preview)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned library interface",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useLibraryV2,
                    onCheckedChange = onUpdateUseLibraryV2
                )
            }
            
            HorizontalDivider()
            
            // Player V2 Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Use Player V2 (Preview) 🚀",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned player interface (UI-first development)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.usePlayerV2,
                    onCheckedChange = onUpdateUsePlayerV2
                )
            }
            
            HorizontalDivider()
            
            // SearchV2 Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SearchV2",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned search interface (UI-first development)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useSearchV2,
                    onCheckedChange = onUpdateUseSearchV2
                )
            }
            
            HorizontalDivider()
            
            // HomeV2 Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HomeV2 (Preview)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned home interface (UI-first development)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useHomeV2,
                    onCheckedChange = onUpdateUseHomeV2
                )
            }
            
            HorizontalDivider()
            
            // PlaylistV2 Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PlaylistV2 (Preview)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned playlist interface (UI-first development)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.usePlaylistV2,
                    onCheckedChange = onUpdateUsePlaylistV2
                )
            }
            
            HorizontalDivider()
            
            // MiniPlayerV2 Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MiniPlayerV2 (Preview)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned global mini-player with recording-based visual identity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useMiniPlayerV2,
                    onCheckedChange = onUpdateUseMiniPlayerV2
                )
            }
            
            HorizontalDivider()
            
            // SplashV2 Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SplashV2 (Preview)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enable the redesigned splash screen with V2 database progress tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useSplashV2,
                    onCheckedChange = onUpdateUseSplashV2
                )
            }
            
            HorizontalDivider()
            
            // V2 Database Management
            OutlinedButton(
                onClick = onClearV2Database,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Clear V2 Database")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
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

@Composable
private fun UpdateSettingsCard(
    settings: AppSettings,
    updateStatus: com.deadarchive.core.model.UpdateStatus?,
    onCheckForUpdates: () -> Unit,
    onSetAutoUpdateEnabled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App Updates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Auto Update Check Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Auto Check for Updates",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Automatically check for app updates on startup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.autoUpdateCheckEnabled,
                    onCheckedChange = onSetAutoUpdateEnabled
                )
            }
            
            HorizontalDivider()
            
            // Manual Check Button and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Check for Updates",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    when {
                        updateStatus?.isUpdateAvailable == true -> {
                            Text(
                                text = "Update available: ${updateStatus.update?.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        updateStatus?.isUpdateAvailable == false -> {
                            Text(
                                text = "App is up to date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        settings.lastUpdateCheckTimestamp > 0 -> {
                            Text(
                                text = "Last checked: ${formatTimestamp(settings.lastUpdateCheckTimestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "Tap to check for updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Button(
                    onClick = onCheckForUpdates,
                    enabled = updateStatus?.isUpdateAvailable != true // Disable if already showing update
                ) {
                    Text(
                        text = if (updateStatus?.isUpdateAvailable == true) "Update Available" else "Check Now"
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
