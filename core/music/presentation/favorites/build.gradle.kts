plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.ComposeMultiplatform.value)
    id(Plugin.Id.KotlinCompose.value)
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "by.tigre.music.player.core.presentation.favorites.resources"
    }
}

kotlin {
    android {
        namespace = "by.tigre.music.player.core.presentation.favorites"
        compileSdk = Application.SDK_COMPILE
        minSdk = Application.SDK_MINIMUM
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
    }
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(Library.CoilCompose.notation)
            implementation(Library.Decompose.notation)
            implementation(project(Project.Tools.Presentation.Compose.name))
            implementation(project(Project.Tools.Presentation.Decompose.name))
            implementation(project(Project.Tools.Analytics.Music.name))
            implementation(project(Project.Core.Music.Data.Favorites.name))
            implementation(project(Project.Core.Music.Data.Playback.name))
            implementation(project(Project.Core.Music.Data.Catalog.name))
            implementation(project(Project.Core.Music.Entity.Catalog.name))
            implementation(project(Project.Core.Music.Presentation.Catalog.name))
            implementation(project(Project.Core.Base.Presentation.Player.name))
        }
    }
}

