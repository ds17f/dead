package com.deadarchive.feature.playlist.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.R

/**
 * PlaylistV2AlbumArt - Album artwork component
 * 
 * Clean V2 implementation displaying the steal_your_face image
 * with proper sizing and styling to match V1 appearance.
 */
@Composable
fun PlaylistV2AlbumArt(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.steal_your_face),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}