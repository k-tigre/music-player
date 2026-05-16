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
            implementation(Library.Decompose.notation)
            implementation(Library.DecomposeExtensions.notation)
        }
    }
}

android {
    namespace = "by.tigre.tools.presentation.decompose"
}
