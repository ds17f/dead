# Dead Archive V2 Architecture Hub

## Overview

This section documents the **V2 Architecture** approach - a proven methodology for redesigning complex features using clean architecture principles, domain-driven design, and stub-first development.

The V2 pattern was established with the successful **LibraryV2** implementation and serves as the template for systematic feature improvements across the app.

## V2 Architecture Principles

### 🏗️ **Clean Architecture First**
- Domain models separate from persistence concerns
- Service composition over monolithic classes
- Clear separation of concerns between layers

### 🚀 **Stub-First Development**
- Start with minimal logging stubs for immediate UI development
- Evolve stubs incrementally to add functionality
- Feature flags enable safe production deployment

### 📊 **Domain-Driven Design**
- Rich domain models (e.g., `LibraryV2Show`) capture business context
- Computed properties eliminate inconsistent state
- Single source of truth for all business logic

### 🔧 **Service Composition**
- Break large classes into focused services
- Services handle single responsibilities
- Maintain backward compatibility through facade patterns

## Current V2 Features

### ✅ **LibraryV2** - Production Ready
- **Status**: 90% complete, production-ready with stubs
- **Key Achievement**: Complex UI redesign with clean architecture
- **Innovation**: Domain model with pin/download integration
- **Performance**: Eliminated N separate queries per show
- **UI**: Grid/list views with proper album cover display

### ✅ **PlayerV2** - UI Complete
- **Status**: Week 2 complete, professional UI with comprehensive stub
- **Key Achievement**: Modern music player interface with V2 architecture
- **Innovation**: Scrolling gradient system, recording-based theming
- **Architecture**: Component composition, 83% ViewModel reduction (184 vs 1,099 lines)
- **UI**: Enhanced controls, mini-player, Material3 design excellence

### ✅ **SearchV2** - Foundation Complete
- **Status**: Foundation ready for UI-first development
- **Key Achievement**: Rapid V2 implementation using `/new-v2-ui` template
- **Innovation**: Standardized V2 scaffolding with feature flag safety
- **Architecture**: Clean service abstraction, debug integration
- **UI**: Material3 scaffold ready for enhanced search and discovery

### 📋 **Planned V2 Features**
- **PlaylistV2**: Playlist management with clean architecture
- **DownloadsV2**: Enhanced download experience and management

## Quick Navigation

### 📚 **Architecture Guides**
- [V2 Architecture Overview](architecture/overview.md) - Core principles and patterns
- [Domain Models Guide](architecture/domain-models.md) - How to design domain models
- [Service Composition](architecture/service-composition.md) - Service-oriented architecture
- [Stub-First Pattern](architecture/stub-first-pattern.md) - Development methodology

### 🎨 **Design Patterns**
- [UI Patterns](design/ui-patterns.md) - V2 UI component patterns
- [State Management](design/state-management.md) - StateFlow & Compose integration
- [Feature Flags](design/feature-flags.md) - Feature flag strategy

### 🚀 **Development**
- [Getting Started](development/getting-started.md) - How to start a new V2 feature
- [Testing Strategy](development/testing-strategy.md) - V2 testing approaches
- [Migration Guide](development/migration-guide.md) - Migrating existing features to V2

### 🏆 **Success Stories**
- [LibraryV2 Overview](features/library-v2/overview.md) - Complete success story
- [LibraryV2 Lessons Learned](features/library-v2/lessons-learned.md) - What worked, what didn't
- [LibraryV2 Implementation](features/library-v2/implementation.md) - Technical deep-dive

## Key V2 Success Metrics

### **Code Quality Improvements**
- **Large Classes Eliminated**: LibraryV2Screen decomposed with domain model approach
- **Service Composition**: Complex logic broken into focused, testable services
- **Clean Architecture**: Proper separation between UI, domain, and data layers

### **Performance Improvements**
- **Query Optimization**: N individual queries → 1 unified query with domain model
- **Real-time Updates**: StateFlow integration for instant UI updates
- **Memory Efficiency**: Proper state management eliminates redundant subscriptions

### **Developer Experience**
- **Predictable Patterns**: V2 template reduces implementation uncertainty
- **Testing**: Domain models and services can be tested in isolation
- **Maintainability**: Focused services are easier to understand and modify

## V2 Architecture Pattern Template

```kotlin
// 1. Domain Model (Rich, Context-Specific)
data class FeatureV2Item(
    val coreData: CoreModel,
    val featureSpecificState: FeatureState,
    val computedProperties: Boolean get() = /* ... */
)

// 2. Service Interface (Clean API)
interface FeatureV2Service {
    fun getItems(): Flow<List<FeatureV2Item>>
    suspend fun performAction(item: FeatureV2Item): Result<Unit>
}

// 3. Stub Implementation (Immediate Development)
@Named("stub")
class FeatureV2ServiceStub : FeatureV2Service {
    // Logging-only → In-memory state → Rich behavior
}

// 4. ViewModel Facade (Compose Integration)
@HiltViewModel
class FeatureV2ViewModel @Inject constructor(
    @Named("stub") private val service: FeatureV2Service
) : ViewModel() {
    // Clean StateFlow integration
}

// 5. Feature Flag Control (Safe Deployment)
@Named("stub") vs @Named("real") with settings toggle
```

## When to Use V2 Architecture

### ✅ **Good Candidates**
- Complex UI with multiple states (Library, Player, Playlist)
- Features requiring clean data modeling
- Areas with performance concerns
- Features needing significant refactoring

### ❌ **Not Needed**
- Simple utility functions
- Already well-architected code
- Features without complex state management

## Getting Started with V2

1. **Read the Architecture Overview** - Understand core principles
2. **Study LibraryV2 Success Story** - See complete implementation example
3. **Follow Getting Started Guide** - Step-by-step V2 feature creation
4. **Use Development Templates** - Proven patterns reduce risk

---

**Last Updated**: January 2025  
**Status**: Living documentation - updated with each V2 feature  
**Contact**: Development team for questions or V2 architecture discussions

The V2 approach transforms complex features into maintainable, testable, and performant systems while providing a consistent development experience across the entire app.