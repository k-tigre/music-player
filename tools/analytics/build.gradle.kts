plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
}

kotlin {
    androidTarget()
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(TigreLogger.Artifact.Core.notation)
            implementation(project(Project.Tools.Coroutines.name))
        }
        androidMain.dependencies {
            implementation(Library.CoroutinesAndroid.notation)
            implementation(Library.Mixpanel.notation)
        }
    }
}

android {
    namespace = "by.tigre.music.player.tools.analytics"
}
