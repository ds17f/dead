package com.deadarchive.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.component.IconResources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerV2Screen(
    recordingId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToPlaylist: (String?) -> Unit = {}
) {
    Log.d("PlayerV2Screen", "=== PLAYERV2 SCREEN LOADED === recordingId: $recordingId")
    
    // TODO: This is a placeholder implementation with mock data
    // We'll build out the real UI components as we discover what we need
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar - similar to current player
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PlayerV2 🚀",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "UI-First Development",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = IconResources.Navigation.KeyboardArrowDown(),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToQueue) {
                        Icon(
                            painter = IconResources.PlayerControls.Queue(),
                            contentDescription = "Queue"
                        )
                    }
                }
            )
            
            // Main content area - placeholder for now
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Placeholder album art
                    Card(
                        modifier = Modifier.size(200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = IconResources.PlayerControls.AlbumArt(),
                                contentDescription = "Album Art",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Mock track info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (recordingId != null) "Mock Track from $recordingId" else "Mock Track Title",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Mock Recording • May 8, 1977",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Barton Hall, Cornell University",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mock progress bar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Slider(
                            value = 0.3f, // Mock 30% progress
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2:30",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "8:15",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mock playback controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { Log.d("PlayerV2Screen", "Previous clicked") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = IconResources.PlayerControls.SkipPrevious(),
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { Log.d("PlayerV2Screen", "Play/Pause clicked") },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = IconResources.PlayerControls.Play(),
                                contentDescription = "Play",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { Log.d("PlayerV2Screen", "Next clicked") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = IconResources.PlayerControls.SkipNext(),
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Development info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚧 PlayerV2 Development",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "This is the UI-first PlayerV2 implementation.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "RecordingId: ${recordingId ?: "none"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Next: Build real UI components and discover domain models",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}