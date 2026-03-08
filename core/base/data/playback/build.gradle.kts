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
}
