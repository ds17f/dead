package com.deadarchive.feature.playlist

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.playlist.components.PlaylistV2Header
import com.deadarchive.feature.playlist.components.PlaylistV2AlbumArt
import com.deadarchive.feature.playlist.components.PlaylistV2ShowInfo
import com.deadarchive.feature.playlist.debug.collectPlaylistV2DebugData

/**
 * PlaylistV2Screen - Clean V2 playlist interface
 * 
 * Recreation of PlaylistV1 using V2 architecture patterns with
 * focused components and clean service integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistV2Screen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToShow: (String, String) -> Unit = { _, _ -> },
    recordingId: String? = null,
    showId: String? = null,
    viewModel: PlaylistV2ViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    Log.d("PlaylistV2Screen", "=== PLAYLISTV2 SCREEN LOADED === recordingId: $recordingId, showId: $showId")
    
    val uiState by viewModel.uiState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // Load show data when screen opens
    LaunchedEffect(showId) {
        viewModel.loadShow(showId)
    }
    
    // Debug panel state - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectPlaylistV2DebugData(recordingId = recordingId, showId = showId)
    } else {
        null
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Back arrow overlay at the top
        PlaylistV2Header(
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        // Main content - Spotify-style LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Error: ${uiState.error}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { viewModel.loadShow(showId) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    // Album cover image - fixed size at top
                    item {
                        PlaylistV2AlbumArt()
                    }
                    
                    // Show info section - with navigation buttons
                    uiState.showData?.let { showData ->
                        item {
                            PlaylistV2ShowInfo(
                                showData = showData,
                                isNavigationLoading = uiState.isNavigationLoading,
                                onPreviousShow = viewModel::navigateToPreviousShow,
                                onNextShow = viewModel::navigateToNextShow
                            )
                        }
                    }
                    
                    // Placeholder for more components
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "More components coming soon...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tracks: ${uiState.trackData.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Debug activator button (bottom-right when debug enabled)
        if (settings.showDebugInfo && debugData != null) {
            DebugActivator(
                isVisible = true,
                onClick = { showDebugPanel = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
    
    // Debug bottom sheet
    if (showDebugPanel && debugData != null) {
        DebugBottomSheet(
            debugData = debugData,
            isVisible = showDebugPanel,
            onDismiss = { showDebugPanel = false }
        )
    }
}