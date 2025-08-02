package com.deadarchive.feature.browse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadarchive.feature.browse.BrowseScreen
import com.deadarchive.feature.browse.SearchV2Screen
import com.deadarchive.core.model.Show

fun NavGraphBuilder.browseScreen(
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToShow: (Show) -> Unit,
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
        if (useSearchV2) {
            SearchV2Screen(
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                initialEra = era
            )
        } else {
            BrowseScreen(
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                initialEra = era
            )
        }
    }
    
    // Handle simple "browse" route without parameters
    composable("browse") {
        if (useSearchV2) {
            SearchV2Screen(
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                initialEra = null
            )
        } else {
            BrowseScreen(
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToShow = onNavigateToShow,
                initialEra = null
            )
        }
    }
}