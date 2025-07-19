package com.deadarchive.feature.playlist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.playlist.PlaylistScreen

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToShow: (String, String) -> Unit // showId, recordingId
) {
    composable("playlist/{recordingId}?showId={showId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
        val showId = backStackEntry.arguments?.getString("showId")
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            onNavigateToShow = onNavigateToShow,
            recordingId = recordingId,
            showId = showId
        )
    }
}