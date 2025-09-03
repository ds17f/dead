package com.deadly.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.deadly.core.settings.api.model.ThemeMode

// Grateful Dead inspired color palette
private val DeadRed = Color(0xFFDC143C)      // Crimson red
private val DeadGold = Color(0xFFFFD700)     // Golden yellow  
private val DeadGreen = Color(0xFF228B22)    // Forest green
private val DeadBlue = Color(0xFF4169E1)     // Royal blue
private val DeadPurple = Color(0xFF8A2BE2)   // Blue violet

// Custom MiniPlayer background - dark red/maroon blend
private val MiniPlayerBackground = Color(0xFF2D1B1B)  // Dark red-brown

private val DarkColorScheme = darkColorScheme(
    primary = DeadRed,
    onPrimary = Color.White,
    secondary = DeadGold,
    onSecondary = Color.Black,
    tertiary = DeadGreen,
    onTertiary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeadRed,
    onPrimary = Color.White,
    secondary = DeadGold,
    onSecondary = Color.Black,
    tertiary = DeadGreen,
    onTertiary = Color.White,
    background = Color.White,
    surface = Color(0xFFFFFBFE),
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun DeadArchiveTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension to access custom colors
val androidx.compose.material3.ColorScheme.miniPlayerBackground: Color
    get() = MiniPlayerBackground