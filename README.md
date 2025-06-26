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
- [x] **Project Architecture** - Complete modular structure with 11 modules
- [x] **Build System** - Gradle with Kotlin DSL, all dependencies resolved
- [x] **Development Workflow** - Automated Make commands for full dev cycle
- [x] **Emulator Integration** - One-command setup from zero to running app
- [x] **Core Implementations** - Domain models, repository stubs, database schema
- [x] **UI Foundation** - Jetpack Compose + Material3 theme + navigation
- [x] **Dependency Injection** - Hilt configured across all modules
- [x] **Quality Tools** - Lint, test framework, code formatting

### 🚧 Ready for Implementation
- [ ] **Archive.org API** - Real data fetching (stubs in place)
- [ ] **Media Player** - ExoPlayer integration (service stub ready)
- [ ] **UI Screens** - Replace placeholder screens with real functionality
- [ ] **Database Layer** - Implement actual data persistence
- [ ] **Download Manager** - Offline content support

### 📋 Planned Features
- **Browse & Search**: Discover concerts by date, venue, or setlist
- **Streaming Playback**: High-quality audio streaming with background support
- **Offline Downloads**: Download concerts for offline listening
- **Favorites Management**: Save and organize favorite shows and tracks
- **Social Sharing**: Share concert links and favorite moments
- **Custom Theming**: Grateful Dead-inspired visual design

## Development Setup

### Quick Start
```bash
# Complete setup and run in one command
make run-emulator
```

This will automatically:
1. 🎬 Start Android emulator
2. 🔨 Build the debug APK  
3. 📱 Install to emulator
4. 🚀 Launch the app

### Prerequisites
- **Android Studio** (latest version)
- **JDK 17+** 
- **Gradle 8.14+**
- **Android SDK API 34**

📖 **[Complete Setup Guide](SETUP.md)** - Detailed installation instructions

### Development Commands
```bash
# Essential workflow
make run-emulator        # Complete: start emulator + build + install + launch
make build              # Build debug APK
make run                # Install and run on connected device

# Emulator management
make emulator           # Start emulator
make emu-list          # List available AVDs
make emu-stop          # Stop all emulators
make devices           # Show connected devices

# Development tools
make test              # Run unit tests
make lint              # Code quality checks
make clean             # Clean build artifacts
make help              # Show all commands
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