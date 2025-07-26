package com.deadarchive.core.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
 * Unified library button component that handles all library operations consistently
 * across the entire application. Features reactive state management that automatically
 * updates when library status changes (e.g., when downloads add shows to library).
 * 
 * @param show Show object to add/remove from library
 * @param isInLibraryFlow Reactive Flow of library status that automatically updates
 * @param onClick Action handler for library operations
 * @param onConfirmationNeeded Callback when confirmation dialog is needed (show, hasDownloads, downloadInfo) -> Unit
 * @param showDownloadConfirmation Whether to show download confirmation dialogs when there are downloads
 * @param alwaysConfirmRemoval Whether to always show confirmation dialog when removing from library
 * @param size Button size (icon size)
 * @param modifier Compose modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryButton(
    show: Show,
    isInLibraryFlow: kotlinx.coroutines.flow.Flow<Boolean>,
    onClick: (LibraryAction) -> Unit,
    onConfirmationNeeded: (LibraryRemovalDialogConfig) -> Unit = { },
    showDownloadConfirmation: Boolean = true,
    alwaysConfirmRemoval: Boolean = false,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    // Reactive state management - automatically updates when library status changes
    val isInLibrary by isInLibraryFlow.collectAsState(initial = false)
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .combinedClickable(
                onClick = {
                    if (isInLibrary) {
                        // Removing from library - check if confirmation is needed
                        if (alwaysConfirmRemoval || showDownloadConfirmation) {
                            // Delegate to parent to show confirmation dialog
                            // Parent will determine download info and show appropriate dialog
                            onConfirmationNeeded(
                                LibraryRemovalDialogConfig(
                                    show = show,
                                    hasDownloads = false, // Parent will update this
                                    downloadInfo = "",     // Parent will update this
                                    alwaysConfirm = alwaysConfirmRemoval
                                )
                            )
                        } else {
                            // No confirmation needed, remove immediately
                            onClick(LibraryAction.REMOVE_FROM_LIBRARY)
                        }
                    } else {
                        // Adding to library - no confirmation needed
                        onClick(LibraryAction.ADD_TO_LIBRARY)
                    }
                }
            )
    ) {
        Icon(
            painter = if (isInLibrary) {
                painterResource(R.drawable.ic_library_add_check)
            } else {
                painterResource(R.drawable.ic_library_add)
            },
            contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
            modifier = Modifier.size(size),
            tint = if (isInLibrary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
