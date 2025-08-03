package com.deadarchive.feature.browse.debug

import com.deadarchive.feature.browse.HomeV2UiState
import com.deadarchive.core.design.component.DebugData
import com.deadarchive.core.design.component.DebugSection
import com.deadarchive.core.design.component.DebugItem

/**
 * Factory for creating HomeV2 debug data
 * Provides comprehensive debug information for development and troubleshooting
 */
object HomeV2DebugDataFactory {
    
    fun createDebugData(
        uiState: HomeV2UiState,
        initialEra: String?
    ): DebugData {
        return DebugData(
            screenName = "HomeV2Screen",
            sections = listOf(
                createGeneralSection(initialEra),
                createUiStateSection(uiState),
                createArchitectureSection(),
                createDevelopmentStatusSection()
            )
        )
    }
    
    private fun createGeneralSection(initialEra: String?): DebugSection {
        return DebugSection(
            title = "General Information",
            items = listOf(
                DebugItem.KeyValue("Screen", "HomeV2Screen"),
                DebugItem.Timestamp("Generated", System.currentTimeMillis()),
                DebugItem.KeyValue("Initial Era", initialEra ?: "None")
            )
        )
    }
    
    private fun createUiStateSection(uiState: HomeV2UiState): DebugSection {
        return DebugSection(
            title = "UI State",
            items = listOf(
                DebugItem.BooleanValue("Is Loading", uiState.isLoading),
                DebugItem.BooleanValue("Is Initialized", uiState.isInitialized),
                DebugItem.BooleanValue("Has Error", uiState.hasError),
                DebugItem.KeyValue("Error Message", uiState.errorMessage ?: "None"),
                DebugItem.KeyValue("Welcome Text", uiState.welcomeText),
                DebugItem.NumericValue("Featured Shows Count", uiState.featuredShows.size),
                DebugItem.NumericValue("Quick Actions Count", uiState.quickActions.size)
            )
        )
    }
    
    private fun createArchitectureSection(): DebugSection {
        return DebugSection(
            title = "V2 Architecture",
            items = listOf(
                DebugItem.KeyValue("Pattern", "UI-First Development"),
                DebugItem.KeyValue("State Management", "StateFlow"),
                DebugItem.KeyValue("Services", "Placeholder (TBD)"),
                DebugItem.BooleanValue("Debug Integration", true)
            )
        )
    }
    
    private fun createDevelopmentStatusSection(): DebugSection {
        return DebugSection(
            title = "Development Status",
            items = listOf(
                DebugItem.BooleanValue("Foundation", true),
                DebugItem.BooleanValue("Settings Integration", true),
                DebugItem.BooleanValue("Navigation", true),
                DebugItem.KeyValue("Next Phase", "UI Components")
            )
        )
    }
}