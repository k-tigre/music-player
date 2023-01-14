dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":androidApp")
include(":core:data:playback")
include(":core:data:catalog")
include(":core:presentation:catalog")
include(":tools:presentation:compose")
include(":tools:presentation:decompose")
include(":tools:entity")
include(":tools:coroutines")
