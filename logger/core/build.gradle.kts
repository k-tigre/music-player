plugins {
    id(Plugin.Id.KotlinJvm.value)
    id(Plugin.Id.JavaLibrary.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
}
