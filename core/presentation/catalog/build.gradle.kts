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
//    implementation(Library.AccompanistPager)
//    implementation(Library.AccompanistPagerInidcators)
    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Data.Catalog)
    implementation(Project.Core.Data.Playback)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation(Project.Core.Platform.Permossion)
    implementation(Project.Core.Entity.Catalog)

    implementation(Toolkit.UI)
}
