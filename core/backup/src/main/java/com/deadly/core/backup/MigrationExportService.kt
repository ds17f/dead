package com.deadly.core.backup

import android.util.Log
import com.deadly.core.backup.model.*
import com.deadly.v2.core.database.dao.LibraryDao
import com.deadly.v2.core.database.dao.RecentShowDao
import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.model.V2Database
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationExportService @Inject constructor(
    @V2Database private val showDao: ShowDao,
    @V2Database private val libraryDao: LibraryDao,
    @V2Database private val recentShowDao: RecentShowDao
) {

    companion object {
        private const val TAG = "MigrationExportService"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun createMigrationData(appVersion: String): MigrationData {
        val library = exportLibrary()
        val recentPlays = exportRecentPlays()

        Log.d(TAG, "Migration data: ${library.size} library, ${recentPlays.size} recent")

        return MigrationData(
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
            library = library,
            recentPlays = recentPlays,
            lastPlayed = null
        )
    }

    fun serialize(data: MigrationData): String = json.encodeToString(data)

    private suspend fun exportLibrary(): List<MigrationLibraryShow> {
        val libraryEntries = libraryDao.getAllLibraryShows()
        Log.d(TAG, "Found ${libraryEntries.size} library entries")

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
                    Log.w(TAG, "Show not found for library entry: ${entry.showId}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export library show ${entry.showId}: ${e.message}")
                null
            }
        }
    }

    private suspend fun exportRecentPlays(): List<MigrationRecentShow> {
        val recentEntries = recentShowDao.getRecentShows(1000)
        Log.d(TAG, "Found ${recentEntries.size} recent entries")

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
                    Log.w(TAG, "Show not found for recent entry: ${entry.showId}")
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export recent show ${entry.showId}: ${e.message}")
                null
            }
        }
    }
}
