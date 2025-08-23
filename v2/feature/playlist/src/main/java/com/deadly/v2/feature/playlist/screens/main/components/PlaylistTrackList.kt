package com.deadly.v2.feature.playlist.screens.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deadly.v2.core.model.PlaylistTrackViewModel
import android.util.Log

/**
 * PlaylistV2TrackList - Scrollable track listing component
 * 
 * Clean V2 implementation as LazyListScope extension for integration
 * with main PlaylistV2Screen LazyColumn. Displays track list with
 * section header and individual track items.
 */
fun LazyListScope.PlaylistTrackList(
    tracks: List<PlaylistTrackViewModel>,
    onPlayClick: (PlaylistTrackViewModel) -> Unit,
    onDownloadClick: (PlaylistTrackViewModel) -> Unit
) {

    // // Debug log all keys
    // tracks.forEach { track ->
    //     Log.d("PlaylistTrackList", "(number=${track.number}, title=${track.title}, format=${track.format}")
    // }

    // TODO: Don't the format, we need to be smart about this
    val filteredTracks = tracks.filter { it.format == "VBR MP3" }

    // // Debug log all keys
    // filteredTracks.forEach { track ->
    //     val key = "${track.number}_${track.title}"
    //     Log.d("PlaylistTrackList", "Track key: $key  (number=${track.number}, title=${track.title})")
    // }

    // Section header - matches V1 format exactly
    item {
        Text(
            text = "Tracks (${filteredTracks.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
    
    // Track items - simple list like V1
    items(
        items = filteredTracks,
        key = { track -> track.number }
    ) { track ->
        PlaylistTrackItem(
            track = track,
            onPlayClick = onPlayClick,
            onDownloadClick = onDownloadClick
        )
    }
    
    // Bottom spacing
    item {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
