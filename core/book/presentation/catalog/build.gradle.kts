plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.ComposeMultiplatform.value)
    id(Plugin.Id.KotlinCompose.value)
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(Library.CoilCompose.notation)
            implementation(Library.Decompose.notation)
            implementation(Library.DecomposeExtensions.notation)
            implementation(project(Project.Tools.Presentation.Compose.name))
            implementation(project(Project.Tools.Presentation.Decompose.name))
            implementation(project(Project.Tools.Coroutines.name))
            implementation(project(Project.Core.Book.Data.Catalog.name))
            implementation(project(Project.Core.Book.Data.Playback.name))
            implementation(project(Project.Core.Book.Entity.Catalog.name))
        }
        androidMain.dependencies {
            implementation(Library.AndroidXDocumentFile.notation)
            implementation(Library.ActivityCompose.notation)
        }
    }
}

android {
    namespace = "by.tigre.audiobook.presentation.catalog"
    buildFeatures.compose = true
}
