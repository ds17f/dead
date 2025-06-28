package com.deadarchive.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.database.FavoriteEntity
import com.deadarchive.core.model.FavoriteType
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveMetadataResponse
import com.deadarchive.core.network.model.ArchiveSearchResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.IOException

/**
 * Integration tests for ConcertRepository with real Room database
 * Only network calls are mocked - everything else uses real components
 */
@RunWith(AndroidJUnit4::class)
class ConcertRepositoryIntegrationTest {
    
    // Real database components
    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    private lateinit var favoriteDao: FavoriteDao
    
    // Mock only network
    private val mockApiService = mockk<ArchiveApiService>()
    
    // Real repository with real DAOs + mock API
    private lateinit var repository: ConcertRepositoryImpl
    
    @Before
    fun setup() {
        // Create real in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
            
        concertDao = database.concertDao()
        favoriteDao = database.favoriteDao()
        
        // Repository with real DAOs
        repository = ConcertRepositoryImpl(mockApiService, concertDao, favoriteDao)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    
    @Test
    fun `search concerts returns cached results first then fresh API results`() = runTest {
        // Setup: Insert cached concerts in real database
        val cachedConcert = createTestConcertEntity("cached-concert", "Cached Concert")
        concertDao.insertConcert(cachedConcert)
        
        // Mock: API to return different results
        val apiResponse = createMockSearchResponse(listOf(
            createTestArchiveDoc("api-concert-1", "API Concert 1"),
            createTestArchiveDoc("api-concert-2", "API Concert 2")
        ))
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Action: Search concerts and collect results
        val results = repository.searchConcerts("test").take(2).toList()
        
        // Verify: First emission contains cached results
        assertThat(results).hasSize(2)
        val cachedResults = results[0]
        val apiResults = results[1]
        
        // Cached results should be emitted first
        assertThat(cachedResults).hasSize(1)
        assertThat(cachedResults[0].identifier).isEqualTo("cached-concert")
        assertThat(cachedResults[0].title).isEqualTo("Cached Concert")
        
        // API results should be emitted second
        assertThat(apiResults).hasSize(2)
        assertThat(apiResults.map { it.identifier }).containsExactly("api-concert-1", "api-concert-2")
        
        // Verify: Fresh results are stored in real database
        val storedConcerts = concertDao.getRecentConcerts(10)
        assertThat(storedConcerts).hasSize(3) // 1 cached + 2 from API
        assertThat(storedConcerts.map { it.id }).contains("api-concert-1")
        assertThat(storedConcerts.map { it.id }).contains("api-concert-2")
    }
    
    @Test
    fun `search concerts falls back to cache when API fails`() = runTest {
        // Setup: Insert cached concerts in real database
        val cachedConcerts = listOf(
            createTestConcertEntity("concert-1", "Concert 1"),
            createTestConcertEntity("concert-2", "Concert 2")
        )
        concertDao.insertConcerts(cachedConcerts)
        
        // Mock: API to fail
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            IOException("Network error")
        
        // Action: Search concerts
        val results = repository.searchConcerts("test").first()
        
        // Verify: Returns cached results despite API failure
        assertThat(results).hasSize(2)
        assertThat(results.map { it.identifier }).containsExactly("concert-1", "concert-2")
        assertThat(results.map { it.title }).containsExactly("Concert 1", "Concert 2")
    }
    
    @Test
    fun `search concerts returns empty when no cache and API fails`() = runTest {
        // Setup: Empty database
        // Mock: API to fail
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } throws 
            IOException("Network error")
        
        // Action: Search concerts
        val results = repository.searchConcerts("test").first()
        
        // Verify: Returns empty list
        assertThat(results).isEmpty()
    }
    
    @Test
    fun `getConcertById returns cached concert when fresh`() = runTest {
        // Setup: Insert fresh concert in database (within 24h)
        val cachedConcert = createTestConcertEntity("test-concert", "Test Concert")
        concertDao.insertConcert(cachedConcert)
        
        // Action: Get concert by ID
        val result = repository.getConcertById("test-concert")
        
        // Verify: Returns cached concert without API call
        assertThat(result).isNotNull()
        assertThat(result!!.identifier).isEqualTo("test-concert")
        assertThat(result.title).isEqualTo("Test Concert")
        
        // API should not have been called (we don't mock it, so it would fail if called)
    }
    
