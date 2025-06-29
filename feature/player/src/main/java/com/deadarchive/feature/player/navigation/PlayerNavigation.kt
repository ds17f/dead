package com.deadarchive.feature.player.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.player.PlayerScreen

fun NavGraphBuilder.playerScreen(
    onNavigateBack: () -> Unit
) {
    // Handle both routes for now - with and without concert ID
    composable("player") {
        PlayerScreen(
            onNavigateBack = onNavigateBack
        )
    }
    
    composable("player/{concertId}") { backStackEntry ->
        val concertId = backStackEntry.arguments?.getString("concertId") ?: ""
        PlayerScreen(
            onNavigateBack = onNavigateBack,
            concertId = concertId
        )
    }
}