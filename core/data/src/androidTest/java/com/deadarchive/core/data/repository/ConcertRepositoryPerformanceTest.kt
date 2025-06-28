package com.deadarchive.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.deadarchive.core.database.ConcertDao
import com.deadarchive.core.database.DeadArchiveDatabase
import com.deadarchive.core.database.FavoriteDao
import com.deadarchive.core.network.ArchiveApiService
import com.deadarchive.core.network.model.ArchiveSearchResponse
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import kotlin.system.measureTimeMillis

/**
 * Performance and concurrency tests for ConcertRepository
 * These tests ensure the repository handles edge cases and concurrent operations correctly
 */
@RunWith(AndroidJUnit4::class)
class ConcertRepositoryPerformanceTest {
    
    private lateinit var database: DeadArchiveDatabase
    private lateinit var concertDao: ConcertDao
    private lateinit var favoriteDao: FavoriteDao
    private val mockApiService = mockk<ArchiveApiService>()
    private lateinit var repository: ConcertRepositoryImpl
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DeadArchiveDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
            
        concertDao = database.concertDao()
        favoriteDao = database.favoriteDao()
        repository = ConcertRepositoryImpl(mockApiService, concertDao, favoriteDao)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun `concurrent searches dont corrupt database state`() = runTest {
        // Mock API to return different results for different queries
        coEvery { mockApiService.searchConcerts(match { it.contains("query1") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("query1", 50))
        coEvery { mockApiService.searchConcerts(match { it.contains("query2") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("query2", 50))
        coEvery { mockApiService.searchConcerts(match { it.contains("query3") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("query3", 50))
        
        // Launch multiple concurrent searches
        val searches = listOf(
            async { repository.searchConcerts("query1").first() },
            async { repository.searchConcerts("query2").first() },
            async { repository.searchConcerts("query3").first() }
        )
        
        // Wait for all to complete
        val results = searches.awaitAll()
        
        // Verify all searches completed successfully
        assertThat(results).hasSize(3)
        results.forEach { result ->
            assertThat(result).hasSize(50)
        }
        
        // Verify database state is consistent (should have all concerts)
        val allConcerts = concertDao.getRecentConcerts(200)
        assertThat(allConcerts).hasSize(150) // 50 each from 3 queries
        
        // Verify no duplicate concerts based on ID
        val uniqueIds = allConcerts.map { it.id }.toSet()
        assertThat(uniqueIds).hasSize(150) // All should be unique
    }
    
    @Test
    fun `search performance scales reasonably with result size`() = runTest {
        // Test with different result sizes
        val smallSize = 10
        val mediumSize = 50  
        val largeSize = 100
        
        // Mock API responses
        coEvery { mockApiService.searchConcerts(match { it.contains("small") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("small", smallSize))
        coEvery { mockApiService.searchConcerts(match { it.contains("medium") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("medium", mediumSize))
        coEvery { mockApiService.searchConcerts(match { it.contains("large") }, any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("large", largeSize))
        
        // Measure execution times
        val smallTime = measureTimeMillis {
            repository.searchConcerts("small").first()
        }
        
        val mediumTime = measureTimeMillis {
            repository.searchConcerts("medium").first()
        }
        
        val largeTime = measureTimeMillis {
            repository.searchConcerts("large").first()
        }
        
        // Performance should scale reasonably (not exponentially)
        // Large shouldn't be more than 10x slower than small
        assertThat(largeTime).isLessThan(smallTime * 10L)
        assertThat(mediumTime).isLessThan(largeTime)
        
        // All operations should complete within reasonable time (2 seconds)
        assertThat(smallTime).isLessThan(2000L)
        assertThat(mediumTime).isLessThan(2000L)
        assertThat(largeTime).isLessThan(2000L)
    }
    
    @Test
    fun `cache lookup performance remains fast with large cached dataset`() = runTest {
        // Setup: Insert large number of concerts in cache
        val largeCachedDataset = (1..500).map { i ->
            createTestConcertEntity("cached-$i", "Cached Concert $i")
        }
        concertDao.insertConcerts(largeCachedDataset)
        
        // Mock API to return empty (force cache usage)
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("empty", 0))
        
        // Measure cache lookup time
        val lookupTime = measureTimeMillis {
            repository.searchConcerts("Concert 250").first() // Should find in cache
        }
        
        // Cache lookup should be fast even with large dataset
        assertThat(lookupTime).isLessThan(1000L) // Less than 1 second
    }
    
    @Test
    fun `memory usage remains stable during multiple operations`() = runTest {
        // This test ensures we don't have memory leaks
        // Perform many operations to stress test memory usage
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Mock API responses
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(createMockSearchResponse("memory-test", 50))
        
        // Perform many search operations
        repeat(20) { i ->
            repository.searchConcerts("memory-test-$i").first()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 50MB)
        val maxReasonableIncrease = 50 * 1024 * 1024 // 50MB
        assertThat(memoryIncrease).isLessThan(maxReasonableIncrease)
    }
    
    @Test
    fun `database operations handle constraint violations gracefully`() = runTest {
        // Setup: Insert concert with specific ID
        val originalConcert = createTestConcertEntity("duplicate-id", "Original Concert")
        concertDao.insertConcert(originalConcert)
        
        // Mock API to return concert with same ID but different data
        val conflictingDoc = ArchiveSearchResponse.ArchiveDoc(
            identifier = "duplicate-id",
            title = "Updated Concert", // Different title
            date = "1977-05-08"
        )
        coEvery { mockApiService.searchConcerts(any(), any(), any(), any(), any(), any()) } returns 
            Response.success(ArchiveSearchResponse(
                response = ArchiveSearchResponse.ArchiveResponse(
                    docs = listOf(conflictingDoc),
                    numFound = 1,
                    start = 0
                )
            ))
        
        // Action: Search should handle the ID conflict gracefully
        val results = repository.searchConcerts("test").first()
        
        // Verify: Operation completes successfully and returns updated data
        assertThat(results).hasSize(1)
        assertThat(results[0].identifier).isEqualTo("duplicate-id")
        assertThat(results[0].title).isEqualTo("Updated Concert") // Should be updated
        
        // Verify: Database state is consistent
        val cachedConcert = concertDao.getConcertById("duplicate-id")
        assertThat(cachedConcert?.title).isEqualTo("Updated Concert")
    }
    
    // Helper methods
    private fun createMockSearchResponse(queryPrefix: String, count: Int) = 
        ArchiveSearchResponse(
            response = ArchiveSearchResponse.ArchiveResponse(
                docs = (1..count).map { i ->
                    ArchiveSearchResponse.ArchiveDoc(
                        identifier = "$queryPrefix-concert-$i",
                        title = "$queryPrefix Concert $i",
                        date = "1977-05-08",
                        venue = "Test Venue $i"
                    )
                },
                numFound = count,
                start = 0
            )
        )
    
    private fun createTestConcertEntity(id: String, title: String) = 
        com.deadarchive.core.database.ConcertEntity(
            id = id,
            title = title,
            date = "1977-05-08",
            venue = "Test Venue",
            location = "Test Location",
            year = "1977",
            source = "SBD",
            taper = null,
            transferer = null,
            lineage = null,
            description = null,
            setlistRaw = null,
            uploader = null,
            addedDate = null,
            publicDate = null
        )
}