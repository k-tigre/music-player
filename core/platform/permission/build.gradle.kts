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
        }
        androidMain.dependencies {
            implementation(Library.AndroidXCore.notation)
        }
    }
}

android {
    namespace = "by.tigre.core.platform.permission"
}
