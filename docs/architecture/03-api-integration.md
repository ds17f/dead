# Archive.org API Integration Architecture

## API Overview

The Dead Archive application integrates extensively with Archive.org's API to access the vast collection of Grateful Dead concert recordings. This document details the complete API integration architecture, error handling strategies, and best practices.

## API Endpoints & Usage

### Base Configuration
- **Base URL**: `https://archive.org/`
- **User-Agent**: `DeadArchive/1.0 (Android; Grateful Dead Concert Archive App)`
- **Authentication**: No authentication required for read-only access
- **Rate Limiting**: Exponential backoff with respect for service limits

### Primary Endpoints

#### 1. Advanced Search API
**Endpoint**: `/advancedsearch.php`

**Purpose**: Search the Grateful Dead collection with sophisticated filtering

**Key Parameters**:
```kotlin
data class SearchParams(
    val q: String,           // Search query: "collection:GratefulDead"
    val fl: String,          // Fields: "identifier,title,date,venue,coverage,source"
    val rows: Int = 50,      // Results per page (max 10,000)
    val start: Int = 0,      // Pagination offset
    val sort: String,        // Sort: "date desc", "downloads desc"
    val output: String = "json"
)
```

**Common Query Patterns**:
```kotlin
// Collection-specific search
"collection:GratefulDead"

// Date range queries
"collection:GratefulDead AND date:[1977-05-01 TO 1977-05-31]"

// Venue-specific search  
"collection:GratefulDead AND venue:\"Fillmore West\""

// Popular recordings
"collection:GratefulDead" + "&sort=downloads desc"
```

#### 2. Metadata API
**Endpoint**: `/metadata/{identifier}`

**Purpose**: Fetch detailed metadata and file information for specific recordings

**Response Structure**:
```json
{
  "files": [
    {
      "name": "gd77-05-08d1t01.mp3",
      "format": "MP3",
      "size": "12345678",
      "length": "123.45",
      "title": "Dancing in the Street",
      "track": "1",
      "bitrate": "256"
    }
  ],
  "metadata": {
    "identifier": "gd1977-05-08.sbd.hicks.4982.sbeok.shnf",
    "title": "Grateful Dead Live at Barton Hall, Cornell University on 1977-05-08",
    "date": "1977-05-08",
    "venue": ["Barton Hall, Cornell University"],
    "coverage": "Ithaca, NY",
    "setlist": ["Dancing in the Street", "Help on the Way > Slipknot!", ...]
  },
  "reviews": [
    {
      "title": "Amazing show!",
      "body": "One of the best concerts ever recorded...",
      "reviewer": "username",
      "reviewdate": "2020-01-15",
      "stars": 5
    }
  ]
}
```

#### 3. File Access
**Endpoint**: `/download/{identifier}/{filename}`

**Purpose**: Direct streaming/download of audio files

**URL Construction**:
```kotlin
fun getStreamUrl(identifier: String, filename: String): String {
    return "https://archive.org/download/$identifier/$filename"
}
```

## Response Handling Architecture

### Flexible Serialization System

The Archive.org API has several quirks that require defensive parsing:

#### FlexibleStringSerializer
```kotlin
@Serializer(forClass = String::class)
object FlexibleStringSerializer : KSerializer<String> {
    override fun deserialize(decoder: Decoder): String {
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content
            is JsonArray -> element.joinToString(", ") { it.jsonPrimitive.content }
            else -> ""
        }
    }
}
```

**Handles**:
- Fields returned as both strings and arrays
- Numeric fields returned as strings
- Missing/null field graceful handling
- Inconsistent date formats

#### Response Processing Pipeline
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
}

