package com.deadly.v2.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.deadly.v2.feature.home.HomeScreen
import com.deadly.v2.feature.settings.navigation.navigateToSettings

/**
 * Navigation graph for home feature
 */
fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable("home") {
        HomeScreen()
    }
}