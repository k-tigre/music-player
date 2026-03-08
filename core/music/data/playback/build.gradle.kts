plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Project.Tools.Entity)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Logger.Core)
    implementation(Project.Core.Music.Entity.Catalog)
    implementation(Project.Core.Music.Entity.Playback)
    implementation(Project.Core.Music.Data.Storage.Database)
    implementation(Project.Core.Music.Data.Catalog)
    api(Project.Core.Base.Data.Playback)
}
