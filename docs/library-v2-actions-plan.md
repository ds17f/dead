# LibraryV2 Show Actions Implementation Plan

## Overview
This document outlines the plan to implement the show actions bottom sheet for the LibraryV2 screen, which will appear on long press of a show item. The bottom sheet will offer functionality for "Share", "Remove from Library", "Download", "Pin", and "Show QR Code".

## Requirements
1. Long-press on show items in both list and grid views should bring up an actions bottom sheet
2. Bottom sheet should follow existing UI patterns and Material3 design
3. QR code action should display a QR code linking to the archive.org recording
4. All actions should be properly connected to the LibraryV2 services
5. All implementations should stay within the "V2" namespace to avoid affecting existing functionality

## Implementation Tasks

### 1. Add Required Dependencies
- Add ZXing library dependency for QR code generation in feature/library/build.gradle.kts
- Use implementation("com.google.zxing:core:3.5.3") for the core QR code generation

### 2. Create ShowActionsBottomSheet Component
- Create new component following existing bottom sheet patterns
- Include all required action buttons with proper icons:
  - Share: IconResources.Content.Share()
  - Remove from Library: IconResources.Content.Delete()
  - Download: IconResources.Content.CloudDownload()
  - Pin: IconResources.Content.PushPin()
  - Show QR Code: IconResources.Content.QrCode()
- Implement callback pattern for all actions

### 3. Create QrCodeBottomSheet Component
- Implement a separate bottom sheet to display QR codes
- Use ZXing to generate QR code bitmap for archive.org recording URL
- Allow user to close or share the QR code
- Display recording title and date

### 4. Enhance Show Items with Long Press
- Add combinedClickable modifier to both ShowListItem and ShowGridItem components
- Connect long press events to the selectedShowForActions state
- Ensure visual feedback on long press

### 5. Update LibraryV2Screen Integration
- Uncomment and update the commented-out ShowActionsBottomSheet section
- Add new QrCodeBottomSheet component with appropriate state management
- Ensure proper state management for showing/hiding bottom sheets

### 6. Connect Required Services
- Use existing LibraryV2ViewModel methods for:
  - removeFromLibrary()
  - downloadShow()
- Add the ShareService injection and functionality
- Implement Pin functionality in LibraryV2Service interface and stub

### 7. Testing and Verification
- Test all bottom sheet actions
- Verify QR code generation displays proper URLs
- Test in both list and grid views
- Ensure all actions work as expected with proper error handling

## UI Design Notes
- Follow Material3 bottom sheet patterns as seen in SortOptionsBottomSheet
- Use consistent spacing and typography from other components
- Implement proper animation for bottom sheet transitions
- Ensure bottom sheets work in both light and dark themes

## Scope Limitations
- The initial implementation will focus on UI functionality
- Pin functionality will be stubbed at first
- We'll stay within the V2 namespace to avoid affecting existing functionality
- QR code implementation will use basic ZXing generation without custom styling initially