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
    implementation(compose.components.resources)

    implementation(Library.KotlinStd.notation)
    implementation(Library.CoroutinesCore.notation)
    implementation(Library.CoroutinesSwing.notation)
    implementation(Library.Jnativehook)
    implementation(Library.Decompose.notation)
    implementation(Library.DecomposeExtensions.notation)
    implementation(Library.CoilCompose.notation)
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("com.github.hypfvieh:dbus-java-core:4.3.2")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.3.2")

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

compose {
    resources {
        publicResClass = true
        packageOfResClass = "by.tigre.music.player.desktop.resources"
    }
}

compose.desktop {
    application {
        mainClass = "by.tigre.music.player.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MusicPlayer"
            packageVersion = "1.0.4"
            modules("java.sql")

            macOS {
                iconFile.set(project.file("icons/app.icns"))
            }
            windows {
                iconFile.set(project.file("icons/app.ico"))
                menu = true
                menuGroup = "Music Player"
                shortcut = true
            }
            linux {
                iconFile.set(project.file("icons/app.png"))
            }
        }
    }
}
