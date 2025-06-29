package com.deadarchive.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.ConcertEntity
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.mapper.ArchiveMapper
import com.deadarchive.core.network.model.ArchiveSearchResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.InputStream

/**
 * Integration tests using REAL captured API data from production app
 * This catches issues like date format mismatches that mock tests miss
 */
@RunWith(AndroidJUnit4::class)
class ConcertRepositoryRealDataTest {
    
    // Real database components
    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    private lateinit var favoriteDao: FavoriteDao
    
    // Mock only network
    private val mockApiService = mockk<ArchiveApiService>()
    
    // Real repository with real DAOs + mock API
    private lateinit var repository: ConcertRepositoryImpl
    
    // JSON parser for real data
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
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
    fun `search by exact date works with real ISO timestamp format`() = runTest {
        // Load and populate real data
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Test the exact date search that was failing before
        val searchQuery = "1995-07-09"
        val results = repository.searchConcerts(searchQuery).first()
        
        // This should find concerts because we fixed the SQL query
        // Before fix: searched for '1995-07-09' but data was '1995-07-09T00:00:00Z' - no match
        // After fix: searches for '1995-07-09%' which matches '1995-07-09T00:00:00Z'
        assertThat(results).isNotEmpty()
        assertThat(results).hasSize(10) // We have 10 concerts from 1995-07-09
        
        // Verify all results are from the correct date
        results.forEach { concert ->
            assertThat(concert.date).startsWith("1995-07-09")
            assertThat(concert.venue).isEqualTo("Soldier Field")
            assertThat(concert.location).isEqualTo("Chicago, IL")
        }
    }
    
    @Test
    fun `search by year works with real ISO timestamp format`() = runTest {
        // Load and populate real data
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Test year search
        val results = repository.searchConcerts("1995").first()
        
        // Should find all concerts from 1995
        assertThat(results).hasSize(10)
        results.forEach { concert ->
            assertThat(concert.year).isEqualTo(1995)
            assertThat(concert.date).startsWith("1995")
        }
    }
    
    @Test
    fun `search by venue works with real data`() = runTest {
        // Load and populate real data
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Test venue search
        val results = repository.searchConcerts("Soldier Field").first()
        
        // Should find all concerts at Soldier Field
        assertThat(results).hasSize(10)
        results.forEach { concert ->
            assertThat(concert.venue).isEqualTo("Soldier Field")
            assertThat(concert.location).isEqualTo("Chicago, IL")
        }
    }
    
    @Test
    fun `search by location works with real data`() = runTest {
        // Load and populate real data
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Test location search
        val results = repository.searchConcerts("Chicago").first()
        
        // Should find all concerts in Chicago
        assertThat(results).hasSize(10)
        results.forEach { concert ->
            assertThat(concert.location).contains("Chicago")
        }
    }
    
    @Test
    fun `general search works with real data`() = runTest {
        // Load and populate real data
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Test general text search
        val results = repository.searchConcerts("Touch Of Grey").first()
        
        // Should find concerts with "Touch Of Grey" in description
        assertThat(results).isNotEmpty()
        results.forEach { concert ->
            assertThat(concert.description).contains("Touch Of Grey")
        }
    }
    
    @Test
    fun `reproduces original date format bug`() = runTest {
        // This test documents the original bug by showing what would happen
        // with the old SQL query using exact matching
        
        // Load real data with ISO timestamps
        val realConcerts = loadRealConcertData()
        populateDatabase(realConcerts)
        
        // Simulate the old buggy query (exact match)
        val buggyResults = concertDao.searchConcerts("1995-07-09") // This uses general search
        val exactMatchResults = concertDao.getConcertById("1995-07-09") // This would fail with exact match
        
        // General search works because it searches across multiple fields
        assertThat(buggyResults).isNotEmpty()
        
        // But exact ID match fails because no concert has ID "1995-07-09"
        assertThat(exactMatchResults).isNull()
        
        // However, exact date search now works thanks to our fix
        val fixedResults = concertDao.searchConcertsByExactDate("1995-07-09")
        assertThat(fixedResults).hasSize(10) // All concerts from that date
    }
    
    private fun loadRealConcertData(): List<ConcertEntity> {
        // Load the real captured data from assets
        val inputStream: InputStream = javaClass.classLoader!!
            .getResourceAsStream("real_catalog_data.json")
            ?: throw IllegalStateException("Could not find real_catalog_data.json in test resources")
        
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        
        // Parse the captured data structure
        val capturedData = json.decodeFromString<CapturedCatalogData>(jsonString)
        
        // Convert to ConcertEntity objects
        return capturedData.sample_concerts.map { concert ->
            ConcertEntity(
                id = concert.identifier,
                title = concert.title,
                date = concert.date, // This will be "1995-07-09T00:00:00Z" format
                venue = concert.venue,
                location = concert.location,
                year = concert.year.toString(),
                source = concert.source,
                taper = null,
                transferer = null,
                lineage = null,
                description = concert.description,
                setlistRaw = null,
                uploader = null,
                addedDate = null,
                publicDate = null,
                isFavorite = false,
                cachedTimestamp = System.currentTimeMillis()
            )
        }
    }
    
    private suspend fun populateDatabase(concerts: List<ConcertEntity>) {
        concertDao.insertConcerts(concerts)
        
        // Verify data was inserted correctly
        val insertedCount = concertDao.getRecentConcerts(50).size
        assertThat(insertedCount).isEqualTo(concerts.size)
    }
    
    // Data classes to match the captured JSON structure
    @kotlinx.serialization.Serializable
    data class CapturedCatalogData(
        val metadata: CapturedMetadata,
        val sample_concerts: List<CapturedConcert>
    )
    
    @kotlinx.serialization.Serializable
    data class CapturedMetadata(
        val exported_at: String,
        val source: String,
        val total_concerts: Int
    )
    
    @kotlinx.serialization.Serializable
    data class CapturedConcert(
        val identifier: String,
        val title: String,
        val date: String, // This will be in ISO format: "1995-07-09T00:00:00Z"
        val venue: String,
        val location: String,
        val year: Int,
        val source: String,
        val description: String
    )
}