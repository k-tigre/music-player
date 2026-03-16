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
            implementation(project(Project.Tools.Coroutines.name))
            implementation(project(Project.Logger.Core.name))
            implementation(project(Project.Core.Book.Entity.Catalog.name))
            implementation(project(Project.Core.Base.Data.Playback.name))
            implementation(project(Project.Core.Book.Data.Catalog.name))
            implementation(project(Project.Core.Book.Data.Storage.Database.name))
        }
    }
}

android {
    namespace = "by.tigre.core.book.data.playback"
}
