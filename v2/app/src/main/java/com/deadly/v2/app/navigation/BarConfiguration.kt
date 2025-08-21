package com.deadly.v2.app.navigation

// Re-export core design configuration types for convenience
import com.deadly.v2.core.design.scaffold.BarConfiguration
import com.deadly.v2.core.design.scaffold.BottomBarConfig
import com.deadly.v2.feature.home.screens.main.HomeBarConfiguration
import com.deadly.v2.feature.search.screens.main.SearchBarConfiguration
import com.deadly.v2.feature.settings.screens.main.SettingsBarConfiguration

/**
 * Central route mapping to feature bar configurations
 * 
 * This delegates to feature-specific configuration objects,
 * keeping the actual configurations colocated with their features.
 */
object NavigationBarConfig {
    fun getBarConfig(route: String?): BarConfiguration = when (route) {
        // Home routes
        "home" -> HomeBarConfiguration.getHomeBarConfig()
        
        // Search routes - delegate to SearchBarConfiguration
        "search" -> SearchBarConfiguration.getSearchBarConfig()
        "search-results" -> SearchBarConfiguration.getSearchResultsBarConfig()
        
        // Settings routes
        "settings" -> SettingsBarConfiguration.getSettingsBarConfig()
        
        // Library routes
        "library" -> BarConfiguration(
            topBar = null, // Library doesn't need top bar yet
            bottomBar = BottomBarConfig(visible = true)
        )
        
        // Splash and other routes
        "splash" -> BarConfiguration(
            topBar = null,
            bottomBar = BottomBarConfig(visible = false) // Hide bottom nav on splash
        )
        
        // Default configuration
        else -> BarConfiguration(
            topBar = null,
            bottomBar = BottomBarConfig(visible = true)
        )
    }
}