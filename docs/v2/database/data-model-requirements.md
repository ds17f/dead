# V2 Data Model Requirements

## Overview

This document defines the requirements for a greenfield V2 data model for the Dead Archive application. The V2 model prioritizes date-centric organization, clear domain boundaries, and comprehensive user activity tracking.

## Core Domain Concepts

### Primary Entities

#### Show
- **Definition**: A concert performance by the Grateful Dead on a specific date
- **Key Properties**:
  - Date (YYYY-MM-DD format)
  - Venue (where the show was performed)
  - Location (city, state, country hierarchy)
  - Setlist information
- **Relationships**:
  - Has multiple Recordings
  - Belongs to a Venue
  - Can be part of multiple Collections
  - Can be in user's Library

#### Recording
- **Definition**: A specific audio recording of a Show, sourced from Archive.org
- **Key Properties**:
  - Archive.org identifier
  - Recording type (soundboard, matrix, audience, FM broadcast)
  - Source information
  - Audio quality metadata
- **Relationships**:
  - Belongs to exactly one Show
  - Has multiple Reviews (which aggregate to Rating)
  - Contains multiple Tracks

#### Venue
- **Definition**: Physical location where Shows are performed
- **Key Properties**:
  - Name
  - City, State, Country
  - Capacity
  - Historical information
- **Relationships**:
  - Hosts multiple Shows

#### Collection
- **Definition**: Curated groupings of Shows based on themes or criteria
- **Types**:
  - Official releases (Dick's Picks, Road Trips, etc.)
  - Guest musician appearances (Bruce Hornsby, Branford Marsalis)
  - Tours (Dylan & Dead, Europe '72)
  - Era-based collections (Golden Age, Late Era)
- **Key Properties**:
  - Name and description
  - Collection type
  - Ordering/sequence information
- **Relationships**:
  - Contains multiple Shows (many-to-many)

#### Library
- **Definition**: User's personal collection of saved Shows
- **Key Properties**:
  - Show reference
  - Date added timestamp
  - User notes (optional)
- **Relationships**:
  - References Shows
  - Belongs to User

#### User Activity Tracking
- **Definition**: Comprehensive tracking of user listening behavior
- **Key Entities**:
  - **Listen Session**: Complete listening session with start/end times
  - **Track Play**: Individual track playback events
  - **Resume Point**: Last playback position for resuming
- **Key Properties**:
  - Timestamp information
  - Playback duration
  - Completion percentage
  - Device/platform information
- **Use Cases**:
  - Resume last played content
  - Show recent listening history
  - Generate personalized recommendations
  - Identify listening patterns (era preferences, time of day, etc.)

## Data Sources

### Primary Sources
1. **Archive.org API**: Show metadata, recording information, track listings
2. **Scraped Archive.org Data**: Reviews, ratings, additional metadata
3. **JSON Caches**: Pre-collected data to avoid API rate limits
4. **Setlist Data**: CS.CMU.EDU, GDSets.com integration
5. **User Input**: Library additions, play history, preferences

## Core Requirements

### Date-Centric Organization
- Shows are the primary organizing principle
- Date is the fundamental identifier
- Historical timeline navigation
- Era-based filtering and organization

### Performance Requirements
- Fast show lookup by date
- Efficient recording comparison within shows
- Quick library access and management
- Responsive user activity queries

### Data Integrity
- Unique show identification (date + venue)
- Consistent venue normalization
- Reliable recording-to-show relationships
- Accurate user activity timestamps

### Scalability
- Support for 30+ years of Dead shows (~2,300+ shows)
- Multiple recordings per show (5-20+ recordings typical)
- Large user libraries (100s-1000s of shows)
- Extensive listening history over time

## User Experience Goals

### Discovery
- Find shows by date, venue, era
- Explore collections and themes
- Discover highly-rated shows
- Browse by guest musicians

### Personal Organization
- Build and manage personal library
- Sort by date added, show date, rating
- Quick access to favorites
- Library statistics and insights

### Listening Experience
- Resume where left off
- View recent listening history
- Get recommendations based on listening patterns
- Seamless offline/online playback

### Data Continuity
- Preserve existing user libraries
- Maintain listening history
- Support data export/import
- Backup and restore capabilities

## Technical Constraints

### Storage
- Local SQLite database for core data
- Efficient JSON caching for API responses
- Minimal network requests for cached data
- Support for offline operation

### API Integration
- Archive.org rate limiting compliance
- Graceful handling of API failures
- Incremental data updates
- Background sync capabilities

### Platform Requirements
- Android Room database
- Kotlin serialization support
- Hilt dependency injection
- Flow-based reactive data streams

## Success Metrics

### Performance
- Sub-100ms show lookup by date
- Library operations under 50ms
- Smooth scrolling through large datasets
- Background sync without UI blocking

### User Experience
- Intuitive date-based navigation
- Fast library management
- Accurate resume functionality
- Relevant recommendations

### Data Quality
- 100% show-to-recording relationship accuracy
- Consistent venue naming across all shows
- Complete user activity tracking
- Reliable offline data access

## Next Steps

1. Design V2 entity relationships and schema
2. Define migration strategy from V1 data
3. Create V2 repository interfaces
4. Implement V2 database layer
5. Build V2 service layer
6. Integrate with existing V2 features

This requirements document will guide the V2 data model implementation, ensuring we build a robust, scalable, and user-focused data architecture.