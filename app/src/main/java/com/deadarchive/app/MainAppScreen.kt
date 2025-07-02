package com.deadarchive.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deadarchive.core.design.component.DeadArchiveBottomNavigation
import com.deadarchive.core.settings.SettingsScreen
import com.deadarchive.core.settings.model.VersionInfo
import com.deadarchive.feature.browse.navigation.browseScreen
import com.deadarchive.feature.player.navigation.playerScreen
import com.deadarchive.feature.playlist.navigation.playlistScreen
import com.deadarchive.feature.playlist.MiniPlayerContainer
import androidx.media3.common.util.UnstableApi

/**
 * Main app screen with bottom navigation
 * This wraps the entire app and provides the bottom navigation bar
 */
@UnstableApi
@Composable
fun MainAppScreen(
    navController: NavHostController = rememberNavController(),
    onNavigateToPlayer: (String) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            // Show bottom navigation on main screens, hide on detail screens
            if (shouldShowBottomNavigation(currentRoute)) {
                Column {
                    // Global MiniPlayer - shows above bottom navigation
                    if (shouldShowMiniPlayer(currentRoute)) {
                        MiniPlayerContainer(
                            onTapToExpand = { concertId ->
                                // Navigate to full player when tapping mini player
                                if (concertId != null) {
                                    android.util.Log.d("MainAppNavigation", "MiniPlayer tapped - navigating to 'player/$concertId'")
                                    navController.navigate("player/$concertId")
                                } else {
                                    android.util.Log.d("MainAppNavigation", "MiniPlayer tapped - navigating to 'player' (no concertId)")
                                    navController.navigate("player")
                                }
                            }
                        )
                    }
                    
                    // Bottom Navigation
                    DeadArchiveBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigateToDestination = { route ->
                            when (route) {
                                "home" -> {
                                    // For home button, always navigate to fresh home screen
                                    navController.navigate(route) {
                                        popUpTo("home") {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                                else -> {
                                    // For other tabs, use standard bottom nav behavior
                                    navController.navigate(route) {
                                        // Pop up to the graph's start destination to avoid building up a large stack
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when re-selecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToDebug = { navController.navigate("debug") },
                    onNavigateToBrowse = { navController.navigate("browse") }
                )
            }
            
            // Browse/Search functionality
            browseScreen(
                onNavigateToPlayer = { concertId -> 
                    android.util.Log.d("MainAppNavigation", "Browse navigating to playlist with concertId: '$concertId'")
                    navController.navigate("playlist/$concertId")
                }
            )
            
            composable("library") {
                LibraryScreen()
            }
            
            composable("debug") {
                SettingsScreen(
                    onNavigateToDebug = { navController.navigate("debug_screen") },
                    versionInfo = VersionInfo(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        buildType = BuildConfig.BUILD_TYPE,
                        buildTime = BuildConfig.BUILD_TIME,
                        gitCommitHash = if (BuildConfig.GIT_COMMIT_HASH.isNotBlank()) BuildConfig.GIT_COMMIT_HASH else null
                    )
                )
            }
            
            // Debug screen (accessible from Settings)
            composable("debug_screen") {
                DebugScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToRepositoryTest = { navController.navigate("repository_test") },
                    onNavigateToDatabaseTest = { navController.navigate("database_test") },
                    onNavigateToNetworkTest = { navController.navigate("network_test") },
                    onNavigateToMediaPlayerTest = { navController.navigate("media_player_test") }
                )
            }
            
            // Debug screens (hidden from bottom nav)
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
            
            composable("repository_test") {
                RepositoryTestScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("media_player_test") {
                MediaPlayerTestScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Playlist screen
            playlistScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { concertId -> 
                    android.util.Log.d("MainAppNavigation", "Playlist navigating to player with concertId: '$concertId'")
                    navController.navigate("player/$concertId") 
                }
            )
            
            // Player screen
            playerScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
        }
    }
}

/**
 * Determines whether to show the bottom navigation bar based on current route
 */
private fun shouldShowBottomNavigation(currentRoute: String?): Boolean {
    return when {
        currentRoute == null -> false
        currentRoute.startsWith("player") -> false
        currentRoute.startsWith("playlist") -> true
        currentRoute in listOf("home", "browse", "library", "debug") -> true
        else -> false
    }
}

/**
 * Determines whether to show the miniplayer based on current route
 * Shows miniplayer on all main screens except full player
 */
private fun shouldShowMiniPlayer(currentRoute: String?): Boolean {
    return when {
        currentRoute == null -> false
        currentRoute.startsWith("player") -> false  // Hide on full player screen
        currentRoute.startsWith("playlist") -> true
        currentRoute in listOf("home", "browse", "library", "debug") -> true
        else -> false
    }
}