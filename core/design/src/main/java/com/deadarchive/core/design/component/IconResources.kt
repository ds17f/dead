package com.deadarchive.core.design.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.deadarchive.core.design.R

/**
 * Centralized resource for all icons used in the DeadArchive application.
 * 
 * This object provides consistent access to Material Icons and custom icons,
 * ensuring consistent icon usage throughout the application.
 * 
 * Usage pattern:
 * - All icons are accessed via @Composable functions that return a Painter
 * - This provides a uniform API whether the icon comes from a drawable or Material Icons
 */
object IconResources {
    /**
     * Navigation icons used in the application.
     */
    object Navigation {
        @Composable
        fun Home() = customIcon(R.drawable.ic_home)
        
        @Composable
        fun HomeOutlined() = vectorIcon(Icons.Outlined.Home)
        
        @Composable
        fun Search() = vectorIcon(Icons.Filled.Search)
        
        @Composable
        fun SearchOutlined() = vectorIcon(Icons.Outlined.Search)
        
        @Composable
        fun Library() = vectorIcon(Icons.Filled.Favorite)
        
        @Composable
        fun LibraryOutlined() = vectorIcon(Icons.Outlined.FavoriteBorder)
        
        @Composable
        fun Settings() = vectorIcon(Icons.Filled.Settings)
        
        @Composable
        fun SettingsOutlined() = vectorIcon(Icons.Outlined.Settings)
        
        @Composable
        fun Add() = vectorIcon(Icons.Filled.Add)
        
        @Composable
        fun Back() = customIcon(R.drawable.ic_arrow_back)
        
        @Composable
        fun Forward() = customIcon(R.drawable.ic_arrow_forward)
        
        @Composable
        fun Close() = customIcon(R.drawable.ic_close)
        
        @Composable
        fun MoreVertical() = customIcon(R.drawable.ic_more_vert)
        
        @Composable
        fun KeyboardArrowDown() = customIcon(R.drawable.ic_keyboard_arrow_down)
        
        @Composable
        fun KeyboardArrowUp() = customIcon(R.drawable.ic_keyboard_arrow_up)
        
        @Composable
        fun KeyboardArrowLeft() = customIcon(R.drawable.ic_keyboard_arrow_left)
        
        @Composable
        fun KeyboardArrowRight() = customIcon(R.drawable.ic_keyboard_arrow_right)
        
        @Composable
        fun ChevronLeft() = customIcon(R.drawable.ic_chevron_left)
        
        @Composable
        fun ChevronRight() = customIcon(R.drawable.ic_chevron_right)
        
        @Composable
        fun ExpandMore() = customIcon(R.drawable.ic_expand_more)
        
        @Composable
        fun ExpandLess() = customIcon(R.drawable.ic_expand_less)
        
        @Composable
        fun Fullscreen() = customIcon(R.drawable.ic_fullscreen)
        
        @Composable
        fun FullscreenExit() = customIcon(R.drawable.ic_fullscreen_exit)
        
        @Composable
        fun Menu() = customIcon(R.drawable.ic_menu)
        // PlayArrow is accessed via the Play() function
        @Composable
        fun playArrow() = customIcon(R.drawable.ic_play_arrow)
        @Composable
        fun pause() = customIcon(R.drawable.ic_pause)
        @Composable
        fun skipNext() = customIcon(R.drawable.ic_skip_next)
        @Composable
        fun skipPrevious() = customIcon(R.drawable.ic_skip_previous)
        @Composable
        fun fastForward() = customIcon(R.drawable.ic_fast_forward)
        @Composable
        fun fastRewind() = customIcon(R.drawable.ic_fast_rewind)
        @Composable
        fun album() = customIcon(R.drawable.ic_album)
        @Composable
        fun queueMusic() = customIcon(R.drawable.ic_queue_music)
        @Composable
        fun repeat() = customIcon(R.drawable.ic_repeat)
        @Composable
        fun repeatOne() = customIcon(R.drawable.ic_repeat_one)
        @Composable
        fun shuffle() = customIcon(R.drawable.ic_shuffle)
        @Composable
        fun volumeUp() = customIcon(R.drawable.ic_volume_up)
        @Composable
        fun volumeDown() = customIcon(R.drawable.ic_volume_down)
        @Composable
        fun volumeOff() = customIcon(R.drawable.ic_volume_off)
        @Composable
        fun volumeMute() = customIcon(R.drawable.ic_volume_mute)
    }

    /**
     * Player control icons used for media playback.
     */
    object PlayerControls {
        @Composable
        fun Play() = customIcon(R.drawable.ic_play_arrow)
        
        @Composable
        fun Pause() = customIcon(R.drawable.ic_pause)
        
        @Composable
        fun SkipNext() = customIcon(R.drawable.ic_skip_next)
        
        @Composable
        fun SkipPrevious() = customIcon(R.drawable.ic_skip_previous)
        
