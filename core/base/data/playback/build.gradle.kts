plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.playback"
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
            implementation(project(Project.Tools.Coroutines.name))
            implementation(TigreLogger.Artifact.Core.notation)
            implementation(project(Project.Core.Data.Storage.Preferences.name))
        }
        androidMain.dependencies {
            implementation(Library.MediaCommon.notation)
            implementation(Library.MediaPlayer.notation)
        }
        val desktopMain by getting {
            dependencies {
                implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
                implementation(Library.JAudioTagger.notation)
                implementation(Library.JavaCv.notation)
                implementation(Library.FfmpegPlatform.notation)
            }
        }
    }
}

