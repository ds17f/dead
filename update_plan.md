# Implementation Plan for Module Structure Update

## Completed Steps

1. ✅ Created API modules with interfaces and model classes:
   - `core:settings-api`: Contains the `SettingsRepository` interface and `AppSettings` models
   - `core:data-api`: Contains repository interfaces like `ShowRepository` and `TodayInHistoryRepository`
   - `core:media-api`: Contains `MediaPlayer` interface and related models

2. ✅ Set up project structure:
   - Added new modules to `settings.gradle.kts`
   - Created proper directory structure for new modules
   - Set up build files with minimal dependencies

## Next Steps

### Phase 1: Update Settings Module

1. Update `core:settings` `build.gradle.kts` to depend on `core:settings-api`:
   ```kotlin
   dependencies {
       implementation(project(":core:model"))
       implementation(project(":core:settings-api"))
       // ...
   }
   ```

2. Refactor `SettingsRepositoryImpl` to implement the interface from `settings-api`:
   - Change import from `com.deadarchive.core.settings.data.SettingsRepository` to `com.deadarchive.core.settings.api.SettingsRepository`
   - Update any references to model classes to use the ones from `settings-api`

3. Update the `SettingsModule` to bind to the interface from `settings-api`:
   ```kotlin
   @Binds
   @Singleton
   abstract fun bindSettingsRepository(
       settingsRepositoryImpl: SettingsRepositoryImpl
   ): com.deadarchive.core.settings.api.SettingsRepository
   ```

### Phase 2: Update Data Module

1. Update `core:data` `build.gradle.kts` to depend on API modules instead of implementations:
   ```kotlin
   dependencies {
       implementation(project(":core:model"))
       implementation(project(":core:network"))
       implementation(project(":core:database"))
       implementation(project(":core:settings-api")) // Changed from :core:settings
       implementation(project(":core:data-api")) 
       // ...
   }
   ```

2. Refactor repository implementations to implement interfaces from `data-api`:
   - Update `ShowRepositoryImpl` to implement `com.deadarchive.core.data.api.repository.ShowRepository`
   - Update `TodayInHistoryRepository` to implement `com.deadarchive.core.data.api.repository.TodayInHistoryRepository`
   - Update any references to use interfaces from the API modules

3. Update repository constructor parameters to use interfaces:
   ```kotlin
   class ShowRepositoryImpl @Inject constructor(
       private val archiveApiService: ArchiveApiService,
       private val recordingDao: RecordingDao,
       private val showDao: ShowDao,
       private val libraryDao: LibraryDao,
       private val audioFormatFilterService: AudioFormatFilterService,
       private val ratingsRepository: RatingsRepository,
       private val settingsRepository: com.deadarchive.core.settings.api.SettingsRepository // Updated to use API interface
   ) : com.deadarchive.core.data.api.repository.ShowRepository { 
       // ...
   }
   ```

4. Create Dagger module to bind implementations to interfaces:
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   abstract class DataModule {
       @Binds
       @Singleton
       abstract fun bindShowRepository(
           showRepositoryImpl: ShowRepositoryImpl
       ): com.deadarchive.core.data.api.repository.ShowRepository

       @Binds
       @Singleton
       abstract fun bindTodayInHistoryRepository(
           todayInHistoryRepositoryImpl: TodayInHistoryRepositoryImpl
       ): com.deadarchive.core.data.api.repository.TodayInHistoryRepository

       // Other bindings...
   }
   ```

### Phase 3: Update Media Module

1. Update `core:media` `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":core:model"))
       implementation(project(":core:data-api")) // Changed from :core:data
       implementation(project(":core:media-api"))
       implementation(project(":core:database"))
       // ...
   }
   ```

2. Refactor media player implementation to implement the interface from `media-api`:
   ```kotlin
   class MediaPlayerImpl @Inject constructor(
       private val context: Context,
       private val showRepository: com.deadarchive.core.data.api.repository.ShowRepository, // Using API interface
       // ...
   ) : com.deadarchive.core.media.api.MediaPlayer {
       // Implementation...
   }
   ```

3. Create Dagger module to bind implementation:
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   abstract class MediaModule {
       @Binds
       @Singleton
       abstract fun bindMediaPlayer(
           mediaPlayerImpl: MediaPlayerImpl
       ): com.deadarchive.core.media.api.MediaPlayer
   }
   ```

### Phase 4: Update Feature Modules and App

1. Update feature modules to depend on API modules instead of implementations:
   ```kotlin
   dependencies {
       implementation(project(":core:design"))
       implementation(project(":core:model"))
       implementation(project(":core:data-api")) // Changed from :core:data
       implementation(project(":core:settings-api")) // Changed from :core:settings
       implementation(project(":core:media-api")) // Changed from :core:media
       // ...
   }
   ```

2. Update ViewModels and other consumers to use the interfaces from API modules:
   ```kotlin
   class PlayerViewModel @Inject constructor(
       private val mediaPlayer: com.deadarchive.core.media.api.MediaPlayer, // Using API interface
       private val showRepository: com.deadarchive.core.data.api.repository.ShowRepository, // Using API interface
       private val settingsRepository: com.deadarchive.core.settings.api.SettingsRepository // Using API interface
   ) : ViewModel() {
       // Implementation...
   }
   ```

## Testing Plan

After completing each phase:

1. Build the project to verify there are no compilation errors
2. Run tests to ensure functionality works as expected
3. Run the app to verify that features work correctly

## Final Validation

1. Verify that there are no circular dependencies by analyzing the module dependency graph
2. Ensure that each module only depends on the interfaces it needs
3. Check that adding new functionality (like backup) doesn't recreate circular dependencies