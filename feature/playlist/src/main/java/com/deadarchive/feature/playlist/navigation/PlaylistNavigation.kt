package com.deadarchive.feature.playlist.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.playlist.PlaylistScreen

fun NavGraphBuilder.playlistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    composable("playlist/{concertId}") { backStackEntry ->
        val concertId = backStackEntry.arguments?.getString("concertId") ?: ""
        PlaylistScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayer = onNavigateToPlayer,
            concertId = concertId
        )
    }
}