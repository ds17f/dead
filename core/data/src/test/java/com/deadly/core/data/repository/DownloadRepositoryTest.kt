package com.deadly.core.data.repository

import com.deadly.core.database.DownloadDao
import com.deadly.core.model.AudioFile
import com.deadly.core.model.Concert
import com.deadly.core.model.DownloadStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Simplified tests focusing on behavior rather than implementation details.
 * Tests what the system DOES, not HOW it does it.
 */
class DownloadRepositoryTest {

    private lateinit var mockDownloadDao: DownloadDao
    private lateinit var mockConcertRepository: ConcertRepository
    private lateinit var repository: DownloadRepositoryImpl

    @Before
    fun setup() {
        mockDownloadDao = mockk(relaxed = true)
        mockConcertRepository = mockk(relaxed = true)
        repository = DownloadRepositoryImpl(mockDownloadDao, mockConcertRepository)
    }

    // Behavior-focused tests - what matters to users

    @Test
    fun `can start new download`() = runTest {
        // Given
        val concert = createTestConcert("gd1977", "Cornell")
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null

        // When
        val downloadId = repository.startDownload(concert, "track1.flac")

        // Then - verify a download was started
        assertThat(downloadId).isNotEmpty()
        coVerify { mockDownloadDao.insertDownload(any()) }
    }

    @Test
    fun `can start multiple downloads for concert`() = runTest {
        // Given
        val concert = createTestConcert("gd1977", "Cornell")
        val trackUrls = listOf(
            createTestAudioFile("track1.flac") to "https://example.com/track1.flac",
            createTestAudioFile("track2.flac") to "https://example.com/track2.flac"
        )
        coEvery { mockConcertRepository.getTrackStreamingUrls("gd1977") } returns trackUrls
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null

        // When
        val downloadIds = repository.startConcertDownload(concert)

        // Then - should create multiple downloads
        assertThat(downloadIds).hasSize(2)
        coVerify(exactly = 2) { mockDownloadDao.insertDownload(any()) }
    }

    @Test
    fun `can restart failed download`() = runTest {
        // Given - existing failed download
        val concert = createTestConcert("gd1977", "Cornell")
        val failedDownload = mockk<com.deadly.core.database.DownloadEntity>(relaxed = true) {
            io.mockk.every { status } returns "FAILED"
        }
        coEvery { mockDownloadDao.getDownloadById(any()) } returns failedDownload

        // When
        repository.startDownload(concert, "track1.flac")

        // Then - should update the existing download
        coVerify { mockDownloadDao.updateDownload(any()) }
    }

    @Test
    fun `does not interfere with active downloads`() = runTest {
        // Given - existing active download
        val concert = createTestConcert("gd1977", "Cornell")
        val activeDownload = mockk<com.deadly.core.database.DownloadEntity>(relaxed = true) {
            io.mockk.every { status } returns "DOWNLOADING"
        }
        coEvery { mockDownloadDao.getDownloadById(any()) } returns activeDownload

        // When
        repository.startDownload(concert, "track1.flac")

        // Then - should not modify active downloads
        coVerify(exactly = 0) { mockDownloadDao.insertDownload(any()) }
        coVerify(exactly = 0) { mockDownloadDao.updateDownload(any()) }
    }

    @Test
    fun `can update download progress`() = runTest {
        // When
        repository.updateDownloadProgress("test_id", 0.5f, 500L)

        // Then
        coVerify { mockDownloadDao.updateDownloadProgress("test_id", 0.5f, 500L) }
    }

    @Test
    fun `can pause download`() = runTest {
        // When
        repository.pauseDownload("test_id")

        // Then - should update status to PAUSED
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "PAUSED", null) }
    }

    @Test
    fun `can resume download`() = runTest {
        // When
        repository.resumeDownload("test_id")

        // Then - should update status to QUEUED
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "QUEUED", null) }
    }

    @Test
    fun `can cancel download`() = runTest {
        // When
        repository.cancelDownload("test_id")

        // Then - should update status to CANCELLED
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "CANCELLED", null) }
    }

    @Test
    fun `completion updates timestamp`() = runTest {
        // When
        repository.updateDownloadStatus("test_id", DownloadStatus.COMPLETED)

        // Then - should set completion timestamp
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "COMPLETED", any()) }
    }

    @Test
    fun `non-completion states do not update timestamp`() = runTest {
        // When
        repository.updateDownloadStatus("test_id", DownloadStatus.PAUSED)
        repository.updateDownloadStatus("test_id", DownloadStatus.FAILED)

        // Then - should not set timestamp
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "PAUSED", null) }
        coVerify { mockDownloadDao.updateDownloadStatus("test_id", "FAILED", null) }
    }

    // Note: isTrackDownloaded() and getLocalFilePath() are simple delegation methods
    // that just call DAO and check status. These are better tested via integration 
    // tests with real database operations rather than complex mock setups.

    // Helper methods
    private fun createTestConcert(identifier: String, title: String): Concert {
        return Concert(
            identifier = identifier,
            title = title,
            date = "1977-05-08",
            venue = "Test Venue",
            location = "Test Location"
        )
    }

    private fun createTestAudioFile(filename: String): AudioFile {
        return AudioFile(
            filename = filename,
            format = "FLAC",
            sizeBytes = "1000000",
            durationSeconds = "180"
        )
    }

}