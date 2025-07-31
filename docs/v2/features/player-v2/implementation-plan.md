# PlayerV2 Implementation Plan: UI-First with V2 Services Only

## Overview

PlayerV2 represents the next evolution of the Dead Archive player, following the proven V2 architecture pattern established by LibraryV2. This implementation takes a **UI-first approach**, letting the user interface drive the discovery of domain models and services rather than over-engineering upfront.

**Key Principle**: PlayerV2 will **only integrate with V2 services** - no direct dependencies on V1 services like MediaController, DownloadService, etc.

## Current Player Analysis

### V1 Player Issues
- **Complex ViewModel**: 1,099 lines with mixed responsibilities
- **Multiple Service Dependencies**: 8+ injected services with unclear boundaries
- **Manual State Synchronization**: Complex Flow composition for real-time updates
- **UI State Complexity**: Large PlayerUiState with manual field management
- **Service Coordination**: Direct MediaController dependencies create tight coupling

### V2 Improvement Goals
- **Clean Architecture**: V2 layer completely separate from V1 layer
- **UI-Driven Development**: Build only what the UI actually needs
- **Service Abstraction**: V2 services abstract away V1 implementation details
- **Feature Flag Safety**: Safe deployment with instant rollback capability

## Implementation Phases

### Phase 1: Documentation & Foundation Setup

#### 1.1 Documentation First ✅
- **Create Implementation Guide**: This document for future reference
- **Document Architecture**: UI-first approach with V2 services only
- **Record Decisions**: Feature flag strategy, service isolation principles

#### 1.2 Feature Flag Setup
- **Add PlayerV2 Toggle**: Add `usePlayerV2: Boolean = false` to AppSettings
  - Location: `core/settings-api/src/main/java/com/deadarchive/core/settings/api/model/AppSettings.kt`
- **Navigation Logic**: Modify PlayerNavigation to route to PlayerV2Screen when flag enabled
  - Location: `feature/player/src/main/java/com/deadarchive/feature/player/navigation/PlayerNavigation.kt`
- **Settings UI**: Add PlayerV2 toggle in developer settings section

### Phase 2: PlayerV2Screen Foundation

#### 2.1 Initial Screen Creation
- **Create PlayerV2Screen.kt**: Start with current PlayerScreen structure as template
  - Location: `feature/player/src/main/java/com/deadarchive/feature/player/PlayerV2Screen.kt`
- **Basic Layout**: Implement core player UI without any service dependencies
- **Mock Data**: Use hardcoded/sample data to build UI components
- **Navigation Integration**: Ensure PlayerV2Screen works with existing navigation

#### 2.2 Visual Parity
- **Match Current Design**: Ensure PlayerV2Screen looks identical to current player
- **Test Navigation**: Verify routing works correctly with feature flag
- **Mock Interactions**: Make buttons work with fake state

### Phase 3: UI Component Development (Discover Models Through UI Needs)

#### 3.1 Core Components
As we build each UI component, we'll discover what domain models we actually need:

- **Track Display**: Build track info display → discover need for PlayerV2Track model
- **Playback Controls**: Implement play/pause/skip → discover playback state models
- **Progress Bar**: Build seek/progress → discover position/duration models
- **Queue Display**: Show current position in queue → discover PlayerV2Queue model
- **Recording Info**: Show concert details → discover PlayerV2Recording model

#### 3.2 Component Extraction Strategy
Following LibraryV2 patterns:
- **PlayerV2TopBar**: Recording display with navigation
- **PlayerV2Controls**: Media controls with queue integration
- **PlayerV2TrackInfo**: Track information display
- **PlayerV2ProgressBar**: Seek and progress display

### Phase 4: V2 Service Creation (Built from UI Requirements)

#### 4.1 Service Discovery
Only create services when UI components demand them:

- **PlayerV2Service**: Core service interface based on what UI components need
- **PlayerV2ServiceStub**: Implement stub that satisfies UI requirements with mock data
- **PlayerV2ViewModel**: Create ViewModel that only depends on PlayerV2Service

#### 4.2 Domain Model Extraction
Extract domain models when UI state becomes complex:
- Start with simple data classes
- Add computed properties as UI needs them
- Follow LibraryV2 domain model patterns

### Phase 5: V2 Service Integration (Only V2 Services)

