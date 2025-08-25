# V2 Player Implementation Plan - Build & Test Incrementally

## Phase 1: Skeleton Navigation (Test Immediately)

**Create**: `v2/feature/player/build.gradle.kts`
- Basic module with V2 dependencies (core/model, core/design, navigation)

**Create**: `v2/feature/player/screens/main/PlayerScreen.kt`  
- Simple Composable with just "Player Screen" text and recordingId parameter
- No ViewModel, no real functionality - just prove navigation works

**Create**: `v2/feature/player/navigation/PlayerNavigation.kt`
- Basic destination: `"player/{recordingId}"`
- Navigation function to call from other features

**Update**: `v2/app/MainNavigation.kt`
- Add player destination to NavHost
- Wire up route with recordingId parameter

**Update**: `v2/feature/playlist/` (or wherever we want to test from)
- Add navigation call to player when track is tapped

**TEST**: `make install-quiet` - Verify we can navigate to empty player screen

## Phase 2: UI Shell with Mock Data (Test UI Layout)

**Create**: `v2/feature/player/screens/main/models/PlayerViewModel.kt`
- Stub ViewModel with hardcoded mock data
- No real service - just static TrackDisplayInfo, progress values
- Simple UiState data class

**Update**: `v2/feature/player/screens/main/PlayerScreen.kt`
- Copy V1 PlayerV2Screen UI structure, remove V2 naming
- Wire to ViewModel mock data
- All buttons present but non-functional (empty onClick handlers)
- Display mock: track title, progress bar, play/pause button

**TEST**: `make install-quiet` - Verify full UI renders with mock data

## Phase 3: Service Interface (Test Service Wiring)

**Create**: `v2/core/api/player/PlayerService.kt`
- Interface with StateFlows and media commands
- Simple data classes for UI display models

**Create**: `v2/core/player/service/PlayerServiceStub.kt` 
- Stub implementation with mock data (like MiniPlayer pattern)
- Just enough to wire up ViewModel → Service → UI flow

**Update**: PlayerViewModel to inject PlayerService
- Replace hardcoded mocks with service calls

**Create**: `v2/core/player/di/PlayerModule.kt`
- Bind service interface to stub

**TEST**: `make install-quiet` - Verify service layer wiring works

## Phase 4: Real MediaController Integration (Test Media Functions)

**Update**: `v2/core/player/service/PlayerServiceImpl.kt`
- Replace stub with real MediaControllerRepository integration
- Follow MiniPlayer pattern exactly
- Add missing MediaController methods (seekToNext, seekToPrevious, seekTo)

**Update**: PlayerModule to use real implementation

**Update**: PlayerViewModel to handle real service commands
- Wire up onClick handlers to actual service methods

**TEST**: `make install-quiet` - Verify media controls actually work

## Phase 5: MiniPlayer Integration (Test Global State)

**Update**: `v2/app/MainAppScreen.kt`
- Add V2 MiniPlayer overlay
- Wire tap-to-expand to navigate to V2 player

**TEST**: `make install-quiet` - Verify MiniPlayer ↔ Player coordination

## Build & Test Strategy

After **each phase**: 
1. Run `make install-quiet`
2. Navigate to player and verify new functionality
3. Fix any compilation/runtime issues before proceeding
4. Only move to next phase when current phase works

This ensures we build on solid foundation and catch issues early.