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
            api(project(Project.Tools.Analytics.Common.name))
            implementation(project(Project.Tools.Coroutines.name))
        }
    }
}

android {
    namespace = "by.tigre.media.platform.tools.analytics.book"
}
