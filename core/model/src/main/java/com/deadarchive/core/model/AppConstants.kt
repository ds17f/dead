package com.deadarchive.core.model

object AppConstants {
    // Archive.org API endpoints
    const val ARCHIVE_BASE_URL = "https://archive.org/"
    const val ARCHIVE_SEARCH_URL = "https://archive.org/advancedsearch.php"
    const val ARCHIVE_METADATA_URL = "https://archive.org/metadata/"
    const val ARCHIVE_DOWNLOAD_URL = "https://archive.org/download/"
    const val ARCHIVE_DETAILS_URL = "https://archive.org/details/"
    
    // Collection identifiers
    const val GRATEFUL_DEAD_COLLECTION = "GratefulDead"
    
    // Search parameters
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val DEFAULT_SEARCH_FIELDS = "identifier,title,date,venue,coverage,source,year,description"
    
    // Audio formats in preference order
    val PREFERRED_AUDIO_FORMATS = listOf("Ogg Vorbis", "VBR MP3", "MP3", "Flac")
    
    // Supported audio file extensions
    val SUPPORTED_AUDIO_EXTENSIONS = setOf("flac", "mp3", "ogg", "wav", "m4a")
    
    // Source types
    val COMMON_SOURCES = listOf(
        "Soundboard" to "SBD",
        "Audience" to "AUD", 
        "FM Broadcast" to "FM",
        "Matrix" to "MTX",
        "Digital Audio Tape" to "DAT"
    )
    
    // Venues and locations (partial list of common ones)
    val COMMON_VENUES = listOf(
        "Fillmore West",
        "Fillmore East", 
        "Winterland",
        "Capitol Theatre",
        "Madison Square Garden",
        "Red Rocks Amphitheatre",
        "Greek Theatre",
        "Warfield Theatre"
    )
    
    // Years range for Grateful Dead
    const val FIRST_YEAR = 1965
    const val LAST_YEAR = 1995
    
    // Database constants
    const val DATABASE_NAME = "dead_archive_db"
    const val DATABASE_VERSION = 1
    
    // Preferences keys
    const val PREF_AUDIO_QUALITY = "audio_quality"
    const val PREF_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_REPEAT_MODE = "repeat_mode"
    const val PREF_SHUFFLE_ENABLED = "shuffle_enabled"
    const val PREF_SHOW_DEBUG_INFO = "show_debug_info"
    
    // Notification constants
    const val NOTIFICATION_CHANNEL_ID = "playback_channel"
    const val NOTIFICATION_ID = 1001
    
    // Error messages
    const val ERROR_NETWORK = "Network error occurred"
    const val ERROR_PLAYBACK = "Playback error occurred"
    const val ERROR_DOWNLOAD = "Download failed"
    const val ERROR_SEARCH = "Search failed"
    const val ERROR_NO_RESULTS = "No results found"
}