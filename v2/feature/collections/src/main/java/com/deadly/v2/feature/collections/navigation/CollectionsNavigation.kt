package com.deadly.v2.feature.collections.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.deadly.v2.feature.collections.screens.main.CollectionsScreen

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
                    // TODO: Navigate to collection detail when implemented
                    // navController.navigate("collection/$collectionId")
                },
                onNavigateToShow = { showId ->
                    navController.navigate("playlist/$showId")
                }
            )
        }
        
        // TODO: Add collection detail screen when implemented
        // composable("collection/{collectionId}") { backStackEntry ->
        //     val collectionId = backStackEntry.arguments?.getString("collectionId")
        //     CollectionDetailScreen(
        //         collectionId = collectionId,
        //         onNavigateBack = { navController.popBackStack() },
        //         onNavigateToShow = { showId -> navController.navigate("playlist/$showId") }
        //     )
        // }
    }
}