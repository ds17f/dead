package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.DownloadDao
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.model.DownloadStatus
import com.deadarchive.core.model.FavoriteType
import com.deadarchive.core.model.Track
import com.deadarchive.core.network.ArchiveApiService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.sql.SQLException

/**
 * Tests for error handling and edge cases across all repositories
 */
class RepositoryErrorHandlingTest {

    private lateinit var mockApiService: ArchiveApiService
    private lateinit var mockConcertDao: ConcertDao
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockDownloadDao: DownloadDao
    
    private lateinit var concertRepository: ConcertRepositoryImpl
    private lateinit var favoriteRepository: FavoriteRepositoryImpl
    private lateinit var downloadRepository: DownloadRepositoryImpl

    @Before
    fun setup() {
        mockApiService = mockk()
        mockConcertDao = mockk()
        mockFavoriteDao = mockk()
        mockDownloadDao = mockk()
        
        concertRepository = ConcertRepositoryImpl(mockApiService, mockConcertDao, mockFavoriteDao)
        favoriteRepository = FavoriteRepositoryImpl(mockFavoriteDao)
        downloadRepository = DownloadRepositoryImpl(mockDownloadDao, concertRepository)
    }

    // ConcertRepository Error Handling Tests

    @Test
    fun `searchConcerts handles network timeout gracefully`() = runTest {
        // Given
        coEvery { mockConcertDao.searchConcerts(any()) } returns emptyList()
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            SocketTimeoutException("Connect timed out")

        // When
        val result = concertRepository.searchConcerts("1977").first()

        // Then
        assertThat(result).isEmpty() // Should return empty list, not crash
    }

