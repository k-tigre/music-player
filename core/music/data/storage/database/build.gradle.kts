plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
    id(Plugin.Id.SQLDelight.value)
}

kotlin {
    androidTarget()
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.SQLDelightCoroutines.notation)
            implementation(project(Project.Core.Music.Entity.Catalog.name))
            implementation(project(Project.Core.Music.Entity.Playback.name))
            implementation(project(Project.Core.Data.Storage.Preferences.name))
            implementation(project(Project.Tools.Entity.name))
            implementation(project(Project.Tools.Coroutines.name))
        }
        androidMain.dependencies {
            implementation(Library.SQLDelightAndroid.notation)
        }
        val desktopMain by getting {
            dependencies {
                implementation(Library.SQLDelightJvm.notation)
            }
        }
    }
}

android {
    namespace = "by.tigre.music.player.core.music.data.storage.database"
}

sqldelight {
    databases {
        create("DatabaseMusic") {
            packageName = "by.tigre.music.player.core.data.storage.music"
            generateAsync = true
        }
    }
}
