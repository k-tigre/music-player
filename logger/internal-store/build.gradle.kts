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
            implementation(Library.CoroutinesCore.notation)
            implementation(project(Project.Logger.Core.name))
            implementation(project(Project.Tools.Coroutines.name))
            implementation(Library.SQLDelightCoroutines.notation)
            implementation(Library.SQLDelightApapter.notation)
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
    namespace = "by.tigre.logger.internal_store"
}

sqldelight {
    databases {
        create("DatabaseLog") {
            packageName.set("by.tigre.music.player.logger.db")
            generateAsync.set(true)
        }
    }
}
