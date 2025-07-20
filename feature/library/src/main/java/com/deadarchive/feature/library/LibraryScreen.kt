package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.deadarchive.core.design.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import com.deadarchive.core.design.component.IconResources
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.design.component.ExpandableConcertItem
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.design.component.ConfirmationDialog
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToRecording: (Recording) -> Unit,
    onNavigateToShow: (Show) -> Unit = { show ->
        // Default fallback - navigate to best recording
        show.bestRecording?.let { recording ->
            onNavigateToRecording(recording)
        }
    },
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val latestBackupInfo by settingsViewModel.latestBackupInfo.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val showConfirmationDialog by viewModel.showConfirmationDialog.collectAsState()
    var showToRemove by remember { mutableStateOf<Show?>(null) }
    
    // Bottom sheet and confirmation states
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Library") },
            actions = {
                IconButton(onClick = { showBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Library options"
                    )
                }
            }
        )
        
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LibraryUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = "Error",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = "Error loading library",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            is LibraryUiState.Success -> {
                if (state.shows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_library_add),
                                contentDescription = "Empty library",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "Your library is empty",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Add shows to your library by tapping the library button on any show or recording.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Show restore information and button
                            val backupInfo = latestBackupInfo
                            val hasValidBackup = backupInfo != null && backupInfo.showCount > 0
                            val isLoading = settingsUiState.isLoading
                            
                            if (backupInfo != null) {
                                val formattedDate = remember(backupInfo.createdAt) {
                                    java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault())
                                        .format(java.util.Date(backupInfo.createdAt))
                                }
                                
                                if (hasValidBackup) {
                                    Text(
                                        text = "You have a backup from $formattedDate with ${backupInfo.showCount} shows",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                    )
                                } else {
                                    Text(
                                        text = "You have a backup from $formattedDate, but it contains no shows",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            // Always show the restore button
                            Button(
                                onClick = { 
                                    showRestoreConfirmation = true
                                },
                                enabled = hasValidBackup && !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        painter = IconResources.DataManagement.Restore(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Restore Library")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Library Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${state.shows.size} shows in your library",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        items(
                            items = state.shows,
                            key = { show -> show.showId }
                        ) { show ->
                            ExpandableConcertItem(
                                show = show,
                                settings = settings,
                                onShowClick = { clickedShow: Show ->
                                    // Navigate to show (playlist)
                                    onNavigateToShow(clickedShow)
                                },
                                onRecordingClick = { recording: Recording ->
                                    onNavigateToRecording(recording)
                                },
                                onLibraryClick = { clickedShow: Show ->
                                    showToRemove = clickedShow
                                },
                                onDownloadClick = { recording: Recording ->
                                    viewModel.downloadRecording(recording)
                                },
                                getDownloadState = { recording: Recording ->
                                    viewModel.getDownloadState(recording)
                                },
                                onShowDownloadClick = { show: Show ->
                                    viewModel.downloadShow(show)
                                },
                                onCancelDownloadClick = { show: Show ->
                                    viewModel.cancelShowDownloads(show)
                                },
                                onRemoveDownloadClick = { show: Show ->
                                    viewModel.showRemoveDownloadConfirmation(show)
                                },
                                getShowDownloadState = { show: Show ->
                                    viewModel.getShowDownloadState(show)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Bottom sheet for library options
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            LibraryOptionsBottomSheet(
                hasLibraryItems = when (val state = uiState) {
                    is LibraryUiState.Success -> state.shows.isNotEmpty()
                    else -> false
                },
                hasBackup = latestBackupInfo != null,
                isLoading = settingsUiState.isLoading,
                onBackupClick = {
                    showBottomSheet = false
                    showBackupConfirmation = true
                },
                onClearClick = {
                    showBottomSheet = false
                    showClearConfirmation = true
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }
    
    // Confirmation dialog for backup
    if (showBackupConfirmation) {
        AlertDialog(
            onDismissRequest = { showBackupConfirmation = false },
            title = { Text("Backup Library") },
            text = { 
                Text("This will create a backup of your current library and save it to app storage. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupConfirmation = false
                        settingsViewModel.backupLibrary()
                    }
                ) {
                    Text("Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Confirmation dialog for clear library
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Library") },
            text = { 
                Text("This will remove all shows from your library. This action cannot be undone. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        viewModel.clearLibrary()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Confirmation dialog for restore
    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text("Restore Library") },
            text = { 
                val backupInfo = latestBackupInfo
                val message = if (backupInfo != null) {
                    val formattedDate = remember(backupInfo.createdAt) {
                        java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(backupInfo.createdAt))
                    }
                    "This will restore your library from the backup created on $formattedDate with ${backupInfo.showCount} shows. Continue?"
                } else {
                    "This will restore your library from the most recent backup. Continue?"
                }
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmation = false
                        settingsViewModel.restoreLibrary()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Confirmation dialog for removing downloads
    showConfirmationDialog?.let { show ->
        ConfirmationDialog(
            title = "Remove Download",
            message = "Are you sure you want to remove the download for \"${show.displayDate} - ${show.displayVenue}\"?",
            onConfirm = {
                viewModel.confirmRemoveDownload()
            },
            onCancel = {
                viewModel.hideConfirmationDialog()
            }
        )
    }
    
    // Confirmation dialog for removing shows from library
    showToRemove?.let { show ->
        AlertDialog(
            onDismissRequest = { showToRemove = null },
            title = { Text("Remove from Library") },
            text = { 
                Text("Are you sure you want to remove \"${show.displayDate} - ${show.displayVenue}\" from your library?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeShowFromLibrary(show.showId)
                        showToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showToRemove = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LibraryOptionsBottomSheet(
    hasLibraryItems: Boolean,
    hasBackup: Boolean,
    isLoading: Boolean,
    onBackupClick: () -> Unit,
    onClearClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Library Options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Backup option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasLibraryItems && !isLoading) { 
                    onBackupClick()
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = IconResources.DataManagement.Backup(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (hasLibraryItems && !isLoading) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Backup Library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (hasLibraryItems && !isLoading) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = if (hasLibraryItems) {
                        "Save your library to app storage"
                    } else {
                        "No shows to backup"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider()
        
        // Clear option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasLibraryItems && !isLoading) { 
                    onClearClick()
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (hasLibraryItems && !isLoading) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clear Library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (hasLibraryItems && !isLoading) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = if (hasLibraryItems) {
                        "Remove all shows from library"
                    } else {
                        "Library is already empty"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Bottom padding for gesture area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

