package com.deadarchive.core.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.R
import com.deadarchive.core.model.Show

/**
 * Actions available for library button interactions
 */
enum class LibraryAction {
    ADD_TO_LIBRARY,
    REMOVE_FROM_LIBRARY,
    REMOVE_WITH_DOWNLOADS
}

/**
 * Information about downloads when removing from library
 */
data class LibraryRemovalDialogInfo(
    val show: Show,
    val hasDownloads: Boolean,
    val downloadInfo: String
)

/**
 * Unified library button component that handles all library operations consistently
 * across the entire application. Features built-in optimistic state management
 * for immediate visual feedback and automatic download integration.
 * 
 * @param show Show object to add/remove from library
 * @param isInLibrary Current library status
 * @param onClick Action handler for library operations
 * @param onRemovalInfoNeeded Callback to get download info for removal confirmation
 * @param showDownloadConfirmation Whether to show download confirmation dialogs
 * @param size Button size (icon size)
 * @param modifier Compose modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryButton(
    show: Show,
    isInLibrary: Boolean,
    onClick: (LibraryAction) -> Unit,
    onRemovalInfoNeeded: (Show) -> LibraryRemovalDialogInfo = { LibraryRemovalDialogInfo(it, false, "") },
    showDownloadConfirmation: Boolean = true,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    // Internal optimistic state management
    var internalOptimisticState by remember { mutableStateOf<Boolean?>(null) }
    var showRemovalDialog by remember { mutableStateOf(false) }
    var removalDialogInfo by remember { mutableStateOf<LibraryRemovalDialogInfo?>(null) }
    
    // Clear optimistic state when actual state catches up
    LaunchedEffect(isInLibrary) {
        if (internalOptimisticState != null && internalOptimisticState == isInLibrary) {
            internalOptimisticState = null
        }
    }
    
    // Auto-clear optimistic state after timeout to prevent stuck states
    LaunchedEffect(internalOptimisticState) {
        if (internalOptimisticState != null) {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            internalOptimisticState = null
        }
    }
    
    // Use optimistic state if available, otherwise use actual state
    val displayState = internalOptimisticState ?: isInLibrary
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .combinedClickable(
                onClick = {
                    if (displayState) {
                        // Removing from library - check for downloads if confirmation enabled
                        if (showDownloadConfirmation) {
                            val info = onRemovalInfoNeeded(show)
                            if (info.hasDownloads) {
                                removalDialogInfo = info
                                showRemovalDialog = true
                            } else {
                                // No downloads, remove immediately with optimistic state
                                internalOptimisticState = false
                                onClick(LibraryAction.REMOVE_FROM_LIBRARY)
                            }
                        } else {
                            // No confirmation, remove immediately with optimistic state
                            internalOptimisticState = false
                            onClick(LibraryAction.REMOVE_FROM_LIBRARY)
                        }
                    } else {
                        // Adding to library - immediate optimistic state
                        internalOptimisticState = true
                        onClick(LibraryAction.ADD_TO_LIBRARY)
                    }
                }
            )
    ) {
        Icon(
            painter = if (displayState) {
                painterResource(R.drawable.ic_library_add_check)
            } else {
                painterResource(R.drawable.ic_library_add)
            },
            contentDescription = if (displayState) "Remove from Library" else "Add to Library",
            modifier = Modifier.size(size),
            tint = if (displayState) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        
        // Removal confirmation dialog with download options
        if (showRemovalDialog && removalDialogInfo != null) {
            LibraryRemovalConfirmationDialog(
                info = removalDialogInfo!!,
                onConfirm = { removeDownloads ->
                    showRemovalDialog = false
                    internalOptimisticState = false
                    val action = if (removeDownloads) {
                        LibraryAction.REMOVE_WITH_DOWNLOADS
                    } else {
                        LibraryAction.REMOVE_FROM_LIBRARY
                    }
                    onClick(action)
                    removalDialogInfo = null
                },
                onCancel = {
                    showRemovalDialog = false
                    removalDialogInfo = null
                }
            )
        }
    }
}

/**
 * Confirmation dialog for removing shows from library with download cleanup options
 */
@Composable
private fun LibraryRemovalConfirmationDialog(
    info: LibraryRemovalDialogInfo,
    onConfirm: (removeDownloads: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var removeDownloads by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Remove from Library",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to remove \"${info.show.displayDate} - ${info.show.displayVenue}\" from your library?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (info.hasDownloads) {
                    Text(
                        text = "\nThis show has downloads: ${info.downloadInfo}",
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
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}