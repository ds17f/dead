# V2 ShowRepository Clean Architecture Refactor Plan

## Goal
Refactor V2 ShowRepository to return domain models and keep data models internal, following clean architecture principles.

## Current State Analysis
✅ **Domain models exist**: `Show`, `Venue`, `Location`, `Setlist`, `Lineup` in `v2/core/model/`  
❌ **Repository leaks data models**: Returns `ShowEntity` instead of `Show`  
⚠️ **Conversion exists**: `SearchServiceImpl` has `ShowEntity.toDomainShow()` extension  
❌ **No Recording domain model**: Only `RecordingEntity` exists  

## Implementation Plan

### 1. Create Missing Domain Models
**Location**: `v2/core/model/src/main/java/com/deadly/v2/core/model/`

**New File**: `Recording.kt`
```kotlin
@Serializable
data class Recording(
    val identifier: String,
    val showId: String,
    val sourceType: RecordingSourceType,
    val rating: Double,
    val reviewCount: Int
    // title: String - DEFERRED until Archive API integration
    // tracks: List<Track> - DEFERRED until Archive API integration  
    // format: AudioFormat - DEFERRED until Archive API integration
)

@Serializable
enum class RecordingSourceType(val displayName: String) {
    SOUNDBOARD("SBD"),
    AUDIENCE("AUD"), 
    FM("FM"),
    MATRIX("Matrix"),
    REMASTER("Remaster"),
    UNKNOWN("Unknown");
    
    companion object {
        fun fromString(value: String?): RecordingSourceType {
            return when (value?.uppercase()) {
                "SBD", "SOUNDBOARD" -> SOUNDBOARD
                "AUD", "AUDIENCE" -> AUDIENCE
                "FM" -> FM
                "MATRIX", "MTX" -> MATRIX
                "REMASTER" -> REMASTER
                else -> UNKNOWN
            }
        }
    }
}
```

**Note**: No `SetlistEntry` needed - existing `Setlist` model is sufficient.

### 2. Create Domain Repository Interface
**New File**: `v2/core/domain/src/main/java/com/deadly/v2/core/domain/repository/ShowRepository.kt`

**New Module**: `v2/core/domain/` (pure Kotlin, no Android deps)

**Interface**:
```kotlin
package com.deadly.v2.core.domain.repository

import com.deadly.v2.core.model.Show
import com.deadly.v2.core.model.Recording
import kotlinx.coroutines.flow.Flow

interface ShowRepository {
    suspend fun getShowById(showId: String): Show?
    suspend fun getAllShows(): List<Show>
    fun getAllShowsFlow(): Flow<List<Show>>
    suspend fun getShowsByYear(year: Int): List<Show>
    suspend fun getShowsByYearMonth(yearMonth: String): List<Show>
    suspend fun getShowsByVenue(venueName: String): List<Show>
    suspend fun getShowsByCity(city: String): List<Show>
    suspend fun getShowsByState(state: String): List<Show>
    suspend fun getShowsBySong(songName: String): List<Show>
    suspend fun getTopRatedShows(limit: Int = 20): List<Show>
    suspend fun getRecentShows(limit: Int = 20): List<Show>
    suspend fun getShowCount(): Int
    
    // Recording queries
    suspend fun getRecordingsForShow(showId: String): List<Recording>
    suspend fun getBestRecordingForShow(showId: String): Recording?
    suspend fun getRecordingById(identifier: String): Recording?
    suspend fun getTopRatedRecordings(minRating: Double = 2.0, minReviews: Int = 5, limit: Int = 50): List<Recording>
}
```

### 3. Create ShowMappers Class
**Location**: `v2/core/database/src/main/java/com/deadly/v2/core/database/mappers/`

