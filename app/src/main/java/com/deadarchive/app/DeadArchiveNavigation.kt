package com.deadarchive.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deadarchive.feature.browse.navigation.browseScreen
import com.deadarchive.feature.downloads.navigation.downloadsScreen
import com.deadarchive.feature.favorites.navigation.favoritesScreen
import com.deadarchive.feature.player.navigation.playerScreen

@UnstableApi
@Composable
fun DeadArchiveNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    showSplash: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (showSplash) "splash" else "main_app",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                onSplashComplete = { 
                    navController.navigate("main_app") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        
        composable("main_app") {
            MainAppScreen(
                onNavigateToPlayer = { concertId -> 
                    navController.navigate("player/$concertId") 
                }
            )
        }
        
        // Player screens (full screen, no bottom nav)
        playerScreen(
            onNavigateBack = { navController.popBackStack() },
            navController = navController
        )
        
        downloadsScreen(
            onNavigateToPlayer = { navController.navigate("player") }
        )
        
        favoritesScreen(
            onNavigateToPlayer = { navController.navigate("player") }
        )
    }
}