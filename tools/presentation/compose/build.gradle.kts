plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.ComposeMultiplatform.value)
    id(Plugin.Id.KotlinCompose.value)
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "by.tigre.media.platform.tools.platform.compose.resources"
    }
}

kotlin {
    android {
        namespace = "by.tigre.media.platform.tools.compose"
        compileSdk = Application.SDK_COMPILE
        minSdk = Application.SDK_MINIMUM
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
    }
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(Library.CoilCompose.notation)
        }
        androidMain.dependencies {
            implementation(Library.ActivityCompose.notation)
            implementation(Library.AndroidXCore.notation)
            implementation(Library.ComposeUiTextGoogleFonts.notation)
        }
    }
}

