package com.deadarchive.core.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadarchive.core.model.Show

/**
 * Configuration for library removal confirmation dialog
 */
data class LibraryRemovalDialogConfig(
    val show: Show,
    val hasDownloads: Boolean,
    val downloadInfo: String,
    val alwaysConfirm: Boolean = false
)

/**
 * Reusable confirmation dialog for removing shows from library.
 * Handles both simple confirmation and download cleanup options.
 * 
 * @param config Configuration containing show info and download details
 * @param onConfirm Called when user confirms removal (removeDownloads: Boolean) -> Unit
 * @param onDismiss Called when user cancels or dismisses dialog
 */
@Composable
fun LibraryRemovalConfirmationDialog(
    config: LibraryRemovalDialogConfig,
    onConfirm: (removeDownloads: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var removeDownloads by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove from Library",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to remove \"${config.show.displayDate} - ${config.show.displayVenue}\" from your library?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (config.hasDownloads) {
                    Text(
                        text = "\nThis show has downloads: ${config.downloadInfo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Checkbox(
                            checked = removeDownloads,
                            onCheckedChange = { removeDownloads = it }
                        )
                        Text(
                            text = "Also remove downloads",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(removeDownloads) }
            ) {
                Text(
                    text = "Remove",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}