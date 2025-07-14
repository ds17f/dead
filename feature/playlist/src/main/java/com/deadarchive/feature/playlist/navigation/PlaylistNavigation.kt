package com.deadarchive.feature.playlist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.playlist.PlaylistScreen

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    composable("playlist/{recordingId}?showId={showId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
        val showId = backStackEntry.arguments?.getString("showId")
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = { onNavigateToPlayer(recordingId) },
            recordingId = recordingId,
            showId = showId
        )
    }
}