# Android Project Modularization Guide

## Gradle Modules vs Packages

### Gradle Module

-   A **Gradle module** is a build unit with its own
    `build.gradle[.kts]` file.
-   Types of modules:
    -   `:app` -- the main application module that builds the APK/AAB.
    -   `:feature:*` -- feature-specific modules (e.g., login, profile).
    -   `:core:*` -- shared modules (e.g., database, network).
    -   External dependencies (`com.squareup.retrofit2:retrofit:2.9.0`).

### Package

-   A **package** is a namespace for classes and functions.
-   Example: `package com.example.myapp.core.database`.
-   Packages are about **namespacing**, not build boundaries.
-   Multiple modules can have the same package name, but if they define
    the **same class name** in the same package, you'll get a
    **duplicate class error**.

### What happens with duplicates?

-   If two modules define the same fully qualified class (e.g.,
    `com.example.shared.Utils`):
    -   If both are on the same classpath, you'll get a **duplicate
        class compile error**.
    -   If only one is exposed (`api` vs `implementation`), then the
        other remains hidden.

------------------------------------------------------------------------

## Project Structure: `app`, `core`, `feature`

### `app/`

-   The **application module**.
-   Contains the `AndroidManifest.xml` and produces the APK/AAB.
-   Responsibilities:
    -   App-level setup (`Application` class, DI setup, navigation
        host).
    -   Dependency wiring.
-   Should not contain business logic.

### `feature/`

-   Each **feature module** is a self-contained slice of functionality.
-   Contains:
    -   UI (Activities, Fragments, Composables).
    -   ViewModels and feature-specific domain logic.
    -   Feature-specific resources.
-   Can be dynamic features (delivered on demand).

### `core/`

-   **Shared libraries** used across features.
-   Examples:
    -   `core:network` → API clients, Retrofit setup.
    -   `core:database` → Room entities and DAOs.
    -   `core:ui` → Shared UI components (buttons, themes).
    -   `core:common` → Utility classes, extensions.
    -   `core:domain` → Business logic, use cases.

------------------------------------------------------------------------

## Example Layout

    root/
     ├── app/                     # The main app entry point
     │    └── build.gradle
     │
     ├── core/
     │    ├── common/             # General utils/extensions
     │    ├── ui/                 # Shared UI toolkit
     │    ├── network/            # Retrofit/OkHttp setup
     │    └── database/           # Room, DAOs, entities
     │
     ├── feature/
     │    ├── login/              # Login screen + logic
     │    ├── profile/            # Profile screen + logic
     │    └── search/             # Search feature
     │
     └── build.gradle (root)

------------------------------------------------------------------------

## Dependency Graph

-   `app` depends on all `feature` modules.
-   Each `feature` depends on the `core` modules it needs.
-   `core` modules generally don't depend on features.

```{=html}
<!-- -->
```
    app
     └── feature:login
     └── feature:profile
     └── feature:search
          └── core:network
          └── core:ui
     └── core:common

------------------------------------------------------------------------

## Why Modularize?

-   **Scalability** → large projects with 20+ features stay manageable.
-   **Reusability** → shared code in `core`, no duplication.
-   **Faster builds** → Gradle rebuilds only the changed modules.
-   **Clear ownership** → teams can "own" a feature module.

------------------------------------------------------------------------

## Rule of Thumb

-   If it's app-wide setup → put it in `app/`
-   If it's feature-specific → put it in `feature/xxx`
-   If it's reusable/shared → put it in `core/xxx`
