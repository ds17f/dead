package com.deadarchive.feature.library.debug

import androidx.compose.runtime.*
import com.deadarchive.core.design.component.DebugData
import com.deadarchive.core.design.component.DebugItem
import com.deadarchive.core.design.component.DebugSection
import com.deadarchive.core.design.component.DebugItemFactory
import com.deadarchive.feature.library.LibraryV2ViewModel
import com.deadarchive.feature.library.LibraryV2UiState

/**
 * Debug data collector for LibraryV2Screen.
 * Collects debug information about service calls, stub integration, and real-time logs.
 */
@Composable
fun collectLibraryV2DebugData(
    viewModel: LibraryV2ViewModel
): DebugData {
    // Collect reactive state
    val uiState by viewModel.uiState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    val serviceLogs by viewModel.serviceLogs.collectAsState()
    
    return DebugData(
        screenName = "LibraryV2",
        sections = listOf(
            createStubModeSection(),
            createServiceCallsSection(serviceLogs),
            createUiStateSection(uiState),
            createLibraryStatsSection(libraryStats),
            createServiceIntegrationSection(),
            createTestActionsSection(),
            createLogInstructionsSection()
        )
    )
}

private fun createStubModeSection(): DebugSection {
    return DebugSection(
        title = "Stub Mode Configuration",
        items = listOf(
            DebugItem.BooleanValue("Using Stub Services", true),
            DebugItem.KeyValue("LibraryV2Service", "@Named(\"stub\") LibraryV2ServiceStub"),
            DebugItem.KeyValue("DownloadV2Service", "@Named(\"stub\") DownloadV2ServiceStub"),
            DebugItem.KeyValue("Functionality", "Logging Only - No Side Effects"),
            DebugItemFactory.createTimestamp("Debug Panel Created"),
            DebugItem.KeyValue("Development Phase", "Phase 1: Minimal Logging Stubs")
        )
    )
}

private fun createServiceCallsSection(serviceLogs: List<String>): DebugSection {
    return DebugSection(
        title = "Real-Time Service Logs",
        items = listOf(
            DebugItem.NumericValue("Total Log Entries", serviceLogs.size),
            DebugItem.BooleanValue("Has Recent Activity", serviceLogs.isNotEmpty()),
            DebugItemFactory.createTimestamp("Last Log Update"),
            DebugItem.Multiline(
                "Recent Service Calls",
                if (serviceLogs.isEmpty()) {
                    "No service calls logged yet.\nTap test buttons below to generate logs."
                } else {
                    serviceLogs.takeLast(10).joinToString("\n") { log ->
                        "• $log"
                    }
                }
            ),
            DebugItem.KeyValue("Log Collection", "Real-time via ViewModel.serviceLogs StateFlow"),
            DebugItem.KeyValue("Log Source", "LibraryV2ServiceStub + DownloadV2ServiceStub")
        )
    )
}

private fun createUiStateSection(uiState: LibraryV2UiState): DebugSection {
    return DebugSection(
        title = "UI State Analysis",
        items = listOf(
            DebugItem.KeyValue("Current State", when (uiState) {
                is LibraryV2UiState.Loading -> "Loading"
                is LibraryV2UiState.Success -> "Success (${uiState.shows.size} shows)"
                is LibraryV2UiState.Error -> "Error: ${uiState.message}"
            }),
            DebugItem.BooleanValue("Is Loading", uiState is LibraryV2UiState.Loading),
            DebugItem.BooleanValue("Has Data", uiState is LibraryV2UiState.Success),
            DebugItem.BooleanValue("Has Error", uiState is LibraryV2UiState.Error),
            when (uiState) {
                is LibraryV2UiState.Success -> DebugItem.NumericValue("Show Count", uiState.shows.size)
                is LibraryV2UiState.Error -> DebugItemFactory.createErrorItem(uiState.message)
                else -> DebugItem.KeyValue("State Details", "Loading state")
            },
            DebugItemFactory.createTimestamp("State Last Updated")
        )
    )
}

private fun createLibraryStatsSection(libraryStats: com.deadarchive.core.library.api.LibraryStats?): DebugSection {
    return DebugSection(
        title = "Library Statistics",
        items = if (libraryStats != null) {
            listOf(
                DebugItem.NumericValue("Total Shows", libraryStats.totalShows),
                DebugItem.NumericValue("Downloaded Shows", libraryStats.totalDownloaded),
                DebugItem.NumericValue("Storage Used", libraryStats.totalStorageUsed, " bytes"),
                DebugItem.KeyValue("Stats Source", "LibraryV2ServiceStub.getLibraryStats()"),
                DebugItem.BooleanValue("Stats Available", true),
                DebugItemFactory.createTimestamp("Stats Retrieved")
            )
        } else {
            listOf(
                DebugItem.KeyValue("Stats Status", "NULL - Not loaded"),
                DebugItem.BooleanValue("Stats Available", false),
                DebugItemFactory.createErrorItem("Stats failed to load from stub service"),
                DebugItem.KeyValue("Expected Values", "All 0s from minimal stub implementation")
            )
        }
    )
}

private fun createServiceIntegrationSection(): DebugSection {
    return DebugSection(
        title = "Service Integration Status",
        items = listOf(
            DebugItem.BooleanValue("Hilt Injection Working", true),
            DebugItem.KeyValue("DI Configuration", "@Named qualifiers for stub services"),
            DebugItem.KeyValue("Module Status", "LibraryV2StubModule + DownloadV2StubModule"),
            DebugItem.BooleanValue("Cross-Service Coordination", true),
            DebugItem.KeyValue("Architecture Pattern", "Clean Architecture with Service Composition"),
            DebugItem.Multiline(
                "Service Dependencies",
                """LibraryV2ViewModel depends on:
  • LibraryV2Service (@Named("stub"))
  • DownloadV2Service (@Named("stub"))
  
Both services return safe defaults and log all calls."""
            )
        )
    )
}

private fun createTestActionsSection(): DebugSection {
    return DebugSection(
        title = "Available Test Actions",
        items = listOf(
            DebugItem.KeyValue("Add to Library", "addToLibrary(\"test-show-1\")"),
            DebugItem.KeyValue("Remove from Library", "removeFromLibrary(\"test-show-1\")"),
            DebugItem.KeyValue("Download Show", "downloadShow(Show(...))"),
            DebugItem.KeyValue("Clear Library", "clearLibrary()"),
            DebugItem.KeyValue("Action Results", "All actions log to serviceLogs + Android Log"),
            DebugItem.BooleanValue("Test Buttons Available", true),
            DebugItem.KeyValue("Validation Method", "Check serviceLogs section above for call traces")
        )
    )
}

private fun createLogInstructionsSection(): DebugSection {
    return DebugSection(
        title = "Logcat Instructions",
        items = listOf(
            DebugItem.KeyValue("Android Studio", "Open Logcat tab and filter by 'LibraryV2' or 'DownloadV2'"),
            DebugItem.Multiline(
                "Command Line",
                """# View all LibraryV2 logs:
adb logcat -s LibraryV2ServiceStub

# View all DownloadV2 logs:  
adb logcat -s DownloadV2ServiceStub

# View combined service logs:
adb logcat | grep -E "(LibraryV2|DownloadV2)ServiceStub\""""
            ),
            DebugItem.KeyValue("Log Tags", "LibraryV2ServiceStub, DownloadV2ServiceStub"),
            DebugItem.KeyValue("Log Level", "Log.d() - Debug level"),
            DebugItem.BooleanValue("Real-time Updates", true)
        )
    )
}