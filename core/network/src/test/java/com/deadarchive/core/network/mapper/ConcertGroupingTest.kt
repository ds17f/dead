package com.deadarchive.core.network.mapper

import com.deadarchive.core.model.Recording
import com.deadarchive.core.network.mapper.ArchiveMapper.toShows
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
        
        // Convert test data to Recording models
        val recordings = testData.sample_concerts.map { testConcert ->
            Recording(
                identifier = testConcert.identifier,
                title = testConcert.title,
                concertDate = testConcert.date,
                concertVenue = testConcert.venue,
                concertLocation = testConcert.location,
                source = testConcert.source,
                description = testConcert.description,
                isFavorite = false,
                isDownloaded = false
            )
        }
        
        println("\nInput recordings by date:")
        recordings.groupBy { it.concertDate }.forEach { (date, dateRecordings) ->
            println("  $date: ${dateRecordings.size} recordings")
            dateRecordings.forEach { recording ->
                println("    - ${recording.identifier} venue='${recording.concertVenue}'")
            }
        }
        
        // Apply the grouping logic
        val groupedShows = recordings.toShows()
        
        println("\nOutput grouped shows:")
        groupedShows.forEach { show ->
            println("  ${show.date} at '${show.venue}': ${show.recordings.size} recordings")
            show.recordings.forEach { recording ->
                println("    - ${recording.identifier}")
            }
        }
        
        // Assertions - this is the critical test
        val showsFor1995_07_09 = groupedShows.filter { it.date == "1995-07-09" }
        assertEquals(
            "Should have exactly 1 show for 1995-07-09 (all recordings should be grouped together)",
            1,
            showsFor1995_07_09.size
        )
        
        val show = showsFor1995_07_09.first()
        assertEquals("Should have 10 recordings for 1995-07-09", 10, show.recordings.size)
        assertEquals("Venue should be normalized", "Soldier Field", show.venue)
        
        // Verify all recordings are accounted for
        val originalCount = recordings.count { it.concertDate == "1995-07-09T00:00:00Z" }
        assertEquals("Should preserve all recordings", originalCount, show.recordings.size)
        
        println("\n✅ SUCCESS: 1995-07-09 correctly grouped into 1 show with ${show.recordings.size} recordings")
    }
    
    @Test
    fun testVenueFuzzyMatching() {
        // Test the fuzzy matching specifically for Sam Boyd Silver Bowl variations
        val testRecordings = listOf(
            Recording(
                identifier = "test1",
                title = "Test 1",
                concertDate = "1993-05-16",
                concertVenue = "Sam Boyd Silver Bowl",
                source = "SBD",
                isFavorite = false,
                isDownloaded = false
            ),
            Recording(
                identifier = "test2", 
                title = "Test 2",
                concertDate = "1993-05-16",
                concertVenue = "Sam Boyd Silver Bowl, U.N.L.V.",
                source = "AUD",
                isFavorite = false,
                isDownloaded = false
            ),
            Recording(
                identifier = "test3",
                title = "Test 3", 
                concertDate = "1993-05-16",
                concertVenue = "Sam Boyd Silver Bowl",
                source = "Matrix",
                isFavorite = false,
                isDownloaded = false
            )
        )
        
        val grouped = testRecordings.toShows()
        
        println("=== VENUE FUZZY MATCHING TEST ===")
        println("Input: ${testRecordings.size} recordings")
        println("Input venues:")
        testRecordings.forEach { recording ->
            println("  - ${recording.identifier}: '${recording.concertVenue}'")
        }
        println("Output: ${grouped.size} shows")
        
        grouped.forEach { show ->
            println("Show: date=${show.date}, venue='${show.venue}', recordings=${show.recordings.size}")
            show.recordings.forEach { recording ->
                println("  - ${recording.identifier}: '${recording.concertVenue}'")
            }
        }
        
        assertEquals("Should group all recordings into 1 show", 1, grouped.size)
        assertEquals("Should have 3 recordings", 3, grouped.first().recordings.size)
        println("✅ SUCCESS: Sam Boyd Silver Bowl variations correctly grouped into 1 show")
    }

    // ==================== PROBLEMATIC CASE TEST FRAMEWORK ====================
    
    /**
     * Data class representing a problematic show grouping case
     */
    data class ProblematicCase(
        val name: String,
        val description: String,
        val recordings: List<Recording>,
        val expectedGroups: Int,
        val expectedRecordingsPerGroup: Map<String, Int> = emptyMap(),
        val additionalAssertions: ((grouped: List<com.deadarchive.core.model.Show>) -> Unit)? = null
    )
    
    /**
     * Test runner for problematic cases
     */
    private fun testProblematicCase(case: ProblematicCase) {
        println("=== TESTING: ${case.name} ===")
        println("Description: ${case.description}")
        println("Input: ${case.recordings.size} recordings")
        
        // Show input data
        case.recordings.groupBy { it.concertDate }.forEach { (date, dateRecordings) ->
            println("  $date: ${dateRecordings.size} recordings")
            dateRecordings.forEach { recording ->
                println("    - ${recording.identifier} venue='${recording.concertVenue}' source='${recording.source}'")
            }
        }
        
        // Apply grouping
        val grouped = case.recordings.toShows()
        
        println("\nOutput: ${grouped.size} groups")
        grouped.forEach { show ->
            println("  ${show.date} at '${show.venue}': ${show.recordings.size} recordings")
            show.recordings.forEach { recording ->
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
                group!!.recordings.size
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
                recordings = listOf(
                    Recording(
                        identifier = "gd1993-05-16.sbd.smith.shnf",
                        title = "1993-05-16 Sam Boyd Silver Bowl",
                        concertDate = "1993-05-16",
                        concertVenue = "Sam Boyd Silver Bowl",
                        source = "SBD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1993-05-16.aud.jones.flac16",
                        title = "1993-05-16 Sam Boyd Silver Bowl, U.N.L.V.",
                        concertDate = "1993-05-16",
                        concertVenue = "Sam Boyd Silver Bowl, U.N.L.V.",
                        source = "AUD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1993-05-16.matrix.doe.shnf",
                        title = "1993-05-16 Sam Boyd Silver Bowl (UNLV)",
                        concertDate = "1993-05-16",
                        concertVenue = "Sam Boyd Silver Bowl (UNLV)",
                        source = "Matrix",
                        isFavorite = false,
                        isDownloaded = false
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1993-05-16_Sam Boyd Silver Bowl" to 3)
            ),
            
            // Case 2: Madison Square Garden variations (using real Dead show 1981-10-19)
            ProblematicCase(
                name = "Madison Square Garden Variations", 
                description = "MSG vs Madison Square Garden vs Madison Sq Garden should group together",
                recordings = listOf(
                    Recording(
                        identifier = "gd1981-10-19.sbd.anonymous.shnf",
                        title = "1981-10-19 Madison Square Garden",
                        concertDate = "1981-10-19",
                        concertVenue = "Madison Square Garden",
                        source = "SBD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1981-10-19.aud.taper.flac16",
                        title = "1981-10-19 MSG",
                        concertDate = "1981-10-19", 
                        concertVenue = "MSG",
                        source = "AUD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1981-10-19.matrix.source.shnf",
                        title = "1981-10-19 Madison Sq Garden",
                        concertDate = "1981-10-19",
                        concertVenue = "Madison Sq Garden", 
                        source = "Matrix",
                        isFavorite = false,
                        isDownloaded = false
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1981-10-19_Madison Square Garden" to 3)
            ),
            
            // Case 3: Amphitheater name variations
            ProblematicCase(
                name = "Amphitheater Name Variations",
                description = "Amphitheater vs Amphitheatre vs Ampitheater spelling variations",
                recordings = listOf(
                    Recording(
                        identifier = "gd1987-07-19.sbd.miller.shnf",
                        title = "1987-07-19 Alpine Valley Music Theatre",
                        concertDate = "1987-07-19",
                        concertVenue = "Alpine Valley Music Theatre",
                        source = "SBD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1987-07-19.aud.taper2.flac16",
                        title = "1987-07-19 Alpine Valley Music Theater",
                        concertDate = "1987-07-19",
                        concertVenue = "Alpine Valley Music Theater",
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
                recordings = listOf(
                    Recording(
                        identifier = "gd1995-07-09.sbd.unknown.shnf",
                        title = "1995-07-09",
                        concertDate = "1995-07-09",
                        concertVenue = null,
                        source = "SBD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1995-07-09.aud.mystery.flac16",
                        title = "1995-07-09",
                        concertDate = "1995-07-09",
                        concertVenue = "",
                        source = "AUD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1995-07-09.matrix.anon.shnf",
                        title = "1995-07-09",
                        concertDate = "1995-07-09",
                        concertVenue = null,
                        source = "Matrix",
                        isFavorite = false,
                        isDownloaded = false
                    )
                ),
                expectedGroups = 1,
                expectedRecordingsPerGroup = mapOf("1995-07-09_Unknown" to 3)
            ),
            
            // Case 5: Different dates should NOT group
            ProblematicCase(
                name = "Different Dates Should Not Group",
                description = "Same venue but different dates should create separate concerts",
                recordings = listOf(
                    Recording(
                        identifier = "gd1995-07-08.sbd.early.shnf",
                        title = "1995-07-08 Soldier Field",
                        concertDate = "1995-07-08",
                        concertVenue = "Soldier Field",
                        source = "SBD",
                        isFavorite = false,
                        isDownloaded = false
                    ),
                    Recording(
                        identifier = "gd1995-07-09.sbd.late.shnf", 
                        title = "1995-07-09 Soldier Field",
                        concertDate = "1995-07-09",
                        concertVenue = "Soldier Field",
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
        val recordings = realWorldConcerts.map { (identifier, dateVenue) ->
            Recording(
                identifier = identifier,
                title = "${dateVenue.first} ${dateVenue.second ?: "Unknown"}",
                concertDate = dateVenue.first,
                concertVenue = dateVenue.second,
                source = "SBD", // Default, can be extracted from identifier if needed
                isFavorite = false,
                isDownloaded = false
            )
        }
        
        return ProblematicCase(
            name = caseName,
            description = description,
            recordings = recordings,
            expectedGroups = expectedGroupCount
        )
    }
}