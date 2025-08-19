package com.deadarchive.v2.core.design.component.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadarchive.v2.core.design.R
import com.deadarchive.v2.core.design.component.IconResources

/**
 * TopBar - Reusable top bar component for V2 screens
 * 
 * This component provides consistent height and styling across all V2 screens.
 * It includes the Steal Your Face logo and properly sized layout that matches
 * the V2 design system.
 * 
 * Used by:
 * - SearchScreen
 * - LibraryScreen  
 * - PlayerScreen (future)
 * - Other V2 implementations
 */
@Composable
fun TopBar(
    title: String? = null,
    titleContent: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: SYF logo + (title OR titleContent)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.steal_your_face),
                    contentDescription = "Dead Archive",
                    modifier = Modifier.size(32.dp)
                )
                
                // Backward compatible: title string or custom content
                when {
                    title != null -> {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    titleContent != null -> {
                        titleContent()
                    }
                }
            }
            
            // Right side: Action buttons (customizable)
            Row(content = actions)
        }
    }
}

/**
 * TopBarDefaults - Default action buttons for common TopBar use cases
 */
object TopBarDefaults {
    
    /**
     * Search and Add actions for LibraryScreen
     */
    @Composable
    fun LibraryActions(
        onSearchClick: () -> Unit,
        onAddClick: () -> Unit
    ): @Composable RowScope.() -> Unit = {
        IconButton(onClick = onSearchClick) {
            Icon(
                painter = IconResources.Content.Search(),
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onAddClick) {
            Icon(
                painter = IconResources.Navigation.Add(),
                contentDescription = "Add to Library",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    
    /**
     * QR Scanner action for SearchScreen
     */
    @Composable
    fun SearchActions(
        onCameraClick: () -> Unit
    ): @Composable RowScope.() -> Unit = {
        IconButton(onClick = onCameraClick) {
            Icon(
                painter = IconResources.Content.QrCodeScanner(),
                contentDescription = "QR Code Scanner",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}