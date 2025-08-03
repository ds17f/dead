# V2 Database Documentation

This directory contains all documentation related to the V2 database architecture and design.

## Documents

### [Data Model Requirements](./data-model-requirements.md)
Requirements and goals for the V2 data model, including:
- Core domain concepts (Show, Venue, Recording, Track)
- Collection system requirements
- User activity tracking needs
- Performance and scalability goals

### [Data Model Architecture](./data-model-architecture.md)
Complete technical specification of all V2 database entities:
- Entity definitions with field specifications
- Relationships and foreign keys
- Database indices for performance
- Migration strategy from V1

### [Database Schema Diagram](./database-schema-diagram.md)
Visual representation of the complete V2 database schema:
- Mermaid ER diagram showing all entities and relationships
- Key relationships summary
- Data size estimates and performance considerations

## Key Design Principles

### Date-Centric Organization
- Shows organized primarily by date with flexible search components
- Year, month, yearMonth fields for efficient date range queries
- Era-based filtering and timeline navigation

### Search-Optimized Structure
- Denormalized location data in shows for fast searches
- Song lists for LIKE queries against setlists
- Multiple indices supporting core search patterns:
  1. Date searches ("1977", "1977-05", "1977-05-08")
  2. Song searches ("Scarlet Begonias", "1977 Ramble on Rose")
  3. Location searches (venue, city, state)

### Multiple Format Support
- Composite keys on tracks handle MP3, FLAC, OGG versions
- Download tracking per format with local file paths
- Efficient queries for format-specific playlists

### Comprehensive User Activity Tracking
- Session-based listening analytics
- Track-level play events with completion tracking
- Resume functionality with queue state preservation
- Foundation for personalized recommendations

### Collection System
- Metadata-driven collections with pre-populated relationships
- Build-time generation from JSON definitions
- Support for official releases, guest musicians, tours, eras
- Multiple image assets for different UI contexts

## Database Structure

```
Core Data Flow:
Venue → Show → Recording → Track (multiple formats)

Collection System:
Collection ↔ Show (many-to-many via junction table)

User Data:
Library → Show (user's personal collection)
Listen Session → Track Play → Track/Recording/Show

Activity Tracking:
Session-based with comprehensive analytics
Resume points for Spotify-like continuity
```

## Implementation Status

- ✅ Requirements defined
- ✅ Architecture designed  
- ✅ Schema documented
- 📋 Entity implementation (planned)
- 📋 Migration from V1 (planned)
- 📋 Data import pipeline (planned)