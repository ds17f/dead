package com.deadly.v2.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.deadly.v2.app.navigation.BottomNavDestination
import com.deadly.v2.app.screens.BottomNavSettingsScreen
import com.deadly.v2.core.theme.api.ThemeAssets

/**
 * MainAppScreen - V2 app with Spotify-style bottom navigation
 * 
 * This is the main container for the V2 app providing:
 * - Scaffold with bottom navigation bar
 * - 4 navigation tabs: Home, Search, Library, Settings
 * - NavHost for screen navigation
 * - Integration with existing V2 screens and placeholder implementations
 */
@Composable
fun MainAppScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigateToDestination = { route ->
                    // Navigate to destination with proper stack management
                    navController.navigate(route) {
                        // Pop up to start destination to avoid stack buildup
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting previously selected item
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Home screen - clean welcome screen without hub navigation
            composable(BottomNavDestination.Home.route) {
                CleanHomeScreen()
            }
            
            // Search navigation graph - nested navigation for search flow
            navigation(
                startDestination = "search-main",
                route = BottomNavDestination.Search.route
            ) {
                composable("search-main") {
                    com.deadly.v2.feature.search.ui.components.SearchScreen(
                        onNavigateToPlayer = { recordingId ->
                            // TODO: Navigate to player when implemented in V2
                        },
                        onNavigateToShow = { showId ->
                            // TODO: Navigate to show details when implemented in V2
                        },
                        onNavigateToSearchResults = {
                            navController.navigate("search-results")
                        },
                        initialEra = null
                    )
                }
                
                composable("search-results") {
                    com.deadly.v2.feature.search.ui.components.SearchResultsScreen(
                        initialQuery = "",
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToShow = { showId ->
                            // TODO: Navigate to show details when implemented in V2
                        },
                        onNavigateToPlayer = { recordingId ->
                            // TODO: Navigate to player when implemented in V2
                        }
                    )
                }
            }
            
            // Library screen - placeholder implementation
            composable(BottomNavDestination.Library.route) {
                LibraryPlaceholderScreen()
            }
            
            // Settings screen - use adapted V2 SettingsScreen for bottom nav
            composable(BottomNavDestination.Settings.route) {
                BottomNavSettingsScreen()
            }
        }
    }
}

/**
 * Bottom navigation bar with Material3 styling matching V1 design
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


/**
 * Placeholder screen for Library functionality
 */
@Composable
private fun LibraryPlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = com.deadly.v2.core.design.resources.IconResources.Navigation.Library(),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Library Coming Soon",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Your saved shows and favorites will be available here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Clean home screen without hub navigation buttons
 * Navigation is handled by the bottom navigation bar
 */
@Composable
private fun CleanHomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Logo and title
            Image(
                painter = ThemeAssets.current.primaryLogo(),
                contentDescription = "Deadly Logo",
                modifier = Modifier.size(120.dp)
            )
            
            Text(
                text = "Deadly V2",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Grateful Dead Recordings & Shows",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Welcome message without navigation buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Welcome to Deadly V2",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Use the navigation below to explore thousands of Grateful Dead recordings and shows.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Version info
            Text(
                text = "Enhanced search • FTS integration • V2 architecture",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}