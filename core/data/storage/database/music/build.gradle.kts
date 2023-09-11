@file:Suppress("UnstableApiUsage")

plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.SQLDelight.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.SQLDelightAndroid)
    implementation(Library.SQLDelightCoroutines)
    implementation(Project.Core.Entity.Catalog)
    implementation(Project.Core.Entity.Playback)
    implementation(Project.Tools.Entity)
    implementation(Project.Tools.Coroutines)
}

sqldelight {
    databases {
        create("DatabaseMusic") {
            packageName = "by.tigre.music.player.core.data.storage.music"
            generateAsync = true
        }
    }
}
