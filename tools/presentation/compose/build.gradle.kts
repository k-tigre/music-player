plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

android {
    buildFeatures.compose = true
}

dependencies {
    implementation(Toolkit.Compose)

    // Preview
    debugApi(Library.DebugComposeCustomView)
    debugApi(Library.DebugComposeCustomViewPoolingcontainer)
    debugApi(Library.DebugComposeUiToolingPreview)
}
