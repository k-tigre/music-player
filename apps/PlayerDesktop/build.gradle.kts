import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id(Plugin.Id.KotlinJvm.value)
    id(Plugin.Id.ComposeMultiplatform.value)
    id(Plugin.Id.KotlinCompose.value)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)

    implementation(Library.KotlinStd.notation)
    implementation(Library.CoroutinesCore.notation)
    implementation(Library.CoroutinesSwing.notation)
    implementation(Library.Decompose.notation)
    implementation(Library.DecomposeExtensions.notation)
    implementation(Library.CoilCompose.notation)

    implementation(project(Project.Tools.Entity.name))
    implementation(project(Project.Tools.Coroutines.name))
    implementation(project(Project.Tools.Presentation.Compose.name))
    implementation(project(Project.Tools.Presentation.Decompose.name))
    implementation(project(Project.Logger.Core.name))
    implementation(project(Project.Logger.Console.name))

    implementation(project(Project.Core.Music.Entity.Catalog.name))
    implementation(project(Project.Core.Music.Entity.Playback.name))
    implementation(project(Project.Core.Music.Data.Catalog.name))
    implementation(project(Project.Core.Music.Data.Playback.name))
    implementation(project(Project.Core.Base.Data.Playback.name))
    implementation(project(Project.Core.Data.Storage.Preferences.name))
    implementation(project(Project.Core.Music.Data.Storage.Database.name))

    implementation(project(Project.Core.Base.Presentation.Player.name))
    implementation(project(Project.Core.Music.Presentation.Catalog.name))
    implementation(project(Project.Core.Music.Presentation.PlaylistCurrentQueue.name))
}

compose.desktop {
    application {
        mainClass = "by.tigre.music.player.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MusicPlayer"
            packageVersion = "1.0.0"
            modules("java.sql")
        }
    }
}
