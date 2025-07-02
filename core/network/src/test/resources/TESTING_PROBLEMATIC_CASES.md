# Concert Grouping Test Framework

This framework makes it easy to test and fix problematic concert grouping cases in the Dead Archive app.

## The Problem

The app was showing duplicate concerts for the same show because venue names vary slightly:
- "Sam Boyd Silver Bowl" vs "Sam Boyd Silver Bowl, U.N.L.V."
- "Madison Square Garden" vs "MSG" vs "Madison Sq Garden"
- "Alpine Valley Music Theatre" vs "Alpine Valley Music Theater"

## How to Add a New Problematic Case

When you find a problematic show in the app:

### 1. Identify the Problem
Search for a date/venue and notice multiple concert entries instead of one concert with multiple recordings.

### 2. Extract the Data
Gather the concert identifiers, dates, and venue names for all the duplicates:
```
gd1993-05-16.sbd.smith.shnf | 1993-05-16 | Sam Boyd Silver Bowl
gd1993-05-16.aud.jones.flac16 | 1993-05-16 | Sam Boyd Silver Bowl, U.N.L.V.
gd1993-05-16.matrix.doe.shnf | 1993-05-16 | Sam Boyd Silver Bowl (UNLV)
```

### 3. Add Test Case
Add a new `ProblematicCase` to the `testAllProblematicCases()` method:

```kotlin
ProblematicCase(
    name = "Your Case Name",
    description = "What should happen vs what was happening",
    concerts = listOf(
        Concert(
            identifier = "gd1993-05-16.sbd.smith.shnf",
            title = "1993-05-16 Sam Boyd Silver Bowl", 
            date = "1993-05-16",
            venue = "Sam Boyd Silver Bowl",
            source = "SBD"
        ),
        // ... more concerts
    ),
    expectedGroups = 1, // Should group into 1 concert
    expectedRecordingsPerGroup = mapOf("1993-05-16_Sam Boyd Silver Bowl" to 3)
)
```

### 4. Run Test (Should Fail)
```bash
./gradlew :core:network:testDebugUnitTest --tests "*testAllProblematicCases*"
```

### 5. Fix the Grouping Logic
Enhance the fuzzy matching algorithm in `ArchiveMapper.kt`:
- Adjust similarity thresholds
- Add new venue normalization rules
- Improve the `calculateVenueSimilarity()` function

### 6. Run Test Again (Should Pass)
Re-run the test to verify your fix works.

### 7. Test in App
Build and test the app to make sure the problematic shows now group correctly.

## Test Case Structure

```kotlin
data class ProblematicCase(
    val name: String,                    // Brief descriptive name
    val description: String,             // What the test validates
    val concerts: List<Concert>,         // Input concert data
    val expectedGroups: Int,             // How many concert groups should result
    val expectedRecordingsPerGroup: Map<String, Int>, // Specific group sizes
    val additionalAssertions: ((grouped: List<ConcertNew>) -> Unit)? // Custom validations
)
```

## Helper Method

For quick case creation from real app data:

```kotlin
val case = createProblematicCaseFromRealData(
    caseName = "Madison Square Garden Issue",
    description = "MSG variations should group together",
    realWorldConcerts = listOf(
        "gd1979-10-27.sbd.anonymous.shnf" to ("1979-10-27" to "Madison Square Garden"),
        "gd1979-10-27.aud.taper.flac16" to ("1979-10-27" to "MSG"),
        "gd1979-10-27.matrix.source.shnf" to ("1979-10-27" to "Madison Sq Garden")
    ),
    expectedGroupCount = 1
)
```

## Current Test Cases

1. **Sam Boyd Silver Bowl Variations** - Institutional suffixes
2. **Madison Square Garden Variations** - Abbreviations and contractions  
3. **Amphitheater Name Variations** - Spelling variations
4. **Empty Venue Handling** - Missing venue data
5. **Different Dates Should Not Group** - Negative test case

## Running Tests

Run all problematic cases:
```bash
./gradlew :core:network:testDebugUnitTest --tests "*testAllProblematicCases*"
```

Run a specific test:
```bash
./gradlew :core:network:testDebugUnitTest --tests "*testVenueFuzzyMatching*"
```

Run all concert grouping tests:
```bash
./gradlew :core:network:testDebugUnitTest --tests "*ConcertGroupingTest*"
```