**New File**: `ShowMappers.kt`
```kotlin
package com.deadly.v2.core.database.mappers

import com.deadly.v2.core.database.entities.ShowEntity
import com.deadly.v2.core.database.entities.RecordingEntity
import com.deadly.v2.core.model.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowMappers @Inject constructor(
    private val json: Json
) {
    
    fun entityToDomain(entity: ShowEntity): Show {
        return Show(
            id = entity.showId,
            date = entity.date,
            year = entity.year,
            band = entity.band,
            venue = Venue(entity.venueName, entity.city, entity.state, entity.country),
            location = Location.fromRaw(entity.locationRaw, entity.city, entity.state),
            setlist = parseSetlist(entity.setlistRaw, entity.setlistStatus),
            lineup = parseLineup(entity.lineupRaw, entity.lineupStatus),
            recordingIds = parseRecordingIds(entity.recordingsRaw),
            bestRecordingId = entity.bestRecordingId,
            recordingCount = entity.recordingCount,
            averageRating = entity.averageRating,
            totalReviews = entity.totalReviews,
            isInLibrary = entity.isInLibrary,
            libraryAddedAt = entity.libraryAddedAt
        )
    }
    
    fun entitiesToDomain(entities: List<ShowEntity>): List<Show> = 
        entities.map { entityToDomain(it) }
    
    fun recordingEntityToDomain(entity: RecordingEntity): Recording {
        return Recording(
            identifier = entity.identifier,
            showId = entity.showId,
            sourceType = RecordingSourceType.fromString(entity.sourceType),
            rating = entity.rating,
            reviewCount = entity.reviewCount
            // title will come from Archive API metadata calls
        )
    }
    
    fun recordingEntitiesToDomain(entities: List<RecordingEntity>): List<Recording> = 
        entities.map { recordingEntityToDomain(it) }
    
    private fun parseRecordingIds(json: String?): List<String> {
        return try {
            json?.let { this.json.decodeFromString<List<String>>(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList() // Safe fallback - no crashes
        }
    }
    
    private fun parseSetlist(json: String?, status: String?): Setlist? = 
        Setlist.parse(json, status)
    
    private fun parseLineup(json: String?, status: String?): Lineup? = 
        Lineup.parse(json, status)
}
```

### 4. Create ShowRepositoryImpl
**Location**: `v2/core/database/src/main/java/com/deadly/v2/core/database/repository/`

**Rename Current**: `ShowRepository.kt` → `ShowRepositoryImpl.kt`

**Update Implementation**:
```kotlin
package com.deadly.v2.core.database.repository

import com.deadly.v2.core.database.dao.ShowDao
import com.deadly.v2.core.database.dao.RecordingDao
import com.deadly.v2.core.database.dao.DataVersionDao
import com.deadly.v2.core.database.mappers.ShowMappers
import com.deadly.v2.core.domain.repository.ShowRepository
import com.deadly.v2.core.model.Show
import com.deadly.v2.core.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowRepositoryImpl @Inject constructor(
    private val showDao: ShowDao,
    private val recordingDao: RecordingDao,
    private val dataVersionDao: DataVersionDao,
    private val showMappers: ShowMappers
) : ShowRepository {
    
    override suspend fun getShowById(showId: String): Show? {
        return showDao.getShowById(showId)?.let { 
            showMappers.entityToDomain(it) 
        }
    }
    
    override suspend fun getAllShows(): List<Show> {
        return showMappers.entitiesToDomain(showDao.getAllShows())
    }
    
    override fun getAllShowsFlow(): Flow<List<Show>> {
        return showDao.getAllShowsFlow().map { entities ->
            showMappers.entitiesToDomain(entities)
        }
    }
    
    override suspend fun getShowsByYear(year: Int): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsByYear(year))
    }
    
    override suspend fun getShowsByYearMonth(yearMonth: String): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsByYearMonth(yearMonth))
    }
    
    override suspend fun getShowsByVenue(venueName: String): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsByVenue(venueName))
    }
    
    override suspend fun getShowsByCity(city: String): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsByCity(city))
    }
    
    override suspend fun getShowsByState(state: String): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsByState(state))
    }
    
    override suspend fun getShowsBySong(songName: String): List<Show> {
        return showMappers.entitiesToDomain(showDao.getShowsBySong(songName))
    }
    
    override suspend fun getTopRatedShows(limit: Int): List<Show> {
        return showMappers.entitiesToDomain(showDao.getTopRatedShows(limit))
    }
    
    override suspend fun getRecentShows(limit: Int): List<Show> {
        return showMappers.entitiesToDomain(showDao.getRecentShows(limit))
    }
    
    override suspend fun getShowCount(): Int = showDao.getShowCount()
    
    // Recording queries
    override suspend fun getRecordingsForShow(showId: String): List<Recording> {
        return showMappers.recordingEntitiesToDomain(recordingDao.getRecordingsForShow(showId))
    }
    
    override suspend fun getBestRecordingForShow(showId: String): Recording? {
        return recordingDao.getBestRecordingForShow(showId)?.let { 
            showMappers.recordingEntityToDomain(it) 
        }
    }
    
    override suspend fun getRecordingById(identifier: String): Recording? {
        return recordingDao.getRecordingById(identifier)?.let { 
            showMappers.recordingEntityToDomain(it) 
        }
    }
    
    override suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<Recording> {
        return showMappers.recordingEntitiesToDomain(
            recordingDao.getTopRatedRecordings(minRating, minReviews, limit)
        )
    }
}
```

