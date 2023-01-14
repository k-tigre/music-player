plugins {
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.AndroidLibrary.value)
}

dependencies {
    implementation(Toolkit.Decompose)
    implementation(Library.CoroutinesCore)
}
