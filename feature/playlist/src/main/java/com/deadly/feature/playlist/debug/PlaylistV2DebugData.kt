package com.deadly.feature.playlist.debug

import androidx.compose.runtime.Composable
import com.deadly.core.design.component.DebugData
import com.deadly.core.design.component.DebugItem
import com.deadly.core.design.component.DebugSection

/**
 * Collect debug data for PlaylistV2Screen
 * 
 * Following V2 architecture patterns, this provides minimal debug information
 * for development and troubleshooting. Will expand as the feature develops.
 */
@Composable
fun collectPlaylistV2DebugData(
    recordingId: String? = null,
    showId: String? = null
): DebugData {
    return DebugData(
        screenName = "PlaylistV2Screen",
        sections = listOf(
            DebugSection(
                title = "Screen Parameters",
                items = listOf(
                    DebugItem.KeyValue("Recording ID", recordingId ?: "None"),
                    DebugItem.KeyValue("Show ID", showId ?: "None")
                )
            ),
            DebugSection(
                title = "V2 Architecture Status",
                items = listOf(
                    DebugItem.KeyValue("Feature Status", "Foundation Complete"),
                    DebugItem.KeyValue("Service Layer", "Pending"),
                    DebugItem.KeyValue("UI Components", "Minimal Scaffold"),
                    DebugItem.KeyValue("Navigation", "Feature Flag Ready")
                )
            ),
            DebugSection(
                title = "Development Info",
                items = listOf(
                    DebugItem.KeyValue("Implementation Phase", "Milestone 1"),
                    DebugItem.KeyValue("Debug System", "Functional"),
                    DebugItem.KeyValue("Next Steps", "Service Layer + Components")
                )
            )
        )
    )
}