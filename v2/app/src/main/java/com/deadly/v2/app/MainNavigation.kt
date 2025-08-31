package com.deadly.v2.app

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deadly.v2.app.navigation.BottomNavDestination
import com.deadly.v2.app.navigation.NavigationBarConfig
import com.deadly.v2.core.design.scaffold.AppScaffold
import com.deadly.v2.feature.home.screens.main.HomeScreen
import com.deadly.v2.feature.settings.SettingsScreen
import com.deadly.v2.feature.splash.navigation.splashGraph
import com.deadly.v2.feature.search.navigation.searchGraph
import com.deadly.v2.feature.playlist.navigation.playlistGraph
import com.deadly.v2.feature.playlist.navigation.navigateToPlaylist
import com.deadly.v2.feature.player.navigation.playerScreen
import com.deadly.v2.feature.miniplayer.screens.main.MiniPlayerScreen
import com.deadly.v2.feature.library.navigation.libraryNavigation
import com.deadly.v2.core.theme.api.DeadlyTheme
import com.deadly.v2.core.theme.api.ThemeAssetProvider
import com.deadly.v2.core.theme.ThemeManager

/**
 * MainNavigation - Scalable navigation architecture for V2 app with theme system
 * 
 * This is the main navigation coordinator that orchestrates routing between
 * all feature modules. Each feature owns its own navigation subgraph,
 * maintaining clean separation of concerns.
 * 
 * The V2 app includes a theme system that auto-detects ZIP themes and provides
 * themed assets throughout the application via DeadlyTheme composition provider.
 * 
 * Navigation Flow:
 * 1. splash → home (after database initialization) 
 * 2. home → search-graph (user taps search)
 * 3. search → search-results (user taps search box)
 * 4. search-results → search (back navigation)
 * 5. Any screen → playlist/{showId} or playlist/{showId}/{recordingId}
 * 
 * Architecture Benefits:
 * - Scalable: Easy to add new feature subgraphs
 * - Modular: Each feature manages its own navigation
 * - Testable: Features accept navigation callbacks
 * - Clean: App module stays minimal and focused
 * - Themeable: Unified theme system across all V2 components
 */
@Composable
fun MainNavigation(
    themeManager: ThemeManager,
    themeProvider: ThemeAssetProvider
) {
    Log.d("MainNavigation", "MainNavigation: Starting with injected provider: ${themeProvider.getThemeName()}")
    
    // Initialize theme system on app startup
    LaunchedEffect(Unit) {
        Log.d("MainNavigation", "MainNavigation: Starting theme auto-initialization")
        themeManager.autoInitialize()
        Log.d("MainNavigation", "MainNavigation: Theme auto-initialization completed")
    }
    
    // Observe the current theme provider from ThemeManager
    val currentProvider by themeManager.currentProvider.collectAsState()
    Log.d("MainNavigation", "MainNavigation: Current provider from ThemeManager: ${currentProvider.getThemeName()}")
    
    // Wrap entire navigation with the current theme provider
    DeadlyTheme(themeProvider = currentProvider) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        
        // Get bar configuration based on current route
        val barConfig = NavigationBarConfig.getBarConfig(currentRoute)
        
        AppScaffold(
            topBarConfig = barConfig.topBar,
            bottomBarConfig = barConfig.bottomBar,
            bottomNavigationContent = if (barConfig.bottomBar?.visible == true) {
                {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigateToDestination = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            } else null,
            miniPlayerConfig = barConfig.miniPlayer,
            miniPlayerContent = {
                MiniPlayerScreen(
                    onTapToExpand = { _ ->
                        Log.d("MainNavigation", "MiniPlayer tapped - navigating to player")
                        navController.navigate("player")
                    }
                )
            },
            onNavigationClick = {
                navController.popBackStack()
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
            ) {
                // Splash feature - handles V2 database initialization
                splashGraph(navController)
                
                // Home feature - main hub screen
                composable("home") {
                    HomeScreen()
                }
                
                // Library feature - user's saved content  
                libraryNavigation(navController)
                
                // Search feature - search and browse functionality
                searchGraph(navController)
                
                // Playlist feature - show and recording details
                playlistGraph(navController)
                
                // Player feature - playback interface
                playerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlaylist = { showId, recordingId ->
                        navController.navigateToPlaylist(showId, recordingId)
                    }
                )
                
                // Settings feature - app configuration
                composable("settings") {
                    SettingsScreen()
                }
            }
        }
    }
}

/**
 * Bottom navigation bar component 
 */
@Composable
private fun BottomNavigationBar(
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
            BottomNavDestination.destinations.forEach { destination ->
                BottomNavItem(
                    destination = destination,
                    isSelected = currentRoute == destination.route,
                    onClick = { onNavigateToDestination(destination.route) }
                )
            }
        }
    }
}

/**
 * Individual bottom navigation item
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
            painter = if (isSelected) destination.selectedIcon() else destination.unselectedIcon(),
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

