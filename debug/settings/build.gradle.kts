plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.KotlinCompose.value)
}
android {
    buildFeatures {
        compose = true
    }
}
dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Project.Tools.Coroutines)
    implementation(TigreLogger.Artifact.InternalStore)
    implementation(TigreLogger.Artifact.Core)

    implementation(Toolkit.UI)

    implementation(Library.ComposeFoundation)
    implementation(Library.ComposeMaterial1)
}
