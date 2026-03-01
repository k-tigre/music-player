plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":PlayerApp")
include(":AudioBook")

include(":core:data:playback")
include(":core:data:catalog")
include(":core:data:audiobook")
include(":core:data:storage:preferences")
include(":core:data:storage:database:music")
include(":core:data:storage:database:audiobook")
include(":core:platform:permission")

include(":core:entity:catalog")
include(":core:entity:playback")
include(":core:entity:audiobook")

include(":core:presentation:catalog")
include(":core:presentation:audiobook_catalog")
include(":core:presentation:playlist:queue")
include(":core:presentation:player")
include(":core:presentation:background_player")

include(":tools:presentation:compose")
include(":tools:presentation:decompose")
include(":tools:entity")
include(":tools:coroutines")
include(":tools:platform:utils")

include(":logger:core")
include(":logger:logcat")
include(":logger:crashlytics")
include(":logger:internal-store")
include(":debug:settings")
