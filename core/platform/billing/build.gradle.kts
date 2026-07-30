plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.billing"
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
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain.dependencies {
            implementation(Library.BillingKtx.notation)
        }
    }
}
