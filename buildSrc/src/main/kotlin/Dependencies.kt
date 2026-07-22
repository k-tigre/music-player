@file:Suppress("SpellCheckingInspection")

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.kotlin.dsl.project

enum class Library(group: String, artifact: String, version: Version) {
    AndroidXAppcompat("androidx.appcompat", "appcompat", Version.AndroidXAppcompat),
    AndroidXCore("androidx.core", "core-ktx", Version.AndroidXCore),
    AndoirdXAnnotation("androidx.annotation", "annotation", Version.AndroidXAnnotation),
    AndroidXSplash("androidx.core", "core-splashscreen", Version.AndroidXSplash),
    AndroidXDocumentFile("androidx.documentfile", "documentfile", Version.AndroidXDocumentFile),
    AndroidXPalette("androidx.palette", "palette-ktx", Version.AndroidXPalette),

    KotlinStd("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", Version.Kotlin),
    KotlinxSerialization("org.jetbrains.kotlinx", "kotlinx-serialization-core", Version.KotlinSerialization),
    KotlinxSerializationJson("org.jetbrains.kotlinx", "kotlinx-serialization-json", Version.KotlinSerialization),

    CoroutinesCore("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Version.Coroutines),
    CoroutinesAndroid("org.jetbrains.kotlinx", "kotlinx-coroutines-android", Version.Coroutines),
    CoroutinesSwing("org.jetbrains.kotlinx", "kotlinx-coroutines-swing", Version.Coroutines),

    KtorClientCore("io.ktor", "ktor-client-core", Version.Ktor),
    KtorClientContentNegotiation("io.ktor", "ktor-client-content-negotiation", Version.Ktor),
    KtorSerializationJson("io.ktor", "ktor-serialization-kotlinx-json", Version.Ktor),
    KtorClientOkHttp("io.ktor", "ktor-client-okhttp", Version.Ktor),
    KtorClientCio("io.ktor", "ktor-client-cio", Version.Ktor),

    Okio("com.squareup.okio", "okio", Version.Okio),

    SQLDelightAndroid("app.cash.sqldelight", "android-driver", Version.SQLDelight),
    SQLDelightJvm("app.cash.sqldelight", "sqlite-driver", Version.SQLDelight),
    SQLDelightCoroutines("app.cash.sqldelight", "coroutines-extensions", Version.SQLDelight),
    SQLDelightApapter("app.cash.sqldelight", "primitive-adapters", Version.SQLDelight),

    MediaPlayer("androidx.media3", "media3-exoplayer", Version.Media3),
    MediaUi("androidx.media3", "media3-ui", Version.Media3),
    MediaCommon("androidx.media3", "media3-common", Version.Media3),
    MediaSession("androidx.media3", "media3-session", Version.Media3),

    Leakcanary("com.squareup.leakcanary", "leakcanary-android", Version.Leakcanary),

    ComposeUI("androidx.compose.ui", "ui", Version.Compose),
    ComposeUIToolkit("androidx.compose.ui", "ui-tooling", Version.Compose),
    ComposeFoundation("androidx.compose.foundation", "foundation", Version.ComposeFoundation),
    ComposeMaterial1("androidx.compose.material", "material", Version.ComposeMaterial),
    ComposeMaterial("androidx.compose.material3", "material3", Version.ComposeMaterial3),
    ComposeMaterialWindowSize("androidx.compose.material3", "material3-window-size-class", Version.ComposeMaterial3),
    ComposeMaterialIconsExtended("androidx.compose.material", "material-icons-extended", Version.ComposeMaterialIcons),
    ComposeMaterialIconsCore("androidx.compose.material", "material-icons-core", Version.ComposeMaterialIcons),
    ActivityCompose("androidx.activity", "activity-compose", Version.ActivityCompose),

    CoilCompose("io.coil-kt.coil3", "coil-compose", Version.CoilCompose),
    JAudioTagger("net.jthink", "jaudiotagger", Version.JAudioTagger),
    JavaCv("org.bytedeco", "javacv", Version.JavaCv),
    FfmpegPlatform("org.bytedeco", "ffmpeg-platform", Version.FfmpegPlatform),

    DebugComposeUiToolingPreview("androidx.compose.ui", "ui-tooling-preview", Version.Compose),

    AccompanistPermission("com.google.accompanist", "accompanist-permissions", Version.Accompanist),

    Decompose("com.arkivanov.decompose", "decompose", Version.Decompose),
    DecomposeExtensions("com.arkivanov.decompose", "extensions-compose", Version.Decompose),

    Reorderable("sh.calvin.reorderable", "reorderable", Version.Reorderable),

    Jnativehook("com.github.kwhat", "jnativehook", Version.Jnativehook),

    Mixpanel("com.mixpanel.android", "mixpanel-android", Version.Mixpanel),

    ComposeUiTestJunit4("androidx.compose.ui", "ui-test-junit4", Version.Compose),
    ComposeUiTestManifest("androidx.compose.ui", "ui-test-manifest", Version.Compose),
    ComposeUiTextGoogleFonts("androidx.compose.ui", "ui-text-google-fonts", Version.ComposeUiTextGoogleFonts),
    ComposeComponentsResources(
        "org.jetbrains.compose.components",
        "components-resources",
        Version.ComposeMultiplatform
    ),
    AndroidXTestCore("androidx.test", "core", Version.AndroidXTest),
    JUnit4("junit", "junit", Version.JUnit4),
    Robolectric("org.robolectric", "robolectric", Version.Robolectric),
    Roborazzi("io.github.takahirom.roborazzi", "roborazzi", Version.Roborazzi),
    RoborazziCompose("io.github.takahirom.roborazzi", "roborazzi-compose", Version.Roborazzi),

    // TODO compose preview not working, check issue: https://issuetracker.google.com/issues/227767363
    DebugComposeCustomView("androidx.customview", "customview", Version.DebugComposeCustomView),
    DebugComposeCustomViewPoolingcontainer(
        "androidx.customview",
        "customview-poolingcontainer",
        Version.DebugComposeCustomViewPoolingcontainer
    ),
    ;

    val notation = "$group:$artifact:${version.value}"

    internal enum class Version(val value: String) {
        ActivityCompose("1.13.0"),
        AndroidXAppcompat("1.7.1"),
        AndroidXCore("1.19.0"),
        AndroidXAnnotation("1.10.0"),
        AndroidXSplash("1.2.0"),
        AndroidXDocumentFile("1.1.0"),
        AndroidXPalette("1.0.0"),
        Kotlin("2.4.10"),
        KotlinSerialization("1.11.0"),
        Coroutines("1.11.0"),
        Ktor("3.5.1"),
        Okio("3.18.0"),
        SQLDelight("2.3.2"),
        Media3("1.10.1"),
        Leakcanary("2.14"),
        Compose("1.11.4"), /*MUST BE CHANGED WITH ACCOMPANIST VERSION*/
        ComposeFoundation("1.11.4"), /*MUST BE CHANGED WITH ACCOMPANIST VERSION*/
        ComposeMaterial("1.11.4"),
        ComposeMaterialIcons("1.7.8"),
        ComposeMaterial3("1.4.0"),
        ComposeUiTextGoogleFonts("1.11.4"),
        ComposeMultiplatform("1.11.1"),
        Accompanist("0.37.3") /*MUST BE CHANGED WITH COMPOSE VERSION*/,
        CoilCompose("3.5.0"),
        JAudioTagger("3.0.1"),
        JavaCv("1.5.11"),
        FfmpegPlatform("7.1-1.5.11"),
        Decompose("3.5.0"),
        Reorderable("3.1.0"),
        Jnativehook("2.2.2"),
        Mixpanel("8.9.0"),
        JUnit4("4.13.2"),
        AndroidXTest("1.7.0"),
        Robolectric("4.16.1"),
        Roborazzi("1.70.0"),

        DebugComposeCustomView("1.2.0-alpha02"),
        DebugComposeCustomViewPoolingcontainer("1.0.0"),
    }
}

enum class Toolkit(
    internal val libs: List<Library> = emptyList(),
    internal val projects: List<Project> = emptyList()
) {
    Compose(
        listOf(
            Library.ComposeUI,
            Library.ComposeUIToolkit,
            Library.ComposeFoundation,
            Library.ComposeMaterial,
            Library.ComposeMaterialWindowSize,
            Library.CoilCompose,
            Library.ActivityCompose
        )
    ),
    Decompose(
        listOf(
            Library.Decompose,
            Library.DecomposeExtensions
        )
    ),
    UI(
        libs = listOf(
            Library.ComposeUI,
            Library.ComposeUIToolkit,
            Library.ComposeFoundation,
            Library.ComposeMaterial,
            Library.ComposeMaterialIconsCore,
            Library.ComposeMaterialIconsExtended,
            Library.ComposeMaterialWindowSize,
            Library.CoilCompose,
            Library.ActivityCompose,
            Library.Decompose,
            Library.DecomposeExtensions
        ),
        projects = listOf(
            Project.Tools.Presentation.Compose,
            Project.Tools.Presentation.Decompose,
        )
    )
}

enum class FirebaseLibrary(group: String, artifact: String) {
    FirebaseCrashLytics("com.google.firebase", "firebase-crashlytics"),
    FirebaseAnalytics("com.google.firebase", "firebase-analytics"),
    FirebaseConfig("com.google.firebase", "firebase-config"),
    ;

