plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Project.Logger.Core)
    implementation(FirebaseLibrary.FirebaseCrashLytics)
}
