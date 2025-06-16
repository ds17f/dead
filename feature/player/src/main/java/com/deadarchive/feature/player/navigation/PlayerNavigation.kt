package com.deadarchive.feature.player.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.player.PlayerScreen

fun NavGraphBuilder.playerScreen(
    onNavigateBack: () -> Unit
) {
    composable("player") {
        PlayerScreen(
            onNavigateBack = onNavigateBack
        )
    }
}