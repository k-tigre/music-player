plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.ComposeMultiplatform.value)
    id(Plugin.Id.KotlinCompose.value)
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "by.tigre.music.player.tools.platform.compose.resources"
    }
}

kotlin {
    androidTarget()
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
        }
    }
}

android {
    namespace = "by.tigre.compose"
    buildFeatures.compose = true
}
