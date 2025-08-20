package com.deadly.core.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadly.core.design.R

/**
 * V2TopBar - Reusable top bar component for V2 screens
 * 
 * This component provides consistent height and styling across all V2 screens.
 * It includes the Steal Your Face logo and properly sized layout that matches
 * the SearchV2Screen design pattern.
 * 
 * Used by:
 * - SearchV2Screen
 * - LibraryV2Screen  
 * - PlayerV2Screen (future)
 * - Other V2 implementations
 */
@Composable
fun V2TopBar(
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
                    painter = painterResource(R.drawable.deadly_logo),
                    contentDescription = "Deadly",
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
 * V2TopBarDefaults - Default action buttons for common V2TopBar use cases
 */
object V2TopBarDefaults {
    
    /**
     * Search and Add actions for LibraryV2Screen
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
     * QR Scanner action for SearchV2Screen
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
