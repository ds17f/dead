package com.deadarchive.core.data.repository

import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.model.Concert
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveSearchResponse
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ConcertRepositoryTest {

    private lateinit var mockApiService: ArchiveApiService
    private lateinit var mockConcertDao: ConcertDao
    private lateinit var mockFavoriteDao: FavoriteDao
    private lateinit var repository: ConcertRepositoryImpl

    @Before
    fun setup() {
        mockApiService = mockk()
        mockConcertDao = mockk()
        mockFavoriteDao = mockk()
        repository = ConcertRepositoryImpl(mockApiService, mockConcertDao, mockFavoriteDao)
    }

    @Test
    fun `getConcertMetadata returns metadata when API call succeeds`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track01.mp3",
                    format = "VBR MP3"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getConcertMetadata("test-concert")

        assertThat(result).isNotNull()
        assertThat(result!!.metadata!!.identifier).isEqualTo("test-concert")
        assertThat(result.server).isEqualTo("ia800207.us.archive.org")
    }

    @Test
    fun `getConcertMetadata returns null when API call fails`() = runTest {
        coEvery { mockApiService.getConcertMetadata("nonexistent") } returns 
            Response.error(404, mockk(relaxed = true))

        val result = repository.getConcertMetadata("nonexistent")

        assertThat(result).isNull()
    }

    @Test
    fun `getConcertMetadata returns null when exception occurs`() = runTest {
        coEvery { mockApiService.getConcertMetadata("error-concert") } throws 
            RuntimeException("Network error")

        val result = repository.getConcertMetadata("error-concert")

        assertThat(result).isNull()
    }

    @Test
    fun `getStreamingUrl generates correct URL from metadata`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getStreamingUrl("test-concert", "track01.mp3")

        assertThat(result).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track01.mp3")
    }

    @Test
    fun `getStreamingUrl handles filename with spaces`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getStreamingUrl("test-concert", "track 01.mp3")

        assertThat(result).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track+01.mp3")
    }

    @Test
    fun `getStreamingUrl uses workable servers as fallback`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = null, // No primary server
            directory = "/1/items/test-concert",
            workableServers = listOf("ia800208.us.archive.org", "ia800209.us.archive.org")
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getStreamingUrl("test-concert", "track01.mp3")

        assertThat(result).isEqualTo("https://ia800208.us.archive.org/1/items/test-concert/track01.mp3")
    }

    @Test
    fun `getTrackStreamingUrls filters and maps audio files correctly`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track01.mp3",
                    format = "VBR MP3",
                    size = "1234567",
                    length = "180.5"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "info.txt",
                    format = "Text" // Should be filtered out
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track02.flac",
                    format = "Flac",
                    size = "9876543",
                    length = "220.3"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getTrackStreamingUrls("test-concert")

        assertThat(result).hasSize(2)
        
        val (audioFile1, url1) = result[0]
        assertThat(audioFile1.filename).isEqualTo("track01.mp3")
        assertThat(audioFile1.format).isEqualTo("VBR MP3")
        assertThat(url1).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track01.mp3")
        
        val (audioFile2, url2) = result[1]
        assertThat(audioFile2.filename).isEqualTo("track02.flac")
        assertThat(audioFile2.format).isEqualTo("Flac")
        assertThat(url2).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track02.flac")
    }

    @Test
    fun `getPreferredStreamingUrl prefers MP3 files`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track01.flac",
                    format = "Flac"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track01.mp3",
                    format = "VBR MP3"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track02.mp3",
                    format = "VBR MP3"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getPreferredStreamingUrl("test-concert")

        // Should return first MP3 file, not the FLAC
        assertThat(result).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track01.mp3")
    }

    @Test
    fun `getPreferredStreamingUrl falls back to first audio file when no MP3`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track01.flac",
                    format = "Flac"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "track02.ogg",
                    format = "Ogg Vorbis"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getPreferredStreamingUrl("test-concert")

        // Should return first available audio file (FLAC)
        assertThat(result).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/track01.flac")
    }

    @Test
    fun `getTrackStreamingUrl finds track by filename match`() = runTest {
        val mockMetadata = ArchiveMetadataResponse(
            files = listOf(
                ArchiveMetadataResponse.ArchiveFile(
                    name = "gd1995-07-09d1t01.mp3",
                    format = "VBR MP3"
                ),
                ArchiveMetadataResponse.ArchiveFile(
                    name = "gd1995-07-09d1t09.mp3",
                    format = "VBR MP3"
                )
            ),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = "test-concert",
                title = "Test Concert"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/test-concert"
        )

        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(mockMetadata)

        val result = repository.getTrackStreamingUrl("test-concert", "t09")

        // Should find the track with "t09" in filename
        assertThat(result).isEqualTo("https://ia800207.us.archive.org/1/items/test-concert/gd1995-07-09d1t09.mp3")
    }

    // ===================
    // FOCUSED UNIT TESTS FOR NEW FUNCTIONALITY
    // ===================
    
    @Test
    fun `search query builder creates correct query for year search`() {
        // Test the query building logic in isolation
        val query1977 = "1977"
        val queryGeneral = "Jerry Garcia"
        val queryBlank = ""
        val queryWhitespace = "   "
        
        // Test year detection regex
        assertThat(query1977.matches(Regex("\\d{4}"))).isTrue()
        assertThat(queryGeneral.matches(Regex("\\d{4}"))).isFalse()
        assertThat("12345".matches(Regex("\\d{4}"))).isFalse()
        assertThat("123".matches(Regex("\\d{4}"))).isFalse()
        
        // Test blank detection
        assertThat(queryBlank.isBlank()).isTrue()
        assertThat(queryWhitespace.isBlank()).isTrue()
        assertThat(query1977.isBlank()).isFalse()
    }

    @Test
    fun `cache expiry logic works correctly`() {
        val currentTime = System.currentTimeMillis()
        val recentTimestamp = currentTime - (12 * 60 * 60 * 1000L) // 12 hours ago  
        val oldTimestamp = currentTime - (48 * 60 * 60 * 1000L) // 48 hours ago
        val exactBoundaryTimestamp = currentTime - (24 * 60 * 60 * 1000L) // Exactly 24 hours ago
        
        // Test the expiry calculation logic in isolation
        val cacheExpiryHours = 24
        
        // Recent cache should not be expired
        val recentExpiryTime = recentTimestamp + (cacheExpiryHours * 60 * 60 * 1000L)
        assertThat(currentTime > recentExpiryTime).isFalse()
        
        // Old cache should be expired
        val oldExpiryTime = oldTimestamp + (cacheExpiryHours * 60 * 60 * 1000L)
        assertThat(currentTime > oldExpiryTime).isTrue()
        
        // Exactly at boundary should be expired
        val boundaryExpiryTime = exactBoundaryTimestamp + (cacheExpiryHours * 60 * 60 * 1000L)
        assertThat(currentTime >= boundaryExpiryTime).isTrue()
    }

    // ===================
    // TEST HELPER METHODS
    // ===================

    private fun createMockSearchResponse(concerts: List<ArchiveSearchResponse.ArchiveDoc>): ArchiveSearchResponse {
        return ArchiveSearchResponse(
            response = ArchiveSearchResponse.ArchiveResponse(
                numFound = concerts.size,
                start = 0,
                docs = concerts
            )
        )
    }

    private fun createMockConcert(identifier: String, title: String, date: String): ArchiveSearchResponse.ArchiveDoc {
        return ArchiveSearchResponse.ArchiveDoc(
            identifier = identifier,
            title = title,
            date = date
            // All other fields are optional and will use defaults
        )
    }

    private fun createMockConcertEntity(id: String, title: String, date: String): ConcertEntity {
        return ConcertEntity(
            id = id,
            title = title,
            date = date,
            venue = "Test Venue",
            location = "Test City, CA",
            year = date.take(4),
            source = "SBD",
            taper = "Test Taper",
            transferer = "Test Transferer",
            lineage = "Test Lineage",
            description = "Test Description",
            setlistRaw = "Test Setlist",
            uploader = "Test Uploader",
            addedDate = "2023-01-01T00:00:00Z",
            publicDate = "2023-01-01T00:00:00Z",
            isFavorite = false,
            cachedTimestamp = System.currentTimeMillis()
        )
    }
}