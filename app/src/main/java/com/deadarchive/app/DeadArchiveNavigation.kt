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
        startDestination = if (showSplash) "splash" else "home",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                onSplashComplete = { 
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToNetworkTest = { navController.navigate("network_test") },
                onNavigateToDatabaseTest = { navController.navigate("database_test") },
                onNavigateToMediaPlayerTest = { navController.navigate("media_player_test") }
            )
        }
        
        composable("network_test") {
            NetworkTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("database_test") {
            DatabaseTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("media_player_test") {
            MediaPlayerTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
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