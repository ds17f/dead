package com.deadarchive.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.deadarchive.core.design.component.DebugPanel
import com.deadarchive.core.design.component.DebugText
import com.deadarchive.core.design.component.DebugDivider
import com.deadarchive.core.design.component.DebugMultilineText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkManagerTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkManagerTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WorkManager Testing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshWorkManagerStatus() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isInitialized) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isInitialized) "✅ WorkManager Ready" else "❌ WorkManager Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action Buttons Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.testWorkManagerInitialization() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Test Initialization")
                }
                
                OutlinedButton(
                    onClick = { 
                        copyToClipboard(
                            context,
                            "WorkManager Status",
                            viewModel.getDetailedStatusReport()
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Report")
                }
            }
            
            // Action Buttons Row 2 - Download Testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.testDownloadWorker() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Test Download Worker")
                }
                
                OutlinedButton(
                    onClick = { viewModel.cancelAllWork() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel All Work")
                }
            }
            
            // WorkManager Configuration Debug Panel
            DebugPanel(
                title = "Configuration",
                isVisible = true,
                initiallyExpanded = true
            ) {
                DebugText("Max Scheduler Limit", "20")
                DebugText("Minimum Logging Level", "INFO")
                DebugText("Worker Factory", "HiltWorkerFactory")
                DebugDivider()
                DebugText("Initialization Status", if (uiState.isInitialized) "✅ Ready" else "❌ Failed")
                DebugText("Last Test", uiState.lastTestTime)
            }
            
            // Work Queue Status Debug Panel
            DebugPanel(
                title = "Work Queue Status",
                isVisible = true,
                initiallyExpanded = true
            ) {
                DebugText("Total Work Items", uiState.totalWorkItems.toString())
                DebugText("Enqueued", uiState.enqueuedWork.toString())
                DebugText("Running", uiState.runningWork.toString())
                DebugText("Succeeded", uiState.succeededWork.toString())
                DebugText("Failed", uiState.failedWork.toString())
                DebugText("Cancelled", uiState.cancelledWork.toString())
                DebugText("Blocked", uiState.blockedWork.toString())
            }
            
            // Work Items Detail Debug Panel (if any work exists)
            if (uiState.workDetails.isNotEmpty()) {
                DebugPanel(
                    title = "Work Items Detail",
                    isVisible = true,
                    initiallyExpanded = true
                ) {
                    uiState.workDetails.forEach { workDetail ->
                        DebugMultilineText(
                            label = "Work ID ${workDetail.id.take(8)}",
                            value = "State: ${workDetail.state}\nTags: ${workDetail.tags.joinToString()}\nAttempts: ${workDetail.runAttemptCount}"
                        )
                        DebugDivider()
                    }
                }
            }
            
            // Error Details (if any)
            if (uiState.errorMessage.isNotEmpty()) {
                DebugPanel(
                    title = "Error Details",
                    isVisible = true,
                    initiallyExpanded = true
                ) {
                    DebugMultilineText(
                        label = "Error",
                        value = uiState.errorMessage
                    )
                }
            }
            
            // System Information Debug Panel
            DebugPanel(
                title = "System Information",
                isVisible = true,
                initiallyExpanded = true
            ) {
                DebugText("Android Version", "API ${android.os.Build.VERSION.SDK_INT}")
                DebugText("Device Model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                DebugText("App Process", android.os.Process.myPid().toString())
                DebugDivider()
                DebugText("WorkManager Version", "2.9.0")
                DebugText("Hilt Work Version", "1.1.0")
            }
        }
    }
    
    // Show snackbar for copy confirmation
    LaunchedEffect(uiState.showCopyConfirmation) {
        if (uiState.showCopyConfirmation) {
            viewModel.clearCopyConfirmation()
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

data class WorkManagerTestUiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val statusMessage: String = "Not tested yet",
    val lastTestTime: String = "Never",
    val totalWorkItems: Int = 0,
    val enqueuedWork: Int = 0,
    val runningWork: Int = 0,
    val succeededWork: Int = 0,
    val failedWork: Int = 0,
    val cancelledWork: Int = 0,
    val blockedWork: Int = 0,
    val workDetails: List<WorkDetail> = emptyList(),
    val errorMessage: String = "",
    val showCopyConfirmation: Boolean = false
)

data class WorkDetail(
    val id: String,
    val state: WorkInfo.State,
    val tags: Set<String>,
    val runAttemptCount: Int
)