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
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    var showToRemove by remember { mutableStateOf<Show?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Browse Concerts",
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
                                onShowClick = { clickedShow: Show ->
                                    // Navigate to best recording of this show
                                    Log.d("BrowseScreen", "onShowClick: Show ${clickedShow.showId} has ${clickedShow.recordings.size} recordings")
                                    Log.d("BrowseScreen", "onShowClick: bestRecording = ${clickedShow.bestRecording?.identifier}")
                                    clickedShow.bestRecording?.let { recording ->
                                        Log.d("BrowseScreen", "onShowClick: Navigating to best recording: ${recording.identifier}")
                                        onNavigateToRecording(recording)
                                    } ?: run {
                                        Log.w("BrowseScreen", "onShowClick: No best recording found for show ${clickedShow.showId}")
                                    }
                                },
                                onRecordingClick = { recording: Recording ->
                                    Log.d("BrowseScreen", "onRecordingClick: Navigating to player for recording ${recording.identifier} - ${recording.title}")
                                    onNavigateToRecording(recording)
                                },
                                onLibraryClick = { clickedShow: Show ->
                                    if (clickedShow.isInLibrary) {
                                        // Show confirmation for removal
                                        showToRemove = clickedShow
                                    } else {
                                        // Add to library immediately
                                        viewModel.toggleLibrary(clickedShow)
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