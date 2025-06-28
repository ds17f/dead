package com.deadarchive.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.model.DownloadStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {

    private lateinit var database: DeadArchiveDatabase
    private lateinit var downloadDao: DownloadDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        ).allowMainThreadQueries().build()
        
        downloadDao = database.downloadDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertDownload_storesAndRetrievesDownload() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.QUEUED)
        
        // When
        downloadDao.insertDownload(download)
        
        // Then
        val retrieved = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.id).isEqualTo("test1_track1.mp3")
        assertThat(retrieved.concertIdentifier).isEqualTo("test1")
        assertThat(retrieved.trackFilename).isEqualTo("track1.mp3")
        assertThat(retrieved.status).isEqualTo("QUEUED")
        assertThat(retrieved.progress).isEqualTo(0f)
    }

    @Test
    fun insertMultipleDownloads_storesAllCorrectly() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test2_track1.mp3", "test2", "track1.mp3", DownloadStatus.QUEUED)
        )
        
        // When
        downloadDao.insertDownloads(downloads)
        
        // Then
        val allDownloads = downloadDao.getAllDownloads().first()
        assertThat(allDownloads).hasSize(3)
        
        val completedDownloads = allDownloads.filter { it.status == "COMPLETED" }
        val downloadingDownloads = allDownloads.filter { it.status == "DOWNLOADING" }
        val queuedDownloads = allDownloads.filter { it.status == "QUEUED" }
        
        assertThat(completedDownloads).hasSize(1)
        assertThat(downloadingDownloads).hasSize(1)
        assertThat(queuedDownloads).hasSize(1)
    }

    @Test
    fun getAllDownloads_returnsAllDownloads() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test2_track1.mp3", "test2", "track1.mp3", DownloadStatus.DOWNLOADING)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        val result = downloadDao.getAllDownloads().first()
        
        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly("test1_track1.mp3", "test2_track1.mp3")
    }

    @Test
    fun getDownloadsByStatus_filtersCorrectly() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test2_track1.mp3", "test2", "track1.mp3", DownloadStatus.COMPLETED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        val completedDownloads = downloadDao.getDownloadsByStatus("COMPLETED").first()
        val downloadingDownloads = downloadDao.getDownloadsByStatus("DOWNLOADING").first()
        
        // Then
        assertThat(completedDownloads).hasSize(2)
        assertThat(completedDownloads.all { it.status == "COMPLETED" }).isTrue()
        
        assertThat(downloadingDownloads).hasSize(1)
        assertThat(downloadingDownloads[0].status).isEqualTo("DOWNLOADING")
    }

    @Test
    fun getDownloadsForConcert_filtersCorrectly() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test2_track1.mp3", "test2", "track1.mp3", DownloadStatus.QUEUED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        val test1Downloads = downloadDao.getDownloadsForConcert("test1").first()
        val test2Downloads = downloadDao.getDownloadsForConcert("test2").first()
        
        // Then
        assertThat(test1Downloads).hasSize(2)
        assertThat(test1Downloads.all { it.concertIdentifier == "test1" }).isTrue()
        assertThat(test1Downloads.map { it.trackFilename }).containsExactly("track1.mp3", "track2.mp3")
        
        assertThat(test2Downloads).hasSize(1)
        assertThat(test2Downloads[0].concertIdentifier).isEqualTo("test2")
    }

    @Test
    fun getActiveDownloads_returnsOnlyActiveStatuses() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.QUEUED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track3.mp3", "test1", "track3.mp3", DownloadStatus.PAUSED),
            createTestDownloadEntity("test1_track4.mp3", "test1", "track4.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track5.mp3", "test1", "track5.mp3", DownloadStatus.FAILED),
            createTestDownloadEntity("test1_track6.mp3", "test1", "track6.mp3", DownloadStatus.CANCELLED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        val activeDownloads = downloadDao.getActiveDownloads().first()
        
        // Then
        assertThat(activeDownloads).hasSize(3) // QUEUED, DOWNLOADING, PAUSED
        val activeStatuses = activeDownloads.map { it.status }
        assertThat(activeStatuses).containsExactly("QUEUED", "DOWNLOADING", "PAUSED")
    }

    @Test
    fun getCompletedDownloads_returnsOnlyCompleted() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track3.mp3", "test1", "track3.mp3", DownloadStatus.COMPLETED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        val completedDownloads = downloadDao.getCompletedDownloads().first()
        
        // Then
        assertThat(completedDownloads).hasSize(2)
        assertThat(completedDownloads.all { it.status == "COMPLETED" }).isTrue()
    }

    @Test
    fun updateDownload_modifiesExistingDownload() = runTest {
        // Given
        val original = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.QUEUED)
        downloadDao.insertDownload(original)
        
        // When
        val updated = original.copy(
            status = DownloadStatus.DOWNLOADING.name,
            progress = 0.5f,
            bytesDownloaded = 500L
        )
        downloadDao.updateDownload(updated)
        
        // Then
        val result = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(result).isNotNull()
        assertThat(result!!.status).isEqualTo("DOWNLOADING")
        assertThat(result.progress).isEqualTo(0.5f)
        assertThat(result.bytesDownloaded).isEqualTo(500L)
    }

    @Test
    fun deleteDownload_removesDownload() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        downloadDao.insertDownload(download)
        
        // Verify it exists
        assertThat(downloadDao.getDownloadById("test1_track1.mp3")).isNotNull()
        
        // When
        downloadDao.deleteDownload(download)
        
        // Then
        assertThat(downloadDao.getDownloadById("test1_track1.mp3")).isNull()
    }

    @Test
    fun deleteDownloadById_removesDownload() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        downloadDao.insertDownload(download)
        
        // When
        downloadDao.deleteDownloadById("test1_track1.mp3")
        
        // Then
        assertThat(downloadDao.getDownloadById("test1_track1.mp3")).isNull()
    }

    @Test
    fun deleteCompletedDownloads_removesOnlyCompleted() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track3.mp3", "test1", "track3.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track4.mp3", "test1", "track4.mp3", DownloadStatus.FAILED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        downloadDao.deleteCompletedDownloads()
        
        // Then
        val remaining = downloadDao.getAllDownloads().first()
        assertThat(remaining).hasSize(2) // Only DOWNLOADING and FAILED should remain
        assertThat(remaining.map { it.status }).containsExactly("DOWNLOADING", "FAILED")
    }

    @Test
    fun deleteFailedDownloads_removesOnlyFailed() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.FAILED),
            createTestDownloadEntity("test1_track3.mp3", "test1", "track3.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track4.mp3", "test1", "track4.mp3", DownloadStatus.FAILED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        downloadDao.deleteFailedDownloads()
        
        // Then
        val remaining = downloadDao.getAllDownloads().first()
        assertThat(remaining).hasSize(2) // Only COMPLETED and DOWNLOADING should remain
        assertThat(remaining.map { it.status }).containsExactly("COMPLETED", "DOWNLOADING")
    }

    @Test
    fun cancelAllActiveDownloads_updatesActiveStatuses() = runTest {
        // Given
        val downloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.QUEUED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track3.mp3", "test1", "track3.mp3", DownloadStatus.PAUSED),
            createTestDownloadEntity("test1_track4.mp3", "test1", "track4.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track5.mp3", "test1", "track5.mp3", DownloadStatus.FAILED)
        )
        downloadDao.insertDownloads(downloads)
        
        // When
        downloadDao.cancelAllActiveDownloads()
        
        // Then
        val allDownloads = downloadDao.getAllDownloads().first()
        val cancelledDownloads = allDownloads.filter { it.status == "CANCELLED" }
        val unchangedDownloads = allDownloads.filter { it.status in listOf("COMPLETED", "FAILED") }
        
        assertThat(cancelledDownloads).hasSize(3) // QUEUED, DOWNLOADING, PAUSED -> CANCELLED
        assertThat(unchangedDownloads).hasSize(2) // COMPLETED and FAILED unchanged
    }

    @Test
    fun updateDownloadProgress_updatesProgressAndBytes() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        downloadDao.insertDownload(download)
        
        // When
        downloadDao.updateDownloadProgress("test1_track1.mp3", 0.75f, 750L)
        
        // Then
        val updated = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(updated).isNotNull()
        assertThat(updated!!.progress).isEqualTo(0.75f)
        assertThat(updated.bytesDownloaded).isEqualTo(750L)
        // Other fields should remain unchanged
        assertThat(updated.status).isEqualTo("DOWNLOADING")
        assertThat(updated.concertIdentifier).isEqualTo("test1")
    }

    @Test
    fun updateDownloadStatus_updatesStatusAndTimestamp() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        downloadDao.insertDownload(download)
        
        val completionTime = System.currentTimeMillis()
        
        // When
        downloadDao.updateDownloadStatus("test1_track1.mp3", "COMPLETED", completionTime)
        
        // Then
        val updated = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(updated).isNotNull()
        assertThat(updated!!.status).isEqualTo("COMPLETED")
        assertThat(updated.completedTimestamp).isEqualTo(completionTime)
    }

    @Test
    fun updateDownloadStatus_withoutTimestamp_updatesOnlyStatus() = runTest {
        // Given
        val download = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        downloadDao.insertDownload(download)
        
        // When
        downloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null)
        
        // Then
        val updated = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(updated).isNotNull()
        assertThat(updated!!.status).isEqualTo("PAUSED")
        assertThat(updated.completedTimestamp).isNull()
    }

    @Test
    fun downloadEntity_convertsToDownloadStateCorrectly() = runTest {
        // Given
        val entity = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
            .copy(
                progress = 0.6f,
                bytesDownloaded = 600L,
                totalBytes = 1000L,
                localPath = "/storage/music/track1.mp3",
                errorMessage = null
            )
        downloadDao.insertDownload(entity)
        
        // When
        val stored = downloadDao.getDownloadById("test1_track1.mp3")!!
        val downloadState = stored.toDownloadState()
        
        // Then
        assertThat(downloadState.id).isEqualTo("test1_track1.mp3")
        assertThat(downloadState.concertIdentifier).isEqualTo("test1")
        assertThat(downloadState.trackFilename).isEqualTo("track1.mp3")
        assertThat(downloadState.status).isEqualTo(DownloadStatus.DOWNLOADING)
        assertThat(downloadState.progress).isEqualTo(0.6f)
        assertThat(downloadState.progressPercentage).isEqualTo(60)
        assertThat(downloadState.bytesDownloaded).isEqualTo(600L)
        assertThat(downloadState.totalBytes).isEqualTo(1000L)
        assertThat(downloadState.localPath).isEqualTo("/storage/music/track1.mp3")
        assertThat(downloadState.isInProgress).isTrue()
        assertThat(downloadState.isCompleted).isFalse()
        assertThat(downloadState.isFailed).isFalse()
    }

    @Test
    fun insertDownload_withConflict_replacesExisting() = runTest {
        // Given
        val original = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.QUEUED)
            .copy(progress = 0.2f)
        downloadDao.insertDownload(original)
        
        // When - Insert with same ID but different data
        val replacement = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
            .copy(progress = 0.8f)
        downloadDao.insertDownload(replacement)
        
        // Then
        val result = downloadDao.getDownloadById("test1_track1.mp3")
        assertThat(result).isNotNull()
        assertThat(result!!.status).isEqualTo("DOWNLOADING")
        assertThat(result.progress).isEqualTo(0.8f)
        
        // Should still be only one download total
        val allDownloads = downloadDao.getAllDownloads().first()
        assertThat(allDownloads).hasSize(1)
    }

    // Helper method for creating test data
    private fun createTestDownloadEntity(
        id: String,
        concertId: String,
        trackFilename: String,
        status: DownloadStatus,
        progress: Float = 0f,
        bytesDownloaded: Long = 0L,
        totalBytes: Long = 1000L
    ): DownloadEntity {
        return DownloadEntity(
            id = id,
            concertIdentifier = concertId,
            trackFilename = trackFilename,
            status = status.name,
            progress = progress,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            startedTimestamp = System.currentTimeMillis()
        )
    }
}