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

// Core modules
include(":core:model")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:media")
include(":core:design")
include(":core:common")
include(":core:settings")

// Feature modules
include(":feature:browse")
include(":feature:player")
include(":feature:playlist")
include(":feature:downloads")
include(":feature:library")

// New API modules
include(":core:settings-api")
include(":core:data-api")
include(":core:media-api")