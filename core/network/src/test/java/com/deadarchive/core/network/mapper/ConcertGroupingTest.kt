package com.deadarchive.core.network.mapper

import com.deadarchive.core.model.Concert
import com.deadarchive.core.network.mapper.ArchiveMapper.groupByConcert
import com.deadarchive.core.network.mapper.ArchiveMapper.migrateToConcertNew
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Test concert grouping using real data from testdata/complete_catalog_response.json
 */
class ConcertGroupingTest {

    @Serializable
    data class TestCatalogResponse(
        val metadata: TestMetadata,
        val sample_concerts: List<TestConcert>
    )

    @Serializable
    data class TestMetadata(
        val exported_at: String,
        val source: String,
        val total_concerts: Int
    )

    @Serializable
    data class TestConcert(
        val identifier: String,
        val title: String,
        val date: String,
        val venue: String? = null,
        val location: String? = null,
        val year: Int? = null,
        val source: String? = null,
        val description: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testRealDataGrouping() {
        // Load the real test data from resources
        val testDataStream = this.javaClass.classLoader.getResourceAsStream("complete_catalog_response.json")
        assertNotNull("Test data file should exist in resources", testDataStream)
        
        val jsonContent = testDataStream!!.bufferedReader().use { it.readText() }
        val testData = json.decodeFromString<TestCatalogResponse>(jsonContent)
        
        println("=== REAL DATA GROUPING TEST ===")
        println("Loaded ${testData.sample_concerts.size} concerts from test data")
        
        // Convert test data to Concert models
        val concerts = testData.sample_concerts.map { testConcert ->
            Concert(
                identifier = testConcert.identifier,
                title = testConcert.title,
                date = testConcert.date,
                venue = testConcert.venue,
                location = testConcert.location,
                year = testConcert.year?.toString(),
                source = testConcert.source,
                description = testConcert.description
            )
        }
        
        println("\nInput concerts by date:")
        concerts.groupBy { it.date }.forEach { (date, dateConcerts) ->
            println("  $date: ${dateConcerts.size} recordings")
            dateConcerts.forEach { concert ->
                println("    - ${concert.identifier} venue='${concert.venue}'")
            }
        }
        
        // Apply the grouping logic
        val groupedConcerts = concerts.migrateToConcertNew()
        
        println("\nOutput grouped concerts:")
        groupedConcerts.forEach { concert ->
            println("  ${concert.date} at '${concert.venue}': ${concert.recordingCount} recordings")
            concert.recordings.forEach { recording ->
                println("    - ${recording.identifier}")
            }
        }
        
        // Assertions - this is the critical test
        val concertsFor1995_07_09 = groupedConcerts.filter { it.date == "1995-07-09" }
        assertEquals(
            "Should have exactly 1 concert for 1995-07-09 (all recordings should be grouped together)",
            1,
            concertsFor1995_07_09.size
        )
        
        val concert = concertsFor1995_07_09.first()
        assertEquals("Should have 10 recordings for 1995-07-09", 10, concert.recordingCount)
        assertEquals("Venue should be normalized", "Soldier Field", concert.venue)
        
        // Verify all recordings are accounted for
        val originalCount = concerts.count { it.date == "1995-07-09T00:00:00Z" }
        assertEquals("Should preserve all recordings", originalCount, concert.recordingCount)
        
        println("\n✅ SUCCESS: 1995-07-09 correctly grouped into 1 concert with ${concert.recordingCount} recordings")
    }
    
    @Test
    fun testVenueFuzzyMatching() {
        // Test the fuzzy matching specifically
        val testConcerts = listOf(
            Concert(
                identifier = "test1",
                title = "Test 1",
                date = "1993-05-16",
                venue = "Sam Boyd Silver Bowl",
                source = "SBD"
            ),
            Concert(
                identifier = "test2", 
                title = "Test 2",
                date = "1993-05-16",
                venue = "Sam Boyd Silver Bowl, U.N.L.V.",
                source = "AUD"
            ),
            Concert(
                identifier = "test3",
                title = "Test 3", 
                date = "1993-05-16",
                venue = "Sam Boyd Silver Bowl",
                source = "Matrix"
            )
        )
        
        val grouped = testConcerts.migrateToConcertNew()
        
        println("=== VENUE FUZZY MATCHING TEST ===")
        println("Input: ${testConcerts.size} concerts")
        println("Output: ${grouped.size} concerts")
        
        grouped.forEach { concert ->
            println("Concert: date=${concert.date}, venue='${concert.venue}', recordings=${concert.recordingCount}")
        }
        
        assertEquals("Should group all recordings into 1 concert", 1, grouped.size)
        assertEquals("Should have 3 recordings", 3, grouped.first().recordingCount)
    }

    // ==================== PROBLEMATIC CASE TEST FRAMEWORK ====================
    
    /**
     * Data class representing a problematic concert grouping case
     */
    data class ProblematicCase(
        val name: String,
        val description: String,
        val concerts: List<Concert>,
        val expectedGroups: Int,
        val expectedRecordingsPerGroup: Map<String, Int> = emptyMap(),
        val additionalAssertions: ((grouped: List<com.deadarchive.core.model.ConcertNew>) -> Unit)? = null
    )
    
    /**
     * Test runner for problematic cases
     */
    private fun testProblematicCase(case: ProblematicCase) {
        println("=== TESTING: ${case.name} ===")
        println("Description: ${case.description}")
        println("Input: ${case.concerts.size} recordings")
        
        // Show input data
        case.concerts.groupBy { it.date }.forEach { (date, dateConcerts) ->
            println("  $date: ${dateConcerts.size} recordings")
            dateConcerts.forEach { concert ->
                println("    - ${concert.identifier} venue='${concert.venue}' source='${concert.source}'")
            }
        }
        
        // Apply grouping
        val grouped = case.concerts.migrateToConcertNew()
        
        println("\nOutput: ${grouped.size} groups")
        grouped.forEach { concert ->
            println("  ${concert.date} at '${concert.venue}': ${concert.recordingCount} recordings")
            concert.recordings.forEach { recording ->
                println("    - ${recording.identifier} (${recording.source})")
            }
        }
        
        // Basic assertions
        assertEquals(
            "Expected ${case.expectedGroups} groups for case '${case.name}'",
            case.expectedGroups,
            grouped.size
        )
        
        // Specific group size assertions
        case.expectedRecordingsPerGroup.forEach { (groupKey, expectedCount) ->
            val group = grouped.find { "${it.date}_${it.venue}" == groupKey }
            assertNotNull("Group '$groupKey' should exist", group)
            assertEquals(
                "Group '$groupKey' should have $expectedCount recordings",
                expectedCount,
                group!!.recordingCount
            )
        }
        
        // Custom assertions
        case.additionalAssertions?.invoke(grouped)
        
        println("✅ SUCCESS: ${case.name} passed all assertions")
        println()
    }
    
    @Test
    fun testAllProblematicCases() {
        val problematicCases = listOf(
            // Case 1: Venue name variations with punctuation
            ProblematicCase(
                name = "Sam Boyd Silver Bowl Variations",
                description = "Venue names with and without institutional suffixes should group together",
                concerts = listOf(
                    Concert(
                        identifier = "gd1993-05-16.sbd.smith.shnf",
                        title = "1993-05-16 Sam Boyd Silver Bowl",
                        date = "1993-05-16",
                        venue = "Sam Boyd Silver Bowl",
                        source = "SBD"
                    ),
                    Concert(
                        identifier = "gd1993-05-16.aud.jones.flac16",
                        title = "1993-05-16 Sam Boyd Silver Bowl, U.N.L.V.",
                        date = "1993-05-16",
                        venue = "Sam Boyd Silver Bowl, U.N.L.V.",
                        source = "AUD"
                    ),
                    Concert(
                        identifier = "gd1993-05-16.matrix.doe.shnf",
                        title = "1993-05-16 Sam Boyd Silver Bowl (UNLV)",
                        date = "1993-05-16",
                        venue = "Sam Boyd Silver Bowl (UNLV)",
                        source = "Matrix"
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1993-05-16_Sam Boyd Silver Bowl" to 3)
            ),
            
            // Case 2: Madison Square Garden variations (using real Dead show 1981-10-19)
            ProblematicCase(
                name = "Madison Square Garden Variations", 
                description = "MSG vs Madison Square Garden vs Madison Sq Garden should group together",
                concerts = listOf(
                    Concert(
                        identifier = "gd1981-10-19.sbd.anonymous.shnf",
                        title = "1981-10-19 Madison Square Garden",
                        date = "1981-10-19",
                        venue = "Madison Square Garden",
                        source = "SBD"
                    ),
                    Concert(
                        identifier = "gd1981-10-19.aud.taper.flac16",
                        title = "1981-10-19 MSG",
                        date = "1981-10-19", 
                        venue = "MSG",
                        source = "AUD"
                    ),
                    Concert(
                        identifier = "gd1981-10-19.matrix.source.shnf",
                        title = "1981-10-19 Madison Sq Garden",
                        date = "1981-10-19",
                        venue = "Madison Sq Garden", 
                        source = "Matrix"
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1981-10-19_Madison Square Garden" to 3)
            ),
            
            // Case 3: Amphitheater name variations
            ProblematicCase(
                name = "Amphitheater Name Variations",
                description = "Amphitheater vs Amphitheatre vs Ampitheater spelling variations",
                concerts = listOf(
                    Concert(
                        identifier = "gd1987-07-19.sbd.miller.shnf",
                        title = "1987-07-19 Alpine Valley Music Theatre",
                        date = "1987-07-19",
                        venue = "Alpine Valley Music Theatre",
                        source = "SBD"
                    ),
                    Concert(
                        identifier = "gd1987-07-19.aud.taper2.flac16",
                        title = "1987-07-19 Alpine Valley Music Theater",
                        date = "1987-07-19",
                        venue = "Alpine Valley Music Theater",
                        source = "AUD"
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1987-07-19_Alpine Valley Music Theatre" to 2)
            ),
            
            // Case 4: Empty venue handling
            ProblematicCase(
                name = "Empty Venue Handling",
                description = "Recordings with missing venue info should still group by date",
                concerts = listOf(
                    Concert(
                        identifier = "gd1995-07-09.sbd.unknown.shnf",
                        title = "1995-07-09",
                        date = "1995-07-09",
                        venue = null,
                        source = "SBD"
                    ),
                    Concert(
                        identifier = "gd1995-07-09.aud.mystery.flac16",
                        title = "1995-07-09",
                        date = "1995-07-09",
                        venue = "",
                        source = "AUD"
                    ),
                    Concert(
                        identifier = "gd1995-07-09.matrix.anon.shnf",
                        title = "1995-07-09",
                        date = "1995-07-09",
                        venue = null,
                        source = "Matrix"
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1995-07-09_Unknown" to 3)
            ),
            
            // Case 5: Different dates should NOT group
            ProblematicCase(
                name = "Different Dates Should Not Group",
                description = "Same venue but different dates should create separate concerts",
                concerts = listOf(
                    Concert(
                        identifier = "gd1995-07-08.sbd.early.shnf",
                        title = "1995-07-08 Soldier Field",
                        date = "1995-07-08",
                        venue = "Soldier Field",
                        source = "SBD"
                    ),
                    Concert(
                        identifier = "gd1995-07-09.sbd.late.shnf", 
                        title = "1995-07-09 Soldier Field",
                        date = "1995-07-09",
                        venue = "Soldier Field",
                        source = "SBD"
                    )
                ),
                expectedGroups = 2,
                expectedRecordingsPerGroup = mapOf(
                    "1995-07-08_Soldier Field" to 1,
                    "1995-07-09_Soldier Field" to 1
                )
            )
        )
        
        // Run all problematic cases
        problematicCases.forEach { case ->
            testProblematicCase(case)
        }
        
        println("🎉 ALL PROBLEMATIC CASES PASSED!")
    }
    
    /**
     * Helper method to easily add a new problematic case from real-world data
     * 
     * Usage:
     * 1. Find a problematic show in your app (e.g., duplicate concerts for same date/venue)
     * 2. Export the concert data (identifiers, dates, venues, sources)
     * 3. Call this method with the data to create a test case
     * 4. Run the test to verify it fails initially
     * 5. Fix the grouping logic
     * 6. Run again to verify it passes
     * 7. Add the case to testAllProblematicCases() to prevent regression
     */
    fun createProblematicCaseFromRealData(
        caseName: String,
        description: String,
        realWorldConcerts: List<Pair<String, Pair<String, String?>>>, // (identifier, (date, venue))
        expectedGroupCount: Int
    ): ProblematicCase {
        val concerts = realWorldConcerts.map { (identifier, dateVenue) ->
            Concert(
                identifier = identifier,
                title = "${dateVenue.first} ${dateVenue.second ?: "Unknown"}",
                date = dateVenue.first,
                venue = dateVenue.second,
                source = "SBD" // Default, can be extracted from identifier if needed
            )
        }
        
        return ProblematicCase(
            name = caseName,
            description = description,
            concerts = concerts,
            expectedGroups = expectedGroupCount
        )
    }
}