# Dead Archive Android App - Architecture Overview

## Executive Summary

Dead Archive is a sophisticated Android application for browsing, streaming, and downloading Grateful Dead concert recordings from Archive.org. The project demonstrates modern Android development practices with clean architecture principles, featuring a modular design with 14 modules, Jetpack Compose UI, and advanced media playback capabilities.

## Architecture Grade: A-

**Strengths:**
- ✅ Modern Android development stack (Compose, Media3, Hilt DI)
- ✅ Clean modular architecture with proper separation of concerns
- ✅ Sophisticated media playback system with background support
- ✅ Comprehensive data layer with offline-first approach
- ✅ Well-structured dependency injection with Hilt
- ✅ Reactive programming patterns with StateFlow/Flow

**Areas for Improvement:**
- 🚧 Some classes are extremely large (1700+ lines) - **IN PROGRESS** [See refactoring plan](../plans/large-class-refactoring-plan.md)
- ⚠️ Complex data flows that are difficult to test
- ⚠️ Feature-to-feature dependencies that reduce modularity
- ⚠️ Limited error recovery mechanisms in some areas

## Project Overview

### Purpose
Provide Grateful Dead fans with a mobile-first experience to explore Archive.org's vast concert collection, featuring:
- Concert browsing and search by date, venue, or setlist
- High-quality audio streaming with offline download support
- Personal library management with favorites and ratings
- Background playback with rich media controls
- Comprehensive metadata and setlist integration

### Technical Stack
- **Language**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt for dependency injection
- **Database**: Room with SQLite
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization
- **Media**: Media3/ExoPlayer for audio playback
- **Background Processing**: WorkManager for downloads
- **State Management**: StateFlow/Flow reactive streams

## Module Architecture

### Core Layer (9 modules)
- **core:model** - Domain models and business entities
- **core:database** - Room database with entities and DAOs
- **core:network** - Retrofit API clients for Archive.org
- **core:data** - Repository implementations coordinating data sources
- **core:media** - Media3/ExoPlayer audio playback system
- **core:design** - UI components, themes, and design system
- **core:common** - Shared utilities and constants
- **core:settings** - App settings with DataStore persistence
- **core:backup** - Backup and restore functionality

### API Layer (3 modules)
- **core:data-api** - Repository interfaces for clean architecture
- **core:settings-api** - Settings interfaces and models
- **core:media-api** - Media player interfaces

### Feature Layer (5 modules)
- **feature:browse** - Concert browsing and search
- **feature:player** - Full-screen audio player
- **feature:playlist** - Playlist and recording management
- **feature:downloads** - Download management for offline content
- **feature:library** - Personal favorites and library

### App Module
- **app** - Application entry point, navigation, and DI setup

## Key Architectural Patterns

### 1. Clean Architecture
- **Separation of Concerns**: Clear boundaries between layers
- **Dependency Inversion**: Features depend on abstractions, not implementations
- **Single Responsibility**: Each module has a focused purpose

### 2. Repository Pattern
- **Data Abstraction**: Repositories coordinate between local/remote sources
- **Caching Strategy**: Database-first with API fallback
- **Error Handling**: Graceful degradation when sources fail

### 3. MVVM with Reactive State
- **ViewModels**: Business logic with StateFlow state management
- **Compose Integration**: UI observes StateFlow for automatic updates
- **Single Source of Truth**: State flows from repositories to UI

### 4. Service-Oriented Architecture
- **Background Services**: Media playback and download processing
- **WorkManager Integration**: Reliable background task execution
- **Service Communication**: MediaController pattern for UI-service interaction

## Data Flow Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   UI Layer      │    │ Business Layer   │    │  Data Layer     │
│                 │    │                  │    │                 │
│ • Compose UI    │───▶│ • ViewModels     │───▶│ • Repositories  │
│ • Navigation    │    │ • Use Cases      │    │ • Room Database │
│ • State Obs.    │◄───│ • StateFlow      │◄───│ • Retrofit API  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Service Layer  │    │ Domain Layer     │    │ Infrastructure  │
│                 │    │                  │    │                 │
│ • Media Service │    │ • Domain Models  │    │ • Hilt DI       │
│ • Download Mgr  │    │ • Business Logic │    │ • Coroutines    │
│ • WorkManager   │    │ • Validation     │    │ • Serialization │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Key System Components

### 1. Show Management System
- **Show Aggregation**: Groups recordings by normalized venue and date
- **Rating Integration**: Multi-tier rating system with confidence scoring
- **Library Management**: User favorites with real-time state synchronization

### 2. Media Playback System
- **Background Service**: Foreground service for continuous playback
- **MediaSession Integration**: System media controls and notifications
- **Queue Management**: Sophisticated playlist handling with track progression
- **Offline Support**: Local file resolution for downloaded content

### 3. Download Management
- **WorkManager Queue**: Reliable background download processing
- **Concurrent Downloads**: Configurable parallel download limits
- **Progress Tracking**: Real-time download progress with StateFlow
- **Storage Management**: Organized local file storage with metadata

### 4. Data Synchronization
- **Offline-First**: Database as single source of truth
- **API Integration**: Archive.org metadata fetching and enrichment
- **Cache Management**: Timestamp-based cache expiry and refresh
- **Conflict Resolution**: Merge strategies for local/remote data

## Quality Metrics

### Code Organization
- **Total Kotlin Files**: ~120 files
- **Average File Size**: ~320 lines
- **Largest Files**: 🚧 DebugViewModel (1702 lines - **REMOVING**), PlaylistScreen (1393 lines), ShowRepositoryImpl (1132 lines)
- **Test Coverage**: Unit tests for core business logic
- **Documentation**: Comprehensive inline documentation

### Architecture Adherence
- **Dependency Flow**: Proper bottom-up dependencies (core → features → app)
- **Interface Segregation**: API modules provide clean abstractions
- **Single Responsibility**: Most modules have focused purposes
- **Open/Closed**: Repository pattern enables extension without modification

### Performance Characteristics
- **Memory Usage**: Efficient StateFlow-based state management
- **Database Performance**: Strategic indexing for query optimization
- **Network Efficiency**: Response caching and smart retry logic
- **UI Responsiveness**: Compose with proper state hoisting

## Next Steps

This architecture overview provides the foundation for understanding the Dead Archive system. See the detailed documentation in subsequent files for deep dives into:

- **Module Dependencies** (`01-module-architecture.md`)
- **Core Systems** (`02-core-systems.md`)
- **Feature Implementation** (`03-feature-architecture.md`)
- **Service Layer** (`04-service-architecture.md`)
- **Technical Debt Analysis** (`05-technical-debt.md`)
- **Performance Optimization** (`06-performance-analysis.md`)