package com.deadarchive.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBackClick: () -> Unit,
    onNavigateToWorkManagerTest: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Testing") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Test Data Export Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Test Data Export",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Export real API data for integration testing",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.exportTestData()
                            }
                        },
                        enabled = !uiState.isExporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporting...")
                        } else {
                            Text("Export Test Data")
                        }
                    }
                    
                    if (uiState.exportStatus.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.exportSuccess) 
                                    MaterialTheme.colorScheme.secondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Export Results",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.exportStatus))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy to clipboard"
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.exportStatus,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            
            // Download Testing Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Download System Testing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Test the WorkManager-based download queue system",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.testSampleDownload()
                                }
                            },
                            enabled = !uiState.isDownloadTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isDownloadTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test Download")
                            }
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.testMultipleDownloads()
                                }
                            },
                            enabled = !uiState.isDownloadTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Queue")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.checkDownloadStatus()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Check Status")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.listAllDownloads()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("List Downloads")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.verifyDownloadedFiles()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50) // Green for verification
                            )
                        ) {
                            Text("Verify Files")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.troubleshootDownloads()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722) // Orange for troubleshooting
                            )
                        ) {
                            Text("Troubleshoot")
                        }
                    }
                    
                    if (uiState.downloadTestStatus.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.downloadTestSuccess) 
                                    MaterialTheme.colorScheme.tertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Download Test Results",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.downloadTestStatus))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy to clipboard"
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.downloadTestStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
            
            // Test Screens Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Test Screens",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedButton(
                        onClick = onNavigateToWorkManagerTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("WorkManager Testing")
                    }
                }
            }
            
            // App Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "App Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text("Cached Recordings: ${uiState.cachedRecordingCount}")
                    Text("Last Sync: ${uiState.lastSyncTime}")
                    Text("Database Size: ${uiState.databaseSize}")
                    Text("Setlists: ${uiState.setlistCount}")
                    Text("Songs: ${uiState.songCount}")
                    Text("Venues: ${uiState.venueCount}")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.debugDatabaseState() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Debug Database")
                        }
                        
                        Button(
                            onClick = { 
                                scope.launch {
                                    viewModel.debugShowRecordingRelationships()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Debug Relations")
                        }
                    }
                    
                    // Song name debug button
                    Button(
                        onClick = { 
                            scope.launch {
                                viewModel.debugSongNamePopulation()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("🎵 Debug Song Names")
                    }
                    
                    // Detailed song name debug button
                    Button(
                        onClick = { 
                            scope.launch {
                                viewModel.debugSongNamePopulationDetailed()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🔍 Detailed Song Debug")
                    }
                    
                    if (uiState.databaseDebugInfo.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Database Debug Info",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.databaseDebugInfo))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy to clipboard"
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.databaseDebugInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Setlist Data Testing Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Setlist Data Testing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Test the setlist integration and explore setlist data",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.testSetlistData()
                                }
                            },
                            enabled = !uiState.isLoadingSetlistData,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isLoadingSetlistData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Test Setlist Data")
                            }
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.refreshSetlistData()
                                }
                            },
                            enabled = !uiState.isLoadingSetlistData,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refresh Data")
                        }
                    }
                    
                    // Song ID Resolution button
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.resolveSongIds()
                            }
                        },
                        enabled = !uiState.isLoadingSetlistData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("🔧 Resolve Song IDs")
                    }
                    
                    // Populate Show Song Names button
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.populateShowSongNames()
                            }
                        },
                        enabled = !uiState.isLoadingSetlistData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("🎵 Fix Main Browse Search")
                    }
                    
                    
                    // Search functionality
                    var searchQuery by remember { mutableStateOf("1977") }
                    var searchType by remember { mutableStateOf("Date/Venue") }
                    val searchTypes = listOf("Date/Venue", "Song", "Statistics")
                    
                    // Search type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchTypes.forEach { type ->
                            FilterChip(
                                onClick = { searchType = type },
                                label = { Text(type) },
                                selected = searchType == type,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(
                                when (searchType) {
                                    "Song" -> "Song name (e.g., 'Dark Star')"
                                    "Statistics" -> "Song for stats (e.g., 'Ripple')"
                                    else -> "Date/venue (e.g., '1977')"
                                }
                            ) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    when (searchType) {
                                        "Song" -> viewModel.searchSetlistsBySong(searchQuery)
                                        "Statistics" -> viewModel.getSongStatistics(searchQuery)
                                        else -> viewModel.searchSetlistsByDate(searchQuery)
                                    }
                                }
                            },
                            enabled = !uiState.isLoadingSetlistData && searchQuery.isNotBlank()
                        ) {
                            Text("Search")
                        }
                        
                        if (searchType == "Song") {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.debugSongSearch(searchQuery)
                                    }
                                },
                                enabled = !uiState.isLoadingSetlistData && searchQuery.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Debug", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    // Quick search buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val quickSearches = when (searchType) {
                            "Song" -> listOf("Dark Star", "Ripple", "Fire", "Scarlet")
                            "Statistics" -> listOf("Playing in the Band", "Eyes of the World", "China Cat Sunflower")
                            else -> listOf("1977", "1995", "Winterland")
                        }
                        
                        quickSearches.forEach { query ->
                            AssistChip(
                                onClick = { 
                                    searchQuery = query
                                    scope.launch {
                                        when (searchType) {
                                            "Song" -> viewModel.searchSetlistsBySong(query)
                                            "Statistics" -> viewModel.getSongStatistics(query)
                                            else -> viewModel.searchSetlistsByDate(query)
                                        }
                                    }
                                },
                                label = { Text(query, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    if (uiState.setlistTestStatus.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.setlistTestSuccess) 
                                    MaterialTheme.colorScheme.secondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Setlist Test Results",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.setlistTestStatus))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy to clipboard"
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.setlistTestStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                        .heightIn(max = 300.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Database Management Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Database Management",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Text(
                        text = "⚠️ Advanced database operations. Use with caution!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.wipeDatabase()
                            }
                        },
                        enabled = !uiState.isWipingDatabase,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (uiState.isWipingDatabase) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wiping Database...")
                        } else {
                            Text("Wipe Database (Keep Library)")
                        }
                    }
                    
                    // Progress display for database wipe
                    if (uiState.wipeProgress.isInProgress) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Database Wipe Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Progress bar
                                if (uiState.wipeProgress.totalItems > 0) {
                                    LinearProgressIndicator(
                                        progress = { uiState.wipeProgress.progressPercentage / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${uiState.wipeProgress.processedItems} / ${uiState.wipeProgress.totalItems}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${uiState.wipeProgress.progressPercentage.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // Current phase and item
                                Text(
                                    text = "Phase: ${uiState.wipeProgress.phase.name.replace('_', ' ')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (uiState.wipeProgress.currentItem.isNotEmpty()) {
                                    Text(
                                        text = uiState.wipeProgress.currentItem,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }
                                
                                // Error display
                                if (uiState.wipeProgress.error != null) {
                                    Text(
                                        text = "Error: ${uiState.wipeProgress.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (uiState.databaseWipeStatus.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.databaseWipeSuccess) 
                                    MaterialTheme.colorScheme.secondaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Database Wipe Status",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.databaseWipeStatus))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy to clipboard"
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.databaseWipeStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}