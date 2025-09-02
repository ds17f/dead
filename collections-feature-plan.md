# Collections Feature Development Plan

## Overview
Add Collections as a new bottom navigation destination in V2, replacing the existing 4-tab layout with a 5-tab layout that includes Collections. This will be a complete feature module following the established V2 architecture patterns.

## Phase 1: Material Icon Setup
1. **Add 'cards_star' icon to material_icons_config.json**
   - Add to the "Navigation" section of the config
   
2. **Download the new icon**
   - Run: `python scripts/download_material_icons.py --from-json "scripts/material_icons_config.json"`
   - This will download and generate the vector drawable

## Phase 2: Bottom Navigation Updates
3. **Update BottomNavDestination.kt**
   - Add Collections as the 4th destination (before Settings)
   - Use cards_star for selected/unselected icons
   - Update companion object destinations list to include Collections

4. **Update MainNavigation.kt**
   - Add collectionsGraph import and navigation call
   - Ensure proper routing between features

## Phase 3: Collections Feature Module Structure
5. **Create v2/feature/collections module**
   ```
   v2/feature/collections/
   ├── build.gradle.kts (based on search module)
   ├── src/main/java/com/deadly/v2/feature/collections/
   │   ├── navigation/
   │   │   └── CollectionsNavigation.kt
   │   └── screens/
   │       └── main/
   │           ├── CollectionsBarConfiguration.kt
   │           ├── CollectionsScreen.kt
   │           └── models/
   │               └── CollectionsViewModel.kt
   ```

## Phase 4: Collections Implementation
6. **CollectionsNavigation.kt**
   - Follow search navigation pattern
   - Single route "collections" for now
   - Placeholder for future collection detail screens

7. **CollectionsBarConfiguration.kt**
   - Standard top bar with "Collections" title
   - Bottom nav visible, mini-player visible
   - QR code or other actions as needed

8. **CollectionsScreen.kt**
   - Placeholder screen with debug integration
   - Use AppScaffold pattern
   - Include debug panel for development

9. **CollectionsViewModel.kt**
   - Basic ViewModel stub with Hilt injection
   - Integrate with existing DeadCollectionsService
   - Prepare for future collection browsing

## Phase 5: Module Integration
10. **Update settings.gradle.kts**
    - Include `:v2:feature:collections` module

11. **Update v2/app dependencies**
    - Add collections feature module to dependencies
    - Ensure proper navigation wiring

## Architecture Notes
- Follow established V2 patterns from search/library features
- Use existing DeadCollectionsService for data
- Maintain clean separation with navigation graphs
- Include debug integration for development
- Prepare for future collection detail screens and browsing