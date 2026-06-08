plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    fun envOrPropertyNullable(key: String): String? {
        return providers.gradleProperty(key)
            .orElse(providers.environmentVariable(key))
            .orNull
    }


    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/k-tigre/logger")
            credentials {
                username = envOrPropertyNullable("GITHUB_ACTOR")
                password = envOrPropertyNullable("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "MusicPlayer"
include(":apps:PlayerApp")
include(":apps:AudioBook")
include(":apps:PlayerDesktop")

include(":core:base:data:playback")
include(":core:music:data:playback")
include(":core:music:data:catalog")
include(":core:music:data:storage:database")
include(":core:book:data:catalog")
include(":core:book:data:playback")
include(":core:book:data:storage:database")
include(":core:data:storage:preferences")
include(":core:platform:permission")

include(":core:music:entity:catalog")
include(":core:music:entity:playback")
include(":core:book:entity:catalog")

include(":core:base:presentation:player")
include(":core:base:presentation:background_player")
include(":core:music:presentation:catalog")
include(":core:music:presentation:playlist:queue")
include(":core:book:presentation:catalog")

include(":tools:presentation:compose")
include(":tools:presentation:decompose")
include(":tools:analytics")
include(":tools:analytics:common")
include(":tools:analytics:music")
include(":tools:analytics:book")
include(":tools:entity")
include(":tools:coroutines")
include(":tools:platform:utils")

include(":debug:settings")
