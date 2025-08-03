# V2 Database Schema Diagram

## Entity Relationship Diagram

```mermaid
erDiagram
    %% Core Entities
    VENUES_V2 {
        string venueId PK
        string name
        string normalizedName
        string aliases
        string url
        string city
        string state
        string country
        int showCount
        string firstShowDate
        string lastShowDate
        long createdAt
        long updatedAt
    }

    SHOWS_V2 {
        string showId PK
        string date
        int year
        int month
        string yearMonth
        string venueId FK
        string venueName
        string city
        string state
        string country
        string setlistRaw
        string songList
        int showSequence
        int recordingCount
        string bestRecordingId FK
        float averageRating
        int totalReviews
        long createdAt
        long updatedAt
    }

    RECORDINGS_V2 {
        string recordingId PK 
        string showId FK 
        string title 
        string source 
        float averageRating
        int reviewCount 
        string archiveUrl 
        long createdAt
        long updatedAt
    }

    TRACKS_V2 {
        string trackId PK
        int trackNumber
        string recordingId FK
        string format
        int setNumber
        string filename
        string title
        string duration
        int durationSeconds
        string bitrate
        long fileSize
        string streamUrl
        string downloadUrl
        boolean isDownloaded
        string downloadedFilePath
        long createdAt
        long updatedAt
    }

    %% Collection System
    COLLECTIONS_V2 {
        string collectionId PK 
        string name 
        string description 
        string tags 
        string coverImageUrl 
        string thumbnailUrl 
        string bannerUrl 
        int sortOrder 
        boolean isActive 
        int showCount 
        long createdAt
        long updatedAt
    }

    COLLECTION_SHOWS_V2 {
        string collectionId FK
        string showId FK
        int sequenceNumber
        long createdAt
    }

    %% User Data
    LIBRARY_V2 {
        string libraryId PK 
        string showId FK 
        long addedAt 
        int userRating 
        string userNotes 
        string tags 
        int timesPlayed 
        long lastPlayedAt
        long totalListenTime 
        long createdAt
        long updatedAt
    }

    %% User Activity Tracking
    LISTEN_SESSIONS_V2 {
        string sessionId PK 
        long startedAt 
        long endedAt 
        string deviceType 
        string platform 
        string contextType 
        string contextId 
        long totalDuration 
        int tracksPlayed 
        int showsPlayed 
        float completionRate 
        long createdAt
        long updatedAt
    }

    TRACK_PLAYS_V2 {
        string playId PK 
        string sessionId FK 
        string trackId 
        string recordingId FK 
        string showId FK 
        long startedAt 
        long endedAt 
        int startPosition 
        int endPosition 
        boolean wasCompleted 
        string playSource 
        int queuePosition 
        long createdAt
        long updatedAt
    }

    RESUME_POINTS_V2 {
        string resumeId PK 
        string trackId 
        string recordingId FK 
        string showId FK 
        int position 
        string queueJson 
        int queuePosition 
        boolean shuffleMode 
        string repeatMode 
        string contextType 
        string contextId 
        long lastUpdatedAt
        long createdAt
    }

    %% Relationships
    VENUES_V2 ||--o{ SHOWS_V2 : hosts
    SHOWS_V2 ||--o{ RECORDINGS_V2 : has
    RECORDINGS_V2 ||--o{ TRACKS_V2 : contains
    
    COLLECTIONS_V2 ||--o{ COLLECTION_SHOWS_V2 : includes
    SHOWS_V2 ||--o{ COLLECTION_SHOWS_V2 : belongs_to
    
    SHOWS_V2 ||--o{ LIBRARY_V2 : saved_in
    
    LISTEN_SESSIONS_V2 ||--o{ TRACK_PLAYS_V2 : contains
    TRACKS_V2 ||--o{ TRACK_PLAYS_V2 : played_in
    SHOWS_V2 ||--o{ TRACK_PLAYS_V2 : played_from
    RECORDINGS_V2 ||--o{ TRACK_PLAYS_V2 : played_from
    
    TRACKS_V2 ||--o| RESUME_POINTS_V2 : current_track
    SHOWS_V2 ||--o| RESUME_POINTS_V2 : current_show
    RECORDINGS_V2 ||--o| RESUME_POINTS_V2 : current_recording 
```

## Key Relationships Summary

### Core Data Flow
1. **Venue** → hosts multiple **Shows**
2. **Show** → has multiple **Recordings** (different sources: SBD, AUD, etc.)
3. **Recording** → contains multiple **Tracks** (in different formats: MP3, FLAC, etc.)

### Collection System
4. **Collection** ← many-to-many → **Show** (via junction table)
5. Collections are pre-populated at build time from JSON definitions

### User Data
6. **Library** → references **Shows** (user's personal collection)
7. **Listen Session** → contains multiple **Track Plays**
8. **Track Play** → references specific **Track**, **Recording**, **Show**
9. **Resume Point** → singleton pointing to current **Track**

### Search & Performance Indices
- **Shows**: Indexed by year, yearMonth, date, city, state, venue, songList
- **Tracks**: Composite key (trackNumber + recordingId + format)
- **Collections**: Pre-computed show counts and relationships
- **User Activity**: Indexed by session, date, show for analytics

## Data Size Estimates
- **Shows**: ~2,300 shows (30+ years of Dead shows)
- **Recordings**: ~15,000 recordings (5-7 per show average)
- **Tracks**: ~2,000,000 tracks (multiple formats × ~130 tracks/recording average)
- **Collections**: ~100-200 curated collections
- **User Data**: Grows with usage (library, listening history)