package com.deadarchive.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.library.LibraryScreen

fun NavGraphBuilder.libraryScreen(
    onNavigateToPlayer: () -> Unit
) {
    composable("library") {
        LibraryScreen(
            onRecordingSelected = { onNavigateToPlayer() }
        )
    }
}