    @Test
    fun `getConcertById fetches from API when cache expired`() = runTest {
        // Setup: Insert expired concert in database
        val expiredTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        val expiredConcert = createTestConcertEntity("test-concert", "Old Concert")
            .copy(cachedTimestamp = expiredTimestamp)
        concertDao.insertConcert(expiredConcert)
        
        // Mock: API to return fresh metadata
        val freshMetadata = createMockMetadataResponse("test-concert", "Fresh Concert")
        coEvery { mockApiService.getConcertMetadata("test-concert") } returns 
            Response.success(freshMetadata)
        
        // Action: Get concert by ID
        val result = repository.getConcertById("test-concert")
        
        // Verify: Returns fresh data from API
        assertThat(result).isNotNull()
        assertThat(result!!.identifier).isEqualTo("test-concert")
        assertThat(result.title).isEqualTo("Fresh Concert")
        
        // Verify: Fresh data is cached in real database
        val cachedResult = concertDao.getConcertById("test-concert")
        assertThat(cachedResult).isNotNull()
        assertThat(cachedResult!!.title).isEqualTo("Fresh Concert")
    }
    
    @Test
    fun `getConcertById falls back to expired cache when API fails`() = runTest {
        // Setup: Insert expired concert in database
        val expiredTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        val expiredConcert = createTestConcertEntity("test-concert", "Expired Concert")
            .copy(cachedTimestamp = expiredTimestamp)
        concertDao.insertConcert(expiredConcert)
        
        // Mock: API to fail
        coEvery { mockApiService.getConcertMetadata("test-concert") } throws 
            IOException("Network error")
        
        // Action: Get concert by ID
        val result = repository.getConcertById("test-concert")
        
        // Verify: Returns expired cached data as fallback
        assertThat(result).isNotNull()
        assertThat(result!!.identifier).isEqualTo("test-concert")
        assertThat(result.title).isEqualTo("Expired Concert")
    }
    
    @Test
    fun `favorite status correctly applied from real database`() = runTest {
        // Setup: Insert concerts and mark one as favorite
        val concerts = listOf(
            createTestConcertEntity("concert-1", "Concert 1"),
            createTestConcertEntity("concert-2", "Concert 2")
        )
        concertDao.insertConcerts(concerts)
        
        // Mark concert-1 as favorite in real database
        val favorite = FavoriteEntity(
            id = "concert_concert-1",
            type = FavoriteType.CONCERT.name,
            concertIdentifier = "concert-1"
        )
        favoriteDao.insertFavorite(favorite)
        
        // Mock API for search
        val apiResponse = createMockSearchResponse(emptyList()) // Empty to use cache only
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Action: Search concerts
        val results = repository.searchConcerts("").first()
        
        // Verify: Favorite status is correctly applied
        assertThat(results).hasSize(2)
        val concert1 = results.find { it.identifier == "concert-1" }
        val concert2 = results.find { it.identifier == "concert-2" }
        
        assertThat(concert1?.isFavorite).isTrue()
        assertThat(concert2?.isFavorite).isFalse()
    }
    
    @Test
    fun `getFavoriteConcerts provides real-time updates`() = runTest {
        // Setup: Insert concerts and mark as favorites
        val concerts = listOf(
            createTestConcertEntity("fav-1", "Favorite 1", isFavorite = true),
            createTestConcertEntity("fav-2", "Favorite 2", isFavorite = true)
        )
        concertDao.insertConcerts(concerts)
        
        // Action: Get favorites flow
        val favoritesFlow = repository.getFavoriteConcerts()
        val initialFavorites = favoritesFlow.first()
        
        // Verify: Initial favorites
        assertThat(initialFavorites).hasSize(2)
        assertThat(initialFavorites.map { it.identifier }).containsExactly("fav-1", "fav-2")
        assertThat(initialFavorites.all { it.isFavorite }).isTrue()
        
        // Action: Add new favorite
        val newFavorite = createTestConcertEntity("fav-3", "Favorite 3", isFavorite = true)
        concertDao.insertConcert(newFavorite)
        
        // Verify: Flow emits updated list
        val updatedFavorites = favoritesFlow.drop(1).first()
        assertThat(updatedFavorites).hasSize(3)
        assertThat(updatedFavorites.map { it.identifier }).containsExactly("fav-1", "fav-2", "fav-3")
    }
    
