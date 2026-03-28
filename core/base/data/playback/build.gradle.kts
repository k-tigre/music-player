plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
}

kotlin {
    androidTarget()
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(project(Project.Tools.Coroutines.name))
            implementation(project(Project.Logger.Core.name))
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

android {
    namespace = "by.tigre.music.player.core.base.data.playback"
}
