package com.deadarchive.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onBackClick: () -> Unit,
    onNavigateToRepositoryTest: () -> Unit,
    onNavigateToDatabaseTest: () -> Unit,
    onNavigateToNetworkTest: () -> Unit,
    onNavigateToMediaPlayerTest: () -> Unit,
    onNavigateToWorkManagerTest: () -> Unit,
    viewModel: DebugViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
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
                            Text(
                                text = uiState.exportStatus,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
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
                        onClick = onNavigateToRepositoryTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Repository Testing")
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToDatabaseTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Database Testing")
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToNetworkTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Network Testing")
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToMediaPlayerTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Media Player Testing")
                    }
                    
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
                    
                    Button(
                        onClick = { viewModel.debugDatabaseState() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Debug Database State")
                    }
                    
                    if (uiState.databaseDebugInfo.isNotEmpty()) {
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
}