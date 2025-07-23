package com.deadarchive.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Floating debug activation button that appears when debug mode is enabled.
 * Positioned in the bottom-right corner of the screen.
 */
@Composable
fun DebugActivator(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    // Fixed: Don't create a Box that fills the entire screen - just render the button directly
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .zIndex(999f),
        containerColor = Color(0xFFFF5722),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Text(
            text = "🐛",
            fontSize = 20.sp
        )
    }
}

/**
 * Alternative debug activator as a small corner button
 */
@Composable
fun DebugCornerButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .zIndex(999f),
            shape = CircleShape,
            color = Color(0xFFFF5722).copy(alpha = 0.8f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🐛",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Debug info overlay that shows debug status in corner
 */
@Composable
fun DebugStatusIndicator(
    isVisible: Boolean,
    isDebugPanelOpen: Boolean,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .zIndex(998f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            color = if (isDebugPanelOpen) {
                Color(0xFFFF5722).copy(alpha = 0.9f)
            } else {
                Color(0xFFFF5722).copy(alpha = 0.6f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🐛",
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "$itemCount",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}