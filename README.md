# Dead Archive - Grateful Dead Concert Archive Android App

A modern Android application for browsing, streaming, and downloading Grateful Dead concert recordings from Archive.org.

## Project Overview

Dead Archive provides Grateful Dead fans with a mobile-first experience to explore the vast collection of concert recordings available on Archive.org. The app features streaming playback, offline downloads, favorites management, and social sharing capabilities.

## Architecture

The project follows modern Android development practices with a modular, clean architecture approach:

### Core Modules

- **`:core:model`** - Data models and domain entities
- **`:core:data`** - Repository implementations and data access layer
- **`:core:database`** - Room database setup for local storage
- **`:core:network`** - Retrofit networking layer for Archive.org API
- **`:core:media`** - Media3/ExoPlayer integration for audio playback
- **`:core:design`** - UI components, themes, and design system
- **`:core:common`** - Shared utilities and common code

### Feature Modules

- **`:feature:browse`** - Concert browsing and search functionality
- **`:feature:player`** - Audio player with playback controls
- **`:feature:downloads`** - Download management for offline listening
- **`:feature:favorites`** - User favorites and bookmarking

## Tech Stack

### Core Technologies
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material3** - Design system and components
- **Navigation Compose** - Type-safe navigation

### Architecture & DI
- **MVVM** - Architectural pattern
- **Hilt** - Dependency injection
- **Clean Architecture** - Separation of concerns

### Data & Networking
- **Room** - Local database for caching and favorites
- **Retrofit** - HTTP client for Archive.org API integration
- **Kotlin Serialization** - JSON parsing and serialization

### Media Playback
- **Media3/ExoPlayer** - Audio streaming and playback
- **MediaSession** - Background playback support

## Project Status

### ✅ Completed
- [x] Project setup with modular architecture
- [x] Gradle build configuration with Kotlin DSL
- [x] Core module structure with proper dependencies
- [x] Feature module scaffolding with navigation
- [x] Basic UI theme and design system foundation
- [x] Dependency injection setup with Hilt
- [x] Navigation graph with placeholder screens

### 🚧 In Progress
- [ ] Core domain models for concerts, sets, and tracks
- [ ] Archive.org API integration
- [ ] Local database schema design
- [ ] Media player implementation
- [ ] UI implementation for all features

### 📋 Planned Features
- **Browse & Search**: Discover concerts by date, venue, or setlist
- **Streaming Playback**: High-quality audio streaming with background support
- **Offline Downloads**: Download concerts for offline listening
- **Favorites Management**: Save and organize favorite shows and tracks
- **Social Sharing**: Share concert links and favorite moments
- **Custom Theming**: Grateful Dead-inspired visual design

## Development Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android SDK API 24+ (Android 7.0)

### Build Commands
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Install debug APK
./gradlew installDebug
```

### Module Dependencies

```
app
├── feature:browse
├── feature:player  
├── feature:downloads
├── feature:favorites
└── core:design
    └── core:common

core:data
├── core:model
├── core:network
└── core:database

core:media
└── core:model
```

## API Integration

The app integrates with Archive.org's API to access the Grateful Dead concert collection:

- **Base URL**: `https://archive.org/`
- **Search Endpoint**: `/advancedsearch.php`
- **Metadata Endpoint**: `/metadata/{identifier}`
- **Download Endpoint**: `/download/{identifier}`

## Permissions

The app requires the following Android permissions:

- `INTERNET` - Network access for streaming and API calls
- `FOREGROUND_SERVICE` - Background music playback
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Media playback service
- `WAKE_LOCK` - Keep device awake during playback

## Contributing

This project follows standard Android development practices:

1. **Code Style**: Official Kotlin style guide
2. **Architecture**: Clean Architecture with MVVM
3. **Testing**: Unit tests for business logic, UI tests for user flows
4. **Git Flow**: Feature branches with descriptive commit messages

## License

This project is for educational and personal use. All Grateful Dead recordings are used with respect to the band's taping and sharing policies as maintained by Archive.org.

---

*Built with ❤️ for the Grateful Dead community*