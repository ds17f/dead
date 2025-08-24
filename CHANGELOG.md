# Changelog

## [0.54.0] - 2025-08-23

### New Features
* Wire up real recordings and tracks (4a42da8d)
* implement MediaSessionService playback system (a5c1aeb9)


## [0.53.0] - 2025-08-23

### New Features
* Prefetch 2 recordings in each direction (b859535a)
* implement background prefetch system for adjacent shows (44f44f5f)
## [0.52.0] - 2025-08-22

### New Features
* implement progressive loading for responsive navigation (0651ac1e)
* add archive cache clearing functionality (f0bf470d)
## [0.51.0] - 2025-08-22

### New Features
* enhance search functionality with dot delimiter support (ffe86417)
* add V1/V2 app toggle buttons with confirmation dialogs (a7db5fe7)
* implement V2 Archive service with real track lists and reviews (ca6d5734)
* implement real PlaylistServiceImpl with V2 domain architecture (f483ff5d)
* refactor ShowRepository to clean architecture with domain models (f5c3d3d6)

### Bug Fixes
* synchronize currentRecordingId during show navigation (01ac5d4f)

### Code Refactoring
* implement smart audio format filtering in service layer (2f1e4326)
## [0.50.0] - 2025-08-21

### New Features
* add file chooser for backup restoration (c0965fb4)
* complete migration from v1 to v2 architecture with navigation integration (f175112b)
## [0.49.0] - 2025-08-21

### New Features
* implement AppScaffold-based unified navigation with HomeScreen (cc3a270c)
* implement Spotify-style bottom navigation (88ac776e)

### Bug Fixes
* Reduce card padding (175fec56)
* correct SearchScreen horizontal padding by eliminating double padding (4bb71568)
* correct SettingsBarConfiguration package declaration and improve imports (fcc655d2)

### Code Refactoring
* eliminate search screen double scaffolding and fix component padding control (db54f877)
* eliminate settings screen double scaffolding and organize file structure (68e33f3f)
* consolidate navigation systems by eliminating UnifiedAppNavigation (c9abaf5c)
## [0.48.0] - 2025-08-20

### New Features
* add Clear All Themes functionality (b3ab0d93)
* add theme system and settings screen with navigation (bdbc5119)

### Bug Fixes
* resolve ZIP theme loading and add comprehensive debugging (ae545991)

### Code Refactoring
* rebrand application from "Dead Archive" to "Deadly" (154c7399)
## [0.47.0] - 2025-08-19

### New Features
* improve SearchResults UI with Spotify-style fixed header (31d7eb33)
* implement FTS4 search with comprehensive date format support (8bc775c6)
* implement complete V2 navigation architecture with file-based toggle (7503c985)
* implement immersive status bar mode with debug panel integration (b40e6ecd)
* implement complete V2 UI architecture with AppScaffold and TopBar system (8388f0e9)
* integrate V2 SearchScreen with complete component migration (232816f6)
* complete V2 design module with Material3 theme system (4a74907c)
* implement complete V2 navigation architecture with file-based toggle (0f685ad8)

### Bug Fixes
* resolve SearchScreen import references for successful build (9b149e4a)
* resolve Hilt injection conflicts and splash navigation (722b5d96)

### Code Refactoring
* clean up V2 component naming conventions (5397b4a6)
* Arrange all code v2 modules under v2 packages (4e315b49)

### Other Changes
* chore: update Gradle wrapper to version 8.14.2 (4d3b1ede)
## [0.46.0] - 2025-08-17

### New Features
* Remove assets for database as they can now be downloaded (c0aea5b2)
* re-enable recording import with show-recording matching (8b29de60)
* enhance show data with lineup and recordings array (85d1b0a6)
* implement complete smart download infrastructure (d72a18c8)
* add comprehensive collections import system (221426d9)
* add comprehensive search table integration (1131a135)
* add database source selection UI for ZIP backup vs data import (1e31b481)
* add abort button to cancel V2 database import (e8916950)
* add automatic database restoration from ZIP assets (b2025171)
* add database initialization timer with logging (7dc7b7bb)
* add recordings import progress display (2f040669)
* add recordings and tracks schema with Archive.org integration (86b1b02a)
* add comprehensive setlist schema with songs and performances (e9848506)

### Bug Fixes
* fix clearV2Database to properly clear all tables (4633a188)

### Code Refactoring
* rename feature module from browse to search (9f8d41d0)
* reorganize V2 modules for clean package/module alignment (33f6d85d)
* remove dead code and unused restoration service (a845e15c)
## [0.45.0] - 2025-08-14

