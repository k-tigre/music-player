plugins {
    id(Plugin.Id.KotlinMultiplatform.value)
    id(Plugin.Id.AndroidKmpLibrary.value)
    id(Plugin.Id.KotlinSerialization.value)
}

kotlin {
    android {
        namespace = "by.tigre.core.book.data.catalog"
        compileSdk = Application.SDK_COMPILE
        minSdk = Application.SDK_MINIMUM
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    jvm("desktop")
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(Library.KotlinStd.notation)
            implementation(Library.CoroutinesCore.notation)
            implementation(project(Project.Tools.Coroutines.name))
            implementation(TigreLogger.Artifact.Core.notation)
            implementation(project(Project.Core.Book.Entity.Catalog.name))
            implementation(project(Project.Core.Book.Data.Storage.Database.name))
        }
        androidMain.dependencies {
            implementation(Library.AndroidXDocumentFile.notation)
        }
    }
}

