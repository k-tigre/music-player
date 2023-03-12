plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.KotlinParcelize.value)
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Data.Playback)
    implementation(Project.Core.Entity.Catalog)

    implementation(Toolkit.UI)
}
