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
    implementation(Project.Core.Data.Audiobook)
    implementation(Project.Core.Entity.Audiobook)
    implementation(Toolkit.UI)
}
