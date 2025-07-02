package com.deadarchive.feature.player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.player.PlayerScreen
import com.deadarchive.feature.player.QueueScreen

fun NavGraphBuilder.playerScreen(
    onNavigateBack: () -> Unit,
    navController: NavController
) {
    // Handle both routes for now - with and without concert ID
    composable("player") {
        PlayerScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToQueue = { navController.navigate("queue") }
        )
    }
    
    composable("player/{concertId}") { backStackEntry ->
        val concertId = backStackEntry.arguments?.getString("concertId")
        android.util.Log.d("PlayerNavigation", "Navigating to player with concertId: '$concertId'")
        PlayerScreen(
            concertId = concertId,
            onNavigateBack = onNavigateBack,
            onNavigateToQueue = { navController.navigate("queue") }
        )
    }
    
    // Queue screen
    composable("queue") {
        QueueScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}