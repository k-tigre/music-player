plugins {
    id(Plugin.Id.AndroidLibrary.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.SQLDelight.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(Library.CoroutinesCore)
    implementation(Project.Logger.Core)
    implementation(Project.Tools.Coroutines)
    implementation(Library.SQLDelightAndroid)
    implementation(Library.SQLDelightCoroutines)
    implementation(Library.SQLDelightApapter)
}

sqldelight {
    databases {
        create("DatabaseLog") {
            packageName.set("by.tigre.music.player.logger.db")
            generateAsync.set(true)
        }
    }
}
