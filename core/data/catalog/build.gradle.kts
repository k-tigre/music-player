plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Project.Tools.Entity)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Entity.Catalog)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation("androidx.documentfile:documentfile:1.0.1") // TODO remove
}
