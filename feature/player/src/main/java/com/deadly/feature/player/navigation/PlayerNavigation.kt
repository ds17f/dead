package com.deadly.feature.player.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadly.feature.player.PlayerScreen
import com.deadly.feature.player.PlayerV2Screen
import com.deadly.feature.player.QueueScreen

fun NavGraphBuilder.playerScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    usePlayerV2: Boolean = false
) {
    // Handle both routes for now - with and without recording ID
    composable("player") {
        if (usePlayerV2) {
            PlayerV2Screen(
                onNavigateBack = onNavigateBack,
                onNavigateToQueue = { navController.navigate("queue") },
                onNavigateToPlaylist = { recordingId ->
                    if (recordingId != null) {
                        navController.navigate("playlist/$recordingId")
                    } else {
                        navController.popBackStack() // Fallback to previous screen
                    }
                }
            )
        } else {
            PlayerScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToQueue = { navController.navigate("queue") },
                onNavigateToPlaylist = { recordingId ->
                    if (recordingId != null) {
                        navController.navigate("playlist/$recordingId")
                    } else {
                        navController.popBackStack() // Fallback to previous screen
                    }
                }
            )
        }
    }
    
    composable("player/{recordingId}") { backStackEntry ->
        val recordingId = backStackEntry.arguments?.getString("recordingId")
        android.util.Log.d("PlayerNavigation", "Navigating to player with recordingId: '$recordingId'")
        if (usePlayerV2) {
            PlayerV2Screen(
                recordingId = recordingId,
                onNavigateBack = onNavigateBack,
                onNavigateToQueue = { navController.navigate("queue") },
                onNavigateToPlaylist = { recordingId ->
                    android.util.Log.d("PlayerNavigation", "Navigating to playlist with recordingId: '$recordingId'")
                    if (recordingId != null) {
                        navController.navigate("playlist/$recordingId")
                    } else {
                        navController.popBackStack() // Fallback to previous screen
                    }
                }
            )
        } else {
            PlayerScreen(
                recordingId = recordingId,
                onNavigateBack = onNavigateBack,
                onNavigateToQueue = { navController.navigate("queue") },
                onNavigateToPlaylist = { recordingId ->
                    android.util.Log.d("PlayerNavigation", "Navigating to playlist with recordingId: '$recordingId'")
                    if (recordingId != null) {
                        navController.navigate("playlist/$recordingId")
                    } else {
                        navController.popBackStack() // Fallback to previous screen
                    }
                }
            )
        }
    }
    
    // Queue screen
    composable("queue") {
        QueueScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}