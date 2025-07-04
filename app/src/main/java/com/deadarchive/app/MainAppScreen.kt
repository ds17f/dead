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
                            onTapToExpand = { recordingId ->
                                // Navigate to full player when tapping mini player
                                if (recordingId != null) {
                                    android.util.Log.d("MainAppNavigation", "MiniPlayer tapped - navigating to 'player/$recordingId'")
                                    navController.navigate("player/$recordingId")
                                } else {
                                    android.util.Log.d("MainAppNavigation", "MiniPlayer tapped - navigating to 'player' (no recordingId)")
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
                                    // For other tabs, always clear stack and navigate to destination
                                    // This ensures Settings button always works regardless of current stack
                                    navController.navigate(route) {
                                        // Clear everything back to start destination
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = false
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
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
                onNavigateToPlayer = { recordingId -> 
                    android.util.Log.d("MainAppNavigation", "Browse navigating to playlist with recordingId: '$recordingId'")
                    navController.navigate("playlist/$recordingId")
                }
            )
            
            composable("library") {
                LibraryScreen(
                    onRecordingSelected = { recording ->
                        android.util.Log.d("MainAppNavigation", "Library navigating to player with recordingId: '${recording.identifier}'")
                        navController.navigate("player/${recording.identifier}")
                    }
                )
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
                onNavigateToPlayer = { recordingId -> 
                    android.util.Log.d("MainAppNavigation", "Playlist navigating to player with recordingId: '$recordingId'")
                    navController.navigate("player/$recordingId") 
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