plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinParcelize.value)
}

android {

    namespace = "by.tigre.media.platform.background"
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
    implementation(Project.Core.Base.Data.Playback)
    implementation(Project.Core.Base.Presentation.Player)
    implementation(TigreLogger.Artifact.Core)
}
