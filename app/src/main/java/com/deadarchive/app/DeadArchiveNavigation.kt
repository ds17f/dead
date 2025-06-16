package com.deadarchive.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deadarchive.feature.browse.navigation.browseScreen
import com.deadarchive.feature.downloads.navigation.downloadsScreen
import com.deadarchive.feature.favorites.navigation.favoritesScreen
import com.deadarchive.feature.player.navigation.playerScreen

@Composable
fun DeadArchiveNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "browse",
        modifier = modifier
    ) {
        browseScreen(
            onNavigateToPlayer = { navController.navigate("player") }
        )
        
        playerScreen(
            onNavigateBack = { navController.popBackStack() }
        )
        
        downloadsScreen(
            onNavigateToPlayer = { navController.navigate("player") }
        )
        
        favoritesScreen(
            onNavigateToPlayer = { navController.navigate("player") }
        )
    }
}