#### 5.1 V2 Service Wrappers
Create V2 wrappers for all external dependencies:

- **MediaV2Service**: Create V2 wrapper for media playback (no direct MediaController dependency)
- **DownloadV2Service**: Create V2 wrapper for download states (no direct DownloadService dependency)
- **LibraryV2Service**: Use existing LibraryV2Service for library integration
- **QueueV2Service**: Create V2 service for queue management

#### 5.2 Service Architecture
```
PlayerV2Screen → PlayerV2ViewModel → PlayerV2Service → {
  MediaV2Service (wraps MediaController)
  DownloadV2Service (wraps DownloadService)
  LibraryV2Service (already exists)
  QueueV2Service (new)
}
```

### Phase 6: Real Implementation

#### 6.1 Production Services
- **PlayerV2ServiceImpl**: Replace stub with real implementation that coordinates V2 services
- **Cross-Service Integration**: Connect V2 services through PlayerV2Service coordination
- **State Synchronization**: Ensure V2 services stay in sync with underlying V1 systems

#### 6.2 Performance Optimization
- **StateFlow Configuration**: Proper scoping for memory efficiency
- **Flow Composition**: Clean real-time integration patterns
- **Query Optimization**: Single source of truth for player state

## Key Architecture Principles

### V2 Service Isolation
- **Zero V1 Dependencies**: PlayerV2 never directly imports V1 services
- **Service Abstraction**: V2 services abstract away V1 implementation details
- **Clean Boundaries**: Clear separation between V1 and V2 layers

### UI-Driven Discovery
- **Start Simple**: Begin with basic UI layout and mock data
- **Add Complexity**: Only when UI components demand it
- **Extract Models**: When state management becomes complex
- **Create Services**: When business logic emerges

### Feature Flag Safety
- **Always Gated**: PlayerV2 always behind feature flag
- **Instant Rollback**: Can immediately fall back to PlayerV1
- **Safe Testing**: Can be enabled for specific users/testing

## Key Architectural Innovations Implemented

### Scrolling Gradient Architecture ✅
- **Gradient Integration**: Gradient is part of the scrolling LazyColumn content, not fixed background
- **Mini-Player Foundation**: Content transitions to background color around secondary controls
- **Smooth Scrolling**: Top navigation scrolls naturally with content (not sticky)
- **Visual Hierarchy**: Clear distinction between gradient section and panels below

### Professional Music Player Interface ✅
- **Material3 Design**: Complete Material3 implementation with elevated cards and proper theming
- **Enhanced Controls**: Large 72dp FAB-style play button with 56dp prev/next controls
- **Smart Layout**: Shuffle/repeat justified to far left/right, center controls properly spaced
- **Recording-Based Gradients**: Consistent color generation using recordingId hash for visual identity

### Component Architecture ✅
- **LazyColumn Structure**: Full-screen scrollability with proper item spacing
- **Bottom Sheet Integration**: Queue, connect, and track actions as ModalBottomSheet components
- **Responsive Design**: Handles different screen sizes and orientations
- **Status Bar Integration**: Transparent status bar with proper content flow

## Implementation Strategy

### Development Approach ✅
1. **Document First**: Write comprehensive plan (this document) ✅
2. **Start with Layout**: Build visual structure first ✅
3. **Add Mock Interactions**: Make buttons work with PlayerV2ServiceStub ✅
4. **Create V2 Wrappers**: Build V2 services as UI needs them
5. **Connect Real Data**: V2 services coordinate with underlying V1 systems
6. **Extract Models**: When state gets complex, extract domain models

### Testing Strategy
- **UI Testing**: Test components with mock data first
- **Service Testing**: Test V2 services in isolation
- **Integration Testing**: Test V2 service coordination
- **End-to-End Testing**: Test complete PlayerV2 flow

## Success Criteria

### Architecture Quality
- **V2 Service Isolation**: PlayerV2 has zero direct V1 service dependencies
- **Clean Architecture**: Clear separation between V1 and V2 layers
- **Testable Components**: All V2 services and UI components testable in isolation

### Functionality
- **Working Player**: Full player functionality behind feature flag
- **Feature Parity**: All current player features work in PlayerV2
- **Visual Parity**: PlayerV2 looks identical to current player