### New Features
* upgrade V2 data package to version 2.0.2 (c3cb92d)
* add SplashV2 with progress tracking and settings toggle (605d1c4)
* add V2 database MVP with shows and venues tracer bullet (48b3169)
## [0.44.0] - 2025-08-11

### New Features
* implement complete MiniPlayerV2 with proper V2 architecture (8372a19)
* implement global MiniPlayerV2 with recording-based visual identity (1e7175c)
* fix track section UI to match V1 visual design exactly (35a93d1)
* implement complete menu system with triple dot menu and choose recording UI (0f406d1)
* implement review details modal with V1-style design (af526cf)
* fix library icons and rating component clickable behavior (c16aa40)
* implement core UI components with V1 functionality parity (b70f762)
* implement clean architecture with functional navigation (53a9bfd)
* implement PlaylistV2 foundation with feature flag support (92724cc)
* Remove scripts for metadata (ac298fe)
* implement comprehensive V2 user activity tracking system (01971cf)
* add comprehensive V2 database documentation and schema visualization (ee8c6a8)

### Code Refactoring
* simplify library to embedded fields, separate user reviews (01bf74b)

### Documentation Updates
* playlist v2 docs (a04ae8e)
* add implementation status and realistic next steps documentation (72372d7)
* update implementation guide for production layout completion (21723cd)
## [0.43.0] - 2025-08-03

### New Features
* implement complete layout with compact Recent Shows and enhanced collections (8f31eaa)
* implement complete HomeV2 foundation with corrected navigation (b02a10c)

### Documentation Updates
* add comprehensive HomeV2 documentation following V2 patterns (9c60b16)

### Other Changes
* micro: reduce V2TopBar and HierarchicalFilter padding for tighter layout (4f3c411)
## [0.42.0] - 2025-08-03

### New Features
* implement compact TopAppBar design across all screens (f6287f7)
## [0.41.0] - 2025-08-02

### New Features
* create reusable V2TopBar component for consistent V2 screen headers (c2e4e31)
## [0.40.0] - 2025-08-02

### New Features
* implement comprehensive search UX improvements (d42cf6b)
* implement comprehensive V2 service architecture with UI integration (6e908b0)
* implement SearchV2 professional UI with transparent design patterns (1badd36)
* implement full-screen search results navigation and UI (742a262)
* implement QR code scanner icon and foundation (3e8bedb)
* implement flexible discover cards with full-width layout (033b204)
* change decade browse component to 2x2 grid layout (f5debac)
* implement complete UI design with Material3 components (ac480e1)
* implement complete SearchV2 foundation with V2 architecture (28978d9)

### Documentation Updates
* complete comprehensive QrScannerV2 documentation and placeholder implementation (549033e)
* update documentation with verified V2 architecture compliance (7b37f1a)
* align documentation structure with PlayerV2 patterns (9216172)
* restructure documentation following V2 pattern standards (9d52140)
* add comprehensive in-app auto updater implementation documentation (af6f199)
* complete comprehensive documentation for V2 player implementation (bc728b9)

### Other Changes
* Merge branch 'player-v2' (edfa691)
## [0.39.0] - 2025-08-01

### New Features
* implement working in-app auto updater with proper state management (509d208)

### Bug Fixes
* implement working in-app update installer with FileProvider (9be5c8d)
## [0.38.1] - 2025-07-31

### Other Changes
* bump the gradle version (b74fe21)
## [0.35.1] - 2025-07-31

### Bug Fixes
* handle null values in Archive.org API response parameters (e45cbac)
## [0.38.0] - 2025-07-31

### New Features
* implement shared color system for gradient and mini player consistency (30d395a)
* implement debug panel and bottom mini player (5010cdc)

### Bug Fixes
* resolve enum redeclaration conflicts blocking CI builds (849cb32)
* replace alpha transparency with solid color blending (a8a18fe)

### Other Changes
* Merge branch 'player-v2' (4dd5641)
## [0.37.0] - 2025-07-30

### New Features
* add Material Design cast icon for connect button (c0d011d)
* implement scrolling gradient architecture with status bar transparency (a1ba680)
* fix major UI layout and design issues (678f87c)
## [0.36.0] - 2025-07-30

### New Features
* implement complete UI redesign with professional music player interface (3d0f54a)
* implement complete feature flag infrastructure with UI-first development foundation (f91d9a5)

### Bug Fixes
* correct MiniPlayer navigation to use PlayerV2 when feature flag enabled (56a2721)
## [0.35.0] - 2025-07-29

