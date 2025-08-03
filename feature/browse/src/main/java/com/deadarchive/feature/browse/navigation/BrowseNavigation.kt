package com.deadarchive.feature.browse.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.browse.BrowseScreen
import com.deadarchive.feature.browse.SearchV2Screen
import com.deadarchive.feature.browse.SearchResultsV2Screen
import com.deadarchive.core.model.Show

fun NavGraphBuilder.browseScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
    navController: NavController,
    useSearchV2: Boolean = false
) {
    // Handle both "browse" and "browse?era=..." routes
    composable(
        route = "browse?era={era}",
        arguments = listOf(
            androidx.navigation.navArgument("era") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val era = backStackEntry.arguments?.getString("era")
        when {
            useSearchV2 -> {
                SearchV2Screen(
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToShow = onNavigateToShow,
                    onNavigateToSearchResults = { navController.navigate("search_results") },
                    initialEra = era
                )
            }
            else -> {
                BrowseScreen(
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToShow = onNavigateToShow,
                    initialEra = era
                )
            }
        }
    }
    
    // Handle simple "browse" route without parameters
    composable("browse") {
        when {
            useSearchV2 -> {
                SearchV2Screen(
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToShow = onNavigateToShow,
                    onNavigateToSearchResults = { navController.navigate("search_results") },
                    initialEra = null
                )
            }
            else -> {
                BrowseScreen(
                    onNavigateToPlayer = onNavigateToPlayer,
                    onNavigateToShow = onNavigateToShow,
                    initialEra = null
                )
            }
        }
    }
    
    // Search results screen
    composable("search_results") {
        SearchResultsV2Screen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToShow = onNavigateToShow,
            onNavigateToPlayer = onNavigateToPlayer
        )
    }
}