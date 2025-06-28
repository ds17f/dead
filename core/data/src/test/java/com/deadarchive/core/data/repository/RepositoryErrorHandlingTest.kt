package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.network.ArchiveApiService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.sql.SQLException

/**
 * Tests for meaningful error handling scenarios across repositories:
 * - Network vs infrastructure error classification
 * - Graceful degradation to cached data
 * - Proper error propagation for unrecoverable failures
 */
class RepositoryErrorHandlingTest {

    private lateinit var mockApiService: ArchiveApiService
    private lateinit var mockConcertDao: ConcertDao
    private lateinit var mockFavoriteDao: FavoriteDao
    
    private lateinit var concertRepository: ConcertRepositoryImpl

    @Before
    fun setup() {
        mockApiService = mockk()
        mockConcertDao = mockk()
        mockFavoriteDao = mockk()
        
        concertRepository = ConcertRepositoryImpl(mockApiService, mockConcertDao, mockFavoriteDao)
    }

    // Network Error Handling - Should Degrade Gracefully

    @Test
    fun `searchConcerts gracefully handles network timeout with cached fallback`() = runTest {
        // Given - Network fails but cache has data
        val cachedConcerts = listOf(createTestConcertEntity("gd1977-05-08", "Cornell '77"))
        coEvery { mockConcertDao.searchConcerts("1977") } returns cachedConcerts
        coEvery { mockFavoriteDao.isConcertFavorite("gd1977-05-08") } returns true
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            SocketTimeoutException("Connect timed out")

        // When
        val result = concertRepository.searchConcerts("1977").first()

        // Then - Should return cached data despite network failure
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1977-05-08")
        assertThat(result[0].isFavorite).isTrue()
    }

    @Test
    fun `searchConcerts handles HTTP errors gracefully with empty cache`() = runTest {
        // Given - Both network and cache fail
        val mockResponse = mockk<Response<*>>()
        io.mockk.every { mockResponse.code() } returns 500
        io.mockk.every { mockResponse.message() } returns "Internal Server Error"
        
        coEvery { mockConcertDao.searchConcerts("test") } returns emptyList()
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            HttpException(mockResponse)

        // When
        val result = concertRepository.searchConcerts("test").first()

        // Then - Should return empty list, not crash
        assertThat(result).isEmpty()
    }

    @Test
    fun `getConcertById falls back to cache when API fails`() = runTest {
        // Given - API fails but cache has data
        val cachedConcert = createTestConcertEntity("gd1977-05-08", "Cornell '77")
        coEvery { mockConcertDao.getConcertById("gd1977-05-08") } returns cachedConcert
        coEvery { mockFavoriteDao.isConcertFavorite("gd1977-05-08") } returns false
        coEvery { mockApiService.getConcertMetadata("gd1977-05-08") } throws IOException("Network unavailable")

        // When
        val result = concertRepository.getConcertById("gd1977-05-08")

        // Then - Should return cached data
        assertThat(result).isNotNull()
        assertThat(result!!.identifier).isEqualTo("gd1977-05-08")
        assertThat(result.title).isEqualTo("Cornell '77")
        assertThat(result.isFavorite).isFalse()
    }

    // Infrastructure Error Handling - Should Propagate

    @Test
    fun `searchConcerts propagates database errors as infrastructure failures`() = runTest {
        // Given - Database is corrupted/locked (infrastructure failure)
        coEvery { mockConcertDao.searchConcerts("test") } throws SQLException("Database locked")

        // When & Then - Infrastructure errors should propagate
        try {
            concertRepository.searchConcerts("test").first()
            // Should not reach here
            assertThat(false).isTrue()
        } catch (e: SQLException) {
            assertThat(e.message).isEqualTo("Database locked")
        }
    }

    @Test
    fun `getConcertById propagates database corruption errors`() = runTest {
        // Given - Database integrity issues
        coEvery { mockConcertDao.getConcertById("test") } throws SQLException("Database corrupted")
        coEvery { mockApiService.getConcertMetadata("test") } throws IOException("Network error")

        // When & Then - Should propagate the database error
        try {
            concertRepository.getConcertById("test")
            assertThat(false).isTrue()
        } catch (e: SQLException) {
            assertThat(e.message).isEqualTo("Database corrupted")
        }
    }

    // Business Logic Error Scenarios

    @Test
    fun `searchConcerts handles API parsing errors gracefully`() = runTest {
        // Given - API throws parsing exception, no cache
        coEvery { mockConcertDao.searchConcerts("test") } returns emptyList()
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            RuntimeException("JSON parsing failed")

        // When
        val result = concertRepository.searchConcerts("test").first()

        // Then - Should handle gracefully by returning empty list
        assertThat(result).isEmpty()
    }

    @Test
    fun `getConcertById handles nonexistent concert gracefully`() = runTest {
        // Given - Concert doesn't exist anywhere
        coEvery { mockConcertDao.getConcertById("nonexistent") } returns null
        coEvery { mockApiService.getConcertMetadata("nonexistent") } returns Response.success(null)

        // When
        val result = concertRepository.getConcertById("nonexistent")

        // Then - Should return null without errors
        assertThat(result).isNull()
    }

    // Edge Case Validation

    @Test
    fun `searchConcerts handles year queries with network failure correctly`() = runTest {
        // Given - Year search with network issues
        val cachedConcerts = listOf(createTestConcertEntity("gd1977-05-08", "Cornell Show"))
        coEvery { mockConcertDao.searchConcerts("1977") } returns cachedConcerts
        coEvery { mockFavoriteDao.isConcertFavorite("gd1977-05-08") } returns false
        coEvery { mockApiService.searchConcerts(
            eq("collection:GratefulDead AND date:1977*"), // Year-specific query format
            any(), any(), any(), any(), any()
        ) } throws SocketTimeoutException("Timeout")

        // When
        val result = concertRepository.searchConcerts("1977").first()

        // Then - Should return cached data with correct year query logic
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1977-05-08")
    }

    @Test
    fun `searchConcerts handles empty query strings without errors`() = runTest {
        // Given - Empty query
        coEvery { mockConcertDao.searchConcerts("") } returns emptyList()
        coEvery { mockApiService.searchConcerts(
            eq("collection:GratefulDead"), // Should use default collection query
            any(), any(), any(), any(), any()
        ) } throws IOException("Network error")

        // When
        val result = concertRepository.searchConcerts("").first()

        // Then - Should handle gracefully
        assertThat(result).isEmpty()
    }

    // Helper methods
    private fun createTestConcert(identifier: String, title: String) = Concert(
        identifier = identifier,
        title = title,
        date = "1977-05-08",
        venue = "Barton Hall",
        location = "Ithaca, NY"
    )

    private fun createTestConcertEntity(id: String, title: String) = mockk<com.deadarchive.core.database.ConcertEntity> {
        io.mockk.every { this@mockk.id } returns id
        io.mockk.every { this@mockk.title } returns title
        io.mockk.every { this@mockk.date } returns "1977-05-08"
        io.mockk.every { this@mockk.venue } returns "Barton Hall"
        io.mockk.every { this@mockk.location } returns "Ithaca, NY"
        io.mockk.every { toConcert() } returns createTestConcert(id, title)
    }
}