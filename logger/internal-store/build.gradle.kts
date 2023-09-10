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
}

sqldelight {
    database("DatabaseLog") {
        packageName = "by.tigre.music.player.logger.db"
        sourceFolders = listOf("sqldelight")
        version = 1

        schemaOutputDirectory = File(project.projectDir, "src/main/sqldelight")
    }
}
