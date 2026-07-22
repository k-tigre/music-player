plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
    id(Plugin.Id.SQLDelight.value)
}

kotlin {
    android {
        namespace = "by.tigre.audiobook.core.book.data.storage.database"
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
            implementation(Library.SQLDelightCoroutines.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(project(Project.Core.Book.Entity.Catalog.name))
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

sqldelight {
    databases {
        create("DatabaseAudiobook") {
            packageName = "by.tigre.audiobook.core.data.storage.audiobook"
            generateAsync = true
        }
    }
}
