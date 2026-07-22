plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.tools.analytics.book"
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
            api(project(Project.Tools.Analytics.Common.name))
            implementation(project(Project.Tools.Coroutines.name))
        }
    }
}

