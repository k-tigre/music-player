plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.KotlinSerialization.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.KotlinxSerialization)
}