### New Features
* implement comprehensive app update system with GitHub integration (ba62e98)
## [0.34.0] - 2025-07-29

### New Features
* implement LibraryV2Show domain model with pin functionality and real-time download status (b53ea85)

### Bug Fixes
* improve grid display with proper album covers and icon alignment (2e3cfcd)
## [0.33.0] - 2025-07-28

### New Features
* implement show actions bottom sheet (cb2ce22)

### Other Changes
* Merge branch 'break-up-classes' (e111949)
## [0.32.1] - 2025-07-28

### Bug Fixes
* make LibraryV2Screen build by making onShowLongPress optional and commenting out unimplemented component (5a5f752)
## [0.32.0] - 2025-07-28

### New Features
* implement hierarchical seasonal filtering for LibraryV2 (2412c55)
* add sort selector with bottom sheet and fix grid/list icons (a658a43)
* add hierarchical filter component with configurable spacing (4e6280c)
* implement Spotify-like LibraryV2 UI with decade filters and scrollable logs (31b73ff)
* add LibraryV2 debug panel with proper settings integration (d121ade)
* implement LibraryV2 with minimal stub-first architecture (46ad5a0)
* add Library V2 preview feature flag with toggle in settings (31ad2db)

### Bug Fixes
* ensure decade filter bar remains visible when filtered results are empty (c30d234)
## [0.31.0] - 2025-07-26

### New Features
* implement unified library removal confirmation with download cleanup (eb91358)
* implement download confirmation dialog for PlaylistScreen (e6f2249)
* implement reactive search results for immediate library icon updates (f1a08a5)
* add downloads panel to Library screen options (2ee0567)
* add logcat integration to debug panel copy functionality (16c0c18)
* implement comprehensive debug panel system with settings integration (a0b1a8b)

### Bug Fixes
* resolve downloaded track highlighting and media state synchronization (875a518)
* preserve original library timestamp when downloading existing shows (7a7757a)
* implement reactive library state management for immediate UI updates (d2dd275)
* implement unified download button with pause/resume functionality (fcd3382)
* improve download progress indicator visibility in playlist screen (7a90a78)
* notification shows proper track metadata instead of URLs (9e77c0f)
* resolve recording loading circular dependency bug (483ea8f)
* remove mock data from debug panel to prevent misleading results (93771c8)
* resolve debug panel button click detection issue and clean up logging (c43336f)
* implement proper library Date Added sorting functionality (02f9583)

### Code Refactoring
* implement unified library services and fix critical media player issues (d6ddff4)
* centralize recording selection logic to fix download/viewing inconsistency (5296d82)
* remove duplicate download settings from Settings screen (6e6f8d7)
* centralize download management with shared DownloadService (0197d2e)
* extract SettingsViewModel into service-oriented architecture (52d8f1c)
* extract LibraryViewModel into service-oriented architecture (618b332)
* extract BrowseViewModel into service-oriented architecture (8318efe)
* rename MediaControllerRepositoryRefactored back to MediaControllerRepository (adfda44)
* extract MediaControllerRepository into focused services with rich metadata (bcb0c6a)
* extract ShowCacheService and ShowCreationService from ShowRepositoryImpl (f84072c)
* extract ShowEnrichmentService from ShowRepositoryImpl (dea3c0c)
* remove debug components and panels from application (a9f9170)

### Documentation Updates
* update CLAUDE.md to reflect downloaded track highlighting fixes (0dcc369)
* update architecture documentation with completed progress checkoffs (28bfe43)
* update architecture documentation for service-oriented improvements (dd7b0f3)
* update CLAUDE.md to reflect unified library services and media player fixes (487bf81)
* update CLAUDE.md to reflect current centralized download architecture (7edbb1c)
* update CLAUDE.md with MediaControllerRepository service architecture (a04c7fe)
* update project documentation to include debug panel system (9fa3a52)
* update architecture documentation for service-oriented refactoring (0c5cfa0)
* validate and complete project documentation (1723812)
* clean up and reorganize root-level documentation (a89eb8e)
* consolidate and cleanup project documentation (7a8cede)
* add comprehensive architecture documentation and analysis (a56ae27)

### Other Changes
* clean: remove obsolete debug navigation and references (36b4314)
## [0.30.0] - 2025-07-21

### New Features
* add music note icon for playing tracks in playlist (ca8d568)
* enhance playlist screen with improved controls and menu functionality (68124d7)

