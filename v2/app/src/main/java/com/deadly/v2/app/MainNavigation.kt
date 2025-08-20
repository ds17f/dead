package com.deadly.v2.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.deadly.v2.feature.splash.navigation.splashGraph
import com.deadly.v2.feature.home.navigation.homeGraph
import com.deadly.v2.feature.search.navigation.searchGraph
import com.deadly.v2.feature.settings.navigation.settingsGraph
import com.deadly.v2.core.theme.api.DeadlyTheme
import com.deadly.v2.core.theme.api.ThemeAssetProvider
import com.deadly.v2.core.theme.ThemeManager
import javax.inject.Inject

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
    // Initialize theme system on app startup
    LaunchedEffect(Unit) {
        themeManager.autoInitialize()
    }
    
    // Wrap entire navigation with DI-injected theme provider
    DeadlyTheme(themeProvider = themeProvider) {
        val navController = rememberNavController()
        
        NavHost(
            navController = navController,
            startDestination = "splash"
        ) {
            // Splash feature - handles V2 database initialization
            splashGraph(navController)
            
            // Home feature - main navigation hub
            homeGraph(navController)
            
            // Search feature - search and browse functionality
            searchGraph(navController)
            
            // Settings feature - app configuration
            settingsGraph(navController)
        }
    }
}