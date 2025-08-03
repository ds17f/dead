package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.deadarchive.core.design.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import com.deadarchive.core.design.component.IconResources
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.model.LibraryItem
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.data.download.EnrichedDownloadState
import com.deadarchive.core.design.component.ExpandableConcertItem
import com.deadarchive.core.design.component.ShowDownloadState
import com.deadarchive.core.design.component.ConfirmationDialog
import com.deadarchive.core.design.component.LibraryAction
import com.deadarchive.core.design.component.LibraryRemovalConfirmationDialog
import com.deadarchive.core.design.component.LibraryRemovalDialogConfig
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel

enum class DownloadSortOption(val displayName: String) {
    SHOW("Show"),
    TRACK("Track"),
    STATUS("Status"),
    PROGRESS("Progress")
}

enum class DownloadTab(val displayName: String) {
    ACTIVE("Active Downloads"),
    STORAGE("Storage Browser")
}

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
    val sortOption by viewModel.sortOption.collectAsState()
    val decadeFilter by viewModel.decadeFilter.collectAsState()
    var showToRemove by remember { mutableStateOf<Show?>(null) }
    var removalDialogConfig by remember { mutableStateOf<LibraryRemovalDialogConfig?>(null) }
    
    // LazyListState for scrolling to top when sort changes
    val listState = rememberLazyListState()
    
    // Scroll to top when sort option changes
    LaunchedEffect(sortOption) {
        listState.animateScrollToItem(0)
    }
    
    // Bottom sheet and confirmation states
    var showBottomSheet by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showDownloadsBottomSheet by remember { mutableStateOf(false) }
    var showBackupConfirmation by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        LibraryTopBar(
            onOptionsClick = { showBottomSheet = true }
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Always show decade filter buttons when library has items
                    if (state.libraryItems.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(DecadeFilter.values()) { filter ->
                                FilterChip(
                                    selected = decadeFilter == filter,
                                    onClick = { viewModel.setDecadeFilter(filter) },
                                    label = { Text(filter.displayName) }
                                )
                            }
                        }
                    }
                    
                    if (state.libraryItems.isEmpty()) {
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
                        // Sort options bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showSortBottomSheet = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_swap_vert),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        when (sortOption) {
                                            LibrarySortOption.DATE_ASCENDING, LibrarySortOption.DATE_DESCENDING -> "Show Date"
                                            LibrarySortOption.ADDED_ASCENDING, LibrarySortOption.ADDED_DESCENDING -> "Added"
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painter = painterResource(
                                            when (sortOption) {
                                                LibrarySortOption.DATE_ASCENDING, LibrarySortOption.ADDED_ASCENDING -> R.drawable.ic_keyboard_arrow_up
                                                LibrarySortOption.DATE_DESCENDING, LibrarySortOption.ADDED_DESCENDING -> R.drawable.ic_keyboard_arrow_down
                                            }
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider()
                        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.shows.isEmpty()) {
                                // No results state - shows exist in library but filtered out
                                item {
                                    LibraryEmptyState(decadeFilter = decadeFilter)
                                }
                            } else {
                                LibraryItemsList(
                                    shows = state.shows,
                                    settings = settings,
                                    viewModel = viewModel,
                                    onNavigateToShow = onNavigateToShow,
                                    onNavigateToRecording = onNavigateToRecording,
                                    onLibraryConfirmationNeeded = { config ->
                                        // Get actual download info and show dialog
                                        val info = viewModel.getLibraryRemovalInfo(config.show)
                                        removalDialogConfig = config.copy(
                                            hasDownloads = info.hasDownloads,
                                            downloadInfo = info.downloadInfo
                                        )
                                    }
                                )
                            }
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
                onDownloadsClick = {
                    showBottomSheet = false
                    showDownloadsBottomSheet = true
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
    
    // Sort options bottom sheet
    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false }
        ) {
            SortOptionsBottomSheet(
                currentSortOption = sortOption,
                onSortOptionSelected = { option ->
                    viewModel.setSortOption(option)
                    showSortBottomSheet = false
                }
            )
        }
    }
    
    // Downloads panel bottom sheet
    if (showDownloadsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadsBottomSheet = false }
        ) {
            DownloadsPanel(
                downloadStates = downloadStates,
                settings = settings,
                settingsViewModel = settingsViewModel,
                onDismiss = { showDownloadsBottomSheet = false },
                onNavigateToShow = onNavigateToShow,
                libraryViewModel = viewModel
            )
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
}

