@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":androidApp")

include(":core:data:playback")
include(":core:data:catalog")
include(":core:data:storage:preferences")
include(":core:data:storage:database:music")
include(":core:platform:permission")

include(":core:entity:catalog")
include(":core:entity:playback")

include(":core:presentation:catalog")
include(":core:presentation:playlist:queue")
include(":core:presentation:player")
include(":core:presentation:background_player")

include(":tools:presentation:compose")
include(":tools:presentation:decompose")
include(":tools:entity")
include(":tools:coroutines")
include(":tools:platform:utils")
