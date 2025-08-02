package com.deadarchive.app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deadarchive.core.settings.api.model.AppSettings
import com.deadarchive.feature.browse.navigation.browseScreen
import com.deadarchive.feature.library.navigation.libraryScreen
import com.deadarchive.feature.player.navigation.playerScreen
import com.deadarchive.feature.playlist.navigation.playlistScreen

private const val TAG = "Navigation"

@UnstableApi
@Composable
fun DeadArchiveNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    showSplash: Boolean = false,
    settings: AppSettings = AppSettings()
) {
    Log.d(TAG, "Initializing navigation with showSplash=$showSplash")
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
                },
                settings = settings
            )
        }
        
        // Player screens (full screen, no bottom nav)
        playerScreen(
            onNavigateBack = { navController.popBackStack() },
            navController = navController,
            usePlayerV2 = settings.usePlayerV2
        )
        
        
        libraryScreen(
            onNavigateToPlayer = { recording -> navController.navigate("player/${recording.identifier}") },
            onNavigateToShow = { show -> 
                Log.d(TAG, "Library: attempting to navigate to show ${show.showId}")
                Log.d(TAG, "Library: show has ${show.recordings.size} recordings")
                Log.d(TAG, "Library: show.bestRecording = ${show.bestRecording?.identifier}")
                
                show.bestRecording?.let { recording ->
                    val route = "playlist/${recording.identifier}?showId=${show.showId}"
                    Log.d(TAG, "Library: navigating to $route")
                    navController.navigate(route)
                } ?: run {
                    Log.w(TAG, "Library: show ${show.showId} has no bestRecording - cannot navigate to playlist")
                    if (show.recordings.isNotEmpty()) {
                        // Fallback: navigate to first recording if no best recording available
                        val firstRecording = show.recordings.first()
                        val route = "playlist/${firstRecording.identifier}?showId=${show.showId}"
                        Log.d(TAG, "Library: fallback navigation to $route")
                        navController.navigate(route)
                    } else {
                        Log.e(TAG, "Library: show ${show.showId} has no recordings at all - cannot navigate")
                        Log.e(TAG, "Library: show details - date: ${show.date}, venue: ${show.venue}")
                        Log.e(TAG, "Library: this might be a foreign key relationship issue from the refactor")
                    }
                }
            }
        )
        
        // Playlist screen
        try {
            Log.d(TAG, "Setting up playlist screen route")
            playlistScreen(
                onNavigateBack = { 
                    Log.d(TAG, "Playlist: navigating back")
                    navController.popBackStack() 
                },
                onNavigateToPlayer = { 
                    Log.d(TAG, "Playlist: navigating to player")
                    navController.navigate("player") 
                },
                onNavigateToShow = { showId, recordingId -> 
                    Log.d(TAG, "Playlist: navigating to player/$recordingId for show $showId")
                    navController.navigate("player/$recordingId")
                }
            )
            Log.d(TAG, "Playlist screen route setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up playlist screen route", e)
        }
        
        // Browse screen
        try {
            Log.d(TAG, "Setting up browse screen route")
            browseScreen(
                onNavigateToPlayer = { recordingId -> 
                    Log.d(TAG, "Browse: navigating to player/$recordingId")
                    navController.navigate("player/$recordingId") 
                },
                onNavigateToShow = { show ->
                    Log.d(TAG, "Browse: show has bestRecording: ${show.bestRecording != null}")
                    show.bestRecording?.let { recording ->
                        val route = "playlist/${recording.identifier}?showId=${show.showId}"
                        Log.d(TAG, "Browse: navigating to $route")
                        try {
                            navController.navigate(route)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to navigate to $route", e)
                        }
                    }
                },
                useSearchV2 = settings.useSearchV2
            )
            Log.d(TAG, "Browse screen route setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up browse screen route", e)
        }
    }
}