package com.deadarchive.core.data.repository

import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.DownloadStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests focusing on DownloadRepository business logic:
 * - Download ID generation algorithms
 * - State machine transitions for download lifecycle
 * - Download restart and recovery logic
 * - Business rule validation
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

    // Download ID Generation Business Logic Tests

    @Test
    fun `startDownload generates correct ID format`() = runTest {
        // Given
        val concert = createTestConcert("gd1977-05-08", "Cornell '77")
        val trackFilename = "gd77-05-08d1t01.flac"
        val entitySlot = slot<DownloadEntity>()
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null
        coEvery { mockDownloadDao.insertDownload(capture(entitySlot)) } returns Unit

        // When
        val result = repository.startDownload(concert, trackFilename)

        // Then - Verify ID generation algorithm
        assertThat(result).isEqualTo("gd1977-05-08_gd77-05-08d1t01.flac")
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("gd1977-05-08_gd77-05-08d1t01.flac")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("gd1977-05-08")
        assertThat(capturedEntity.trackFilename).isEqualTo("gd77-05-08d1t01.flac")
    }

    @Test
    fun `ID generation handles special characters in concert and track names`() = runTest {
        // Given
        val concert = createTestConcert("gd1977-05-08.sbd.miller.89174.sbeok.flac16", "Cornell")
        val trackFilename = "gd77-05-08d1t01.shn"
        val entitySlot = slot<DownloadEntity>()
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null
        coEvery { mockDownloadDao.insertDownload(capture(entitySlot)) } returns Unit

        // When
        val result = repository.startDownload(concert, trackFilename)

        // Then - ID should preserve special characters
        val expectedId = "gd1977-05-08.sbd.miller.89174.sbeok.flac16_gd77-05-08d1t01.shn"
        assertThat(result).isEqualTo(expectedId)
        assertThat(entitySlot.captured.id).isEqualTo(expectedId)
    }

    // Download State Machine Logic Tests

    @Test
    fun `startDownload creates new download in QUEUED state`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val entitySlot = slot<DownloadEntity>()
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns null
        coEvery { mockDownloadDao.insertDownload(capture(entitySlot)) } returns Unit

        // When
        repository.startDownload(concert, "track1.mp3")

        // Then - New download should start in QUEUED state
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.status).isEqualTo("QUEUED")
        assertThat(capturedEntity.progress).isEqualTo(0f)
        assertThat(capturedEntity.bytesDownloaded).isEqualTo(0L)
        assertThat(capturedEntity.errorMessage).isNull()
    }

    @Test
    fun `startDownload restarts failed download correctly`() = runTest {
        // Given - Existing failed download
        val concert = createTestConcert("test1", "Test Concert")
        val failedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.FAILED)
            .copy(progress = 0.5f, bytesDownloaded = 500L, errorMessage = "Network timeout")
        val updatedEntitySlot = slot<DownloadEntity>()
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns failedDownload
        coEvery { mockDownloadDao.updateDownload(capture(updatedEntitySlot)) } returns Unit

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then - Should reset download state
        assertThat(result).isEqualTo("test1_track1.mp3")
        val updatedEntity = updatedEntitySlot.captured
        assertThat(updatedEntity.status).isEqualTo("QUEUED")
        assertThat(updatedEntity.progress).isEqualTo(0f)
        assertThat(updatedEntity.bytesDownloaded).isEqualTo(0L)
        assertThat(updatedEntity.errorMessage).isNull()
        assertThat(updatedEntity.startedTimestamp).isNotEqualTo(failedDownload.startedTimestamp)
    }

    @Test
    fun `startDownload restarts cancelled download correctly`() = runTest {
        // Given - Existing cancelled download
        val concert = createTestConcert("test1", "Test Concert")
        val cancelledDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.CANCELLED)
        val updatedEntitySlot = slot<DownloadEntity>()
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns cancelledDownload
        coEvery { mockDownloadDao.updateDownload(capture(updatedEntitySlot)) } returns Unit

        // When
        repository.startDownload(concert, "track1.mp3")

        // Then - Should reset to QUEUED state
        val updatedEntity = updatedEntitySlot.captured
        assertThat(updatedEntity.status).isEqualTo("QUEUED")
    }

    @Test
    fun `startDownload does not modify active downloads`() = runTest {
        // Given - Existing active download
        val concert = createTestConcert("test1", "Test Concert")
        val activeDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns activeDownload

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then - Should return existing ID without modification
        assertThat(result).isEqualTo("test1_track1.mp3")
        coVerify(exactly = 0) { mockDownloadDao.insertDownload(any()) }
        coVerify(exactly = 0) { mockDownloadDao.updateDownload(any()) }
    }

    @Test
    fun `startDownload does not modify completed downloads`() = runTest {
        // Given - Existing completed download
        val concert = createTestConcert("test1", "Test Concert")
        val completedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns completedDownload

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then - Should return existing ID without modification
        assertThat(result).isEqualTo("test1_track1.mp3")
        coVerify(exactly = 0) { mockDownloadDao.insertDownload(any()) }
        coVerify(exactly = 0) { mockDownloadDao.updateDownload(any()) }
    }

    // Batch Download Logic Tests

    @Test
    fun `startConcertDownload creates downloads for all tracks`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val trackUrls = listOf(
            createTestAudioFile("track1.mp3") to "https://example.com/track1.mp3",
            createTestAudioFile("track2.mp3") to "https://example.com/track2.mp3",
            createTestAudioFile("track3.flac") to "https://example.com/track3.flac"
        )
        
        coEvery { mockConcertRepository.getTrackStreamingUrls("test1") } returns trackUrls
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null
        coEvery { mockDownloadDao.insertDownload(any()) } returns Unit

        // When
        val result = repository.startConcertDownload(concert)

        // Then - Should create download for each track with correct IDs
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly(
            "test1_track1.mp3",
            "test1_track2.mp3", 
            "test1_track3.flac"
        )
        coVerify(exactly = 3) { mockDownloadDao.insertDownload(any()) }
    }

    @Test
    fun `startConcertDownload handles empty track list`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockConcertRepository.getTrackStreamingUrls("test1") } returns emptyList()

        // When
        val result = repository.startConcertDownload(concert)

        // Then - Should return empty list
        assertThat(result).isEmpty()
        coVerify(exactly = 0) { mockDownloadDao.insertDownload(any()) }
    }

    // Status Update Business Logic Tests

    @Test
    fun `updateDownloadStatus sets completion timestamp for COMPLETED status`() = runTest {
        // Given
        val beforeTime = System.currentTimeMillis()
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "COMPLETED", any()) } returns Unit

        // When
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.COMPLETED)

        // Then - Should set completion timestamp
        coVerify { 
            mockDownloadDao.updateDownloadStatus(
                eq("test1_track1.mp3"), 
                eq("COMPLETED"), 
                match { timestamp -> timestamp >= beforeTime }
            )
        }
    }

    @Test
    fun `updateDownloadStatus does not set timestamp for non-completion states`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus(any(), any(), any()) } returns Unit

        // When
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.PAUSED)
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.FAILED)

        // Then - Should not set completion timestamp
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null) }
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "FAILED", null) }
    }

    @Test
    fun `updateDownloadStatus with error message updates entity`() = runTest {
        // Given
        val existingDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        val updatedEntitySlot = slot<DownloadEntity>()
        
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "FAILED", null) } returns Unit
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns existingDownload
        coEvery { mockDownloadDao.updateDownload(capture(updatedEntitySlot)) } returns Unit

        // When
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.FAILED, "Network timeout")

        // Then - Should update error message
        val updatedEntity = updatedEntitySlot.captured
        assertThat(updatedEntity.errorMessage).isEqualTo("Network timeout")
    }

    // Download State Query Logic Tests

    @Test
    fun `isTrackDownloaded returns true only for completed downloads`() = runTest {
        // Given
        val completedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        val downloadingDownload = createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING)
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns completedDownload
        coEvery { mockDownloadDao.getDownloadById("test1_track2.mp3") } returns downloadingDownload
        coEvery { mockDownloadDao.getDownloadById("test1_track3.mp3") } returns null

        // When & Then
        assertThat(repository.isTrackDownloaded("test1", "track1.mp3")).isTrue()
        assertThat(repository.isTrackDownloaded("test1", "track2.mp3")).isFalse()
        assertThat(repository.isTrackDownloaded("test1", "track3.mp3")).isFalse()
    }

    @Test
    fun `getLocalFilePath returns path only for completed downloads`() = runTest {
        // Given
        val completedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
            .copy(localPath = "/storage/music/track1.mp3")
        val downloadingDownload = createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING)
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns completedDownload
        coEvery { mockDownloadDao.getDownloadById("test1_track2.mp3") } returns downloadingDownload

        // When & Then
        assertThat(repository.getLocalFilePath("test1", "track1.mp3")).isEqualTo("/storage/music/track1.mp3")
        assertThat(repository.getLocalFilePath("test1", "track2.mp3")).isNull()
    }

    // State Transition Helper Method Tests

    @Test
    fun `pauseDownload transitions to PAUSED state`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null) } returns Unit

        // When
        repository.pauseDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null) }
    }

    @Test
    fun `resumeDownload transitions to QUEUED state`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "QUEUED", null) } returns Unit

        // When
        repository.resumeDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "QUEUED", null) }
    }

    @Test
    fun `cancelDownload transitions to CANCELLED state`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "CANCELLED", null) } returns Unit

        // When
        repository.cancelDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "CANCELLED", null) }
    }

    // Helper methods for creating test data
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

    private fun createTestConcert(identifier: String, title: String): Concert {
        return Concert(
            identifier = identifier,
            title = title,
            date = "1977-05-08",
            venue = "Barton Hall",
            location = "Ithaca, NY"
        )
    }

    private fun createTestAudioFile(filename: String): AudioFile {
        return AudioFile(
            filename = filename,
            format = "MP3",
            sizeBytes = "1000000",
            durationSeconds = "180"
        )
    }
}