        @Composable
        fun FastForward() = customIcon(R.drawable.ic_fast_forward)
        
        @Composable
        fun FastRewind() = customIcon(R.drawable.ic_fast_rewind)
        
        @Composable
        fun AlbumArt() = customIcon(R.drawable.ic_album)
        
        @Composable
        fun Queue() = customIcon(R.drawable.ic_queue_music)
        
        @Composable
        fun Repeat() = customIcon(R.drawable.ic_repeat)
        
        @Composable
        fun RepeatOne() = customIcon(R.drawable.ic_repeat_one)
        
        @Composable
        fun Shuffle() = customIcon(R.drawable.ic_shuffle)
        
        @Composable
        fun VolumeUp() = customIcon(R.drawable.ic_volume_up)
        
        @Composable
        fun VolumeDown() = customIcon(R.drawable.ic_volume_down)
        
        @Composable
        fun VolumeMute() = customIcon(R.drawable.ic_volume_off)
        
        @Composable
        fun VolumeMute2() = customIcon(R.drawable.ic_volume_mute)
        // Icons are accessed via corresponding composable functions
        @Composable
        fun checkCircle() = customIcon(R.drawable.ic_check_circle)
        @Composable
        fun warning() = customIcon(R.drawable.ic_warning)
        @Composable
        fun info() = customIcon(R.drawable.ic_info)
        @Composable
        fun error() = customIcon(R.drawable.ic_error)
        @Composable
        fun done() = customIcon(R.drawable.ic_done)
        @Composable
        fun doneAll() = customIcon(R.drawable.ic_done_all)
        @Composable
        fun sync() = customIcon(R.drawable.ic_sync)
        @Composable
        fun syncProblem() = customIcon(R.drawable.ic_sync_problem)
        @Composable
        fun refresh() = customIcon(R.drawable.ic_refresh)
    }

    /**
     * Status and notification icons.
     */
    object Status {
        @Composable
        fun CheckCircle() = customIcon(R.drawable.ic_check_circle)
        
        @Composable
        fun Warning() = customIcon(R.drawable.ic_warning)
        
        @Composable
        fun Info() = customIcon(R.drawable.ic_info)
        
        @Composable
        fun Error() = customIcon(R.drawable.ic_error)
        
        @Composable
        fun Done() = customIcon(R.drawable.ic_done)
        
        @Composable
        fun DoneAll() = customIcon(R.drawable.ic_done_all)
        
        @Composable
        fun Sync() = customIcon(R.drawable.ic_sync)
        
        @Composable
        fun SyncProblem() = customIcon(R.drawable.ic_sync_problem)
        
        @Composable
        fun Refresh() = customIcon(R.drawable.ic_refresh)
        
        // For compatibility
        @Composable
        fun Success() = CheckCircle()
        // Icons are accessed via corresponding composable functions
        @Composable
        fun getApp() = customIcon(R.drawable.ic_get_app)
        @Composable
        fun cloudDownload() = customIcon(R.drawable.ic_cloud_download)
        @Composable
        fun fileDownload() = customIcon(R.drawable.ic_file_download)
        @Composable
        fun downloadDone() = customIcon(R.drawable.ic_download_done)
        @Composable
        fun downloadForOffline() = customIcon(R.drawable.ic_download_for_offline)
        @Composable
        fun filePresent() = customIcon(R.drawable.ic_file_present)
        @Composable
        fun fileCopy() = customIcon(R.drawable.ic_file_copy)
        @Composable
        fun folder() = customIcon(R.drawable.ic_folder)
        @Composable
        fun folderOpen() = customIcon(R.drawable.ic_folder_open)
        @Composable
        fun libraryMusic() = customIcon(R.drawable.ic_library_music)
        @Composable
        fun queue() = customIcon(R.drawable.ic_queue)
        @Composable
        fun star() = customIcon(R.drawable.ic_star)
        @Composable
        fun starBorder() = customIcon(R.drawable.ic_star_border)
        @Composable
        fun starHalf() = customIcon(R.drawable.ic_star_half)
        @Composable
        fun favorite() = customIcon(R.drawable.ic_favorite)
        @Composable
        fun favoriteBorder() = customIcon(R.drawable.ic_favorite_border)
        @Composable
        fun search() = customIcon(R.drawable.ic_search)
        @Composable
        fun playlistAdd() = customIcon(R.drawable.ic_playlist_add)
        @Composable
        fun playlistAddCheck() = customIcon(R.drawable.ic_playlist_add_check)
        @Composable
        fun playlistPlay() = customIcon(R.drawable.ic_playlist_play)
        @Composable
        fun addCircle() = customIcon(R.drawable.ic_add_circle)
        @Composable
        fun addCircleOutline() = customIcon(R.drawable.ic_add_circle_outline)
        @Composable
        fun share() = customIcon(R.drawable.ic_share)
        @Composable
        fun trendingUp() = customIcon(R.drawable.ic_trending_up)
    }
    
