# Changelog

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
