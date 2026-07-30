plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.entitlements"
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(project(Project.Core.Platform.Billing.name))
        }
    }
}

dependencies {
    add("androidMainImplementation", platform(FirebaseLibrary.bom))
    add("androidMainImplementation", FirebaseLibrary.FirebaseConfig.notation)
}