    /**
     * Content type icons.
     */
    object Content {
        @Composable
        fun GetApp() = customIcon(R.drawable.ic_get_app)
        
        @Composable
        fun CloudDownload() = customIcon(R.drawable.ic_cloud_download)
        
        @Composable
        fun FileDownload() = customIcon(R.drawable.ic_file_download)
        
        @Composable
        fun DownloadDone() = customIcon(R.drawable.ic_download_done)
        
        @Composable
        fun DownloadForOffline() = customIcon(R.drawable.ic_download_for_offline)
        
        @Composable
        fun FilePresent() = customIcon(R.drawable.ic_file_present)
        
        @Composable
        fun FileCopy() = customIcon(R.drawable.ic_file_copy)
        
        @Composable
        fun Folder() = customIcon(R.drawable.ic_folder)
        
        @Composable
        fun FolderOpen() = customIcon(R.drawable.ic_folder_open)
        
        @Composable
        fun LibraryMusic() = customIcon(R.drawable.ic_library_music)
        
        @Composable
        fun Queue() = customIcon(R.drawable.ic_queue)
        
        @Composable
        fun Star() = customIcon(R.drawable.ic_star)
        
        @Composable
        fun StarBorder() = customIcon(R.drawable.ic_star_border)
        
        @Composable
        fun StarHalf() = customIcon(R.drawable.ic_star_half)
        
        @Composable
        fun Favorite() = customIcon(R.drawable.ic_favorite)
        
        @Composable
        fun FavoriteBorder() = customIcon(R.drawable.ic_favorite_border)
        
        @Composable
        fun Search() = customIcon(R.drawable.ic_search)
        
        @Composable
        fun PlaylistAdd() = customIcon(R.drawable.ic_playlist_add)
        
        @Composable
        fun PlaylistAddCheck() = customIcon(R.drawable.ic_playlist_add_check)
        
        @Composable
        fun PlaylistPlay() = customIcon(R.drawable.ic_playlist_play)
        
        @Composable
        fun AddCircle() = customIcon(R.drawable.ic_add_circle)
        
        @Composable
        fun AddCircleOutline() = customIcon(R.drawable.ic_add_circle_outline)
        
        @Composable
        fun Share() = customIcon(R.drawable.ic_share)
        
        @Composable
        fun TrendingUp() = customIcon(R.drawable.ic_trending_up)
        
    }

    /**
     * Get a custom icon from drawable resources.
     *
     * @param resId The resource ID of the drawable
     * @return A Painter for the resource
     */
    @Composable
    fun customIcon(@DrawableRes resId: Int): Painter {
        return painterResource(id = resId)
    }
    
    /**
     * Get a Material Icons ImageVector as a Composable function.
     * This allows us to provide a consistent API for all icons.
     *
     * @param imageVector The Material Icons ImageVector
     * @return A Composable function that renders the ImageVector
     */
    @Composable
    fun vectorIcon(imageVector: ImageVector): Painter {
        return androidx.compose.ui.graphics.vector.rememberVectorPainter(image = imageVector)
    }

    /**
     * Icon size definitions for consistent sizing throughout the app.
     */
    object Size {
        val SMALL = 16.dp
        val MEDIUM = 24.dp
        val LARGE = 32.dp
        val XLARGE = 48.dp
    }
        // Icons are accessed via corresponding composable functions
        @Composable
        fun arrowBack() = customIcon(R.drawable.ic_arrow_back)
        @Composable
        fun arrowForward() = customIcon(R.drawable.ic_arrow_forward)
        @Composable
        fun close() = customIcon(R.drawable.ic_close)
        @Composable
        fun home() = customIcon(R.drawable.ic_home)
        @Composable
        fun keyboardArrowDown() = customIcon(R.drawable.ic_keyboard_arrow_down)
        @Composable
        fun keyboardArrowUp() = customIcon(R.drawable.ic_keyboard_arrow_up)
        @Composable
        fun keyboardArrowLeft() = customIcon(R.drawable.ic_keyboard_arrow_left)
        @Composable
        fun keyboardArrowRight() = customIcon(R.drawable.ic_keyboard_arrow_right)
        @Composable
        fun moreVert() = customIcon(R.drawable.ic_more_vert)
        @Composable
        fun menu() = customIcon(R.drawable.ic_menu)
        @Composable
        fun chevronLeft() = customIcon(R.drawable.ic_chevron_left)
        @Composable
        fun chevronRight() = customIcon(R.drawable.ic_chevron_right)
        @Composable
        fun expandMore() = customIcon(R.drawable.ic_expand_more)
        @Composable
        fun expandLess() = customIcon(R.drawable.ic_expand_less)
        @Composable
        fun fullscreen() = customIcon(R.drawable.ic_fullscreen)
        @Composable
        fun fullscreenExit() = customIcon(R.drawable.ic_fullscreen_exit)
}