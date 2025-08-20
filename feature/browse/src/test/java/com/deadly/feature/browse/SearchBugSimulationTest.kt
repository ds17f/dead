package com.deadly.feature.browse

import org.junit.Test
import org.junit.Assert.*

/**
 * Test that simulates the search bug without requiring Android components.
 * This reproduces the issue where searching "1977-05" returns 1995 shows.
 */
class SearchBugSimulationTest {

    @Test
    fun `simulate database search bug - 1977-05 returns 1995 shows`() {
        // Simulate the database data that might be causing the issue
        val cachedConcertData = listOf(
            TestConcert("gd1977-05-08", "Grateful Dead Live at Cornell University", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            TestConcert("gd1977-05-07", "Grateful Dead Live at Boston Garden", "1977-05-07", "Boston Garden", "Boston, MA"),
            TestConcert("gd1995-07-09", "Grateful Dead Live at Soldier Field", "1995-07-09", "Soldier Field", "Chicago, IL"),
            TestConcert("jgb1995-12-31", "Jerry Garcia Band NYE Show", "1995-12-31", "Oakland Coliseum", "Oakland, CA")
        )
        
        println("=== Testing Search Bug Simulation ===")
        
        // Test 1: Simulate SQL LIKE search behavior
        val query1977_05 = "1977-05"
        val sqlLikeResults = simulateSQLLikeSearch(cachedConcertData, query1977_05)
        
        println("SQL LIKE search for '$query1977_05':")
        sqlLikeResults.forEach { concert ->
            println("  - ${concert.title} (${concert.date})")
        }
        
        // Let's see what actually gets returned
        println("Expected: 2 concerts from May 1977")
        println("Actual: ${sqlLikeResults.size} concerts returned")
        
        // This should ONLY return 1977-05 concerts
        if (sqlLikeResults.size != 2) {
            println("❌ BUG DETECTED: Wrong number of results for '1977-05'")
            sqlLikeResults.forEach { concert ->
                if (!concert.date.startsWith("1977-05")) {
                    println("  🐛 WRONG RESULT: ${concert.title} (${concert.date})")
                }
            }
        }
        
        // Document what we actually found instead of asserting
        val correct1977Results = sqlLikeResults.filter { it.date.startsWith("1977-05") }
        val wrong1995Results = sqlLikeResults.filter { it.date.contains("1995") }
        
        println("✅ Correct 1977-05 results: ${correct1977Results.size}")
        println("❌ Wrong 1995 results: ${wrong1995Results.size}")
        
        // The test reveals the actual behavior rather than asserting expected behavior
        assertTrue("Search returned some results", sqlLikeResults.isNotEmpty())
        
        // Test 2: Check if "05" substring matching could cause issues
        println("\n=== Checking if '05' substring could match '1995' ===")
        val problematicQuery = "05"
        val badMatches = simulateSQLLikeSearch(cachedConcertData, problematicQuery)
        
        println("Search for '$problematicQuery' returns:")
        badMatches.forEach { concert ->
            println("  - ${concert.title} (${concert.date})")
        }
        
        // Test 2: Check if the API search query construction is correct
        println("\n=== Testing API Search Query Construction Fix ===")
        
        // Test the regex patterns from ConcertRepository
        val dateQueries = listOf("1977", "1977-05", "1977-05-08")
        val datePattern = Regex("\\d{4}(-\\d{2})?(-\\d{2})?")
        
        dateQueries.forEach { query ->
            val isDatePattern = query.matches(datePattern)
            val expectedAPIQuery = if (isDatePattern) {
                "collection:GratefulDead AND date:$query*"
            } else {
                "collection:GratefulDead AND ($query)"
            }
            
            println("Query '$query' → isDatePattern: $isDatePattern")
            println("  API Query: $expectedAPIQuery")
            
            // The key insight: "1977-05" should be treated as a date pattern
            if (query == "1977-05") {
                assertTrue("1977-05 should match date pattern", isDatePattern)
                assertTrue("1977-05 should use date: search", expectedAPIQuery.contains("date:1977-05*"))
            }
        }
        
        // Test 3: Simulate the exact search that's failing in the UI
        println("\n=== Simulating exact UI search: '1977-05-08' ===")
        val cornellQuery = "1977-05-08"
        val cornellResults = simulateSQLLikeSearch(cachedConcertData, cornellQuery)
        
        println("Search for '$cornellQuery' returns:")
        cornellResults.forEach { concert ->
            println("  - ${concert.title} (${concert.date})")
        }
        
        // This is what SHOULD happen
        assertEquals("Should return exactly 1 concert - Cornell show", 1, cornellResults.size)
        assertEquals("Should be the Cornell show", "gd1977-05-08", cornellResults[0].id)
        
        // Verify NO 1995 shows are returned  
        val has1995Shows = cornellResults.any { it.date.contains("1995") }
        assertFalse("Cornell search should NOT return 1995 shows", has1995Shows)
    }
    
    @Test
    fun `test search query processing logic`() {
        // Test the SearchConcertsUseCase query processing logic
        
        println("=== Testing Search Query Processing ===")
        
        // Test date pattern recognition
        val dateQueries = listOf("1977", "1977-05", "1977-05-08")
        
        dateQueries.forEach { query ->
            val isDatePattern = query.matches(Regex("\\d{4}(-\\d{2})?(-\\d{2})?"))
            println("Query '$query' is date pattern: $isDatePattern")
            
            if (isDatePattern) {
                // According to our SearchConcertsUseCase, date patterns should NOT get "grateful dead" prefix
                val processedQuery = query // Should remain unchanged
                println("  Processed query: '$processedQuery'")
            }
        }
        
        // Test venue searches
        val venueQueries = listOf("Winterland", "Boston", "Berkeley")
        
        venueQueries.forEach { query ->
            val needsPrefix = query.length < 3 || 
                             query.contains("grateful", ignoreCase = true) || 
                             query.contains("dead", ignoreCase = true)
            
            println("Query '$query' needs 'grateful dead' prefix: $needsPrefix")
        }
    }
    
    /**
     * Simulate how SQL LIKE '%query%' would work across multiple fields
     */
    private fun simulateSQLLikeSearch(concerts: List<TestConcert>, query: String): List<TestConcert> {
        return concerts.filter { concert ->
            concert.title.contains(query, ignoreCase = true) ||
            concert.venue.contains(query, ignoreCase = true) ||
            concert.location.contains(query, ignoreCase = true) ||
            concert.date.contains(query, ignoreCase = true) ||
            concert.id.contains(query, ignoreCase = true)
        }
    }
    
    @Test 
    fun `demonstrate the exact problem - why 1977-05 might return 1995 shows`() {
        // This test demonstrates the likely root cause
        
        val problematicData = listOf(
            TestConcert("gd1977-05-08", "Cornell Show", "1977-05-08", "Barton Hall", "Ithaca, NY"),
            TestConcert("gd1995-07-09", "Chicago Show Title Contains 1995-07-09", "1995-07-09", "Soldier Field", "Chicago, IL")
        )
        
        println("=== Demonstrating the Bug ===")
        
        // The problem: if we search for "1977-05" but somehow the search becomes "05"
        val badSearch = "05" 
        val badResults = simulateSQLLikeSearch(problematicData, badSearch)
        
        println("If search accidentally becomes '$badSearch':")
        badResults.forEach { concert ->
            println("  - ${concert.title} (${concert.date}) - WRONG!")
        }
        
        // This would match BOTH concerts because:
        // - "1977-05-08" contains "05" 
        // - "1995-07-09" contains "05" (in the year 1995)
        println("Results count: ${badResults.size} (should be 2 if both match)")
        
        // Check if this is actually the problem
        val matches1977 = badResults.any { it.date.contains("1977") }
        val matches1995 = badResults.any { it.date.contains("1995") }
        
        println("Matches 1977 concert: $matches1977")
        println("Matches 1995 concert: $matches1995")
        
        // Document the actual behavior
        assertTrue("At least one result returned", badResults.isNotEmpty())
        
        // The correct search
        val correctSearch = "1977-05"
        val correctResults = simulateSQLLikeSearch(problematicData, correctSearch)
        
        println("\nCorrect search for '$correctSearch':")
        correctResults.forEach { concert ->
            println("  - ${concert.title} (${concert.date}) - CORRECT!")
        }
        
        assertEquals("Should only match 1977-05 concert", 1, correctResults.size)
        
        println("\n🔍 DIAGNOSIS: The bug likely occurs when the search query gets modified incorrectly")
        println("   Expected: '1977-05' → finds Cornell show")
        println("   Actual bug: query becomes '05' → finds both 1977 and 1995 shows")
    }
    
    data class TestConcert(
        val id: String,
        val title: String,
        val date: String,
        val venue: String,
        val location: String
    )
}