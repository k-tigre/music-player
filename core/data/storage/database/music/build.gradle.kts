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
    database("DatabaseMusic") {
        packageName = "by.tigre.music.player.core.data.storage.music"
        sourceFolders = listOf("sqldelight")
        version = 4

        schemaOutputDirectory = File(project.projectDir, "src/main/sqldelight")
    }
}
