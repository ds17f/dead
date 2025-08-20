package com.deadly.core.design.component

import android.util.Log

/**
 * Dedicated logger for debug panel system output.
 * Provides consistent logging with searchable tags for logcat viewing.
 */
object DebugLogger {
    
    private const val DEBUG_TAG = "DEAD_DEBUG_PANEL"
    private const val SEPARATOR = "=" // Use a character that's easy to search for
    
    /**
     * Logs debug data with consistent formatting and searchable tags.
     * Use this when copying debug data to also output to logcat.
     */
    fun logDebugData(debugData: DebugData, action: String = "COPY") {
        val header = buildString {
            append(SEPARATOR.repeat(60))
            append("\n")
            append("DEBUG PANEL ${action.uppercase()}: ${debugData.screenName}")
            append("\n")
            append("Timestamp: ${System.currentTimeMillis()}")
            append("\n")
            append("Total Sections: ${debugData.sections.size}")
            append("\n")
            append(SEPARATOR.repeat(60))
        }
        
        Log.d(DEBUG_TAG, header)
        Log.d(DEBUG_TAG, debugData.toFormattedText())
        
        val footer = buildString {
            append(SEPARATOR.repeat(60))
            append("\n")
            append("END DEBUG PANEL: ${debugData.screenName}")
            append("\n")
            append(SEPARATOR.repeat(60))
        }
        
        Log.d(DEBUG_TAG, footer)
    }
    
    /**
     * Logs a single debug section with consistent formatting.
     * Use this when copying individual sections.
     */
    fun logDebugSection(section: DebugSection, screenName: String, action: String = "COPY_SECTION") {
        val header = buildString {
            append(SEPARATOR.repeat(40))
            append("\n")
            append("DEBUG SECTION ${action.uppercase()}: ${section.title}")
            append("\n")
            append("Screen: $screenName")
            append("\n")
            append("Items: ${section.items.size}")
            append("\n")
            append(SEPARATOR.repeat(40))
        }
        
        Log.d(DEBUG_TAG, header)
        Log.d(DEBUG_TAG, section.toFormattedText())
        Log.d(DEBUG_TAG, SEPARATOR.repeat(40))
    }
    
    /**
     * Gets the logcat command users can run to view debug output.
     */
    fun getLogcatCommand(): String {
        return "adb logcat -s $DEBUG_TAG"
    }
    
    /**
     * Gets the logcat command to search for specific screen debug output.
     */
    fun getLogcatCommandForScreen(screenName: String): String {
        return "adb logcat -s $DEBUG_TAG | grep \"$screenName\""
    }
}