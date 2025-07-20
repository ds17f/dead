package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.deadarchive.core.design.R
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
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Library") }
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
                                    settingsViewModel.restoreLibrary()
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

