plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.KotlinCompose.value)
    id(Plugin.Id.KotlinParcelize.value)
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Music.Data.Playback)
    implementation(Project.Core.Music.Entity.Catalog)
    implementation(Project.Core.Music.Entity.Playback)

    implementation(Toolkit.UI)
}
