plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
}

kotlin {
    android {
        namespace = "by.tigre.music.player.core.music.data.favorites"
        compileSdk = Application.SDK_COMPILE
        minSdk = Application.SDK_MINIMUM
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(project(Project.Core.Music.Entity.Catalog.name))
            implementation(project(Project.Core.Music.Data.Storage.Database.name))
            implementation(project(Project.Core.Music.Data.Catalog.name))
        }
    }
}

