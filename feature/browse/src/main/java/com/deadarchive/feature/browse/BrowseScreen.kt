package com.deadarchive.feature.browse

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.deadarchive.core.design.component.IconResources
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording
import com.deadarchive.core.design.component.ExpandableConcertItem
import com.deadarchive.core.design.component.DownloadState
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.design.component.LibraryAction
import com.deadarchive.core.design.component.LibraryRemovalConfirmationDialog
import com.deadarchive.core.design.component.LibraryRemovalDialogConfig
import com.deadarchive.core.design.component.UpdateAvailableDialog
import com.deadarchive.core.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onNavigateToPlayer: (String) -> Unit, // recordingId
    onNavigateToRecording: (Recording) -> Unit = { recording ->
        // Navigate using recording ID
        onNavigateToPlayer(recording.identifier)
    },
    onNavigateToShow: (Show) -> Unit = { show ->
        // Navigate to best recording of this show with showId parameter
        show.bestRecording?.let { recording ->
            onNavigateToPlayer(recording.identifier)
        }
    },
    initialEra: String? = null,
    viewModel: BrowseViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val showConfirmationDialog by viewModel.showConfirmationDialog.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val currentUpdate by viewModel.currentUpdate.collectAsState()
    var showToRemove by remember { mutableStateOf<Show?>(null) }
    var removalDialogConfig by remember { mutableStateOf<LibraryRemovalDialogConfig?>(null) }
    
    // Handle era filtering
    LaunchedEffect(initialEra) {
        android.util.Log.d("BrowseScreen", "📱 LaunchedEffect triggered with initialEra: '$initialEra'")
        initialEra?.let { era ->
            android.util.Log.d("BrowseScreen", "📱 Calling viewModel.filterByEra with era: '$era'")
            viewModel.filterByEra(era)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = if (initialEra != null) "Top ${initialEra} Shows" else "Browse Shows",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search concerts...") },
            leadingIcon = {
                Icon(painter = IconResources.Navigation.Search(), contentDescription = "Search")
            },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        // Quick search buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickSearchButton(
                text = "Popular",
                onClick = { viewModel.loadPopularShows() },
                modifier = Modifier.weight(1f)
            )
            QuickSearchButton(
                text = "Recent",
                onClick = { viewModel.loadRecentShows() },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Content
        when (val state = uiState) {
            is BrowseUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search for Grateful Dead concerts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is BrowseUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BrowseUiState.Success -> {
                if (state.shows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No concerts found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.shows) { show ->
                            ExpandableConcertItem(
                                show = show,
                                settings = settings,
                                onShowClick = { clickedShow: Show ->
                                    // Navigate to show with showId parameter for recording preferences
                                    Log.d("BrowseScreen", "onShowClick: Show ${clickedShow.showId} has ${clickedShow.recordings.size} recordings")
                                    Log.d("BrowseScreen", "onShowClick: bestRecording = ${clickedShow.bestRecording?.identifier}")
                                    Log.d("BrowseScreen", "onShowClick: Navigating to show with showId for recording preferences")
                                    onNavigateToShow(clickedShow)
                                },
                                onRecordingClick = { recording: Recording ->
                                    Log.d("BrowseScreen", "onRecordingClick: Navigating to player for recording ${recording.identifier} - ${recording.title}")
                                    onNavigateToRecording(recording)
                                },
                                isInLibraryFlow = viewModel.getLibraryStatusFlow(show),
                                onLibraryAction = { action ->
                                    viewModel.handleLibraryAction(action, show)
                                },
                                onLibraryConfirmationNeeded = { config ->
                                    // Get actual download info and show dialog
                                    val info = viewModel.getLibraryRemovalInfo(config.show)
                                    removalDialogConfig = config.copy(
                                        hasDownloads = info.hasDownloads,
                                        downloadInfo = info.downloadInfo
                                    )
                                },
                                onDownloadClick = { recording: Recording ->
                                    Log.d("BrowseScreen", "Download requested for recording: ${recording.identifier}")
                                    viewModel.downloadRecording(recording)
                                },
                                getDownloadState = { recording: Recording ->
                                    viewModel.getDownloadState(recording)
                                },
                                onDownloadButtonClick = { show: Show ->
                                    Log.d("BrowseScreen", "Download button clicked for show: ${show.showId}")
                                    viewModel.handleDownloadButtonClick(show)
                                },
                                getShowDownloadState = { show: Show ->
                                    // Use the observed download states for real-time updates
                                    val bestRecording = show.bestRecording
                                    if (bestRecording != null) {
                                        downloadStates[bestRecording.identifier] ?: ShowDownloadState.NotDownloaded
                                    } else {
                                        ShowDownloadState.NotDownloaded
                                    }
                                }
                            )
                        }
                    }
                }
            }
            is BrowseUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error loading concerts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Add confirmation dialog for library removal
    removalDialogConfig?.let { config ->
        LibraryRemovalConfirmationDialog(
            config = config,
            onConfirm = { removeDownloads ->
                val action = if (removeDownloads) {
                    LibraryAction.REMOVE_WITH_DOWNLOADS
                } else {
                    LibraryAction.REMOVE_FROM_LIBRARY
                }
                viewModel.handleLibraryAction(action, config.show)
                removalDialogConfig = null
            },
            onDismiss = {
                removalDialogConfig = null
            }
        )
    }
    
    // Confirmation dialog for removing shows from library (old logic - can be removed later)
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
                        viewModel.toggleLibrary(show)
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
    
    // Confirmation dialog for removing downloads - using AlertDialog directly
    showConfirmationDialog?.let { show ->
        AlertDialog(
            onDismissRequest = { viewModel.hideConfirmationDialog() },
            title = {
                Text(
                    text = "Remove Download",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove the download for \"${show.displayDate} - ${show.displayVenue}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRemoveDownload() }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideConfirmationDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Update available dialog
    updateStatus?.let { status ->
        if (status.isUpdateAvailable) {
            currentUpdate?.let { update ->
                UpdateAvailableDialog(
                    update = update,
                    onDownload = {
                        // Clear state - user should go to Settings to download
                        viewModel.clearUpdateState()
                    },
                    onSkip = {
                        viewModel.skipUpdate()
                    },
                    onInstall = {
                        // Clear state - user should go to Settings to install
                        viewModel.clearUpdateState()
                    },
                    onDismiss = {
                        viewModel.clearUpdateState()
                    }
                )
            }
        }
    }
}

@Composable
fun QuickSearchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}