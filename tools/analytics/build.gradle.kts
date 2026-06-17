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
    namespace = "by.tigre.media.platform.tools.analytics"
}

val generateAnalyticsDocs = tasks.register<JavaExec>("generateAnalyticsDocs") {
    group = "documentation"
    description = "Generates analytics event catalog from @AnalyticsScope annotations"
    val desktopCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktopCompilation.compileAllTaskName)
    classpath(desktopCompilation.output.allOutputs, desktopCompilation.runtimeDependencyFiles)
    mainClass.set("by.tigre.media.platform.tools.analytics.doc.AnalyticsDocGeneratorKt")
    args(
        rootProject.layout.projectDirectory.dir("tools/analytics").asFile.absolutePath,
        rootProject.layout.projectDirectory.dir("docs/analytics-events.md").asFile.absolutePath,
    )
}

tasks.register<Exec>("checkAnalyticsDocs") {
    group = "verification"
    description = "Regenerates analytics docs and fails if docs/analytics-events.md is out of date"
    dependsOn(generateAnalyticsDocs)
    workingDir(rootProject.projectDir)
    commandLine("git", "diff", "--exit-code", "docs/analytics-events.md")
}
