package com.deadarchive.v2.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.v2.feature.home.HomeScreen

/**
 * Navigation graph for home feature
 */
fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable("home") {
        HomeScreen(
            onNavigateToSearch = {
                navController.navigate("search-graph")
            },
            onNavigateToLibrary = {
                // TODO: Navigate to library when implemented
                // navController.navigate("library")
            },
            onNavigateToPlayer = {
                // TODO: Navigate to player when implemented
                // navController.navigate("player")
            },
            onNavigateToSettings = {
                // TODO: Navigate to settings when implemented
                // navController.navigate("settings")
            }
        )
    }
}