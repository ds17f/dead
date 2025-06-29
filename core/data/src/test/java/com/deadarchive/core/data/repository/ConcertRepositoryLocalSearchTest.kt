package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.network.ArchiveApiService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Test the new local-only search functionality in ConcertRepository
 */
class ConcertRepositoryLocalSearchTest {
    
    private lateinit var concertRepository: ConcertRepository
    private lateinit var mockConcertDao: ConcertDao
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var mockApiService: ArchiveApiService

    @Before
    fun setup() {
        mockConcertDao = mockk(relaxed = true)
        mockFavoriteDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true) // Not used in local-only search
        concertRepository = ConcertRepositoryImpl(mockApiService, mockConcertDao, mockFavoriteDao)
    }

    @Test
    fun `searchConcerts handles exact date queries with precise matching`() = runTest {
        // Given
        val exactDateConcerts = listOf(createTestConcert("gd1977-05-08", "Cornell '77"))
        coEvery { mockConcertDao.searchConcertsByExactDate("1977-05-08") } returns exactDateConcerts
        coEvery { mockFavoriteDao.isConcertFavorite("gd1977-05-08") } returns true

        // When
        val result = concertRepository.searchConcerts("1977-05-08").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1977-05-08")
        assertThat(result[0].isFavorite).isTrue()
        
        // Verify precise date search was used
        coVerify { mockConcertDao.searchConcertsByExactDate("1977-05-08") }
        coVerify(exactly = 0) { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `searchConcerts handles year-month queries with precise matching`() = runTest {
        // Given
        val mayShows = listOf(
            createTestConcert("gd1977-05-07", "Boston Garden"),
            createTestConcert("gd1977-05-08", "Cornell"),
            createTestConcert("gd1977-05-09", "Buffalo")
        )
        coEvery { mockConcertDao.searchConcertsByYearMonth("1977-05") } returns mayShows
        coEvery { mockFavoriteDao.isConcertFavorite(any()) } returns false

        // When
        val result = concertRepository.searchConcerts("1977-05").first()

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.identifier }).containsExactly(
            "gd1977-05-07", "gd1977-05-08", "gd1977-05-09"
        )
        
        // Verify year-month search was used
        coVerify { mockConcertDao.searchConcertsByYearMonth("1977-05") }
    }

    @Test
    fun `searchConcerts handles year queries with precise matching`() = runTest {
        // Given
        val year1977Shows = listOf(createTestConcert("gd1977-05-08", "Cornell '77"))
        coEvery { mockConcertDao.searchConcertsByYear("1977") } returns year1977Shows
        coEvery { mockFavoriteDao.isConcertFavorite("gd1977-05-08") } returns false

        // When
        val result = concertRepository.searchConcerts("1977").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1977-05-08")
        
        // Verify year search was used
        coVerify { mockConcertDao.searchConcertsByYear("1977") }
    }

    @Test
    fun `searchConcerts handles venue queries correctly`() = runTest {
        // Given
        val winterlandShows = listOf(createTestConcert("gd1972-11-15", "Winterland"))
        coEvery { mockConcertDao.searchConcertsByVenue("Winterland") } returns winterlandShows
        coEvery { mockFavoriteDao.isConcertFavorite("gd1972-11-15") } returns false

        // When
        val result = concertRepository.searchConcerts("Winterland").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1972-11-15")
        
        // Verify venue search was used
        coVerify { mockConcertDao.searchConcertsByVenue("Winterland") }
    }

    @Test
    fun `searchConcerts handles city queries correctly`() = runTest {
        // Given
        val berkeleyShows = listOf(createTestConcert("gd1971-07-02", "Berkeley Community Theatre"))
        coEvery { mockConcertDao.searchConcertsByLocation("Berkeley") } returns berkeleyShows
        coEvery { mockFavoriteDao.isConcertFavorite("gd1971-07-02") } returns false

        // When
        val result = concertRepository.searchConcerts("Berkeley").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1971-07-02")
        
        // Verify location search was used
        coVerify { mockConcertDao.searchConcertsByLocation("Berkeley") }
    }

    @Test
    fun `searchConcerts handles empty queries with recent concerts`() = runTest {
        // Given
        val recentShows = listOf(createTestConcert("gd1995-07-09", "Chicago"))
        coEvery { mockConcertDao.getRecentConcerts(100) } returns recentShows
        coEvery { mockFavoriteDao.isConcertFavorite("gd1995-07-09") } returns false

        // When
        val result = concertRepository.searchConcerts("").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1995-07-09")
        
        // Verify recent concerts query was used
        coVerify { mockConcertDao.getRecentConcerts(100) }
    }

    @Test
    fun `searchConcerts handles general text queries`() = runTest {
        // Given
        val textSearchResults = listOf(createTestConcert("gd1974-05-19", "Portland Memorial"))
        coEvery { mockConcertDao.searchConcertsGeneral("Portland") } returns textSearchResults
        coEvery { mockFavoriteDao.isConcertFavorite("gd1974-05-19") } returns false

        // When
        val result = concertRepository.searchConcerts("Portland").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1974-05-19")
        
        // Verify general search was used
        coVerify { mockConcertDao.searchConcertsGeneral("Portland") }
    }

    @Test
    fun `searchConcerts returns empty list when no results found`() = runTest {
        // Given - No concerts match the query
        coEvery { mockConcertDao.searchConcertsByExactDate("1993-05-16") } returns emptyList()

        // When
        val result = concertRepository.searchConcerts("1993-05-16").first()

        // Then - Should return empty list, not fall back to wrong results
        assertThat(result).isEmpty()
        
        // Verify exact date search was attempted
        coVerify { mockConcertDao.searchConcertsByExactDate("1993-05-16") }
    }

    @Test
    fun `searchConcerts handles decade queries correctly`() = runTest {
        // Given
        val seventiesShows = listOf(createTestConcert("gd1975-08-13", "Great American Music Hall"))
        coEvery { mockConcertDao.searchConcertsByYearRange(1970, 1979) } returns seventiesShows
        coEvery { mockFavoriteDao.isConcertFavorite("gd1975-08-13") } returns false

        // When
        val result = concertRepository.searchConcerts("1970s").first()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("gd1975-08-13")
        
        // Verify year range search was used
        coVerify { mockConcertDao.searchConcertsByYearRange(1970, 1979) }
    }

    private fun createTestConcert(id: String, title: String): ConcertEntity {
        return ConcertEntity(
            id = id,
            title = title,
            date = id.substring(2, 12), // Extract date from ID
            venue = "Test Venue", 
            location = "Test City, TS",
            year = id.substring(2, 6), // Extract year from ID
            description = "Test concert",
            source = "Test source",
            isFavorite = false,
            cachedTimestamp = System.currentTimeMillis()
        )
    }
}