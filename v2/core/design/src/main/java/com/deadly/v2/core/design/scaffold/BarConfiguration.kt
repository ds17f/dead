package com.deadly.v2.core.design.scaffold

import androidx.compose.runtime.Composable
import com.deadly.v2.core.design.component.topbar.TopBarMode

/**
 * BarConfiguration - Navigation bar configuration data classes
 * 
 * These data classes define how navigation bars should appear and behave.
 * Located in core/design to be accessible by AppScaffold without circular dependencies.
 */
data class BarConfiguration(
    val topBar: TopBarConfig? = null,
    val bottomBar: BottomBarConfig? = null
)

/**
 * Configuration for top bar appearance and behavior
 */
data class TopBarConfig(
    val title: String,
    val mode: TopBarMode = TopBarMode.SOLID,
    val actions: (@Composable () -> Unit)? = null,
    val navigationIcon: (@Composable () -> Unit)? = null
)

/**
 * Configuration for bottom navigation bar
 */
data class BottomBarConfig(
    val visible: Boolean = true,
    val style: BottomBarStyle = BottomBarStyle.DEFAULT
)

/**
 * Bottom bar styling options
 */
enum class BottomBarStyle {
    DEFAULT,
    TRANSPARENT,
    ELEVATED
}