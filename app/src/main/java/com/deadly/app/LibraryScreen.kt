package com.deadly.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.deadly.core.model.Recording
import com.deadly.feature.library.LibraryScreen as FeatureLibraryScreen

/**
 * Library screen showing user's saved shows and recordings
 */
@Composable
fun LibraryScreen(
    onRecordingSelected: (Recording) -> Unit,
    modifier: Modifier = Modifier
) {
    FeatureLibraryScreen(
        onNavigateToRecording = onRecordingSelected,
        modifier = modifier
    )
}

