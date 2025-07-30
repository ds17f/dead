# V2 Architecture Overview

## Introduction

The V2 Architecture represents a proven methodology for redesigning complex features in the Dead Archive app. Developed through the successful LibraryV2 implementation, this approach combines clean architecture principles, domain-driven design, and stub-first development to create maintainable, testable, and performant systems.

## Core Principles

### 🏗️ **Clean Architecture First**
V2 features strictly separate concerns across architectural layers, ensuring that business logic remains independent of UI and infrastructure concerns.

```
┌─────────────────────────────────────────────────────────────────┐
│                    V2 Clean Architecture                        │
├─────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                             │
│  ├── Compose UI Components                                      │
│  ├── ViewModels (StateFlow integration)                         │
│  └── UI State Management                                        │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                   │
│  ├── Rich Domain Models (feature-specific context)             │
│  ├── Business Logic (computed properties)                       │
│  └── Domain Services (business operations)                      │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer                                              │
│  ├── Service Interfaces (clean APIs)                            │
│  ├── Service Composition (coordinated operations)               │
│  └── Feature Flag Control                                       │
├─────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                           │
│  ├── Stub Implementations (development)                         │
│  ├── Database Implementations (production)                      │
│  └── External Service Integration                               │
└─────────────────────────────────────────────────────────────────┘
```

**Key Benefits**:
- **Testability**: Each layer can be tested in isolation
- **Maintainability**: Changes in one layer don't cascade to others
- **Flexibility**: Infrastructure can be swapped without affecting business logic

### 📊 **Domain-Driven Design**
V2 features start with rich domain models that capture business concepts and context, rather than technical database or API structures.

#### Domain Model Characteristics
```kotlin
// Example: LibraryV2Show domain model
data class LibraryV2Show(
    val show: Show,                    // Core business entity
    val addedToLibrary: Long,         // Feature-specific context
    val isPinned: Boolean,            // Feature-specific state
    val downloadStatus: DownloadStatus // Cross-feature integration
) {
    // Business logic as computed properties
    val sortablePinStatus: Int get() = if (isPinned) 0 else 1
    val isDownloaded: Boolean get() = downloadStatus == DownloadStatus.COMPLETED
    
    // Convenient access to core entity properties
    val showId: String get() = show.showId
    val date: String get() = show.date
}
```

**Design Principles**:
- **Composition over inheritance**: Domain models contain, don't extend, core entities
- **Feature-specific context**: Each domain model captures its feature's business concepts
- **Computed properties**: Business logic embedded in domain models
- **Single source of truth**: Domain models eliminate state inconsistencies

### 🚀 **Stub-First Development**
V2 features begin with minimal stub implementations that enable immediate UI development, then evolve incrementally toward full functionality.

#### Stub Evolution Phases
1. **Logging Stubs**: Log method calls for integration verification
2. **Stateful Stubs**: Add in-memory state for realistic UI behavior
3. **Production Stubs**: Feature-complete behavior without database complexity
4. **Real Implementation**: Full database and external service integration

```kotlin
// Phase 1: Logging stub
@Singleton
class FeatureV2ServiceStub @Inject constructor() : FeatureV2Service {
    override suspend fun performAction(item: FeatureV2Item): Result<Unit> {
        Log.d(TAG, "STUB: performAction(${item.id}) called")
        return Result.success(Unit)
    }
}

// Phase 2: Stateful stub  
@Singleton
class FeatureV2ServiceStub @Inject constructor() : FeatureV2Service {
    private val items = MutableStateFlow<List<FeatureV2Item>>(emptyList())
    
    override fun getItems(): Flow<List<FeatureV2Item>> {
        Log.d(TAG, "STUB: getItems() called")
        return items.asStateFlow()
    }
}

// Phase 3: Production stub (feature-complete)
@Singleton
class FeatureV2ServiceStub @Inject constructor() : FeatureV2Service {
    // Comprehensive in-memory implementation with realistic behavior
    // Suitable for extended production use
}
```

**Benefits**:
- **Immediate feedback**: UI development starts immediately
- **Risk reduction**: Working system at every development stage
- **Architecture validation**: Stubs prove interface design works
- **Parallel development**: UI and backend can develop independently

