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
            implementation(TigreLogger.Artifact.Core.notation)
        }
        androidMain.dependencies {
            implementation(Library.CoroutinesAndroid.notation)
        }
        val desktopMain by getting {
            dependencies {
                implementation(Library.CoroutinesSwing.notation)
            }
        }
    }
}

android {
    namespace = "by.tigre.tools.coroutines"
}