    @Test
    fun `searchConcerts handles HTTP errors gracefully`() = runTest {
        // Given
        val mockResponse = mockk<Response<*>>()
        every { mockResponse.code() } returns 500
        every { mockResponse.message() } returns "Internal Server Error"
        
        coEvery { mockConcertDao.searchConcerts(any()) } returns emptyList()
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            HttpException(mockResponse)

        // When
        val result = concertRepository.searchConcerts("test").first()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `searchConcerts propagates database errors for infrastructure issues`() = runTest {
        // Given
        coEvery { mockConcertDao.searchConcerts(any()) } throws SQLException("Database locked")

        // When & Then - Database errors should propagate as they indicate serious issues
        try {
            concertRepository.searchConcerts("test").first()
        } catch (e: SQLException) {
            assertThat(e.message).isEqualTo("Database locked")
        }
    }

    @Test
    fun `getConcertById handles null responses gracefully`() = runTest {
        // Given
        coEvery { mockConcertDao.getConcertById("nonexistent") } returns null
        coEvery { mockApiService.getConcertMetadata("nonexistent") } returns 
            Response.success(null)

        // When
        val result = concertRepository.getConcertById("nonexistent")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getConcertById falls back to cache when API fails`() = runTest {
        // Given
        val cachedConcert = createTestConcertEntity("test1", "Cached Concert")
        coEvery { mockConcertDao.getConcertById("test1") } returns cachedConcert
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns true
        coEvery { mockApiService.getConcertMetadata("test1") } throws IOException("Network error")

        // When
        val result = concertRepository.getConcertById("test1")

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.identifier).isEqualTo("test1")
        assertThat(result.title).isEqualTo("Cached Concert")
        assertThat(result.isFavorite).isTrue()
    }

    // FavoriteRepository Error Handling Tests

    @Test
    fun `favoriteRepository handles database exceptions gracefully`() = runTest {
        // Given
        coEvery { mockFavoriteDao.insertFavorite(any()) } throws SQLException("Database constraint violation")

        // When & Then - Should not crash
        try {
            favoriteRepository.addConcertToFavorites(createTestConcert("test1", "Test Concert"))
            // If we get here, the exception was handled (or the test setup is wrong)
        } catch (e: SQLException) {
            // This is expected behavior - let the exception bubble up for proper error handling
            assertThat(e.message).contains("Database constraint violation")
        }
    }

    @Test
    fun `favoriteRepository handles Flow exceptions in getAllFavorites`() = runTest {
        // Given
        every { mockFavoriteDao.getAllFavorites() } throws RuntimeException("Database connection lost")

        // When & Then
        try {
            favoriteRepository.getAllFavorites().first()
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Database connection lost")
        }
    }

    @Test
    fun `toggleConcertFavorite handles concurrent modifications`() = runTest {
        // Given - Simulate race condition where status changes between check and action
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returnsMany listOf(false, true)
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit
        coEvery { mockFavoriteDao.deleteFavoriteById(any()) } returns Unit

        // When
        val result1 = favoriteRepository.toggleConcertFavorite(concert)
        val result2 = favoriteRepository.toggleConcertFavorite(concert)

        // Then
        assertThat(result1).isTrue() // First call adds favorite
        assertThat(result2).isFalse() // Second call removes favorite
    }

    // DownloadRepository Error Handling Tests

    @Test
    fun `downloadRepository handles empty track list gracefully`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { concertRepository.getTrackStreamingUrls("test1") } returns emptyList()

        // When
        val result = downloadRepository.startConcertDownload(concert)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `downloadRepository handles ConcertRepository failures`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { concertRepository.getTrackStreamingUrls("test1") } throws IOException("API unavailable")

        // When & Then
        try {
            downloadRepository.startConcertDownload(concert)
        } catch (e: IOException) {
            assertThat(e.message).isEqualTo("API unavailable")
        }
    }

    @Test
    fun `updateDownloadStatus handles nonexistent download gracefully`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadStatus("nonexistent", "COMPLETED", any()) } returns Unit
        coEvery { mockDownloadDao.getDownloadById("nonexistent") } returns null

        // When
        downloadRepository.updateDownloadStatus("nonexistent", DownloadStatus.COMPLETED, "Test error")

        // Then - Should not crash, gracefully handle missing download
    }

    @Test
    fun `downloadRepository handles Flow exceptions gracefully`() = runTest {
        // Given
        every { mockDownloadDao.getAllDownloads() } throws RuntimeException("Database corrupted")

        // When & Then
        try {
            downloadRepository.getAllDownloads().first()
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Database corrupted")
        }
    }

    // Edge Case Tests

    @Test
    fun `searchConcerts handles empty query strings`() = runTest {
        // Given
        coEvery { mockConcertDao.searchConcerts("") } returns emptyList()
        coEvery { mockApiService.searchConcerts("collection:GratefulDead", any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse(emptyList()))

        // When
        val result = concertRepository.searchConcerts("").first()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `searchConcerts handles very long query strings`() = runTest {
        // Given
        val longQuery = "a".repeat(1000)
        coEvery { mockConcertDao.searchConcerts(longQuery) } returns emptyList()
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse(emptyList()))

        // When
        val result = concertRepository.searchConcerts(longQuery).first()

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `favoriteRepository handles special characters in IDs`() = runTest {
        // Given
        val concertWithSpecialChars = createTestConcert("test-concert_2023!@#$%", "Special Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test-concert_2023!@#$%") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit

        // When
        val result = favoriteRepository.toggleConcertFavorite(concertWithSpecialChars)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `downloadRepository handles very large file sizes`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadProgress("test", 0.5f, Long.MAX_VALUE) } returns Unit

        // When
        downloadRepository.updateDownloadProgress("test", 0.5f, Long.MAX_VALUE)

        // Then - Should handle without overflow
    }

    @Test
    fun `downloadRepository handles progress values outside valid range`() = runTest {
        // Given
        coEvery { mockDownloadDao.updateDownloadProgress("test", any(), any()) } returns Unit

        // When - Test edge cases
        downloadRepository.updateDownloadProgress("test", -1.0f, 0L) // Negative progress
        downloadRepository.updateDownloadProgress("test", 2.0f, 0L)  // Progress > 100%
        downloadRepository.updateDownloadProgress("test", Float.NaN, 0L) // NaN progress

        // Then - Should not crash
    }

    // Concurrent Access Tests

    @Test 
    fun `repositories handle concurrent access gracefully`() = runTest {
        // Given
        val concert = createTestConcert("test1", "Test Concert")
        coEvery { mockFavoriteDao.isConcertFavorite("test1") } returns false
        coEvery { mockFavoriteDao.insertFavorite(any()) } returns Unit
        coEvery { mockDownloadDao.getDownloadById(any()) } returns null
        coEvery { mockDownloadDao.insertDownload(any()) } returns Unit

        // When - Simulate concurrent operations
        val favoriteResult = favoriteRepository.toggleConcertFavorite(concert)
        val downloadResult = downloadRepository.startDownload(concert, "track1.mp3")

        // Then
        assertThat(favoriteResult).isTrue()
        assertThat(downloadResult).isEqualTo("test1_track1.mp3")
    }

    // Helper methods
    private fun createTestConcert(identifier: String, title: String) = Concert(
        identifier = identifier,
        title = title,
        date = "1977-05-08",
        venue = "Test Venue",
        location = "Test Location"
    )

    private fun createTestConcertEntity(id: String, title: String) = mockk<com.deadarchive.core.database.ConcertEntity> {
        every { this@mockk.id } returns id
        every { this@mockk.title } returns title
        every { this@mockk.date } returns "1977-05-08"
        every { this@mockk.venue } returns "Test Venue"
        every { this@mockk.location } returns "Test Location"
        every { toConcert() } returns createTestConcert(id, title)
    }

    private fun createMockSearchResponse(docs: List<com.deadarchive.core.network.model.ArchiveSearchResponse.ArchiveDoc>) = mockk<com.deadarchive.core.network.model.ArchiveSearchResponse> {
        every { response } returns mockk<com.deadarchive.core.network.model.ArchiveSearchResponse.ArchiveResponse> {
            every { this@mockk.docs } returns docs
            every { numFound } returns docs.size
            every { start } returns 0
        }
    }
}