    val notation = "$group:$artifact"

    companion object {
        val bom = "com.google.firebase:firebase-bom:34.16.0"
    }
}

enum class Plugin(group: String, artifact: String, version: Version) {
    Android("com.android.tools.build", "gradle", Version.Android),
    Kotlin("org.jetbrains.kotlin", "kotlin-gradle-plugin", Version.Kotlin),
    KotlinCompose("org.jetbrains.kotlin", "compose-compiler-gradle-plugin", Version.Kotlin),
    KotlinSerialization("org.jetbrains.kotlin", "kotlin-serialization", Version.Kotlin),
    ComposeMultiplatform("org.jetbrains.compose", "compose-gradle-plugin", Version.ComposeMultiplatform),
    KotlinMultiplatform("org.jetbrains.kotlin", "kotlin-gradle-plugin", Version.Kotlin),
    Google("com.google.gms", "google-services", Version.Google),
    Crashlytics("com.google.firebase", "firebase-crashlytics-gradle", Version.Crashlytics),
    Versions("com.github.ben-manes", "gradle-versions-plugin", Version.Versions),
    SQLDelight("app.cash.sqldelight", "gradle-plugin", Version.SQLDelight),
    GooglePlayPublisher("com.github.triplet.gradle", "play-publisher", Version.GooglePlayPublisher),
    FirebasePublisher("com.google.firebase", "firebase-appdistribution-gradle", Version.FirebasePublisher),
    Roborazzi("io.github.takahirom.roborazzi", "roborazzi-gradle-plugin", Version.Roborazzi)
    ;

