plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Library.ExoPlayerCore)
    implementation(Project.Tools.Entity)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Entity.Catalog)
    implementation(Project.Core.Entity.Playback)
    implementation(Project.Core.Data.Storage.Database.Music)
    implementation(Project.Core.Data.Catalog)
}