@Composable
private fun SortOptionsBottomSheet(
    currentSortOption: LibrarySortOption,
    onSortOptionSelected: (LibrarySortOption) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Sort library by",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LibrarySortOption.values().forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortOptionSelected(option) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentSortOption == option,
                    onClick = { onSortOptionSelected(option) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        // Bottom padding for gesture area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LibraryOptionsBottomSheet(
    hasLibraryItems: Boolean,
    hasBackup: Boolean,
    isLoading: Boolean,
    onBackupClick: () -> Unit,
    onClearClick: () -> Unit,
    onDownloadsClick: () -> Unit,
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
        
        // Downloads option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDownloadsClick() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_file_download),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Manage Downloads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View and manage downloaded shows",
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

@Composable
private fun DownloadsPanel(
    downloadStates: Map<String, ShowDownloadState>,
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onNavigateToShow: (Show) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val allDownloads by libraryViewModel.allDownloads.collectAsState(initial = emptyList())
    var enrichedDownloads by remember { mutableStateOf<List<EnrichedDownloadState>>(emptyList()) }
    
    // Load enriched downloads when regular downloads change
    LaunchedEffect(allDownloads) {
        enrichedDownloads = libraryViewModel.getEnrichedDownloads()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Downloads summary
        val activeDownloadCount = downloadStates.values.count { it is ShowDownloadState.Downloading }
        val completedDownloadCount = downloadStates.values.count { it is ShowDownloadState.Downloaded }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Downloads:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeDownloadCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completed Downloads:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedDownloadCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Download Queue Management Section
        HorizontalDivider()
        
        // Tab-like interface for active downloads vs storage browser
        var selectedTab by remember { mutableStateOf(DownloadTab.ACTIVE) }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            DownloadTab.values().forEach { tab ->
                TextButton(
                    onClick = { selectedTab = tab },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selectedTab == tab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = when (tab) {
                            DownloadTab.ACTIVE -> "Active Downloads"
                            DownloadTab.STORAGE -> "Storage Browser"
                        },
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        // Active downloads only (exclude completed) - use stable key based on content hash
        val activeDownloads = remember(enrichedDownloads.map { "${it.downloadState.id}-${it.downloadState.status}" }.hashCode()) {
            enrichedDownloads.filter { 
                it.downloadState.status != DownloadStatus.COMPLETED
            }.sortedWith(compareBy<EnrichedDownloadState> {
                when (it.downloadState.status) {
                    DownloadStatus.DOWNLOADING -> 0
                    DownloadStatus.FAILED -> 1
                    DownloadStatus.PAUSED -> 2
                    DownloadStatus.QUEUED -> 3
                    DownloadStatus.CANCELLED -> 4
                    else -> 5
                }
            }.thenBy { it.displayShowName })
        }
        
        // Completed downloads for storage browser - use stable key based on completed items
        val completedDownloads = remember(enrichedDownloads.count { it.downloadState.status == DownloadStatus.COMPLETED }) {
            enrichedDownloads.filter { 
                it.downloadState.status == DownloadStatus.COMPLETED
            }.groupBy { it.show?.showId ?: it.downloadState.recordingId }
        }
        
        when (selectedTab) {
            DownloadTab.ACTIVE -> {
                ActiveDownloadsView(
                    activeDownloads = activeDownloads,
                    libraryViewModel = libraryViewModel,
                    onNavigateToShow = onNavigateToShow
                )
            }
            DownloadTab.STORAGE -> {
                StorageBrowserView(
                    completedDownloads = completedDownloads,
                    onNavigateToShow = onNavigateToShow
                )
            }
        }
        
        HorizontalDivider()
        
        // Download Settings Section
        Text(
            text = "Download Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        onCheckedChange = settingsViewModel::updateDownloadWifiOnly
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
                                    settingsViewModel.updateDeletionGracePeriod(settings.deletionGracePeriodDays - 1)
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
                                settingsViewModel.updateDeletionGracePeriod(settings.deletionGracePeriodDays + 1)
                            }
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
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
                                    settingsViewModel.updateLowStorageThreshold(settings.lowStorageThresholdMB - 100L)
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
                                settingsViewModel.updateLowStorageThreshold(settings.lowStorageThresholdMB + 100L)
                            }
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom padding for gesture area
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActiveDownloadsView(
    activeDownloads: List<EnrichedDownloadState>,
    libraryViewModel: LibraryViewModel,
    onNavigateToShow: (Show) -> Unit
) {
    if (activeDownloads.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = activeDownloads,
                    key = { it.downloadState.id }
                ) { enrichedDownload ->
                    ActiveDownloadItem(
                        enrichedDownload = enrichedDownload,
                        onCancel = { 
                            when (enrichedDownload.downloadState.status) {
                                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                                    // Remove failed/cancelled downloads from the system
                                    libraryViewModel.removeDownload(enrichedDownload.downloadState.id)
                                }
                                else -> {
                                    // Cancel active downloads
                                    libraryViewModel.cancelDownload(enrichedDownload.downloadState.id)
                                }
                            }
                        },
                        onRetry = { libraryViewModel.retryDownload(enrichedDownload.downloadState.id) },
                        onForceStart = { libraryViewModel.forceDownload(enrichedDownload.downloadState.id) },
                        onPause = { libraryViewModel.pauseDownload(enrichedDownload.downloadState.id) },
                        onResume = { libraryViewModel.resumeDownload(enrichedDownload.downloadState.id) },
                        onNavigateToShow = {
                            enrichedDownload.show?.let { show ->
                                onNavigateToShow(show)
                            }
                        }
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No active downloads",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Download shows from the library or browse screens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageBrowserView(
    completedDownloads: Map<String, List<EnrichedDownloadState>>,
    onNavigateToShow: (Show) -> Unit
) {
    if (completedDownloads.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = completedDownloads.toList(),
                    key = { it.first }
                ) { (showId, downloads) ->
                    val firstDownload = downloads.first()
                    StoredShowItem(
                        showName = firstDownload.displayShowName,
                        trackCount = downloads.size,
                        totalSize = downloads.sumOf { it.downloadState.totalBytes },
                        recordingId = firstDownload.downloadState.recordingId,
                        downloads = downloads,
                        onNavigateToShow = {
                            firstDownload.show?.let { show ->
                                onNavigateToShow(show)
                            }
                        },
                        onExport = { 
                            // TODO: Implement export to device storage
                            // This would copy files to Downloads folder or Music folder
                        },
                        onViewFiles = {
                            // TODO: Implement file browser view
                            // This would show a dialog with all track files for this show
                        }
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No downloaded shows",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Complete some downloads to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    onOptionsClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text("Library")
            }
        },
        windowInsets = WindowInsets(0.dp),
        actions = {
            IconButton(onClick = onOptionsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Library options"
                )
            }
        }
    )
}

@Composable
private fun LibraryEmptyState(
    decadeFilter: DecadeFilter
) {
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
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "No results",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No shows found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            val filterText = when (decadeFilter) {
                DecadeFilter.ALL -> "Try adjusting your filters"
                else -> "No shows found for the ${decadeFilter.displayName}. Try a different decade or clear filters."
            }
            
            Text(
                text = filterText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun LazyListScope.LibraryItemsList(
    shows: List<Show>,
    settings: AppSettings,
    viewModel: LibraryViewModel,
    onNavigateToShow: (Show) -> Unit,
    onNavigateToRecording: (Recording) -> Unit,
    onLibraryConfirmationNeeded: (LibraryRemovalDialogConfig) -> Unit
) {
    items(
        items = shows,
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
            isInLibraryFlow = viewModel.getLibraryStatusFlow(show),
            onLibraryAction = { action ->
                viewModel.handleLibraryAction(action, show)
            },
            onLibraryConfirmationNeeded = { config ->
                // Get actual download info and show dialog
                val info = viewModel.getLibraryRemovalInfo(config.show)
                onLibraryConfirmationNeeded(config.copy(
                    hasDownloads = info.hasDownloads,
                    downloadInfo = info.downloadInfo
                ))
            },
            alwaysConfirmLibraryRemoval = true, // Always confirm in library since removal is jarring
            onDownloadClick = { recording: Recording ->
                viewModel.downloadRecording(recording)
            },
            getDownloadState = { recording: Recording ->
                viewModel.getDownloadState(recording)
            },
            onDownloadButtonClick = { show: Show ->
                viewModel.handleDownloadButtonClick(show)
            },
            getShowDownloadState = { show: Show ->
                viewModel.getShowDownloadState(show)
            }
        )
    }
}

