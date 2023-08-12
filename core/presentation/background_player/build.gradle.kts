plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.KotlinParcelize.value)
}

android {

}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.AndoirdXAnnotation)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    api(Library.MediaSession)
    implementation(Library.MediaCommon)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Tools.Platform.Utils)
    implementation(Project.Core.Data.Playback)
    implementation(Project.Core.Entity.Catalog)
    implementation(Project.Logger.Core)
}
