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
    implementation(Project.Core.Entity.Audiobook)
    implementation(Project.Core.Data.Playback)
    implementation(Project.Core.Data.Audiobook)
    implementation(Project.Core.Data.Storage.Database.Audiobook)
}
