package com.deadly.core.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deadly.core.design.R

/**
 * Actions available for download button long-press context menu
 */
enum class DownloadAction {
    RETRY,
    REMOVE,
    PAUSE,
    RESUME,
    CANCEL
}

/**
 * Unified download button component that handles all download states consistently
 * across the entire application. Features built-in optimistic state management
 * for immediate visual feedback when clicked.
 * 
 * @param downloadState Current download state to display
 * @param onClick Primary click action (start download, cancel, show confirmation, etc.)
 * @param onLongClick Long-press action handler for context menu actions
 * @param showLongPressMenu Whether to enable long-press context menu (playlist only)
 * @param size Button size (icon size)
 * @param showProgress Whether to show progress indicators for active downloads
 * @param modifier Compose modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadButton(
    downloadState: ShowDownloadState,
    onClick: () -> Unit,
    onLongClick: (DownloadAction) -> Unit = {},
    showLongPressMenu: Boolean = false,
    size: Dp = 24.dp,
    showProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    // Internal optimistic state management
    var internalOptimisticState by remember { mutableStateOf<ShowDownloadState?>(null) }
    
    // Clear optimistic state when actual state catches up or after timeout
    LaunchedEffect(downloadState) {
        if (internalOptimisticState != null) {
            // Clear optimistic state if actual state matches our prediction type
            val shouldClear = when {
                // Both are the same type - prediction was correct
                downloadState::class == internalOptimisticState!!::class -> true
                // If we predicted downloading but got paused, that's close enough
                internalOptimisticState is ShowDownloadState.Downloading && downloadState is ShowDownloadState.Paused -> true
                // If we predicted paused but got downloading (resume worked), that's correct
                internalOptimisticState is ShowDownloadState.Paused && downloadState is ShowDownloadState.Downloading -> true
                // If actual state moved away from what we started with, prediction was successful
                downloadState !is ShowDownloadState.NotDownloaded && internalOptimisticState is ShowDownloadState.Downloading -> true
                else -> false
            }
            
            if (shouldClear) {
                internalOptimisticState = null
            }
        }
    }
    
    // Auto-clear optimistic state after timeout to prevent stuck states
    LaunchedEffect(internalOptimisticState) {
        if (internalOptimisticState != null) {
            kotlinx.coroutines.delay(4000) // 4 second timeout
            internalOptimisticState = null
        }
    }
    
    // Use optimistic state if available, otherwise use actual state
    val displayState = internalOptimisticState ?: downloadState
    
    // Predict next state based on current state for optimistic updates
    fun predictNextState(currentState: ShowDownloadState): ShowDownloadState? {
        return when (currentState) {
            is ShowDownloadState.NotDownloaded -> ShowDownloadState.Downloading()
            is ShowDownloadState.Downloading -> ShowDownloadState.Paused()
            is ShowDownloadState.Paused -> ShowDownloadState.Downloading()
            is ShowDownloadState.Cancelled -> ShowDownloadState.Downloading()
            is ShowDownloadState.Failed -> ShowDownloadState.Downloading()
            is ShowDownloadState.Downloaded -> null // No prediction for removal (needs confirmation)
        }
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .combinedClickable(
                onClick = {
                    // Set optimistic state immediately for responsive UI
                    val predicted = predictNextState(downloadState)
                    if (predicted != null) {
                        internalOptimisticState = predicted
                    }
                    // Then call the actual onClick handler
                    onClick()
                },
                onLongClick = if (showLongPressMenu) {
                    {
                        val availableActions = getAvailableActions(displayState)
                        if (availableActions.isNotEmpty()) {
                            showContextMenu = true
                        }
                    }
                } else null
            )
    ) {
        when (displayState) {
            is ShowDownloadState.NotDownloaded -> {
                Icon(
                    painter = painterResource(R.drawable.ic_file_download),
                    contentDescription = "Download",
                    modifier = Modifier.size(size),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            is ShowDownloadState.Downloading -> {
                if (showProgress) {
                    CircularProgressIndicator(
                        progress = { displayState.trackProgress },
                        modifier = Modifier.size(size),
                        strokeWidth = 2.dp,
                        color = Color.Red,
                        trackColor = Color.White
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_stop),
                    contentDescription = "Stop Download",
                    modifier = Modifier.size((size * 0.6f)),
                    tint = Color.White
                )
            }
            
            is ShowDownloadState.Paused -> {
                if (showProgress) {
                    CircularProgressIndicator(
                        progress = { displayState.trackProgress },
                        modifier = Modifier.size(size),
                        strokeWidth = 2.dp,
                        color = Color.Red,
                        trackColor = Color.White
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_pause),
                    contentDescription = "Paused - Click to resume",
                    modifier = Modifier.size((size * 0.6f)),
                    tint = Color.White
                )
            }
            
            is ShowDownloadState.Cancelled -> {
                if (showProgress) {
                    CircularProgressIndicator(
                        progress = { displayState.trackProgress },
                        modifier = Modifier.size(size),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        trackColor = Color.White
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Cancelled - Click to retry",
                    modifier = Modifier.size((size * 0.6f)),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            is ShowDownloadState.Failed -> {
                if (showProgress) {
                    CircularProgressIndicator(
                        progress = { 1f }, // Show full circle for failed state
                        modifier = Modifier.size(size),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        trackColor = Color.White
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = "Download Failed - Click to retry - ${displayState.errorMessage ?: "Unknown error"}",
                    modifier = Modifier.size((size * 0.6f)),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            is ShowDownloadState.Downloaded -> {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = "Downloaded",
                    modifier = Modifier.size(size),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Context menu for long-press actions
        if (showContextMenu && showLongPressMenu) {
            DownloadContextMenu(
                downloadState = displayState,
                onAction = { action ->
                    showContextMenu = false
                    onLongClick(action)
                },
                onDismiss = { showContextMenu = false }
            )
        }
    }
}

/**
 * Context menu for download button long-press actions
 */
