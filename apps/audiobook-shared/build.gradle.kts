@file:Suppress("UnstableApiUsage")

plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

android {
    namespace = "by.tigre.audiobook.shared"
    buildFeatures {
        buildConfig = false
        compose = false
    }
}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)
    implementation(Library.KotlinStd)
    implementation(Library.MediaCommon)

    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Music.Presentation.Catalog)
    implementation(Project.Core.Book.Presentation.Catalog)
    implementation(Project.Core.Book.Data.Catalog)
    implementation(Project.Core.Book.Data.Playback)
    implementation(Project.Core.Book.Data.Storage.Database)
    implementation(Project.Core.Book.Entity.Catalog)
    implementation(Project.Core.Base.Presentation.Player)
    implementation(Project.Core.Music.Presentation.PlaylistCurrentQueue)
    implementation(Project.Core.Base.Presentation.BackgroundPlayer)
    implementation(Project.Core.Platform.Permission)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation(Project.Core.Music.Data.Storage.Database)
    implementation(Project.Core.Music.Data.Playback)
    implementation(Project.Core.Music.Data.Catalog)
    implementation(Project.Core.Music.Entity.Catalog)

    implementation(Project.Logger.Core)
}
