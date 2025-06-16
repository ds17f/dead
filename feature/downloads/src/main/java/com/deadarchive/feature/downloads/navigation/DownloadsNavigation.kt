package com.deadarchive.feature.downloads.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.downloads.DownloadsScreen

fun NavGraphBuilder.downloadsScreen(
    onNavigateToPlayer: () -> Unit
) {
    composable("downloads") {
        DownloadsScreen(
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}