### 🔧 **Service Composition**
V2 features break complex operations into focused services that handle single responsibilities, then compose them to provide complete functionality.

#### Service Design Patterns
```kotlin
// Clean service interface
interface FeatureV2Service {
    fun getItems(): Flow<List<FeatureV2Item>>
    suspend fun performAction(item: FeatureV2Item): Result<Unit>
}

// Focused service implementation
@Singleton
class FeatureV2ServiceImpl @Inject constructor(
    private val dataService: FeatureDataService,        // Single responsibility
    private val integrationService: IntegrationService, // External integration
    private val cacheService: CacheService              // Performance optimization
) : FeatureV2Service {
    
    override fun getItems(): Flow<List<FeatureV2Item>> {
        return dataService.getBaseItems()
            .map { baseItems ->
                integrationService.enrichItems(baseItems)
            }
            .onEach { enrichedItems ->
                cacheService.updateCache(enrichedItems)
            }
    }
}

// ViewModel coordinates services
@HiltViewModel  
class FeatureV2ViewModel @Inject constructor(
    private val featureService: FeatureV2Service,
    private val relatedService: RelatedService
) : ViewModel() {
    
    // Complex real-time integration via Flow composition
    val items: StateFlow<List<FeatureV2Item>> = featureService.getItems()
        .flatMapLatest { items ->
            combineRelatedData(items, relatedService)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

**Benefits**:
- **Single responsibility**: Each service handles one concern
- **Testability**: Services can be mocked and tested independently
- **Reusability**: Services can be composed differently for different features
- **Maintainability**: Small, focused services are easier to understand and modify

## Architectural Patterns

### **Dependency Injection with Feature Flags**
V2 features use Hilt with named qualifiers to enable switching between stub and real implementations.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureV2Module {
    
    @Binds
    @Singleton
    @Named("stub")
    abstract fun bindFeatureV2ServiceStub(
        impl: FeatureV2ServiceStub
    ): FeatureV2Service
    
    @Binds
    @Singleton  
    @Named("real")
    abstract fun bindFeatureV2ServiceReal(
        impl: FeatureV2ServiceImpl
    ): FeatureV2Service
}

// ViewModel uses feature flag to choose implementation
@HiltViewModel
class FeatureV2ViewModel @Inject constructor(
    @Named("stub") private val stubService: FeatureV2Service,
    @Named("real") private val realService: FeatureV2Service,
    private val settings: SettingsRepository
) : ViewModel() {
    
    private val activeService: FeatureV2Service
        get() = if (settings.useRealImplementation) realService else stubService
}
```

### **Reactive State Management**
V2 features use StateFlow for reactive UI updates with proper scoping for memory efficiency.

```kotlin
// Proper StateFlow configuration
val uiState: StateFlow<UiState> = dataSource.getItems()
    .map { items -> UiState.Success(items) }
    .catch { UiState.Error(it.message) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Stop after 5s of no subscribers
        initialValue = UiState.Loading
    )
```

### **Error Handling Patterns**
V2 features use consistent error handling with Result types and proper logging.

```kotlin
// Service operations return Result types
interface FeatureV2Service {
    suspend fun performAction(item: FeatureV2Item): Result<Unit>
}

// Implementation handles errors consistently
override suspend fun performAction(item: FeatureV2Item): Result<Unit> {
    return try {
        performActualOperation(item)
        Log.d(TAG, "Successfully performed action for ${item.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to perform action for ${item.id}", e)
        Result.failure(e)
    }
}

// ViewModel handles results consistently
fun performAction(item: FeatureV2Item) {
    viewModelScope.launch {
        service.performAction(item)
            .onSuccess { /* Update UI state */ }
            .onFailure { error ->
                _errorState.value = ErrorState.ActionFailed(error.message)
            }
    }
}
```

## Performance Considerations

### **Query Optimization**
V2 features eliminate N+1 query problems by using rich domain models that aggregate related data.

