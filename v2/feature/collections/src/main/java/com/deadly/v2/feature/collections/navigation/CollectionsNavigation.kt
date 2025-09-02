package com.deadly.v2.feature.collections.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.deadly.v2.feature.collections.screens.main.CollectionsScreen
import com.deadly.v2.feature.collections.screens.details.CollectionDetailsScreen

/**
 * Navigation graph for collections feature
 */
fun NavGraphBuilder.collectionsGraph(navController: NavController) {
    navigation(
        startDestination = "collections",
        route = "collections-graph"
    ) {
        composable("collections") {
            CollectionsScreen(
                onNavigateToCollection = { collectionId ->
                    navController.navigate("collection/$collectionId")
                },
                onNavigateToShow = { showId ->
                    navController.navigate("playlist/$showId")
                }
            )
        }
        
        composable("collection/{collectionId}") { backStackEntry ->
            val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
            CollectionDetailsScreen(
                collectionId = collectionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToShow = { showId -> navController.navigate("playlist/$showId") }
            )
        }
    }
}