    internal val notation = "$group:$artifact:${version.value}"

    enum class Id(val value: String) {
        AndroidApplication("com.android.application"),
        AndroidLibrary("com.android.library"),
        AndroidKmpLibrary("com.android.kotlin.multiplatform.library"),
        KotlinAndroid("org.jetbrains.kotlin.android"),
        KotlinCompose("org.jetbrains.kotlin.plugin.compose"),
        KotlinSerialization("org.jetbrains.kotlin.plugin.serialization"),
        KotlinParcelize("kotlin-parcelize"),
        KotlinJvm("org.jetbrains.kotlin.jvm"),
        KotlinMultiplatform("org.jetbrains.kotlin.multiplatform"),
        ComposeMultiplatform("org.jetbrains.compose"),
        JavaLibrary("java-library"),
        GoogleServices("com.google.gms.google-services"),
        Crashlytics("com.google.firebase.crashlytics"),
        Versions("com.github.ben-manes.versions"),
        SQLDelight("app.cash.sqldelight"),
        GooglePlayPublisher("com.github.triplet.play"),
        FirebasePublisher("com.google.firebase.appdistribution"),
        Roborazzi("io.github.takahirom.roborazzi")
    }

    private enum class Version(val value: String) {
        Android("9.3.0"),
        Kotlin(Library.Version.Kotlin.value),
        ComposeMultiplatform("1.11.1"),
        Google("4.5.0"),
        Crashlytics("3.0.7"),
        Versions("0.54.0"),
        SQLDelight(Library.Version.SQLDelight.value),
        GooglePlayPublisher("4.0.0"),
        FirebasePublisher("5.3.0"),
        Roborazzi(Library.Version.Roborazzi.value),
    }
}

/* With Kotlin 2.0+, composeOptions.kotlinCompilerExtensionVersion is no longer used.
   The Compose compiler is now bundled with Kotlin. Apply org.jetbrains.kotlin.plugin.compose
   plugin to each module using Compose instead. */
const val KotlinCompilerExtensionVersion = "2.4.10"

enum class Tools(val version: String) {
    Build("36.0.0"),
}

sealed class Project(id: String) {
    val name: String = ":$id"

    sealed class Core(id: String) : Project("core:$id") {

        sealed class Base(id: String) : Core("base:$id") {
            sealed class Data(id: String) : Base("data:$id") {
                object Playback : Data("playback")
            }

            sealed class Presentation(id: String) : Base("presentation:$id") {
                object Player : Presentation("player")
                object BackgroundPlayer : Presentation("background_player")
            }
        }