```kotlin
// Before: N+1 queries for each item's status
items.forEach { item ->
    val status = statusService.getStatus(item.id) // Separate query per item
}

// After: Single query with domain model containing all data
val domainItems: Flow<List<FeatureDomainItem>> = service.getItemsWithStatus()
// All status information included in domain model
```

### **Memory Management**
V2 features use proper StateFlow scoping to prevent memory leaks.

```kotlin
// Proper StateFlow scoping prevents memory leaks
val items = repository.getItems()
    .stateIn(
        scope = viewModelScope,                        // Tied to ViewModel lifecycle
        started = SharingStarted.WhileSubscribed(5000), // Stop collecting when no subscribers
        initialValue = emptyList()
    )
```

### **Flow Composition Optimization**
V2 features optimize complex Flow compositions for performance.

```kotlin
// Efficient Flow composition with short-circuiting
sourceData.flatMapLatest { items ->
    if (items.isEmpty()) {
        flowOf(emptyList()) // Short-circuit for empty case
    } else {
        // Only process when items exist
        combineWithRelatedData(items)
    }
}
```

## Testing Strategy

### **Domain Model Testing**
```kotlin
class FeatureV2ItemTest {
    @Test
    fun `computed properties work correctly`() {
        val item = FeatureV2Item(
            coreData = coreData,
            featureState = FeatureState.ACTIVE
        )
        
        assertTrue(item.isActive)
        assertEquals("ACTIVE", item.displayStatus)
    }
}
```

### **Service Testing**
```kotlin
class FeatureV2ServiceStubTest {
    @Test
    fun `service operations update state correctly`() = runTest {
        val service = FeatureV2ServiceStub()
        
        val result = service.performAction(testItem)
        
        assertTrue(result.isSuccess)
        val updatedItems = service.getItems().first()
        assertTrue(updatedItems.contains(testItem))
    }
}
```

### **ViewModel Testing**
```kotlin
class FeatureV2ViewModelTest {
    @Mock private lateinit var service: FeatureV2Service
    
    @Test
    fun `ui state updates correctly`() = runTest {
        whenever(service.getItems()).thenReturn(flowOf(testItems))
        
        val viewModel = FeatureV2ViewModel(service)
        
        assertEquals(UiState.Success(testItems), viewModel.uiState.value)
    }
}
```

## Implementation Guidelines

### **When to Use V2 Architecture**
✅ **Good Candidates**:
- Features with complex UI state management
- Features requiring significant performance optimization
- Features needing extensive refactoring
- Features with multiple integration points

❌ **Not Suitable**:
- Simple utility functions
- Already well-architected features
- Features without complex state or business logic

### **V2 Development Process**
1. **Domain Analysis**: Identify business concepts and context
2. **Domain Model Design**: Create rich domain model with computed properties
3. **Service Interface**: Define clean API for all operations
4. **Stub Implementation**: Create logging stubs for immediate UI development
5. **UI Development**: Build complete UI using domain model
6. **Stub Evolution**: Add stateful behavior incrementally
7. **Real Implementation**: Implement production services
8. **Feature Flag Deployment**: Gradual rollout with monitoring

### **Quality Gates**
- [ ] Domain model captures all business concepts
- [ ] Service interfaces are clean and focused
- [ ] Stub implementations enable complete UI development
- [ ] UI components work exclusively with domain models
- [ ] All components are testable in isolation
- [ ] Performance is measurably improved over original implementation
- [ ] Feature flags enable safe deployment and rollback

## Success Metrics

### **Code Quality**
- **Maintainability**: Reduced complexity in individual components
- **Testability**: All layers can be tested independently
- **Reusability**: Services can be composed for different use cases

### **Performance**
- **Query Optimization**: Elimination of N+1 query patterns
- **Memory Efficiency**: Proper StateFlow scoping prevents leaks
- **UI Responsiveness**: Rich domain models enable fast UI updates

### **Development Velocity**
- **Parallel Development**: UI and services can develop independently
- **Risk Reduction**: Working system at every development stage
- **Feature Confidence**: Comprehensive testing and feature flags

---

The V2 Architecture provides a systematic approach to complex feature development that balances development speed, code quality, and system performance. By following these principles and patterns, teams can confidently tackle challenging features while maintaining architectural integrity.