### 5. Update Hilt Module
**File**: `v2/core/database/src/main/java/com/deadly/v2/core/database/di/DatabaseModule.kt`

**Add Providers**:
```kotlin
@Provides
@Singleton
fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// ShowMappers automatically provided via @Inject constructor

@Binds
abstract fun bindShowRepository(
    impl: ShowRepositoryImpl
): com.deadly.v2.core.domain.repository.ShowRepository
```

### 6. Update SearchServiceImpl
**File**: `v2/core/search/src/main/java/com/deadly/v2/core/search/SearchServiceImpl.kt`

**Changes**:
- Remove `ShowEntity.toDomainShow()` extension (lines 162-180)
- Remove `String.parseRecordingIds()` extension (lines 185-191)
- Update constructor to use `com.deadly.v2.core.domain.repository.ShowRepository`
- Update `updateSearchQuery()` method to work with `Show` objects directly
- Update `determineMatchType()` to work with `Show` instead of `ShowEntity`

### 7. Create Unit Tests
**New File**: `v2/core/database/src/test/java/com/deadly/v2/core/database/mappers/ShowMappersTest.kt`

**Test Cases**:
- Valid JSON parsing for setlist, lineup, recordings
- Null/empty JSON handling (returns empty lists)
- Malformed JSON handling (returns empty lists, no crashes)
- Complete ShowEntity → Show conversion
- Complete RecordingEntity → Recording conversion
- List conversion accuracy
- RecordingSourceType.fromString() with various inputs

## Module Structure

```
v2/core/
├── domain/                          # NEW - Pure Kotlin
│   └── repository/ShowRepository.kt  # Interface
├── model/                           # EXISTING + NEW
│   ├── Show.kt                      # ✅ Exists
│   ├── ShowComponents.kt            # ✅ Exists
│   └── Recording.kt                 # NEW
└── database/                        # EXISTING
    ├── mappers/ShowMappers.kt       # NEW
    ├── repository/ShowRepositoryImpl.kt  # RENAMED + Updated
    └── di/DatabaseModule.kt         # Updated
```

## Dependencies & Constraints

✅ **Domain module**: No Room/Android deps (pure Kotlin)  
✅ **Data depends on domain**: Database module can import domain  
✅ **Flows mapped**: Using `.map { mapper.convert(it) }`  
✅ **Schema unchanged**: No Room database modifications  
✅ **Safe JSON parsing**: Empty lists on error, no crashes  
✅ **Deferred complexity**: Archive API integration for titles, tracks, formats

## Benefits

1. **Clean Architecture**: Proper separation of data/domain/UI layers
2. **Single Conversion Point**: All mapping logic centralized in ShowMappers  
3. **Testable**: JSON parsing and conversion isolated and unit-testable
4. **Maintainable**: Domain models independent of database structure
5. **Type Safe**: RecordingSourceType enum prevents invalid values
6. **Future Ready**: Simple to extend when Archive API integration is added
7. **Safe**: Graceful error handling in JSON parsing

## Implementation Order

1. Create `v2/core/domain/` module with `ShowRepository` interface
2. Create `Recording` domain model with `RecordingSourceType` enum
3. Create `ShowMappers` class with comprehensive JSON parsing
4. Rename and update `ShowRepositoryImpl` with domain conversion
5. Update `DatabaseModule` for DI bindings  
6. Update `SearchServiceImpl` to use domain repository
7. Write comprehensive unit tests for `ShowMappers`

## Future Archive API Integration Notes

When implementing Archive.org API integration, the following will be added to domain models:

```kotlin
// Future additions to Recording.kt:
data class Recording(
    // ... existing fields ...
    val title: String,           // From Archive API metadata
    val tracks: List<Track>,     // From Archive API files listing
    val format: AudioFormat      // Detected from track analysis
)

enum class AudioFormat(val displayName: String, val extension: String) {
    FLAC_16("16-bit FLAC", "flac"),
    FLAC_24("24-bit FLAC", "flac"), 
    VBR_MP3("VBR MP3", "mp3"),
    OGG_VORBIS("OGG Vorbis", "ogg"),
    SHN("SHN", "shn"),
    UNKNOWN("Unknown", "")
}

data class Track(
    val number: Int,
    val title: String,
    val duration: String?,
    val format: AudioFormat,
    val filename: String
)
```

Archive API integration will require:
- Archive.org API client and models
- Local caching strategy for API responses  
- File-level metadata parsing and format detection
- Track listing and metadata extraction
- Error handling for API failures and cache misses