class ArchiveApiClient {
    suspend fun searchRecordings(query: String): ApiResult<List<Recording>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchRecordings(query)
                if (response.isSuccessful && response.body() != null) {
                    val recordings = response.body()!!.response.docs.map { doc ->
                        doc.toRecording() // Safe mapping with null handling
                    }
                    ApiResult.Success(recordings)
                } else {
                    ApiResult.Error(
                        ArchiveApiException(
                            code = response.code(),
                            message = response.message(),
                            url = response.raw().request.url.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(e)
            }
        }
    }
}
```

### Error Handling Strategy

#### HTTP Error Management
```kotlin
// Common error codes and handling
when (response.code()) {
    429 -> {
        // Rate limiting - exponential backoff
        delay(calculateBackoff(retryAttempt))
        retry()
    }
    503 -> {
        // Service unavailable - Archive.org overload
        // Wait longer and retry with reduced frequency
        delay(SERVICE_UNAVAILABLE_DELAY)
        retry()
    }
    404 -> {
        // Item not found - permanent failure
        return ApiResult.Error(ItemNotFoundException(identifier))
    }
    else -> {
        // Other errors - log and fail gracefully
        return ApiResult.Error(NetworkException(response.message()))
    }
}
```

#### Retry Logic with Exponential Backoff
```kotlin
class RetryInterceptor : Interceptor {
    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val BACKOFF_MULTIPLIER = 1.5f
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0

        while (!response.isSuccessful && retryCount < MAX_RETRIES && isRetryableError(response.code)) {
            response.close()
            
            val backoffTime = (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER.toDouble(), retryCount.toDouble())).toLong()
            Thread.sleep(backoffTime)
            
            retryCount++
            response = chain.proceed(request)
        }

        return response
    }
    
    private fun isRetryableError(code: Int): Boolean = code in listOf(429, 503, 502, 504)
}
```

## Data Mapping Architecture

### Archive Document to Domain Models

#### Recording Mapping
```kotlin
fun ArchiveSearchResponse.ArchiveDoc.toRecording(): Recording {
    return Recording(
        identifier = identifier ?: "",
        concertDate = parseArchiveDate(date),
        concertVenue = venue?.firstOrNull() ?: "",
        concertLocation = coverage?.firstOrNull() ?: "",
        source = source?.firstOrNull(),
        taper = taper?.firstOrNull(),
        transferer = transferer?.firstOrNull(),
        lineage = lineage?.joinToString("\n") ?: "",
        description = description?.joinToString("\n") ?: "",
        setlist = setlist?.joinToString("\n") ?: "",
        uploader = uploader?.firstOrNull() ?: "",
        addedDate = parseArchiveDate(addeddate),
        publicDate = parseArchiveDate(publicdate),
        downloads = downloads?.toIntOrNull() ?: 0,
        averageRating = avgRating?.toDoubleOrNull(),
        numberOfReviews = numReviews?.toIntOrNull(),
        // Additional field mappings with null safety
        audioFiles = emptyList() // Populated from metadata API
    )
}
```

#### Show Aggregation Logic
```kotlin
fun List<Recording>.toShows(): List<Show> {
    return this.groupBy { recording ->
        // Normalize venue and date for consistent grouping
        val normalizedDate = normalizeDate(recording.concertDate)
        val normalizedVenue = VenueUtil.normalizeVenue(recording.concertVenue)
        "${normalizedDate}_${normalizedVenue}"
    }.map { (groupKey, recordings) ->
        createShowFromRecordings(recordings)
    }
}

private fun createShowFromRecordings(recordings: List<Recording>): Show {
    val primaryRecording = recordings.first()
    
    return Show(
        showId = generateShowId(primaryRecording.concertDate, primaryRecording.concertVenue),
        concertDate = primaryRecording.concertDate,
        venue = VenueUtil.normalizeVenue(primaryRecording.concertVenue),
        location = primaryRecording.concertLocation,
        recordings = recordings.sortedByDescending { it.sourceQuality() },
        bestRecording = selectBestRecording(recordings),
        aggregatedRating = calculateAggregatedRating(recordings),
        setlistPreview = extractSetlistPreview(recordings),
        isInLibrary = false // Will be updated by repository
    )
}
```

### Venue Normalization System
```kotlin
object VenueUtil {
    private val venueNormalizations = mapOf(
        // Theater variations
        "Theater" to "Theatre",
        "Amphitheatre" to "Amphitheater",
        
        // Common venue name standardizations
        "The Fillmore" to "Fillmore West",
        "Cow Palace" to "Cow Palace",
        
        // International venue handling
        "Lyceum Theatre, London" to "Lyceum Theatre",
        "Olympiahalle, Munich" to "Olympiahalle"
    )
    
