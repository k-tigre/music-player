plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.tools.coroutines"
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