### Bug Fixes
* implement event-driven track restoration with accurate position saving (2d92279)
* prevent auto-play when app restarts with previous show (f261765)
* ensure playlist library icon updates automatically when recording loads (130afe7)
* implement reactive library UI updates and simplify library system (b82658a)
* resolve library management race conditions and orphaned show entries (f9c48ac)
## [0.29.0] - 2025-07-20

### New Features
* implement setlist bottom sheet functionality (47a6ef7)
* redesign track items with transparent backgrounds and play state icons (11b2c7d)
* reorganize playlist layout with full-width reviews and grouped actions (62fe868)
* increase album cover size to 220dp for enhanced visual prominence (88fe24c)
* reorganize playlist layout with vertical album cover design (6c45d24)
* redesign playlist screen with Spotify-style layout (0fea0f8)
## [0.28.0] - 2025-07-20

### New Features
* enhance library sort UI with chevron indicators and scroll behavior (ec3253b)
* implement Spotify-style sort display in library (27013e4)
* implement no results state for decade filtering in library (a448502)
* optimize era loading performance and fix race conditions (2205a33)
## [0.27.1] - 2025-07-20

### Bug Fixes
* standardize venue normalization across all data sync paths (71c75a9)
## [0.27.0] - 2025-07-20

### New Features
* consolidate library management into library screen with triple dot menu (e0af6de)
* enhance backup and restore UX with smart empty library screen (9f8e42b)
* add debug display for backup functionality (0f20052)
* implement basic library backup and restore system (676b5e7)
* create API modules to resolve circular dependencies (e700381)

### Bug Fixes
* resolve venue normalization inconsistency causing shows with 0 recordings (2fc3b04)
* ensure restored shows have recordings for navigation (346916e)
* complete backup and restore functionality (dc9d755)
* resolve circular dependencies and restore audio playback (bbaf62e)
## [0.26.0] - 2025-07-19

### New Features
* implement simple Spotify-like last track restoration (e4ef047)
* complete playback history tracking system with session management (8c4d1b8)
* add playback history database schema and entities (abd06fa)
* implement Media3 event tracking for playback history (b52b6d0)
* add QueueDebugPanel for real-time queue state visibility (8222ac8)
## [0.25.0] - 2025-07-18

### New Features
* enhance track index synchronization for queue-aware playlist highlighting (eb6a21e)
* wire QueueStateManager to provide queue-aware UI navigation (7165741)
* implement QueueManager as single source of truth for Media3 queue operations (abd52c1)

### Bug Fixes
* use commit message and auto-generate release notes for GitHub releases (df539d7)
* URL decode filename for proper track matching in queue metadata lookup (06b3e4e)

### Code Refactoring
* complete QueueManager migration and remove legacy queue management (0e0bcb9)
## [0.24.0] - 2025-07-17

### New Features
* update header format to show Date then Venue/Location consistently (8396e3e)
* update mini player format to show Date / Venue•Location (afc5269)
* gate debug panel visibility based on settings (cf800fa)
* use queue position for track numbering instead of filename parsing (04dc0d1)
* improve track number extraction from filenames (8ac884b)
* implement enriched media notifications with show and track information (3b39d4b)

### Bug Fixes
* correct track duration display in playlist by parsing MM:SS format (08b544f)
* use CurrentTrackInfo for player screen track titles (b582603)
* resolve track title and navigation consistency issues (5b03c05)

### Code Refactoring
* improve data model structure and relationships (d7ad121)
## [0.23.0] - 2025-07-14

### New Features
* add sharing functionality with recording navigation fix (c96cb26)
## [0.22.0] - 2025-07-14

### New Features
* add recording recommendation system with reset functionality (1e388a6)
* add recording preference support to browse functionality (91924aa)
* implement showId-based navigation architecture for recording preferences (17aef51)
* implement functional recording selection modal (dd4a12a)
* optimize show navigation with efficient database queries (93b0748)
* optimize playlist layout with compact reviews and repositioned icons (255aca0)
* complete enhanced playlist features with improved UI layout (2792532)
* implement enhanced playlist features with user preferences (f99d5b1)
* implement Archive.org API integration for review data (45a2cb0)
* implement interactive star ratings with review modal (8b1a715)

### Bug Fixes
* update library navigation to go to playlist instead of player (c125296)
* preserve recording preferences in playlist next/prev show navigation (05b7c28)
* add missing getRecordingPreference method to SettingsRepository (413ed99)
* ensure TodayInHistoryRepository consistently applies user recording preferences (30f1543)
* resolve smart cast issue in recording selection modal (9344b9e)
* resolve smart cast issues in ReviewService (87ab421)
* resolve compilation errors in Archive.org API integration (f919a09)
* resolve Float/Double type compatibility in rating components (b83eb13)
* resolve type mismatches between Float and Double in rating components (9a680f6)
## [0.21.0] - 2025-07-13

