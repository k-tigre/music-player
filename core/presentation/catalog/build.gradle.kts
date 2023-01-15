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
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    implementation(Library.MaterialComponents)
    implementation(Library.AccompanistPager)
    implementation(Library.AccompanistPagerInidcators)
    implementation(Project.Tools.Coroutines)

    implementation(Toolkit.UI)
}
