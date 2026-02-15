package com.deadly.core.backup

import android.util.Log
import com.deadly.core.backup.model.*
import com.deadly.v2.core.database.dao.LibraryDao
import com.deadly.v2.core.database.dao.RecentShowDao
import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.model.V2Database
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationExportService @Inject constructor(
    @V2Database private val showDao: ShowDao,
    @V2Database private val libraryDao: LibraryDao,
    @V2Database private val recentShowDao: RecentShowDao,
    private val v1ShowDao: com.deadly.core.database.ShowDao,
    private val v1RecordingDao: com.deadly.core.database.RecordingDao,
    private val v1PlaybackHistoryDao: com.deadly.core.database.PlaybackHistoryDao
) {

    companion object {
        private const val TAG = "MigrationExportService"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun createMigrationData(appVersion: String): MigrationData {
        val v2Library = exportV2Library()
        val v1Library = exportV1Library()
        val v2RecentPlays = exportV2RecentPlays()
        val v1RecentPlays = exportV1RecentPlays()

        val library = mergeLibrary(v1Library, v2Library)
        val recentPlays = mergeRecentPlays(v1RecentPlays, v2RecentPlays)

        Log.d(TAG, "Migration data: v1=${v1Library.size}+v2=${v2Library.size}→${library.size} library, v1=${v1RecentPlays.size}+v2=${v2RecentPlays.size}→${recentPlays.size} recent")

        return MigrationData(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            library = library,
            recentPlays = recentPlays,
            lastPlayed = null
        )
    }

    fun serialize(data: MigrationData): String = json.encodeToString(data)

    // ---- V2 database exports ----

    private suspend fun exportV2Library(): List<MigrationLibraryShow> {
        val libraryEntries = libraryDao.getAllLibraryShows()
        Log.d(TAG, "Found ${libraryEntries.size} v2 library entries")

        return libraryEntries.mapNotNull { entry ->
            try {
                val show = showDao.getShowById(entry.showId)
                if (show != null) {
                    MigrationLibraryShow(
                        date = show.date,
                        venue = show.venueName,
                        location = show.locationRaw,
                        addedAt = entry.addedToLibraryAt
                    )
                } else {
                    Log.w(TAG, "V2 show not found for library entry: ${entry.showId}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export v2 library show ${entry.showId}: ${e.message}")
                null
            }
        }
    }

    private suspend fun exportV2RecentPlays(): List<MigrationRecentShow> {
        val recentEntries = recentShowDao.getRecentShows(1000)
        Log.d(TAG, "Found ${recentEntries.size} v2 recent entries")

        return recentEntries.mapNotNull { entry ->
            try {
                val show = showDao.getShowById(entry.showId)
                if (show != null) {
                    MigrationRecentShow(
                        date = show.date,
                        venue = show.venueName,
                        location = show.locationRaw,
                        lastPlayedAt = entry.lastPlayedTimestamp,
                        firstPlayedAt = entry.firstPlayedTimestamp,
                        playCount = entry.totalPlayCount
                    )
                } else {
                    Log.w(TAG, "V2 show not found for recent entry: ${entry.showId}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export v2 recent show ${entry.showId}: ${e.message}")
                null
            }
        }
    }

    // ---- V1 database exports ----

    private suspend fun exportV1Library(): List<MigrationLibraryShow> {
        val libraryShows = v1ShowDao.getLibraryShows()
        Log.d(TAG, "Found ${libraryShows.size} v1 library shows")

        return libraryShows.mapNotNull { show ->
            try {
                MigrationLibraryShow(
                    date = show.date,
                    venue = show.venue,
                    location = show.location,
                    addedAt = show.addedToLibraryAt ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export v1 library show ${show.showId}: ${e.message}")
                null
            }
        }
    }

    private suspend fun exportV1RecentPlays(): List<MigrationRecentShow> {
        val history = v1PlaybackHistoryDao.getRecentPlaybackHistory(10000).first()
        Log.d(TAG, "Found ${history.size} v1 playback history entries")
        if (history.isEmpty()) return emptyList()

        // Group by recordingId, then resolve each to a show
        data class ShowPlayStats(
            var firstPlayedAt: Long = Long.MAX_VALUE,
            var lastPlayedAt: Long = 0L,
            var playCount: Int = 0
        )

        val showStatsMap = mutableMapOf<String, ShowPlayStats>() // keyed by concertId

        // Cache recording → concertId lookups
        val recordingToShowCache = mutableMapOf<String, String?>()

        for (entry in history) {
            val concertId = recordingToShowCache.getOrPut(entry.recordingId) {
                try {
                    v1RecordingDao.getRecordingById(entry.recordingId)?.concertId
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to look up recording ${entry.recordingId}: ${e.message}")
                    null
                }
            } ?: continue

            val stats = showStatsMap.getOrPut(concertId) { ShowPlayStats() }
            stats.firstPlayedAt = minOf(stats.firstPlayedAt, entry.playbackTimestamp)
            stats.lastPlayedAt = maxOf(stats.lastPlayedAt, entry.playbackTimestamp)
            stats.playCount++
        }

        Log.d(TAG, "Aggregated v1 playback history into ${showStatsMap.size} shows")

        return showStatsMap.mapNotNull { (concertId, stats) ->
            try {
                val show = v1ShowDao.getShowById(concertId)
                if (show != null) {
                    MigrationRecentShow(
                        date = show.date,
                        venue = show.venue,
                        location = show.location,
                        lastPlayedAt = stats.lastPlayedAt,
                        firstPlayedAt = stats.firstPlayedAt,
                        playCount = stats.playCount
                    )
                } else {
                    Log.w(TAG, "V1 show not found for concertId: $concertId")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export v1 recent show $concertId: ${e.message}")
                null
            }
        }
    }

    // ---- Merge helpers ----

    private fun mergeLibrary(
        v1: List<MigrationLibraryShow>,
        v2: List<MigrationLibraryShow>
    ): List<MigrationLibraryShow> {
        // V2 takes priority; deduplicate by date
        val byDate = LinkedHashMap<String, MigrationLibraryShow>()
        for (show in v2) byDate[show.date] = show
        for (show in v1) byDate.putIfAbsent(show.date, show)
        return byDate.values.toList()
    }

    private fun mergeRecentPlays(
        v1: List<MigrationRecentShow>,
        v2: List<MigrationRecentShow>
    ): List<MigrationRecentShow> {
        // Merge by date, combining play counts and timestamps
        val byDate = LinkedHashMap<String, MigrationRecentShow>()
        for (show in v2) byDate[show.date] = show
        for (show in v1) {
            val existing = byDate[show.date]
            if (existing != null) {
                byDate[show.date] = existing.copy(
                    firstPlayedAt = minOf(existing.firstPlayedAt, show.firstPlayedAt),
                    lastPlayedAt = maxOf(existing.lastPlayedAt, show.lastPlayedAt),
                    playCount = existing.playCount + show.playCount
                )
            } else {
                byDate[show.date] = show
            }
        }
        return byDate.values.toList()
    }
}