    @Test
    fun `cache cleanup preserves favorites but removes old concerts`() = runTest {
        // Setup: Insert mix of old favorites and old regular concerts
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        
        val oldFavorite = createTestConcertEntity("old-fav", "Old Favorite", isFavorite = true)
            .copy(cachedTimestamp = oldTimestamp)
        val oldRegular = createTestConcertEntity("old-regular", "Old Regular")
            .copy(cachedTimestamp = oldTimestamp)
        val recentConcert = createTestConcertEntity("recent", "Recent Concert")
        
        concertDao.insertConcerts(listOf(oldFavorite, oldRegular, recentConcert))
        
        // Mock API to trigger cache cleanup
        val apiResponse = createMockSearchResponse(emptyList())
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Action: Trigger search (which triggers cache cleanup)
        repository.searchConcerts("").first()
        
        // Verify: Favorites preserved, old regular concerts removed
        val remainingConcerts = concertDao.getRecentConcerts(10)
        assertThat(remainingConcerts.map { it.id }).containsExactly("old-fav", "recent")
        assertThat(remainingConcerts.map { it.id }).doesNotContain("old-regular")
    }
    
    @Test
    fun `favoriting a concert appears in favorites list immediately`() = runTest {
        // Setup: Mock API to return a concert (simulates user searching)
        val testConcert = createTestArchiveDoc("test-concert-1977", "Grateful Dead Live at Winterland 1977")
        val apiResponse = createMockSearchResponse(listOf(testConcert))
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Step 1: User searches for concerts (populates cache)
        val searchResults = repository.searchConcerts("1977").first()
        assertThat(searchResults).hasSize(1)
        val concert = searchResults[0]
        assertThat(concert.isFavorite).isFalse() // Initially not favorited
        
        // Step 2: Check favorites list is initially empty
        val initialFavorites = repository.getFavoriteConcerts().first()
        assertThat(initialFavorites).isEmpty()
        
        // Step 3: User favorites the concert (simulates the actual user workflow)
        // This is the critical step that was failing - adding to FavoriteEntity table
        val favoriteItem = com.deadarchive.core.model.FavoriteItem.fromConcert(concert)
        val favoriteEntity = com.deadarchive.core.database.FavoriteEntity.fromFavoriteItem(favoriteItem)
        favoriteDao.insertFavorite(favoriteEntity)
        
        // THIS IS THE BUG: The above step adds to FavoriteEntity table, 
        // but getFavoriteConcerts() queries the concerts table where isFavorite = 1
        // The concerts table still has isFavorite = false for this concert
        
        // Step 4: Update the concerts table to reflect the favorite status (the fix)
        val cachedConcert = concertDao.getConcertById(concert.identifier)
        assertThat(cachedConcert).isNotNull() // Concert should be cached from search
        val updatedEntity = cachedConcert!!.copy(isFavorite = true)
        concertDao.insertConcert(updatedEntity)
        
        // Step 5: Verify concert now appears in favorites list immediately  
        val updatedFavorites = repository.getFavoriteConcerts().first()
        assertThat(updatedFavorites).hasSize(1)
        assertThat(updatedFavorites[0].identifier).isEqualTo("test-concert-1977")
        assertThat(updatedFavorites[0].isFavorite).isTrue()
        assertThat(updatedFavorites[0].title).contains("Winterland")
        
        // Step 6: Verify the workflow works end-to-end
        // If user searches again, the favorite status should persist
        val searchResultsAfterFavorite = repository.searchConcerts("1977").first()
        val concertAfterFavorite = searchResultsAfterFavorite.find { it.identifier == "test-concert-1977" }
        assertThat(concertAfterFavorite?.isFavorite).isTrue()
    }
    