### New Features
* add comprehensive permissions checking and error reporting to release update script (f162d66)
* add script to retroactively update all release tags with changelog content (1a9dd03)
* add tag-release-quick and improve changelog generation (0f0f76e)
* implement weighted rating system for internal sorting and ranking (85dc749)
* add rating context indicators with enhanced visual styling (55076c0)
* implement era-based filtering for top-rated shows (5775228)
* update app terminology from recordings to shows with accurate counts (88a778c)

### Bug Fixes
* resolve release script changelog section extraction bug (228a65c)
* improve GitHub release description update script with better filtering and debugging (4079642)
* update script to modify GitHub releases instead of git tags (af8fba6)
* restore missing rating data for individual recordings in playlist (d9cecbd)
* comprehensive UI fixes and performance optimizations (045336e)

### Code Refactoring
* rename make target to update-release-descriptions for clarity (39c460e)
## [0.21.0] - 2025-07-13

### New Features
* add comprehensive permissions checking and error reporting to release update script (f162d66)
* add script to retroactively update all release tags with changelog content (1a9dd03)
* add tag-release-quick and improve changelog generation (0f0f76e)
* implement weighted rating system for internal sorting and ranking (85dc749)
* add rating context indicators with enhanced visual styling (55076c0)
* implement era-based filtering for top-rated shows (5775228)
* update app terminology from recordings to shows with accurate counts (88a778c)

### Bug Fixes
* improve GitHub release description update script with better filtering and debugging (4079642)
* update script to modify GitHub releases instead of git tags (af8fba6)
* restore missing rating data for individual recordings in playlist (d9cecbd)
* comprehensive UI fixes and performance optimizations (045336e)

### Code Refactoring
* rename make target to update-release-descriptions for clarity (39c460e)
## [0.20.0] - 2025-07-12

### New Features
* implement enhanced rating system with raw ratings and show-level data (8f588dd)
* implement MAX rating system and data packaging pipeline (f8aec15)

### Performance Improvements
* add memory cleanup after JSON processing to reduce app data usage (ce6ce40)
## [0.19.1] - 2025-07-12

### Performance Improvements
* optimize library loading by using library-specific queries (1ba11bb)
## [0.19.0] - 2025-07-12

### New Features
* add enhanced setlist structure debugging for Queen Jane issue (210c5db)
* add comprehensive song search debugging capabilities (2b4eda3)
* implement advanced setlist search capabilities (1a53305)
* implement comprehensive setlist integration system (e18159f)
* implement GDSets-first merge precedence for superior data quality (e2689b9)
* achieve near-perfect data matching with enhanced processing pipeline (b5778c1)
* implement setlist integration with ID referencing and complete data pipeline (0da4193)
* implement song processing with segue handling and comprehensive normalization (43dc0cc)
* implement comprehensive venue processing with international normalization (637a86e)
* add Jerry Garcia memorial hard stop and merge commands (1935294)
* implement setlist merger with CMU set3/encore normalization (d697afe)
* implement GDSets scraper for early years and memorabilia collection (3364946)
* implement CMU setlist scraper and data collection (28153b0)

### Bug Fixes
* resolve song search timing issue with proper setlist data loading (9e77726)
* resolve setlist songs parsing from sets structure (f9c5899)
* resolve song entity access in enhanced search methods (b47ce3f)
* correct venue ID field name in setlist JSON parsing (b2687b3)

### Documentation Updates
* consolidate and enhance setlist pipeline documentation (5229324)

### Other Changes
* chore: clean up scripts directory by removing temporary and redundant files (748917c)
## [0.18.0] - 2025-07-08

### New Features
* streamline ratings file storage for reduced repo size (e87a1ab)
* improve release process and metadata collection (9aeeb6b)

### Bug Fixes
* add weekly breakdown for ultra-high volume years (ed5da68)
* update app sync queries to capture all recordings (83c70a1)
* add monthly breakdown for high-volume years in metadata collection (c1b8bcc)
* work around Archive.org 10k search result limit (4cba4fc)
## [0.17.0] - 2025-07-08

