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

rootProject.name = "DeadArchive"

include(":app")
include(":app-v2")

// Core modules
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:database-v2")
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