        sealed class Music(id: String) : Core("music:$id") {
            sealed class Entity(id: String) : Music("entity:$id") {
                object Catalog : Entity("catalog")
                object Playlist : Entity("playlist")
                object Playback : Entity("playback")
            }

            sealed class Data(id: String) : Music("data:$id") {
                object Playback : Data("playback")
                object Playlist : Data("playlist")
                object Favorites : Data("favorites")
                object Catalog : Data("catalog")
                sealed class Storage(id: String) : Data("storage:$id") {
                    sealed class Database(id: String) : Storage("database") {
                        companion object : Database("")
                    }
                }
            }

            sealed class Presentation(id: String) : Music("presentation:$id") {
                object Catalog : Presentation("catalog")
                object PlaylistCurrentQueue : Presentation("playlist:queue")
                object PlaylistLibrary : Presentation("playlist:library")
                object Favorites : Presentation("favorites")
            }
        }

        sealed class Book(id: String) : Core("book:$id") {
            sealed class Entity(id: String) : Book("entity:$id") {
                object Catalog : Entity("catalog")
            }

            sealed class Data(id: String) : Book("data:$id") {
                object Playback : Data("playback")
                object Catalog : Data("catalog")
                sealed class Storage(id: String) : Data("storage:$id") {
                    sealed class Database(id: String) : Storage("database") {
                        companion object : Database("")
                    }
                }
            }

            sealed class Presentation(id: String) : Book("presentation:$id") {
                object Catalog : Presentation("catalog")
            }
        }

        sealed class Platform(id: String) : Core("platform:$id") {
            object Resources : Platform("resources")
            object Formatter : Platform("formatter")
            object Permission : Platform("permission")
        }

        sealed class Domain(id: String) : Core("domain:$id")

        sealed class Data(id: String) : Core("data:$id") {
            sealed class Storage(id: String) : Data("storage:$id") {
                object Preferences : Storage("preferences")
            }
        }

    }

    sealed class Tools(id: String) : Project("tools:$id") {
        sealed class Presentation(id: String) : Tools("presentation:$id") {
            object Compose : Presentation("compose")
            object Decompose : Presentation("decompose")
        }

        object Entity : Tools("entity")
        object Coroutines : Tools("coroutines")
        sealed class Analytics(id: String) : Tools("analytics$id") {
            object Common : Analytics(":common")
            object Music : Analytics(":music")
            object Book : Analytics(":book")
            companion object : Analytics("")
        }

        sealed class Platform(id: String) : Tools("platform:$id") {
            object Utils : Platform("utils")
        }
    }

    object DebugSettings : Project("debug:settings")
}

object TigreLogger {
    const val version = "1.0.2"
    private const val group = "com.github.k-tigre"

    enum class Artifact(val notation: String) {
        Core("$group:logger-core:$version"),
        Logcat("$group:logger-logcat:$version"),
        Crashlytics("$group:logger-crashlytics:$version"),
        InternalStore("$group:logger-internal-store:$version"),
        Console("$group:logger-console:$version"),
    }
}

fun DependencyHandler.plugin(plugin: Plugin) = add(ScriptHandler.CLASSPATH_CONFIGURATION, plugin.notation)

fun DependencyHandler.implementation(toolkit: Toolkit) {
    toolkit.libs.forEach(::implementation)
    toolkit.projects.forEach(::implementation)
}

fun DependencyHandler.implementation(library: Library) = add("implementation", library.notation)
fun DependencyHandler.implementation(artifact: TigreLogger.Artifact) = add("implementation", artifact.notation)
fun DependencyHandler.debugImplementation(library: Library) = add("debugImplementation", library.notation)
fun DependencyHandler.testImplementation(library: Library) = add("testImplementation", library.notation)
fun DependencyHandler.testDebugImplementation(library: Library) = add("testDebugImplementation", library.notation)
fun DependencyHandler.implementation(vararg firebaseLibrary: FirebaseLibrary) {
    add("implementation", platform(FirebaseLibrary.bom))
    firebaseLibrary.forEach { lib -> add("implementation", lib.notation) }
}

fun DependencyHandler.implementation(project: Project) = add("implementation", project(project.name))
fun DependencyHandler.debugImplementation(project: Project) = add("debugImplementation", project(project.name))
fun DependencyHandler.api(project: Project) = add("api", project(project.name))
fun DependencyHandler.api(library: Library) = add("api", library.notation)
fun DependencyHandler.debugApi(library: Library) = add("debugApi", library.notation)