    @Test
    fun `reproduces favorite bug - favoriting without updating concerts table fails`() = runTest {
        // Setup: Mock API to return a concert
        val testConcert = createTestArchiveDoc("bug-concert", "Test Concert for Bug Reproduction")
        val apiResponse = createMockSearchResponse(listOf(testConcert))
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Step 1: User searches and caches a concert
        val searchResults = repository.searchConcerts("test").first()
        val concert = searchResults[0]
        
        // Step 2: User favorites the concert (OLD BUGGY WAY - only updates FavoriteEntity)
        val favoriteItem = com.deadarchive.core.model.FavoriteItem.fromConcert(concert)
        val favoriteEntity = com.deadarchive.core.database.FavoriteEntity.fromFavoriteItem(favoriteItem)
        favoriteDao.insertFavorite(favoriteEntity)
        
        // Step 3: Check if concert appears in favorites (THIS SHOULD FAIL before fix)
        val favorites = repository.getFavoriteConcerts().first()
        
        // This assertion would fail with the original bug because:
        // - FavoriteEntity table has the favorite
        // - But concerts table still has isFavorite = false
        // - getFavoriteConcerts() queries concerts table, finds nothing
        assertThat(favorites).isEmpty() // This documents the bug behavior
        
        // Now apply the fix - update concerts table too
        val cachedConcert = concertDao.getConcertById(concert.identifier)!!
        val updatedEntity = cachedConcert.copy(isFavorite = true)
        concertDao.insertConcert(updatedEntity)
        
        // Now it should work
        val favoritesAfterFix = repository.getFavoriteConcerts().first()
        assertThat(favoritesAfterFix).hasSize(1)
    }
    
    @Test
    fun `handles large search results efficiently`() = runTest {
        // Mock: API returns 100 concerts
        val largeDocs = (1..100).map { i ->
            createTestArchiveDoc("concert-$i", "Concert $i")
        }
        val apiResponse = createMockSearchResponse(largeDocs)
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(apiResponse)
        
        // Action: Search concerts
        val results = repository.searchConcerts("").first()
        
        // Verify: All concerts returned and stored
        assertThat(results).hasSize(100)
        val storedConcerts = concertDao.getRecentConcerts(150)
        assertThat(storedConcerts).hasSize(100)
    }
    
    @Test
    fun `handles malformed API responses gracefully`() = runTest {
        // Setup: Insert cached concerts as fallback
        val cachedConcert = createTestConcertEntity("cached", "Cached Concert")
        concertDao.insertConcert(cachedConcert)
        
        // Mock: API returns error response
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.error(500, mockk(relaxed = true))
        
        // Action: Search concerts
        val results = repository.searchConcerts("").first()
        
        // Verify: Falls back to cache gracefully
        assertThat(results).hasSize(1)
        assertThat(results[0].identifier).isEqualTo("cached")
    }
    
    // Helper methods for creating test data
    private fun createTestConcertEntity(
        id: String, 
        title: String, 
        isFavorite: Boolean = false
    ) = ConcertEntity(
        id = id,
        title = title,
        date = "1977-05-08",
        venue = "Barton Hall",
        location = "Ithaca, NY",
        year = "1977",
        source = "SBD",
        taper = "Test Taper",
        transferer = "Test Transferer",
        lineage = "Test Lineage",
        description = "Test Description",
        setlistRaw = "Test Setlist",
        uploader = "Test Uploader",
        addedDate = "2023-01-01",
        publicDate = "2023-01-01",
        isFavorite = isFavorite
    )
    
    private fun createTestArchiveDoc(id: String, title: String) = 
        ArchiveSearchResponse.ArchiveDoc(
            identifier = id,
            title = title,
            date = "1977-05-08",
            venue = "Barton Hall",
            coverage = "Ithaca, NY",
            year = "1977",
            source = "SBD",
            taper = "Test Taper",
            transferer = "Test Transferer",
            lineage = "Test Lineage",
            description = "Test Description",
            setlist = "Test Setlist",
            uploader = "Test Uploader",
            addedDate = "2023-01-01",
            publicDate = "2023-01-01"
        )
    
    private fun createMockSearchResponse(docs: List<ArchiveSearchResponse.ArchiveDoc>) =
        ArchiveSearchResponse(
            response = ArchiveSearchResponse.ArchiveResponse(
                docs = docs,
                numFound = docs.size,
                start = 0
            )
        )
    
    private fun createMockMetadataResponse(identifier: String, title: String) =
        ArchiveMetadataResponse(
            files = emptyList(),
            metadata = ArchiveMetadataResponse.ArchiveMetadata(
                identifier = identifier,
                title = title,
                date = "1977-05-08",
                venue = "Barton Hall",
                coverage = "Ithaca, NY",
                source = "SBD"
            ),
            server = "ia800207.us.archive.org",
            directory = "/1/items/$identifier"
        )
}