### New Features
* remove confirmation prompt from tag-release (9df9ac8)
* change default audio format preference to VBR MP3 (8a594ea)
* enhance recording selection to prefer rated recordings over unrated ones (17f2f80)
* add comprehensive progress reporting to database wipe functionality (8572f06)
* add complete 1977 Grateful Dead metadata collection (fb0e039)
* add database wipe functionality with library preservation and ratings refresh (42b6c69)
* add 1995 Grateful Dead metadata collection for TIGDH testing (30bfe42)
* add star rating display to playlist recording header (f269492)
* enhance metadata collection with year filtering and improved ratings data (2d99d1f)
* implement comprehensive show and recording ratings system (197b96a)

### Bug Fixes
* apply ratings consistently across all screens (ce76b58)

### Other Changes
* chore: improve .gitignore for Python cache files and metadata strategy (9cc8b2a)
## [0.16.0] - 2025-07-07

### New Features
* redesign Today in History as carousel display (3690d36)
* implement Today in Grateful Dead History feature (ee409fa)

### Bug Fixes
* resolve Today in History navigation by loading show recordings (0a80975)
* resolve compilation errors in Today in History feature (4516f35)
## [0.15.0] - 2025-07-07

### New Features
* implement offline playback for downloaded tracks (658a35a)
* add Spotify-style playlist UI with download and library controls (a8b3940)
* auto-add shows to library when downloading and update UI state (9c8965a)
* implement Spotify-style soft delete for downloads (f123206)
* add supporting infrastructure for download system (52f0568)
* enhance debug tools with comprehensive download troubleshooting (f86ebe7)
* integrate download system into main app architecture (97a7e67)
* implement comprehensive Downloads screen with management capabilities (3cc151d)
* integrate download state monitoring across Browse and Library screens (ddc7d54)
* enhance download system with improved queue management and cancellation (ece640a)
* add storage permissions for download functionality (3a3b4a7)
* implement Spotify-style download progress with theme colors (afd0370)
* fix queued download icon and auto-add downloaded shows to library (50cfe95)
* implement database-driven progress indicators with track counts (6cd5f66)
* implement comprehensive download status tracking in database (d252f20)
* improve download progress indicator and completion UI (12c5c16)
* add comprehensive download verification and monitoring tools (6b16cfa)
* add show-level download buttons with visual state indicators (4369f90)
* implement UI integration for download management system (a0808f8)
* implement download queue management system with WorkManager (fd69479)
* implement AudioDownloadWorker for background downloads (e01a77d)
* implement WorkManager foundation for download system (8f94bf6)

### Bug Fixes
* add core:data dependency to core:media module (09ff477)
* standardize download icons in playlist RecordingHeader (8302177)
* implement immediate UI feedback for download buttons in Library (6bf550e)
* resolve double-tap download activation issue (d82c433)
* resolve library UI jumping bug and download restart issues (3926e6e)
* resolve cancelled download UI state synchronization (81b74c0)
* enable soft delete and restore functionality in Library screen (11f9d38)
* enable download cancellation functionality in UI (a22fcaa)
* resolve compilation errors in DownloadRepository and SettingsScreen (95e95a8)
* replace non-existent Remove icon with text-based +/- buttons (5129c3a)
* implement proper URL resolution and format filtering for downloads (5211b6e)

### Code Refactoring
* clean up debug system and fix deprecation warning (4f80a5e)

### Documentation Updates
* update TODO with download system progress (2e009fc)
## [0.14.0] - 2025-07-06

### New Features
* implement comprehensive debug system and fix duplicate show creation (fdf2f9b)
* complete show-based library system with confirmation dialogs (da0bc94)
* implement library functionality and migrate to custom icon system (4c31c5f)

### Bug Fixes
* resolve library functionality issues with comprehensive architecture fixes (73338de)
## [0.13.0] - 2025-07-03

### New Features
* implement priority-based recording source visualization (1220aa7)
* simplify concert item UX by removing play button (2ee506f)
* add version information display to settings screen (d2b2f55)
* add navigation from player screen title to playlist screen (5486042)
* implement scrolling text for long titles across player screens (6207b4b)
* implement global miniplayer visible across all navigation screens (bae7607)

### Bug Fixes
* remove broken test dependency from release targets (9bb85d5)
* resolve foreign key constraint issue in sync process (5ae48de)
* resolve gradle build configuration issues for version info (b2a10f3)
* improve carousel content mapping logic in player screen (5677414)
* player screen shows concert information instead of 'Player' (78a5ce7)
* ensure home button always navigates to fresh home screen (7ab52c7)

### Code Refactoring
* complete Concert → Show+Recording model architecture overhaul (0443938)
* replace scrolling text with centered two-line title in player screen (cdd0880)

### Documentation Updates
* Some todo docs (53dd866)
## [0.12.0] - 2025-07-01

