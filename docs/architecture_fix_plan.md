# Architectural Fix Plan for Circular Dependencies

## Problem Analysis

The Dead Archive Android app currently has a circular dependency issue between the `core:data` and `core:settings` modules. This architectural problem is causing the following issues:

1. Circular dependencies that prevent proper modular compilation
2. Tight coupling between modules that should be independent
3. Difficulties in adding new features (like backups) that rely on these modules
4. Crashes and unpredictable behavior when settings are accessed

### Current Dependency Structure

```
core:data
└── Depends directly on core:settings (implementation(project(":core:settings")))
    - ShowRepository and TodayInHistoryRepository use SettingsRepository
    - Uses settings for user preferences and recording preferences
```

```
core:settings
└── Depends on core:model only
    - Provides settings functionality through SettingsRepository interface
    - Does not directly depend on core:data
```

The issue occurs because:
1. `ShowRepository` and `TodayInHistoryRepository` in `core:data` directly consume `SettingsRepository` from `core:settings`
2. When trying to add backup functionality, a new circular dependency path is created

## Additional Architectural Issues

Beyond the direct circular dependency between `core:data` and `core:settings`, our analysis uncovered other unhealthy module dependencies:

1. **core:design depends on core:settings**:
   - Design modules should not depend on settings modules, as design should be a lower-level concern
   - This contributes to circular dependencies through transitive paths

2. **core:media depends on core:data**:
   - Media modules should provide media playback functionality that data modules can use
   - Creates another circular dependency chain

3. **Feature module interdependencies**:
   - Some feature modules depend on other feature modules (e.g., feature:browse depends on feature:playlist)
   - Creates tight coupling between features that should be independent

## Solution Architecture

To resolve these issues, we propose the following architectural changes:

### 1. Extract Common Interfaces into API Modules

Create dedicated API modules containing only interfaces and model classes:

```
core:settings-api
├── Contains SettingsRepository interface
├── Contains AppSettings model class
└── No dependencies on implementation modules
```

```
core:data-api
├── Contains repository interfaces for data access
├── Contains model classes needed by other modules
└── No dependencies on implementation modules
```

### 2. Refactor Module Dependencies

Adjust dependencies to follow cleaner architectural boundaries:

```
core:data
├── Implements core:data-api interfaces
├── Depends on core:settings-api (not core:settings implementation)
└── Contains concrete repository implementations
```

```
core:settings
├── Implements core:settings-api interfaces
├── No dependency on core:data modules
└── Contains settings implementation details
```

### 3. Dependency Inversion for Cross-Module Communication

Use dependency injection to invert control:

```
app module (or a dedicated DI module)
├── Wires together implementations with interfaces
├── Binds concrete implementations to their interfaces
└── Handles cross-cutting concerns
```

## Implementation Plan

### Phase 1: Create API Modules and Interfaces

1. **Create core:settings-api module**:
   - Extract `SettingsRepository` interface and `AppSettings` model
   - Move all interface methods to this module
   - No implementation details in this module

2. **Create core:data-api module**:
   - Extract repository interfaces used by settings
   - Move model classes needed across modules

### Phase 2: Refactor Existing Modules

1. **Update core:settings module**:
   - Implement interfaces from core:settings-api
   - Remove direct references to core:data

2. **Update core:data module**:
   - Implement interfaces from core:data-api
   - Change dependency from core:settings to core:settings-api
   - Refactor repository implementations to use interfaces

### Phase 3: Fix Dependency Injection

1. **Update Hilt modules**:
   - Create proper binding for interfaces to implementations
   - Ensure scopes are correctly defined

2. **Fix transitive dependencies**:
   - Ensure no module creates circular paths
   - Use interfaces for cross-module communication

### Phase 4: Address Other Architectural Issues

1. **Fix core:design dependencies**:
   - Remove dependency on core:settings
   - Have it depend on core:settings-api if needed

2. **Fix core:media dependencies**:
   - Create core:media-api module if necessary
   - Invert dependency relationship with core:data

3. **Decouple feature modules**:
   - Reduce direct dependencies between feature modules
   - Use events or interfaces for communication

## Example Interface Changes

### Before:

```kotlin
// In core:settings
interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateAudioFormatPreference(formatOrder: List<String>)
    // Other methods...
}

// In core:data
class ShowRepositoryImpl @Inject constructor(
    // ...
    private val settingsRepository: com.deadarchive.core.settings.data.SettingsRepository
) : ShowRepository {
    // Uses settingsRepository directly
}
```

### After:

```kotlin
// In core:settings-api
interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateAudioFormatPreference(formatOrder: List<String>)
    // Other methods...
}

// In core:data
class ShowRepositoryImpl @Inject constructor(
    // ...
    private val settingsRepository: com.deadarchive.core.settings.api.SettingsRepository
) : ShowRepository {
    // Uses the interface from settings-api, not concrete implementation
}
```

## Testing Plan

1. After each phase, verify:
   - All modules compile independently
   - No circular dependencies exist
   - App functionality remains intact
   - Settings and repository features work correctly

2. After full implementation:
   - Run comprehensive tests on all app functionality
   - Verify module independence by building modules separately
   - Validate that adding new features doesn't recreate circular dependencies

## Conclusion

This architectural fix will create a cleaner, more modular codebase with proper separation of concerns. By breaking the circular dependencies and properly isolating modules through interfaces, we'll make the codebase more maintainable and easier to extend with new features like backup functionality.

The proposed changes follow clean architecture principles, ensuring that:
1. Modules depend on abstractions, not implementations
2. High-level modules are not dependent on low-level modules
3. Changes to one module don't cascade to others unnecessarily
4. The codebase is testable and maintainable