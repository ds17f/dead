package com.deadarchive.core.data.repository

import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.DownloadEntity
import com.deadarchive.core.model.AudioFile
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.DownloadState
import com.deadarchive.core.model.DownloadStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DownloadRepositoryTest {

    private lateinit var mockDownloadDao: DownloadDao
    private lateinit var mockConcertRepository: ConcertRepository
    private lateinit var repository: DownloadRepositoryImpl

    @Before
    fun setup() {
        mockDownloadDao = mockk()
        mockConcertRepository = mockk()
        repository = DownloadRepositoryImpl(mockDownloadDao, mockConcertRepository)
    }

    @Test
    fun `getAllDownloads returns mapped download states`() = runTest {
        // Given
        val downloadEntities = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING)
        )
        every { mockDownloadDao.getAllDownloads() } returns flowOf(downloadEntities)

        // When
        val result = repository.getAllDownloads().first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].concertIdentifier).isEqualTo("test1")
        assertThat(result[0].trackFilename).isEqualTo("track1.mp3")
        assertThat(result[0].status).isEqualTo(DownloadStatus.COMPLETED)
        assertThat(result[1].status).isEqualTo(DownloadStatus.DOWNLOADING)
    }

    @Test
    fun `getDownloadsByStatus filters by status`() = runTest {
        // Given
        val completedEntities = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        )
        every { mockDownloadDao.getDownloadsByStatus("COMPLETED") } returns flowOf(completedEntities)

        // When
        val result = repository.getDownloadsByStatus(DownloadStatus.COMPLETED).first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(DownloadStatus.COMPLETED)
        coVerify { mockDownloadDao.getDownloadsByStatus("COMPLETED") }
    }

    @Test
    fun `getActiveDownloads delegates to DAO`() = runTest {
        // Given
        val activeEntities = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.QUEUED)  
        )
        every { mockDownloadDao.getActiveDownloads() } returns flowOf(activeEntities)

        // When
        val result = repository.getActiveDownloads().first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.all { it.status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED) }).isTrue()
    }

    @Test
    fun `getCompletedDownloads delegates to DAO`() = runTest {
        // Given
        val completedEntities = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        )
        every { mockDownloadDao.getCompletedDownloads() } returns flowOf(completedEntities)

        // When
        val result = repository.getCompletedDownloads().first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(DownloadStatus.COMPLETED)
    }

    @Test
    fun `getDownloadsForConcert filters by concert ID`() = runTest {
        // Given
        val concertDownloads = listOf(
            createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED),
            createTestDownloadEntity("test1_track2.mp3", "test1", "track2.mp3", DownloadStatus.DOWNLOADING)
        )
        every { mockDownloadDao.getDownloadsForConcert("test1") } returns flowOf(concertDownloads)

        // When
        val result = repository.getDownloadsForConcert("test1").first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.all { it.concertIdentifier == "test1" }).isTrue()
    }

    @Test
    fun `getDownloadById returns mapped download state`() = runTest {
        // Given
        val downloadEntity = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns downloadEntity

        // When
        val result = repository.getDownloadById("test1_track1.mp3")

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.concertIdentifier).isEqualTo("test1")
        assertThat(result.trackFilename).isEqualTo("track1.mp3")
        assertThat(result.status).isEqualTo(DownloadStatus.COMPLETED)
    }

    @Test
    fun `getDownloadById returns null when not found`() = runTest {
        // Given
        coEvery { mockDownloadDao.getDownloadById("nonexistent") } returns null

        // When
        val result = repository.getDownloadById("nonexistent")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `startDownload creates new download when not exists`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val entitySlot = slot<DownloadEntity>()
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns null
        coEvery { mockDownloadDao.insertDownload(capture(entitySlot)) } returns Unit

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then
        assertThat(result).isEqualTo("test1_track1.mp3")
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.id).isEqualTo("test1_track1.mp3")
        assertThat(capturedEntity.concertIdentifier).isEqualTo("test1")
        assertThat(capturedEntity.trackFilename).isEqualTo("track1.mp3")
        assertThat(capturedEntity.status).isEqualTo("QUEUED")
        assertThat(capturedEntity.progress).isEqualTo(0f)
        assertThat(capturedEntity.bytesDownloaded).isEqualTo(0L)
    }

    @Test
    fun `startDownload resets failed download`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val existingDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.FAILED)
        val updatedEntitySlot = slot<DownloadEntity>()
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns existingDownload
        coEvery { mockDownloadDao.updateDownload(capture(updatedEntitySlot)) } returns Unit

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then
        assertThat(result).isEqualTo("test1_track1.mp3")
        val updatedEntity = updatedEntitySlot.captured
        assertThat(updatedEntity.status).isEqualTo("QUEUED")
        assertThat(updatedEntity.progress).isEqualTo(0f)
        assertThat(updatedEntity.bytesDownloaded).isEqualTo(0L)
        assertThat(updatedEntity.errorMessage).isNull()
    }

    @Test
    fun `startDownload returns existing ID for active download`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val existingDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns existingDownload

        // When
        val result = repository.startDownload(concert, "track1.mp3")

        // Then
        assertThat(result).isEqualTo("test1_track1.mp3")
        coVerify(exactly = 0) { mockDownloadDao.insertDownload(any()) }
        coVerify(exactly = 0) { mockDownloadDao.updateDownload(any()) }
    }

    @Test
    fun `startConcertDownload creates downloads for all tracks`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        val trackUrls = listOf(
            createTestAudioFile("track1.mp3") to "https://example.com/track1.mp3",
            createTestAudioFile("track2.mp3") to "https://example.com/track2.mp3"
        )
        
        coEvery { mockConcertRepository.getTrackStreamingUrls("test1") } returns trackUrls
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null
        coEvery { mockDownloadDao.insertDownload(any()) } returns Unit

        // When
        val result = repository.startConcertDownload(concert)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly("test1_track1.mp3", "test1_track2.mp3")
        coVerify(exactly = 2) { mockDownloadDao.insertDownload(any()) }
    }

    @Test
    fun `updateDownloadProgress delegates to DAO`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadProgress("test1_track1.mp3", 0.5f, 1024L) } returns Unit

        // When
        repository.updateDownloadProgress("test1_track1.mp3", 0.5f, 1024L)

        // Then
        coVerify { mockDownloadDao.updateDownloadProgress("test1_track1.mp3", 0.5f, 1024L) }
    }

    @Test
    fun `updateDownloadStatus with completion sets timestamp`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "COMPLETED", any()) } returns Unit

        // When
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.COMPLETED)

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "COMPLETED", any()) }
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
        repository.updateDownloadStatus("test1_track1.mp3", DownloadStatus.FAILED, "Network error")

        // Then
        val updatedEntity = updatedEntitySlot.captured
        assertThat(updatedEntity.errorMessage).isEqualTo("Network error")
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "FAILED", null) }
        coVerify { mockDownloadDao.updateDownload(any()) }
    }

    @Test
    fun `pauseDownload updates status to PAUSED`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null) } returns Unit

        // When
        repository.pauseDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "PAUSED", null) }
    }

    @Test
    fun `resumeDownload updates status to QUEUED`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "QUEUED", null) } returns Unit

        // When
        repository.resumeDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "QUEUED", null) }
    }

    @Test
    fun `cancelDownload updates status to CANCELLED`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "CANCELLED", null) } returns Unit

        // When
        repository.cancelDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.updateDownloadStatus("test1_track1.mp3", "CANCELLED", null) }
    }

    @Test
    fun `deleteDownload delegates to DAO`() = runTest {
        // Given
        coEvery { mockDownloadDao.deleteDownloadById("test1_track1.mp3") } returns Unit

        // When
        repository.deleteDownload("test1_track1.mp3")

        // Then
        coVerify { mockDownloadDao.deleteDownloadById("test1_track1.mp3") }
    }

    @Test
    fun `cancelAllActiveDownloads delegates to DAO`() = runTest {
        // Given
        coEvery { mockDownloadDao.cancelAllActiveDownloads() } returns Unit

        // When
        repository.cancelAllActiveDownloads()

        // Then
        coVerify { mockDownloadDao.cancelAllActiveDownloads() }
    }

    @Test
    fun `deleteCompletedDownloads delegates to DAO`() = runTest {
        // Given
        coEvery { mockDownloadDao.deleteCompletedDownloads() } returns Unit

        // When
        repository.deleteCompletedDownloads()

        // Then
        coVerify { mockDownloadDao.deleteCompletedDownloads() }
    }

    @Test
    fun `deleteFailedDownloads delegates to DAO`() = runTest {
        // Given
        coEvery { mockDownloadDao.deleteFailedDownloads() } returns Unit

        // When
        repository.deleteFailedDownloads()

        // Then
        coVerify { mockDownloadDao.deleteFailedDownloads() }
    }

    @Test
    fun `isTrackDownloaded returns true for completed download`() = runTest {
        // Given
        val completedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns completedDownload

        // When
        val result = repository.isTrackDownloaded("test1", "track1.mp3")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `isTrackDownloaded returns false for non-completed download`() = runTest {
        // Given
        val downloadingDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns downloadingDownload

        // When
        val result = repository.isTrackDownloaded("test1", "track1.mp3")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `isTrackDownloaded returns false when download not found`() = runTest {
        // Given
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns null

        // When
        val result = repository.isTrackDownloaded("test1", "track1.mp3")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `getLocalFilePath returns path for completed download`() = runTest {
        // Given
        val completedDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.COMPLETED)
            .copy(localPath = "/storage/music/track1.mp3")
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns completedDownload

        // When
        val result = repository.getLocalFilePath("test1", "track1.mp3")

        // Then
        assertThat(result).isEqualTo("/storage/music/track1.mp3")
    }

    @Test
    fun `getLocalFilePath returns null for non-completed download`() = runTest {
        // Given
        val downloadingDownload = createTestDownloadEntity("test1_track1.mp3", "test1", "track1.mp3", DownloadStatus.DOWNLOADING)
        coEvery { mockDownloadDao.getDownloadById("test1_track1.mp3") } returns downloadingDownload

        // When
        val result = repository.getLocalFilePath("test1", "track1.mp3")

        // Then
        assertThat(result).isNull()
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
            venue = "Test Venue",
            location = "Test Location"
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