### New Features
* implement audio format filtering service with on-demand filtering (062bcd3)
* implement arrow-based audio format reordering in settings (c9a9656)
* make settings panel accessible from bottom navigation (8ab43b6)
* implement audio format preference reordering with drag controls (4121320)
* create comprehensive Settings Screen UI with Material3 design (8659094)
* implement SettingsViewModel with reactive state management (b5b82a3)
* implement DataStore settings repository with reactive updates (b1203f2)
* create core settings module with foundational architecture (7e2bf6a)

### Code Refactoring
* improve audio format settings UX (592705f)
## [0.11.2] - 2025-07-01

### Bug Fixes
* resolve empty venue handling in concert grouping tests (e9ddde2)

### Other Changes
* update: enhance Makefile with comprehensive quality checks and clean output (e7b6d91)
## [0.11.1] - 2025-07-01

### Bug Fixes
* resolve release build resource linking errors in design system (1f5bf04)
## [0.11.0] - 2025-07-01

### New Features
* create expandable concert list UI with recordings support (fb66dc3)
* add Recording/Concert grouping to API mappers (7ae3d74)
* implement Concert/Recording separation to eliminate duplicate entries (7c420a6)

### Bug Fixes
* resolve critical lint errors in MediaSessionService (f40a5e3)
* resolve compilation errors and finalize Concert/Recording UI (8c47215)
* resolve ArchiveMapper compilation errors (2820147)
## [0.10.0] - 2025-07-01

### New Features
* implement MediaController-based UI synchronization and fix queue navigation (3025c78)
* add QueueScreen UI component for queue management (d192efd)
* implement MediaController queue synchronization for proper media controls (402275c)
* implement service state synchronization for MediaSessionService (a4c7f66)
* connect UI to MediaSessionService architecture (3baeea5)
* create MediaControllerRepository for service-based media architecture (4846c31)

### Bug Fixes
* disable failing PlayerViewModelIntegrationTest to unblock release builds (35a5447)
## [0.9.0] - 2025-07-01

### New Features
* add settings icon as custom vector drawable (ea9661c)
* update download_material_icons.py to only download new icons and provide usage information (3c37235)
* add download-icons make target for simple icon management (8d473d4)
* implement consistent Material Icons utility (70653e2)
* implement audio playback and Spotify-style player integration (db3877e)
* implement Spotify-like playlist navigation architecture (3253b5b)
* add emulator cold boot command and improve audio support (4c30e5f)
* implement Spotify-style bottom navigation and home screen redesign (8c8fce0)

### Bug Fixes
* correct object structure in IconResources.kt (705f115)
* prevent duplicate function definitions in IconResources.kt (e933cbd)
* update material icon script to use Composable functions instead of direct references (83c36f4)
* implement track streaming functionality by generating download URLs (83ccf3d)
* resolve navigation crash and implement basic player screen (d8de66c)
* replace non-existent Material Icons with core alternatives (8e2694b)
* use valid Material Icons (Favorite/FavoriteBorder, Search) to resolve compilation errors (6c7d289)
* resolve icon compilation errors and improve build logging (c4d811a)
## [0.8.0] - 2025-06-29

### New Features
* implement multi-query approach to bypass Archive.org 10k limit (9ca9d4e)
* implement real data testing system and fix date search bug (6d2bb22)
* implement complete dataset caching strategy with local-only search (eeccbcf)
* implement comprehensive search functionality across all concert fields (96e81e4)
* implement comprehensive Browse feature with navigation fix (4f7a6eb)

### Bug Fixes
* resolve search bug where date patterns returned wrong results (64ef916)
* handle nullable date field in ConcertEntity filtering (9904057)
* improve search functionality for date queries and prevent incorrect fallback results (3bc1bf9)

### Tests
* add realistic search bug reproduction tests (e698588)
* add comprehensive end-to-end search functionality tests (905da73)
## [0.7.0] - 2025-06-28

### New Features
* implement comprehensive data layer with enhanced repositories and data mappers (af5c974)
* complete repository implementations with comprehensive test coverage (8683156)

### Bug Fixes
* resolve favorites not updating immediately after toggle (1f44a97)
* resolve database schema conflicts and Flow exception transparency violations (f8e9f4e)
* update RepositoryTestScreen to use repositories instead of direct DAO access (a31108e)
* resolve test failures and compilation warnings (eaf0193)
* resolve test compilation and logic issues in RepositoryErrorHandlingTest (f4dccc9)
* correct type mismatch in RepositoryErrorHandlingTest (422be3d)

