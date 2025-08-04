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
        boolean isInLibrary
        long libraryAddedAt
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
    USER_REVIEWS_V2 {
        string reviewId PK
        string showId FK
        int rating
        string notes
        string tags
        long createdAt
        long updatedAt
    }

    %% User Activity Tracking
    CURRENT_PLAYBACK_V2 {
        string id PK
        string showId FK
        string recordingId FK
        int trackNumber
        string format
        int positionSeconds
        string queueJson
        int queuePosition
        boolean shuffleMode
        string repeatMode
        string contextType
        string contextId
        boolean isActuallyPlaying
        long lastUpdatedAt
        long createdAt
    }

    SHOW_PLAYTHROUGHS_V2 {
        string playthroughId PK
        string showId FK
        string recordingId FK
        long startedAt
        long completedAt
        long totalListenTime
        int furthestTrack
        int tracksCompleted
        float completionPercentage
        boolean isCompleted
        string startedFrom
        string startedFromId
        long createdAt
        long updatedAt
    }

    TRACK_PLAYS_V2 {
        string playId PK
        string playthroughId FK
        int trackNumber
        string recordingId FK
        string showId FK
        long startedAt
        long endedAt
        int startPosition
        int endPosition
        boolean wasCompleted
        string playSource
        long createdAt
    }

    %% Relationships
    VENUES_V2 ||--o{ SHOWS_V2 : hosts
    SHOWS_V2 ||--o{ RECORDINGS_V2 : has
    RECORDINGS_V2 ||--o{ TRACKS_V2 : contains
    
    COLLECTIONS_V2 ||--o{ COLLECTION_SHOWS_V2 : includes
    SHOWS_V2 ||--o{ COLLECTION_SHOWS_V2 : belongs_to
    
    SHOWS_V2 ||--o{ USER_REVIEWS_V2 : reviewed_in
    
    SHOWS_V2 ||--o| CURRENT_PLAYBACK_V2 : current_show
    RECORDINGS_V2 ||--o| CURRENT_PLAYBACK_V2 : current_recording
    
    SHOWS_V2 ||--o{ SHOW_PLAYTHROUGHS_V2 : listened_to
    RECORDINGS_V2 ||--o{ SHOW_PLAYTHROUGHS_V2 : played_from
    
    SHOW_PLAYTHROUGHS_V2 ||--o{ TRACK_PLAYS_V2 : contains
    RECORDINGS_V2 ||--o{ TRACK_PLAYS_V2 : played_from
    SHOWS_V2 ||--o{ TRACK_PLAYS_V2 : played_from 
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
6. **Shows** → contain library status (isInLibrary, libraryAddedAt fields)
7. **User Reviews** → references **Shows** (ratings, notes, tags)
8. **Current Playback** → singleton state for resume functionality
9. **Show Playthrough** → analytics for each show listening session
10. **Track Play** → optional detailed tracking per track (linked to playthrough)

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