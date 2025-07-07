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
                    
                    Button(
                        onClick = { viewModel.debugDatabaseState() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Debug Database State")
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
        }
    }
}