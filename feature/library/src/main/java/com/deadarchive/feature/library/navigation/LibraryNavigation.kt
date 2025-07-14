package com.deadarchive.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.library.LibraryScreen
import com.deadarchive.core.model.Recording
import com.deadarchive.core.model.Show

fun NavGraphBuilder.libraryScreen(
    onNavigateToPlayer: (Recording) -> Unit,
    onNavigateToShow: (Show) -> Unit
) {
    composable("library") {
        LibraryScreen(
            onNavigateToRecording = onNavigateToPlayer,
            onNavigateToShow = onNavigateToShow
        )
    }
}