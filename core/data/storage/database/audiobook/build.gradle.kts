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
    implementation(Library.CoroutinesCore)
    implementation(Project.Core.Entity.Audiobook)
    implementation(Project.Tools.Coroutines)
}

sqldelight {
    databases {
        create("DatabaseAudiobook") {
            packageName = "by.tigre.audiobook.core.data.storage.audiobook"
            generateAsync = true
        }
    }
}