### Performance
- **No Degradation**: PlayerV2 performs as well as or better than V1
- **Memory Efficiency**: Proper StateFlow scoping prevents leaks
- **Smooth UI**: No jank or performance issues

### Deployment Safety
- **Safe Deployment**: Feature flag allows instant rollback
- **Gradual Rollout**: Can enable for specific user groups
- **Monitoring**: Can track PlayerV2 usage and issues

## Timeline: 3-4 weeks

### Week 1: Foundation ✅
- **Documentation**: Complete implementation plan ✅
- **Feature Flag**: Add toggle to AppSettings and navigation ✅
- **PlayerV2Screen**: Complete UI redesign with scrolling gradient architecture ✅
- **Visual Parity**: Professional music player interface exceeding current design ✅

### Week 2: UI Development & Service Discovery ✅
- **UI Components**: Complete component extraction and Material3 design ✅
- **PlayerV2ServiceStub**: Comprehensive stub implementation with all UI requirements ✅
- **PlayerV2ViewModel**: Full ViewModel integration with reactive state flows ✅
- **Mock Interactions**: All controls functional with realistic mock data ✅

### Week 3: V2 Service Integration
- **V2 Service Wrappers**: Create MediaV2Service, DownloadV2Service, QueueV2Service
- **Real Data Integration**: Connect V2 services to underlying V1 systems
- **Domain Models**: Extract models as state complexity emerges

### Week 4: Polish & Production Readiness
- **PlayerV2ServiceImpl**: Replace stub with real implementation
- **Performance Optimization**: Ensure smooth operation
- **Testing**: Comprehensive testing of all components
- **Deployment Readiness**: Ready for feature flag rollout

## File Structure

Following V2 architecture patterns:

```
feature/player/src/main/java/com/deadarchive/feature/player/
├── PlayerV2Screen.kt                 # Main UI screen
├── PlayerV2ViewModel.kt              # ViewModel with V2 service dependencies
├── components/                       # UI components
│   ├── PlayerV2TopBar.kt
│   ├── PlayerV2Controls.kt
│   ├── PlayerV2TrackInfo.kt
│   └── PlayerV2ProgressBar.kt
└── navigation/
    └── PlayerNavigation.kt           # Updated with V2 routing

core/player-api/src/main/java/com/deadarchive/core/player/api/
├── PlayerV2Service.kt                # Core service interface
├── MediaV2Service.kt                 # Media wrapper interface
├── DownloadV2Service.kt              # Download wrapper interface
└── QueueV2Service.kt                 # Queue service interface

core/player/src/main/java/com/deadarchive/core/player/
├── service/
│   ├── PlayerV2ServiceStub.kt        # Stub implementation
│   ├── PlayerV2ServiceImpl.kt        # Real implementation
│   ├── MediaV2ServiceImpl.kt         # Media wrapper implementation
│   ├── DownloadV2ServiceImpl.kt      # Download wrapper implementation
│   └── QueueV2ServiceImpl.kt         # Queue service implementation
└── di/
    └── PlayerV2Module.kt             # Hilt dependency injection

core/model/src/main/java/com/deadarchive/core/model/
├── PlayerV2Track.kt                  # Track domain model (as needed)
├── PlayerV2Recording.kt              # Recording domain model (as needed)
├── PlayerV2Queue.kt                  # Queue domain model (as needed)
└── PlayerV2State.kt                  # Overall state model (as needed)
```

## Notes for Future Development

### Context Clearing
This document serves as the complete reference for PlayerV2 implementation after context clearing. It contains:
- Complete architecture decisions
- Implementation phases with specific tasks
- File locations and structure
- Success criteria and timeline

### Lessons from LibraryV2
- **Domain-First Design Works**: Rich domain models eliminate UI complexity
- **Stub-First Development**: Enables immediate UI development without waiting for services
- **Service Composition**: Breaking functionality into focused services improves maintainability
- **Feature Flags Essential**: Safe deployment requires ability to rollback instantly

### Next Steps After Documentation
1. Add PlayerV2 feature flag to AppSettings
2. Modify PlayerNavigation to route based on flag
3. Create basic PlayerV2Screen with mock data
4. Begin UI component development

---

**Status**: Planning Complete ✅  
**Next Phase**: Feature Flag Setup  
**Owner**: Development Team  
**Created**: January 2025