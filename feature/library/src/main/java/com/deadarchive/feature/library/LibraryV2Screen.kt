package com.deadarchive.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.library.api.LibraryStats
import com.deadarchive.core.model.Show
import com.deadarchive.feature.library.formatBytes

/**
 * Library V2 Screen with stub testing UI.
 * 
 * This screen demonstrates the complete call chain from UI → ViewModel → Service
 * using minimal logging-only stubs. It includes a development banner and test buttons
 * to validate the stub integration before implementing real functionality.
 */
@Composable
fun LibraryV2Screen(
    viewModel: LibraryV2ViewModel = hiltViewModel(),
    onNavigateToShow: (String) -> Unit = {},
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Stub Development Banner
        StubDevelopmentBanner(
            modifier = Modifier.fillMaxWidth()
        )
        
        // Stats Display (will show 0s from stub)
        libraryStats?.let { stats ->
            StatsCard(
                stats = stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        // Main Content
        when (val state = uiState) {
            is LibraryV2UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading library via stubs...")
                    }
                }
            }
            
            is LibraryV2UiState.Success -> {
                if (state.shows.isEmpty()) {
                    // Empty state (expected with minimal stubs)
                    EmptyLibraryContent(
                        onTestStubs = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show list (won't happen with minimal stubs)
                    LibraryShowsList(
                        shows = state.shows,
                        viewModel = viewModel,
                        onNavigateToShow = onNavigateToShow,
                        onNavigateToPlayer = onNavigateToPlayer
                    )
                }
            }
            
            is LibraryV2UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StubDevelopmentBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Library V2 - Minimal Stub Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Services are logging-only stubs • Check logcat for calls",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    stats: LibraryStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Library Stats (From Stub)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Shows",
                    value = stats.totalShows.toString()
                )
                StatItem(
                    label = "Downloaded",
                    value = stats.totalDownloaded.toString()
                )
                StatItem(
                    label = "Storage",
                    value = formatBytes(stats.totalStorageUsed)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyLibraryContent(
    onTestStubs: LibraryV2ViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Library is Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Stubs return empty data. Test the integration by tapping buttons below.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test buttons to verify stub integration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onTestStubs.addToLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Add to Library (Check Logs)")
            }
            
            OutlinedButton(
                onClick = { onTestStubs.removeFromLibrary("test-show-1") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Remove from Library (Check Logs)")
            }
            
            OutlinedButton(
                onClick = { 
                    onTestStubs.downloadShow(
                        Show(
                            date = "1977-05-08",
                            venue = "Test Venue",
                            location = "Test City, NY",
                            recordings = emptyList(),
                            isInLibrary = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Download Show (Check Logs)")
            }
            
            FilledTonalButton(
                onClick = { onTestStubs.clearLibrary() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Clear Library (Check Logs)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "💡 All actions only log to console. No actual data changes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LibraryShowsList(
    shows: List<Show>,
    viewModel: LibraryV2ViewModel,
    onNavigateToShow: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shows) { show ->
            ShowListItem(
                show = show,
                onRemove = { viewModel.removeFromLibrary(show.showId) },
                onDownload = { viewModel.downloadShow(show) },
                onNavigateToShow = { onNavigateToShow(show.showId) },
                onNavigateToPlayer = { onNavigateToPlayer(show.showId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowListItem(
    show: Show,
    onRemove: () -> Unit,
    onDownload: () -> Unit,
    onNavigateToShow: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    Card(
        onClick = onNavigateToShow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = show.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${show.displayVenue}, ${show.displayLocation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onNavigateToPlayer) {
                    Text("Play")
                }
                TextButton(onClick = onDownload) {
                    Text("Download")
                }
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error Loading Library",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