### Code Refactoring
* simplify repository unit tests to focus on behavior over implementation (61682fa)
* implement hybrid testing approach focusing on business logic (691ccb2)
## [0.6.0] - 2025-06-28

### New Features
* add Repository Test Screen for manual testing (436d3ff)
* implement complete ConcertRepository with offline-first caching (4374120)

### Bug Fixes
* implement year-specific search and offline-first caching (b741940)
* bump Room database version for ConcertEntity schema changes (c0e03ed)
* add core:model dependency to app module (731ea67)

### Tests
* add integration tests for favorites synchronization bug (ec51fe1)
## [0.5.1] - 2025-06-27

### Bug Fixes
* use @OptIn annotation for UnstableApi in MainActivity (1ac2f70)
* add @UnstableApi annotation to MainActivity (4a8153c)
* add @UnstableApi annotation for Media3 API usage (c3d5eb6)
* add POST_NOTIFICATIONS permission for Android 13+ compatibility (1062df0)
## [0.5.0] - 2025-06-27

### New Features
* add test fixtures and documentation for test suite (8350130)
* add comprehensive unit test suite for model layer (822e81c)
## [0.4.0] - 2025-06-27

### New Features
* set Steal Your Face as app icon (ddad768)
* add Archive.org streaming and media player with test UI (79e7a73)
## [0.3.0] - 2025-06-27

### New Features
* add database layer, test screens, and Steal Your Face app icon (248c4a0)
## [0.2.9] - 2025-06-27

### Bug Fixes
* correct keystore path in signing.properties for app module (9939bf8)
## [0.2.8] - 2025-06-27

### Bug Fixes
* resolve YAML syntax error in workflow heredoc usage (829c018)
## [0.2.7] - 2025-06-27

### Code Refactoring
* consolidate signing logic into build step using bash conditionals (9eb2972)
## [0.2.6] - 2025-06-27

### Bug Fixes
* use explicit conditional syntax for GitHub Actions tag detection (e451bcd)
## [0.2.5] - 2025-06-27

### Bug Fixes
* resolve GitHub Actions conditional logic for tag detection (619386d)
## [0.2.4] - 2025-06-27

### Code Refactoring
* implement industry-standard signing configuration approach (b9d3fbe)
## [0.2.3] - 2025-06-27

### Bug Fixes
* resolve APK signing configuration issues in release builds (37f0a46)
## [0.2.2] - 2025-06-27

### Bug Fixes
* resolve release script commit counting bug when single commit exists (9a8735f)

### Code Refactoring
* enhance CI workflow with better APK info and error handling (6bf9c41)
## [0.2.1] - 2025-06-27

### Bug Fixes
* repair broken Makefile help command due to unterminated quotes (5426319)

### Other Changes
* security: enforce APK signing for release builds and validate prerequisites (14e5f8e)
## [0.2.0] - 2025-06-27

### New Features
* enhance CI workflow with improved build process and file naming (47ee847)
## [0.1.3] - 2025-06-27

### Bug Fixes
* resolve aapt command not found error in CI workflow (51ab24d)

### Code Refactoring
* unify CI/CD workflows into single pipeline with conditional logic (e94a8a1)
## [0.1.2] - 2025-06-27

### Bug Fixes
* improve GitHub Actions workflow APK handling and artifact uploads (5395a19)

### Code Refactoring
* streamline release process and remove workflow duplication (436487e)
## [0.1.1] - 2025-06-27

### Bug Fixes
* resolve gradle wrapper issues in GitHub Actions workflows (10d71cc)

### Other Changes
* security: enhance GitHub Actions workflow secret protection (c357923)
## [0.1.0] - 2025-06-27

### New Features
* add automatic release script with versioning and changelog (fad04ed)
* add GitHub Actions release build workflow with secure keystore signing (f9469e7)
* implement complete Archive.org API integration with robust JSON parsing (b1d6394)
* add comprehensive CI/CD pipeline with GitHub Actions (437dfdc)
* add automated emulator workflow and comprehensive documentation (d45930f)
* fix build system and add core module implementations (8448059)
* initial project setup for Dead Archive app (ead9c0c)

### Bug Fixes
* improve changelog generation and fix grep pattern errors (a54760e)
* prevent dry-run from modifying files - only show preview (c66bfb4)
* resolve gradle wrapper issues in GitHub Actions workflows (d43b14d)

### Documentation Updates
* add changelog for release process (26c3069)
* add comprehensive CI/CD documentation and workflow guides (ba5feb9)

### Build System
* add release build configuration and documentation (586dfa7)
