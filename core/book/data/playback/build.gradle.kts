plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Library.MediaCommon)
    implementation(Library.MediaPlayer)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Logger.Core)
    implementation(Project.Core.Book.Entity.Catalog)
    implementation(Project.Core.Base.Data.Playback)
    implementation(Project.Core.Book.Data.Catalog)
    implementation(Project.Core.Book.Data.Storage.Database)
}
