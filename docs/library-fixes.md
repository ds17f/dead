# Library Fixes Documentation

## Outstanding Issues

### 1. Navigation Architecture Inconsistency

**Issue**: V2 app uses mixed navigation patterns causing architectural drift and potential dependency resolution issues.

**Root Cause**: Started with consistent graph-based architecture but deviated to direct screen imports for newer features.

**Current Inconsistent State**:
```kotlin
// ✅ Proper Graph Pattern (Working)
splashGraph(navController)
searchGraph(navController) 
playlistGraph(onNavigateBack = ..., onNavigateToPlayer = ..., onNavigateToShow = ...)

// ❌ Direct Screen Pattern (Problematic)
composable("home") { HomeScreen() }
composable("library") { LibraryScreen(...) }
composable("settings") { SettingsScreen() }
```

**Solution Required**: Create proper graph functions for all features:
- `homeGraph()`
- `libraryGraph()` 
- `settingsGraph()`

**Impact**: This architectural inconsistency may be causing the LibraryScreen import resolution failure.

---

## Failed Navigation Implementation Attempts

### Primary Issue: Library Shows Don't Navigate to Playlist

**Expected Behavior**: Tapping a show in Library screen should navigate to playlist page for that show.

**Current State**: Navigation callback is commented out as TODO, no navigation occurs.

**Root Blocker**: Module dependency resolution failure preventing simple 1-line fix.

### Systematic Debug Results

#### What Works ✅
```kotlin
onNavigateToShow = { showId ->
    // TODO: Navigate to show details when implemented
    // navController.navigate("show/$showId")
},
```

#### What ALL Fail with "Unresolved reference: LibraryScreen" ❌

**Test 1 - Explicit Type Annotation**:
```kotlin
onNavigateToShow = { showId: String ->
    navController.navigate("playlist/$showId")
}
```
- Result: Still fails with LibraryScreen import error
- Note: Fixed lambda inference error but core issue persists

**Test 2 - Parameter Usage Isolation**:
```kotlin 
onNavigateToShow = { showId: String ->
    println("Would navigate to: $showId")  // No navigation call
}
```
- Result: Still fails 
- Key Discovery: ANY use of lambda parameter triggers the import failure

**Test 3 - Navigation Call Isolation**:
```kotlin
onNavigateToShow = { _: String ->
    // Don't use parameter at all
}
```
- Result: Still fails
- Conclusion: Issue not related to navController.navigate() calls

**Test 4 - Import Disambiguation**:
```kotlin
import com.deadly.v2.feature.library.screens.main.LibraryScreen as V2LibraryScreen
```
- Result: Still fails, now with "Unresolved reference: V2LibraryScreen"
- Conclusion: Not an import conflict issue

#### Additional Failed Approaches

**Direct Route Navigation**:
```kotlin
navController.navigate("playlist/$showId")
```

**Extension Function Navigation**:  
```kotlin
navController.navigateToPlaylist(showId)
```

**LibraryNavigation Function**:
```kotlin
libraryNavigation(
    onNavigateToShow = { showId -> ... }
)
```
- Result: "Unresolved reference: libraryNavigation"

**Route Mismatch Discovery**: 
- libraryNavigation() creates route "v2_library_main" 
- Bottom navigation expects route "library"
- But this doesn't explain the import resolution failure

### Technical Evidence

**Module Verification**:
- ✅ v2:feature:library builds successfully in isolation
- ✅ v2:app declares dependency on v2:feature:library in build.gradle.kts
- ✅ No circular dependencies found
- ✅ LibraryScreen file exists with correct package: `com.deadly.v2.feature.library.screens.main.LibraryScreen`

**Working Reference Points**:
- ✅ Other navigation works: `onNavigateToPlayer = { navController.navigate("player") }`  
- ✅ Playlist routes exist and work: `"playlist/{showId}"`
- ✅ Player→Playlist navigation works: `navController.navigateToPlaylist(showId, recordingId)`
- ✅ Library UI properly passes callbacks to components

**Paradoxical Behavior**:
- Even REMOVING code from lambda breaks the build
- Only exact original commented code compiles
- Same patterns that work elsewhere fail in library context

### Current Hypothesis

**Compilation-Time Module Dependency Resolution Bug**:

1. **Commented TODO code** compiles because lambda body is effectively inert
2. **Any functional code** triggers stricter dependency resolution 
3. **Resolver fails** to locate v2:feature:library classes despite correct build.gradle dependencies
4. **Potential causes**: Kotlin compilation phases, incremental compilation, Hilt annotation processing conflicts, or architectural inconsistency between graph vs direct screen patterns

### Impact

**User Experience**: Core user workflow broken - users cannot navigate from library shows to playlist details to view tracks/details.

**Technical Debt**: Simple 1-line navigation fix blocked by complex build system issue requiring deeper architectural investigation.

---

## Next Actions

1. **Immediate**: Investigate creating proper `libraryGraph()` following established patterns
2. **Architecture**: Standardize all features to use graph pattern for consistency  
3. **Build System**: Deep dive into v2:app → v2:feature:library dependency resolution
4. **Alternative**: Consider workaround using different navigation approach if architectural fix doesn't resolve import issue

---

## Context Notes

- Library operations (add/remove from database) work correctly
- Library UI reactive feedback works correctly  
- Only navigation to playlist is broken
- V2 Library implementation is otherwise complete and functional