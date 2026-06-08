plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidLibrary.value)
}

kotlin {
    androidTarget()
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(TigreLogger.Artifact.Core.notation)
            implementation(project(Project.Tools.Analytics.Common.name))
        }
        androidMain.dependencies {
            implementation(Library.CoroutinesAndroid.notation)
            implementation(Library.Mixpanel.notation)
            implementation(project(Project.Tools.Coroutines.name))
        }
    }
}

android {
    namespace = "by.tigre.music.player.tools.analytics"
}

tasks.register<JavaExec>("generateAnalyticsDocs") {
    group = "documentation"
    description = "Generates analytics event catalog from @AnalyticsScope annotations"
    val desktopCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktopCompilation.compileAllTaskName)
    classpath(desktopCompilation.output.allOutputs, desktopCompilation.runtimeDependencyFiles)
    mainClass.set("by.tigre.music.player.tools.analytics.doc.AnalyticsDocGeneratorKt")
    args(
        rootProject.layout.projectDirectory.dir("tools/analytics").asFile.absolutePath,
        rootProject.layout.projectDirectory.dir("docs/analytics-events.md").asFile.absolutePath,
    )
}
