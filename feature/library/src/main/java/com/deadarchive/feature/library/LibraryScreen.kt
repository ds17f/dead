package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.deadarchive.core.design.R
import androidx.compose.material3.*
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
import com.deadarchive.core.settings.model.AppSettings
import com.deadarchive.core.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToRecording: (Recording) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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
                                    // Navigate to best recording of this show
                                    clickedShow.bestRecording?.let { recording ->
                                        onNavigateToRecording(recording)
                                    }
                                },
                                onRecordingClick = { recording: Recording ->
                                    onNavigateToRecording(recording)
                                },
                                onLibraryClick = { clickedShow: Show ->
                                    showToRemove = clickedShow
                                }
                            )
                        }
                    }
                }
            }
        }
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