    fun normalizeVenue(venue: String?): String {
        if (venue.isNullOrBlank()) return "Unknown Venue"
        
        var normalized = venue.trim()
        
        // Apply known normalizations
        venueNormalizations.forEach { (pattern, replacement) ->
            normalized = normalized.replace(pattern, replacement, ignoreCase = true)
        }
        
        return normalized
    }
}
```

## Performance Optimization

### Caching Strategy
```kotlin
@Singleton
class ArchiveApiClient @Inject constructor(
    private val apiService: ArchiveApiService,
    private val responseCache: ResponseCache
) {
    
    suspend fun searchRecordings(query: String, useCache: Boolean = true): ApiResult<List<Recording>> {
        // Check cache first if enabled
        if (useCache) {
            responseCache.get(query)?.let { cachedResponse ->
                if (!cachedResponse.isExpired()) {
                    return ApiResult.Success(cachedResponse.recordings)
                }
            }
        }
        
        // Proceed with API call
        val result = executeSearch(query)
        
        // Cache successful responses
        if (result is ApiResult.Success) {
            responseCache.put(query, CachedResponse(result.data, System.currentTimeMillis()))
        }
        
        return result
    }
}
```

### Request Deduplication
```kotlin
class RequestDeduplicator {
    private val activeRequests = mutableMapOf<String, Deferred<ApiResult<Any>>>()
    
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> deduplicate(key: String, request: suspend () -> ApiResult<T>): ApiResult<T> {
        // Return existing request if in flight
        activeRequests[key]?.let { activeRequest ->
            return activeRequest.await() as ApiResult<T>
        }
        
        // Start new request
        val deferred = GlobalScope.async { request() }
        activeRequests[key] = deferred as Deferred<ApiResult<Any>>
        
        try {
            return deferred.await()
        } finally {
            activeRequests.remove(key)
        }
    }
}
```

## API Integration Best Practices

### 1. Request Configuration
```kotlin
class NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(RetryInterceptor())
            .addInterceptor(LoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
```

### 2. Field Selection Optimization
```kotlin
// Only request needed fields to reduce response size
private val SEARCH_FIELDS = "identifier,title,date,venue,coverage,source,taper,transferer,description,setlist,uploader,addeddate,publicdate,downloads,avg_rating,num_reviews"

// Adjust fields based on use case
private val MINIMAL_FIELDS = "identifier,title,date,venue,coverage"
private val DETAILED_FIELDS = SEARCH_FIELDS + ",lineage,notes,subject"
```

### 3. Pagination Strategy
```kotlin
class PaginatedSearcher {
    companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val MAX_PAGE_SIZE = 1000
    }
    
    suspend fun searchAll(query: String): List<Recording> {
        val allRecordings = mutableListOf<Recording>()
        var start = 0
        
        do {
            val result = searchRecordings(query, start = start, rows = DEFAULT_PAGE_SIZE)
            when (result) {
                is ApiResult.Success -> {
                    allRecordings.addAll(result.data)
                    start += DEFAULT_PAGE_SIZE
                }
                is ApiResult.Error -> break
            }
        } while (result is ApiResult.Success && result.data.size == DEFAULT_PAGE_SIZE)
        
        return allRecordings
    }
}
```

## Testing Strategy

### Mock API Responses
```kotlin
class MockArchiveApiService : ArchiveApiService {
    private val fixtureLoader = FixtureLoader()
    
    override suspend fun searchRecordings(query: String): Response<ArchiveSearchResponse> {
        val fixture = when {
            query.contains("1977-05-08") -> "cornell_77_search.json"
            query.contains("Fillmore") -> "fillmore_search.json"
            else -> "generic_search.json"
        }
        
        val response = fixtureLoader.load<ArchiveSearchResponse>(fixture)
        return Response.success(response)
    }
}
```

### Integration Testing
```kotlin
@Test
fun testArchiveApiIntegration() = runTest {
    // Use real fixtures from Archive.org
    val fixture = loadFixture("metadata/gd1977-05-08.sbd.hicks.4982.sbeok.shnf.json")
    
    // Test mapping logic
    val recording = fixture.toRecording()
    
    assertThat(recording.identifier).isEqualTo("gd1977-05-08.sbd.hicks.4982.sbeok.shnf")
    assertThat(recording.concertDate).isEqualTo("1977-05-08")
    assertThat(recording.concertVenue).isEqualTo("Barton Hall, Cornell University")
    assertThat(recording.audioFiles).isNotEmpty()
}
```

This API integration architecture ensures robust, efficient access to Archive.org's vast Grateful Dead collection while handling the complexities and inconsistencies of the API gracefully.