@Composable
private fun DownloadContextMenu(
    downloadState: ShowDownloadState,
    onAction: (DownloadAction) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = getAvailableActions(downloadState)
    
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(getActionText(action)) },
                onClick = { onAction(action) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(getActionIcon(action)),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Get available actions for a given download state
 */
private fun getAvailableActions(downloadState: ShowDownloadState): List<DownloadAction> {
    return when (downloadState) {
        is ShowDownloadState.NotDownloaded -> emptyList()
        is ShowDownloadState.Downloading -> listOf(DownloadAction.PAUSE, DownloadAction.CANCEL)
        is ShowDownloadState.Paused -> listOf(DownloadAction.RESUME, DownloadAction.CANCEL)
        is ShowDownloadState.Cancelled -> listOf(DownloadAction.RETRY, DownloadAction.REMOVE)
        is ShowDownloadState.Failed -> listOf(DownloadAction.RETRY, DownloadAction.REMOVE)
        is ShowDownloadState.Downloaded -> listOf(DownloadAction.REMOVE)
    }
}

/**
 * Get display text for download actions
 */
private fun getActionText(action: DownloadAction): String {
    return when (action) {
        DownloadAction.RETRY -> "Retry Download"
        DownloadAction.REMOVE -> "Remove Download" 
        DownloadAction.PAUSE -> "Pause Download"
        DownloadAction.RESUME -> "Resume Download"
        DownloadAction.CANCEL -> "Cancel Download"
    }
}

/**
 * Get icon for download actions
 */
private fun getActionIcon(action: DownloadAction): Int {
    return when (action) {
        DownloadAction.RETRY -> R.drawable.ic_refresh
        DownloadAction.REMOVE -> R.drawable.ic_close
        DownloadAction.PAUSE -> R.drawable.ic_pause
        DownloadAction.RESUME -> R.drawable.ic_play_arrow
        DownloadAction.CANCEL -> R.drawable.ic_stop
    }
}