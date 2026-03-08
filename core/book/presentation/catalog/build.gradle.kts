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
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.AndroidXDocumentFile)
    implementation(Library.CoroutinesAndroid)
    implementation(Library.KotlinStd)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Book.Data.Catalog)
    implementation(Project.Core.Book.Data.Playback)
    implementation(Project.Core.Book.Entity.Catalog)
    implementation(Toolkit.UI)
}
