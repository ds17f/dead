package com.deadarchive.v2.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.deadarchive.v2.feature.search.ui.components.SearchScreen
import com.deadarchive.v2.feature.search.ui.components.SearchResultsScreen

/**
 * Navigation graph for search feature
 */
fun NavGraphBuilder.searchGraph(navController: NavController) {
    navigation(
        startDestination = "search",
        route = "search-graph"
    ) {
        composable("search") {
            SearchScreen(
                onNavigateToPlayer = { recordingId ->
                    // TODO: Navigate to player when implemented
                    // navController.navigate("player/$recordingId")
                },
                onNavigateToShow = { showId ->
                    // TODO: Navigate to show details when implemented
                    // navController.navigate("show/$showId")
                },
                onNavigateToSearchResults = {
                    navController.navigate("search-results")
                },
                initialEra = null
            )
        }
        
        composable("search-results") {
            SearchResultsScreen(
                initialQuery = "",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToShow = { showId ->
                    // TODO: Navigate to show details when implemented
                    // navController.navigate("show/$showId")
                },
                onNavigateToPlayer = { recordingId ->
                    // TODO: Navigate to player when implemented
                    // navController.navigate("player/$recordingId")
                }
            )
        }
    }
}