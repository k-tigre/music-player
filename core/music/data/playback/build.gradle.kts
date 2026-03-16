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
            implementation(project(Project.Tools.Entity.name))
            implementation(project(Project.Tools.Coroutines.name))
            implementation(project(Project.Logger.Core.name))
            implementation(project(Project.Core.Music.Entity.Catalog.name))
            implementation(project(Project.Core.Music.Entity.Playback.name))
            implementation(project(Project.Core.Music.Data.Storage.Database.name))
            implementation(project(Project.Core.Music.Data.Catalog.name))
            api(project(Project.Core.Base.Data.Playback.name))
        }
    }
}

android {
    namespace = "by.tigre.music.player.core.music.data.playback"
}
