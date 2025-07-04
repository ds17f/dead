package com.deadarchive.feature.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.deadarchive.core.design.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.IconResources
import com.deadarchive.core.model.Show
import com.deadarchive.core.model.Recording

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertListScreen(
    onBackClick: () -> Unit,
    onRecordingSelected: (Recording) -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConcertListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Concerts",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* TODO: Implement search */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Search"
                    )
                }
            }
        )
        
        // Content based on state
        when (val state = uiState) {
            is ConcertListUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is ConcertListUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = IconResources.Status.Error(),
                                contentDescription = "Error",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            
                            Text(
                                text = "Error loading concerts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Button(
                                onClick = { viewModel.retry() }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            
            is ConcertListUiState.Success -> {
                if (state.shows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = IconResources.Status.Error(),
                                contentDescription = "No concerts",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "No concerts found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary header
                        item {
                            ConcertListSummary(
                                totalConcerts = state.shows.size,
                                totalRecordings = state.shows.sumOf { it.recordingCount }
                            )
                        }
                        
                        // Concert list
                        items(
                            items = state.shows,
                            key = { show -> show.showId }
                        ) { show ->
                            ExpandableConcertItem(
                                show = show,
                                onShowClick = { selectedShow ->
                                    // Navigate to playlist with best recording
                                    selectedShow.bestRecording?.let { recording ->
                                        onRecordingSelected(recording)
                                        onNavigateToPlayer()
                                    }
                                },
                                onRecordingClick = { recording ->
                                    onRecordingSelected(recording)
                                    onNavigateToPlayer()
                                },
                                onLibraryClick = { showToToggle ->
                                    viewModel.toggleLibrary(showToToggle)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConcertListSummary(
    totalConcerts: Int,
    totalRecordings: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                label = "Concerts",
                value = totalConcerts.toString()
            )
            
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
            )
            
            SummaryItem(
                label = "Recordings",
                value = totalRecordings.toString()
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}