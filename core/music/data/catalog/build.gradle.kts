plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
}

kotlin {
    android {
        namespace = "by.tigre.music.player.core.music.data.catalog"
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
            implementation(Library.KotlinxSerializationJson.notation)
            implementation(Library.KtorClientCore.notation)
            implementation(Library.KtorClientContentNegotiation.notation)
            implementation(Library.KtorSerializationJson.notation)
            implementation(Library.Okio.notation)
            implementation(project(Project.Tools.Entity.name))
            implementation(project(Project.Tools.Coroutines.name))
            implementation(TigreLogger.Artifact.Core.notation)
            implementation(project(Project.Core.Music.Entity.Catalog.name))
            implementation(project(Project.Core.Data.Storage.Preferences.name))
        }
        val desktopMain by getting {
            dependencies {
                implementation(Library.SQLDelightJvm.notation)
                implementation(Library.JAudioTagger.notation)
                implementation(Library.KtorClientCio.notation)
            }
        }
        androidMain.dependencies {
            implementation(Library.AndroidXCore.notation)
            implementation(Library.ActivityCompose.notation)
            implementation(Library.CoroutinesAndroid.notation)
            implementation(Library.KtorClientOkHttp.notation)
        }
    }
}

