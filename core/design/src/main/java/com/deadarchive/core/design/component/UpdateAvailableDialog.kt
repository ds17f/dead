package com.deadarchive.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadarchive.core.model.AppUpdate
import com.deadarchive.core.model.UpdateDownloadState
import com.deadarchive.core.model.UpdateInstallationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun UpdateAvailableDialog(
    update: AppUpdate,
    downloadState: UpdateDownloadState = UpdateDownloadState(),
    installationStatus: UpdateInstallationState = UpdateInstallationState(),
    onDownload: () -> Unit,
    onSkip: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Version ${update.version}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Released: ${formatDate(update.publishedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (update.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Release Notes:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = update.releaseNotes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Download progress
                if (downloadState.isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (downloadState.totalBytes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${formatBytes(downloadState.downloadedBytes)} / ${formatBytes(downloadState.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Download error
                if (downloadState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Download failed: ${downloadState.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Installation status
                if (installationStatus.isInstalling) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Installing Update...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    
                    installationStatus.statusMessage?.let { statusMessage ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Installation error
                if (installationStatus.isError) {
                    installationStatus.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Installation failed: $errorMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Installation success
                if (installationStatus.isSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "✅ Update installed successfully! Please restart the app to use the new version.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    installationStatus.isInstalling -> {
                        // Installing - show only dismiss
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                    installationStatus.isSuccess -> {
                        // Installation successful - close dialog
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                    installationStatus.isError -> {
                        // Installation failed - allow retry or dismiss
                        Button(onClick = onInstall) {
                            Text("Retry Install")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                    downloadState.isDownloading -> {
                        // Show only dismiss during download
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss")
                        }
                    }
                    downloadState.downloadedFile != null -> {
                        // Ready to install
                        Button(onClick = onInstall) {
                            Text("Install")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                    else -> {
                        // Initial state - offer download or skip
                        Button(onClick = onDownload) {
                            Text("Download")
                        }
                        TextButton(onClick = onSkip) {
                            Text("Skip This Version")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

private fun formatDate(isoDateString: String): String {
    return try {
        // Parse ISO 8601 date string (e.g., "2024-01-01T10:00:00Z")
        val instant = Instant.parse(isoDateString)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        formatter.format(Date.from(instant))
    } catch (e: Exception) {
        // Fallback if parsing fails
        isoDateString
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        bytes >= 1024 -> "${bytes / 1024}KB"
        else -> "${bytes}B"
    }
}