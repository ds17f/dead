pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Deadly"

include(":app")

// V2 modules
include(":v2:app")
include(":v2:core:database")
include(":v2:core:design")
include(":v2:core:domain")
include(":v2:core:model")
include(":v2:core:network")
include(":v2:core:network:archive")
include(":v2:core:theme-api")
include(":v2:core:theme")
include(":v2:core:api:search")
include(":v2:core:search")
include(":v2:core:api:playlist")
include(":v2:core:playlist")
include(":v2:feature:splash")
include(":v2:feature:home")
include(":v2:feature:search")
include(":v2:feature:playlist")
include(":v2:feature:settings")

// Core modules
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:media")
include(":core:design")
include(":core:common")
include(":core:settings")
include(":core:backup")

// Feature modules
include(":feature:browse")
include(":feature:player")
include(":feature:playlist")
include(":feature:library")

// New API modules
include(":core:settings-api")
include(":core:data-api")
include(":core:media-api")
include(":core:download-api")
include(":core:library-api")
include(":core:search-api")

// V2 Service modules
include(":core:library")
include(":core:download")
include(":core:search")