package com.deadly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.deadly.core.design.theme.DeadArchiveTheme
import com.deadly.core.settings.api.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.OptIn
import java.io.File
import com.deadly.app.DeadArchiveNavigation as V1Navigation
import com.deadly.v2.app.MainNavigation as V2Navigation

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    /**
     * Check if V2 app is enabled via file-based toggle.
     * If the enable-v2-app file exists in the app's files directory, use V2 app.
     * Otherwise, fall back to V1 app.
     */
    private fun shouldUseV2App(): Boolean {
        val toggleFile = File(filesDir, "enable-v2-app")
        return toggleFile.exists()
    }
    
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Ensure status bar is transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val settings by settingsRepository.getSettings().collectAsState(initial = com.deadly.core.settings.api.model.AppSettings())
            
            DeadArchiveTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // File-based toggle between V1 and V2 apps
                    if (shouldUseV2App()) {
                        // Use V2 app - completely independent navigation
                        V2Navigation()
                    } else {
                        // Use V1 app - existing navigation
                        V1Navigation(
                            showSplash = true,
                            settings = settings
                        )
                    }
                }
            }
        }
    }
}