package com.deadarchive.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deadarchive.core.settings.SettingsScreen
import com.deadarchive.core.settings.SettingsViewModel
import com.deadarchive.core.settings.model.VersionInfo
import com.deadarchive.core.design.component.UpdateAvailableDialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.deadarchive.feature.browse.navigation.browseScreen
import com.deadarchive.feature.player.navigation.playerScreen
import com.deadarchive.feature.playlist.navigation.playlistScreen
import com.deadarchive.feature.playlist.MiniPlayerContainer
import androidx.media3.common.util.UnstableApi
import com.deadarchive.core.model.Show
import com.deadarchive.feature.library.navigation.libraryScreen
import com.deadarchive.core.settings.api.model.AppSettings

/**
 * Bottom navigation destinations
 */
private enum class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Outlined.Home),
    SEARCH("browse", "Search", Icons.Outlined.Search),
    LIBRARY("library", "Library", Icons.Outlined.LibraryAdd),
    SETTINGS("debug", "Settings", Icons.Outlined.Settings)
}

/**
 * Main app screen with bottom navigation
 * This wraps the entire app and provides the bottom navigation bar
 */
@UnstableApi
@Composable
fun MainAppScreen(
    navController: NavHostController = rememberNavController(),
    onNavigateToPlayer: (String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Observe update status from SettingsViewModel
    val updateStatus by settingsViewModel.updateStatus.collectAsState()
    val currentUpdate by settingsViewModel.currentUpdate.collectAsState()
    val downloadState by settingsViewModel.downloadState.collectAsState()
    val installationStatus by settingsViewModel.installationStatus.collectAsState()
    
    Scaffold(
        bottomBar = {
            // Show bottom navigation on main screens, hide on detail screens
            if (shouldShowBottomNavigation(currentRoute)) {
                Column {
                    // Global MiniPlayer - shows above bottom navigation
                    if (shouldShowMiniPlayer(currentRoute)) {
                        MiniPlayerContainer(
                            onTapToExpand = { recordingId ->
                                android.util.Log.d("MainAppNavigation", "MiniPlayer tapped - navigating to player with recordingId: $recordingId, usePlayerV2: ${settings.usePlayerV2}")
                                if (recordingId != null) {
                                    navController.navigate("player/$recordingId")
                                } else {
                                    navController.navigate("player")
                                }
                            }
                        )
                    }
                    
                    // Bottom Navigation
                    AppBottomNavigation(
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
                    onNavigateToBrowse = { navController.navigate("browse") },
                    onNavigateToShow = { show ->
                        // Navigate to playlist for the best recording of this show
                        show.bestRecording?.let { recording ->
                            android.util.Log.d("MainAppNavigation", "Home navigating to playlist with recordingId: '${recording.identifier}' for show: ${show.displayDate} - ${show.displayVenue}")
                            navController.navigate("playlist/${recording.identifier}?showId=${show.showId}")
                        } ?: run {
                            android.util.Log.w("MainAppNavigation", "No best recording found for show: ${show.displayDate} - ${show.displayVenue}")
                        }
                    },
                    onNavigateToEra = { era ->
                        // Navigate to browse with era filter
                        navController.navigate("browse?era=$era")
                    }
                )
            }
            
            // Browse/Search functionality
            browseScreen(
                onNavigateToPlayer = { recordingId -> 
                    android.util.Log.d("MainAppNavigation", "Browse navigating to playlist with recordingId: '$recordingId'")
                    navController.navigate("playlist/$recordingId")
                },
                onNavigateToShow = { show ->
                    // Navigate to playlist for the best recording of this show with showId parameter
                    show.bestRecording?.let { recording ->
                        android.util.Log.d("MainAppNavigation", "Browse navigating to playlist with recordingId: '${recording.identifier}' for show: ${show.displayDate} - ${show.displayVenue}")
                        navController.navigate("playlist/${recording.identifier}?showId=${show.showId}")
                    } ?: run {
                        android.util.Log.w("MainAppNavigation", "No best recording found for show: ${show.displayDate} - ${show.displayVenue}")
                    }
                }
            )
            
            // Library screen
            libraryScreen(
                onNavigateToPlayer = { recording ->
                    android.util.Log.d("MainAppNavigation", "Library navigating to playlist with recordingId: '${recording.identifier}'")
                    navController.navigate("playlist/${recording.identifier}")
                },
                onNavigateToShow = { show ->
                    // Navigate to playlist for the best recording of this show
                    show.bestRecording?.let { recording ->
                        android.util.Log.d("MainAppNavigation", "Library navigating to playlist with recordingId: '${recording.identifier}' for show: ${show.displayDate} - ${show.displayVenue}")
                        navController.navigate("playlist/${recording.identifier}?showId=${show.showId}")
                    }
                }
            )
            
            composable("debug") {
                SettingsScreen(
                    versionInfo = VersionInfo(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        buildType = BuildConfig.BUILD_TYPE,
                        buildTime = BuildConfig.BUILD_TIME,
                        gitCommitHash = if (BuildConfig.GIT_COMMIT_HASH.isNotBlank()) BuildConfig.GIT_COMMIT_HASH else null
                    )
                )
            }
            
            
            
            // Playlist screen
            playlistScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { 
                    android.util.Log.d("MainAppNavigation", "Playlist navigating to player")
                    navController.navigate("player") 
                },
                onNavigateToShow = { showId, recordingId ->
                    android.util.Log.d("MainAppNavigation", "Playlist navigating to next/prev show with showId: '$showId', recordingId: '$recordingId'")
                    navController.navigate("playlist/$recordingId?showId=$showId")
                }
            )
            
            // Player screen
            playerScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController,
                usePlayerV2 = settings.usePlayerV2
            )
        }
    }
    
    // Update available dialog - shows on top of any screen
    updateStatus?.let { status ->
        if (status.isUpdateAvailable) {
            currentUpdate?.let { update ->
                UpdateAvailableDialog(
                    update = update,
                    downloadState = downloadState,
                    installationStatus = installationStatus,
                    onDownload = {
                        // Download update directly without navigation - don't clear state
                        settingsViewModel.downloadUpdate()
                    },
                    onSkip = {
                        settingsViewModel.skipUpdate()
                    },
                    onInstall = {
                        // Install update directly without navigation - don't clear state
                        settingsViewModel.installUpdate()
                    },
                    onDismiss = {
                        settingsViewModel.clearUpdateState()
                    }
                )
            }
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
        currentRoute.startsWith("browse") -> true  // Handle browse with parameters like "browse?era=1990s"
        currentRoute in listOf("home", "library", "debug") -> true
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

/**
 * App bottom navigation component with Material3 styling
 */
@Composable
private fun AppBottomNavigation(
    currentRoute: String?,
    onNavigateToDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavDestination.entries.forEach { destination ->
                BottomNavItem(
                    destination = destination,
                    isSelected = when (destination.route) {
                        "browse" -> currentRoute?.startsWith("browse") == true
                        else -> currentRoute == destination.route
                    },
                    onClick = { onNavigateToDestination(destination.route) }
                )
            }
        }
    }
}

/**
 * Individual bottom navigation item with Material3 styling
 */
@Composable
private fun BottomNavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = destination.title,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}