package com.deadarchive.feature.playlist

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.core.design.component.DebugActivator
import com.deadarchive.core.design.component.DebugBottomSheet
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.feature.playlist.debug.collectPlaylistV2DebugData

/**
 * PlaylistV2Screen - Minimal V2 playlist interface
 * 
 * Following V2 architecture patterns established by PlayerV2 and SearchV2.
 * This is the foundation for the new playlist interface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistV2Screen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    onNavigateToShow: (String, String) -> Unit = { _, _ -> },
    recordingId: String? = null,
    showId: String? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    Log.d("PlaylistV2Screen", "=== PLAYLISTV2 SCREEN LOADED === recordingId: $recordingId, showId: $showId")
    
    val settings by settingsViewModel.settings.collectAsState()
    
    // Debug panel state - only when debug mode is enabled
    var showDebugPanel by remember { mutableStateOf(false) }
    val debugData = if (settings.showDebugInfo) {
        collectPlaylistV2DebugData(recordingId = recordingId, showId = showId)
    } else {
        null
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        
        // Main content
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "PlaylistV2 (Preview)",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                
                // Placeholder content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎵",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PlaylistV2",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New playlist interface coming soon!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (recordingId != null || showId != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Recording ID: ${recordingId ?: "None"}\nShow ID: ${showId ?: "None"}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons (placeholders)
                OutlinedButton(
                    onClick = onNavigateToPlayer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Player")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to V1 Playlist")
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