# Archive.org Test Fixtures

This document outlines the approach for creating and using test fixtures that simulate Archive.org API responses, allowing us to test our application against real-world data without making network calls.

## Structure

```
core/network/src/test/resources/
└── fixtures/
    ├── metadata/
    │   ├── gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644.json
    │   ├── gd1977-05-08.sbd.hicks.4982.sbeok.shnf.json
    │   └── gd1973-06-10.sbd.miller.25971.sbeok.shnf.json
    ├── details/
    │   └── GratefulDead.html
    └── responses/
        ├── 200_response.json
        ├── 404_response.json
        └── 503_response.json
```

## Fixture Types

### Metadata Responses

These files contain responses from the Archive.org metadata API for specific concerts:
```
https://archive.org/metadata/{identifier}
```

### Detail Responses

HTML responses from the Archive.org details pages:
```
https://archive.org/details/{identifier}
```

### Error Responses

Sample error responses for testing error handling.

## Usage in Tests

### 1. Create Test Resource Loader

```kotlin
object TestResourceLoader {
    fun loadResource(path: String): String {
        return javaClass.classLoader?.getResource(path)?.readText()
            ?: throw IllegalArgumentException("Could not find resource at $path")
    }
    
    fun loadMetadataFixture(identifier: String): String {
        return loadResource("fixtures/metadata/$identifier.json")
    }
}
```

### 2. Create Mock API Service

```kotlin
class MockArchiveApiService(
    private val responseMap: Map<String, String> = emptyMap(),
    private val defaultResponse: String? = null
) : ArchiveApiService {

    override suspend fun getMetadata(identifier: String): ArchiveMetadata {
        val response = responseMap["metadata/$identifier"] 
            ?: defaultResponse 
            ?: TestResourceLoader.loadMetadataFixture(identifier)
        
        return Json.decodeFromString(response)
    }
}
```

### 3. Test Class Implementation

```kotlin
class ArchiveRepositoryTest {
    private lateinit var mockApiService: MockArchiveApiService
    private lateinit var repository: ArchiveRepositoryImpl
    
    @Before
    fun setup() {
        mockApiService = MockArchiveApiService()
        repository = ArchiveRepositoryImpl(mockApiService)
    }
    
    @Test
    fun `getTrackUrl returns correct URL for track`() {
        // Given
        val identifier = "gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644"
        val trackName = "Shakedown Street"
        
        // When
        val trackUrl = runBlocking { repository.getTrackUrl(identifier, trackName) }
        
        // Then
        assertEquals(
            "https://ia801509.us.archive.org/1/items/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644/02%20Shakedown%20Street.mp3",
            trackUrl
        )
    }
    
    @Test
    fun `getMetadata handles 404 response correctly`() {
        // Given
        val errorResponse = TestResourceLoader.loadResource("fixtures/responses/404_response.json")
        mockApiService = MockArchiveApiService(
            responseMap = mapOf("metadata/invalid-id" to errorResponse)
        )
        repository = ArchiveRepositoryImpl(mockApiService)
        
        // When/Then
        assertThrows(ArchiveNotFoundException::class.java) {
            runBlocking { repository.getMetadata("invalid-id") }
        }
    }
}
```

## Collecting New Fixtures

When encountering issues with specific Archive.org content, collect real responses using:

### For Metadata

```bash
curl "https://archive.org/metadata/{identifier}" > core/network/src/test/resources/fixtures/metadata/{identifier}.json
```

Example:
```bash
curl "https://archive.org/metadata/gd1977-05-08.sbd.hicks.4982.sbeok.shnf" > core/network/src/test/resources/fixtures/metadata/gd1977-05-08.sbd.hicks.4982.sbeok.shnf.json
```

### For Error Responses

1. Use specific HTTP status codes or error conditions
2. Save the complete response with headers if relevant

## End-to-End Testing Without Network

To perform end-to-end testing without network calls:

1. Create a test implementation of our MediaPlayer
2. Inject the MockArchiveApiService
3. Add test fixture data for all required API calls
4. Verify media player behavior using the mock data

```kotlin
@Test
fun `playArchiveTrack correctly processes metadata and plays track`() {
    // Given
    val identifier = "gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644"
    val trackTitle = "Shakedown Street"
    val mockPlayer = MockExoPlayer()
    val mediaPlayer = MediaPlayer(
        playerRepository = PlayerRepository(mockPlayer),
        archiveRepository = ArchiveRepositoryImpl(mockApiService)
    )
    
    // When
    runBlocking {
        mediaPlayer.playArchiveTrack(identifier, trackTitle)
    }
    
    // Then
    assertTrue(mockPlayer.prepareWasCalled)
    assertTrue(mockPlayer.playWasCalled)
    assertEquals(
        "https://ia801509.us.archive.org/1/items/gd1990-04-03.158672.UltraMatrix.sbd.cm.miller.t.flac1644/02%20Shakedown%20Street.mp3",
        mockPlayer.lastSetMediaItem?.mediaId
    )
}
```

## Additional Test Cases

1. **Handling malformed responses**: Test with intentionally malformed JSON
2. **Rate limit handling**: Test retry logic with 429 responses
3. **Connection failures**: Test timeout and network failure scenarios
4. **Format preferences**: Test choosing MP3 vs. FLAC based on settings
5. **Edge cases**: Concerts with unusual file naming or structure

## Implementing the Mock ExoPlayer

```kotlin
class MockExoPlayer : ExoPlayer {
    var prepareWasCalled = false
    var playWasCalled = false
    var lastSetMediaItem: MediaItem? = null
    
    override fun setMediaItem(mediaItem: MediaItem) {
        lastSetMediaItem = mediaItem
    }
    
    override fun prepare() {
        prepareWasCalled = true
    }
    
    override fun play() {
        playWasCalled = true
    }
    
    // Implement other ExoPlayer methods as needed
}
```

## Keeping Fixtures Updated

Periodically update the fixtures to ensure they match current Archive.org response formats:

1. Create a maintenance schedule (e.g., quarterly)
2. Update fixtures for frequently used concerts
3. Keep documentation of changes to Archive.org API

This approach ensures we can:
- Test against real-world data without network dependencies
- Quickly identify and fix processing issues
- Maintain compatibility as Archive.org evolves
- Debug specific problem cases without depending on external services