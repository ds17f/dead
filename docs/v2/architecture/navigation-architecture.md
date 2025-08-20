# V2 Navigation Architecture

## Overview

The Dead Archive app uses a **two-app architecture** with complete separation between V1 and V2 systems. There is NO mixing of V1 and V2 components within navigation - each app is entirely self-contained.

## Architecture Principles

### 1. Complete App Separation
- **V1 App**: Complete, existing application (`com.deadarchive.app`)
- **V2 App**: New, independent application (`com.deadarchive.v2.app`)
- **No Feature Mixing**: V1 features never call V2 features and vice versa
- **No Component Sharing**: Each app uses only its own components

### 2. Top-Level App Selection

```kotlin
// Main app startup logic
if (settings.useV1App) {
    com.deadly.app.DeadlyNavigation()  // Entire V1 app
} else {
    com.deadly.v2.app.MainNavigation()      // Entire V2 app  
}
```

### 3. Independent Feature Implementation
- V2 app implements its own versions of all features
- Missing V2 features show "Coming Soon" or are hidden
- V2 app NEVER delegates to V1 features

## Module Structure

### V1 Modules (Unchanged)
```
app/                              (V1 main app)
core/                            (V1 core modules)
feature/                         (V1 features)
├── browse/
├── library/
├── player/
└── playlist/
```

### V2 Modules
```
v2/
├── app/                         (V2 main app - NEW)
├── core/
│   ├── model/                   (V2 domain models)
│   ├── database/                (V2 database)
│   └── search/                  (V2 search service)
└── feature/
    └── search/                  (V2 search UI - ONLY IMPLEMENTED FEATURE)
```

## Package Structure

### V2 App Module (`v2/app`)
```
v2/app/src/main/java/com/deadly/v2/app/
├── MainNavigation.kt            (main navigation function)
├── MainAppScreen.kt             (main app container with bottom nav)
└── build.gradle.kts             (dependencies)
```

### V2 Feature Modules
```
v2/feature/search/src/main/java/com/deadly/v2/feature/search/
├── ui/components/
│   ├── SearchScreen.kt          (IMPLEMENTED)
│   └── SearchResultsScreen.kt   (IMPLEMENTED)
└── (other search components)

v2/feature/browse/               (NOT YET CREATED)
v2/feature/library/              (NOT YET CREATED)  
v2/feature/player/               (NOT YET CREATED)
```

## Navigation Implementation

### V2 MainNavigation.kt

```kotlin
package com.deadly.v2.app

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    settings: AppSettings = AppSettings()
) {
    NavHost(
        navController = navController,
        startDestination = "main_app",
        modifier = modifier
    ) {
        composable("main_app") {
            MainAppScreen(settings = settings)
        }

        // V2-only routes
        // Add player, playlist routes here when V2 versions exist
    }
}
```

### V2 MainAppScreen.kt

```kotlin
package com.deadly.v2.app

@Composable
fun MainAppScreen(
    navController: NavHostController = rememberNavController(),
    settings: AppSettings = AppSettings()
) {
    Scaffold(
        bottomBar = {
            BottomNavigation(
                items = listOf(
                    NavItem("search", "Search", Icons.Search),
                    NavItem("browse", "Browse", Icons.Browse),      // Coming Soon
                    NavItem("library", "Library", Icons.Library),  // Coming Soon
                    NavItem("player", "Player", Icons.Player)      // Coming Soon
                )
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "search",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Search (IMPLEMENTED)
            composable("search") {
                SearchScreen(/* V2 search implementation */)
            }

            // Future V2 features
            composable("browse") {
                ComingSoonScreen("Browse")
            }
            composable("library") {
                ComingSoonScreen("Library")
            }
            composable("player") {
                ComingSoonScreen("Player")
            }
        }
    }
}
```

## Feature Implementation Status

### Implemented V2 Features
- ✅ **Search**: Full V2 FTS implementation with V2 domain models
  - `v2/feature/search` module exists
  - Uses `v2/core/search` service layer
  - Uses `v2/core/model` domain models
  - Uses `v2/core/database` with FTS5

### Missing V2 Features  
- ❌ **Browse**: Not yet implemented
- ❌ **Library**: Not yet implemented
- ❌ **Player**: Not yet implemented
- ❌ **Playlist**: Not yet implemented

## Dependencies

### V2 App Module Dependencies
```kotlin
// v2/app/build.gradle.kts
dependencies {
    // V2 core modules
    implementation(project(":v2:core:model"))
    implementation(project(":v2:core:database"))
    
    // V2 feature modules (only implemented ones)
    implementation(project(":v2:feature:search"))
    
    // Shared core modules (design, settings, etc.)
    implementation(project(":core:design"))
    implementation(project(":core:settings"))
    
    // NO V1 DEPENDENCIES
}
```

### V1 App Module Dependencies (Unchanged)
```kotlin
// app/build.gradle.kts  
dependencies {
    // V1 modules only
    implementation(project(":core:*"))
    implementation(project(":feature:*"))
    
    // NO V2 DEPENDENCIES
}
```

## Migration Strategy

### Phase 1: V2 App Foundation (Current)
1. ✅ Create V2 core modules (model, database, search)
2. ✅ Create V2 search feature
3. 🟡 Create V2 app module with navigation
4. 🟡 Implement app-level feature flag switching

### Phase 2: Feature Migration
1. Implement V2 browse feature
2. Implement V2 library feature  
3. Implement V2 player feature
4. Replace "Coming Soon" screens with real implementations

### Phase 3: V1 Deprecation
1. Set V2 as default app
2. Add V1 fallback for edge cases
3. Eventually remove V1 entirely

## Critical Rules

### ❌ NEVER DO
- Mix V1 and V2 components in the same navigation file
- Import V1 components in V2 modules or vice versa
- Delegate individual V2 features to V1 features
- Create cross-dependencies between V1 and V2 modules

### ✅ ALWAYS DO  
- Keep V1 and V2 completely separate
- Use app-level feature flag for V1/V2 switching
- Implement complete V2 features or show "Coming Soon"
- Use clean naming within v2 packages (no "V2" prefixes)

## Current Development Focus

The immediate goal is completing the V2 app foundation:

1. **Create v2/app module** with MainNavigation and MainAppScreen
2. **Update main app startup** to use app-level feature flag
3. **Test V2 search functionality** within complete V2 app context
4. **Verify V1/V2 separation** - no mixing anywhere

This architecture provides a clean foundation for incrementally building out V2 features while maintaining the existing V1 app as a stable fallback.