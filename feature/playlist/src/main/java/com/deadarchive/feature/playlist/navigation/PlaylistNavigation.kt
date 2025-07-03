package com.deadarchive.feature.playlist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.playlist.PlaylistScreen

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    composable("playlist/{recordingId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = { onNavigateToPlayer(recordingId) },
            recordingId = recordingId
        )
    }
}