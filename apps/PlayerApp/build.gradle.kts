@file:Suppress("UnstableApiUsage")

import com.github.triplet.gradle.androidpublisher.ReleaseStatus


plugins {
    id(Plugin.Id.AndroidApplication.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.GoogleServices.value)
    id(Plugin.Id.Crashlytics.value)
    id(Plugin.Id.GooglePlayPublisher.value)
    id(Plugin.Id.FirebasePublisher.value)
    id(Plugin.Id.KotlinCompose.value)
    id(Plugin.Id.Roborazzi.value)
}

android {
    defaultConfig {
        Application.MusicPlayer.also { app ->
            applicationId = app.id
            // Must match Kotlin package so manifest-relative names (.App, .MainActivity) resolve correctly.
            namespace = "by.tigre.music.player"

            versionName = app.version.name
            versionCode = app.version.code
        }
        androidResources.localeFilters.addAll(listOf("en", "ru"))

        buildConfigField(
            "String",
            "MIXPANEL_TOKEN",
            "\"${envOrPropertyNullable("MUSIC_PLAYER_MIXPANEL_TOKEN")}\""
        )
        buildConfigField(
            "String",
            "MIXPANEL_SERVER_URL",
            envOrPropertyNullable("MIXPANEL_SERVER_URL")?.let { "\"$it\"" } ?: "null"
        )
    }

    signingConfigs {
        named(Environment.Debug.gradleName) {
            storeFile = File(rootDir, "/keys/debug.jks")
            storePassword = "debug123"
            keyAlias = "debug"
            keyPassword = "debug123"
        }
        create(Environment.Qa.gradleName) {
            initWith(getAt(Environment.Debug.gradleName))
        }

        val releaseStorePassword = envOrPropertyNullable("MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD")
        val releaseKeyPassword = envOrPropertyNullable("MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD")

        if (listOf(releaseStorePassword, releaseKeyPassword).any { it.isNullOrBlank() }) {
            System.err.println("Release JKS credentials are not available")
            System.err.println("Release signing config is not available")
        } else {
            create(Environment.Release.gradleName) {
                storeFile = File(rootDir, "/keys/release.jks")
                storePassword = releaseStorePassword
                keyAlias = "upload_release"
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        create(Environment.Qa.gradleName)

        Environment.entries.forEach { env ->
            named(env.gradleName) {
                isDebuggable = env.debuggable
                isMinifyEnabled = env.useProguard
                isShrinkResources = env.useProguard

                signingConfig = signingConfigs.findByName(env.gradleName)

                applicationIdSuffix = env.suffix
                manifestPlaceholders["appName"] = "${Application.MusicPlayer.name}${env.appNameSuffix}"
                resValue("string", "app_name", "${Application.MusicPlayer.name}${env.appNameSuffix}")
                buildConfigField("Boolean", "REMOTE_ANALYTICS_ENABLED", env.remoteAnalytics.toString())
                if (env.useProguard) {
                    proguardFiles(
                        "rules.proguard",
                        getDefaultProguardFile(com.android.build.gradle.ProguardFiles.ProguardFile.DONT_OPTIMIZE.fileName)
                    )
                }

                matchingFallbacks.add(Environment.Release.gradleName)

                if (env == Environment.Qa) {
                    firebaseAppDistribution {
                        artifactType = "APK"
                        groups = "test-group"
                    }
                }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    outputDir.set(rootProject.file("docs/marketing/music-player/assets/screenshots"))
}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    implementation(Toolkit.UI)

    implementation(Project.Tools.Coroutines)
    implementation(Project.Tools.Analytics)
    implementation(Project.Tools.Analytics.Music)
    implementation(Project.Core.Music.Presentation.Catalog)
    implementation(Project.Core.Base.Presentation.Player)
    implementation(Project.Core.Music.Presentation.PlaylistCurrentQueue)
    implementation(Project.Core.Music.Presentation.PlaylistLibrary)
    implementation(Project.Core.Music.Presentation.Favorites)
    implementation(Project.Core.Base.Presentation.BackgroundPlayer)
    implementation(Project.Core.Platform.Permission)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation(Project.Core.Music.Data.Storage.Database)
    implementation(Project.Core.Music.Data.Playback)
    implementation(Project.Core.Music.Data.Playlist)
    implementation(Project.Core.Music.Data.Favorites)
    implementation(Project.Core.Music.Data.Catalog)
    implementation(Project.Core.Music.Entity.Catalog)
    implementation(Project.Core.Music.Entity.Playlist)
    implementation(Project.Core.Music.Entity.Playback)
    implementation(Library.AccompanistPermission)
    implementation(FirebaseLibrary.FirebaseAnalytics, FirebaseLibrary.FirebaseCrashLytics)

    implementation(TigreLogger.Artifact.Core)
    implementation(TigreLogger.Artifact.Crashlytics)
    implementation(TigreLogger.Artifact.Logcat)
    implementation(TigreLogger.Artifact.InternalStore)

    // debugImplementation because LeakCanary should only run in debug builds.
//    debugImplementation(Library.Leakcanary)

    debugImplementation(Project.DebugSettings)

    debugImplementation(Library.ComposeUiTestManifest)
    testDebugImplementation(Library.ComposeUiTestManifest)

    testImplementation(Library.JUnit4)
    testImplementation(Library.AndroidXTestCore)
    testImplementation(Library.Robolectric)
    testImplementation(Library.Roborazzi)
    testImplementation(Library.RoborazziCompose)
    testImplementation(Library.ComposeUiTestJunit4)
    testImplementation(Library.DebugComposeUiToolingPreview)
    testImplementation(Library.ComposeComponentsResources)
}

play {
    track.set("internal")
    userFraction.set(1.0)
    releaseStatus.set(ReleaseStatus.COMPLETED)
}

val syncPlayListingAssetsAction = Action<Task> {
    val outputDir = rootProject.file("docs/marketing/music-player/assets/output")
    val playBase = file("src/main/play/listings")
    fun syncScreenshots(sourceDir: File, targetDir: File) {
        targetDir.mkdirs()
        targetDir.listFiles()?.forEach { it.delete() }
        sourceDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.forEachIndexed { index, sourceFile ->
                sourceFile.copyTo(targetDir.resolve("${index + 1}.png"), overwrite = true)
            }
    }
    fun syncFeatureGraphic(sourceFile: File, targetFile: File) {
        targetFile.parentFile.mkdirs()
        sourceFile.copyTo(targetFile, overwrite = true)
    }
    syncScreenshots(
        outputDir.resolve("screenshots/ru"),
        playBase.resolve("ru-RU/graphics/phone-screenshots"),
    )
    syncScreenshots(
        outputDir.resolve("screenshots/en"),
        playBase.resolve("en-US/graphics/phone-screenshots"),
    )
    syncFeatureGraphic(
        outputDir.resolve("feature-graphic/feature-graphic-ru.png"),
        playBase.resolve("ru-RU/graphics/feature-graphic/1.png"),
    )
    syncFeatureGraphic(
        outputDir.resolve("feature-graphic/feature-graphic-en.png"),
        playBase.resolve("en-US/graphics/feature-graphic/1.png"),
    )
}

tasks.register("buildMarketingAssets") {
    group = "marketing"
    description = "Render final Google Play PNGs from Roborazzi screenshots (Music Player)"
    dependsOn("recordRoborazziDebug")
    doLast {
        val assetsDir = rootProject.file("docs/marketing/music-player/assets")
        val script = assetsDir.resolve("scripts/build_assets.py")
        val exitCode = ProcessBuilder("python", script.absolutePath)
            .directory(assetsDir)
            .inheritIO()
            .start()
            .waitFor()
        check(exitCode == 0) { "build_assets.py failed with exit code $exitCode" }
    }
}

tasks.register("syncPlayListingAssets") {
    group = "marketing"
    description = "Copy generated marketing PNGs into src/main/play/listings for GPP upload"
    dependsOn("buildMarketingAssets")
    doLast(syncPlayListingAssetsAction)
}

tasks.register("buildMarketingScreenshots") {
    group = "marketing"
    description = "Record Roborazzi screenshots and build final Google Play assets (Music Player)"
    dependsOn("syncPlayListingAssets")
}

tasks.register("recordMarketingScreenshots") {
    group = "marketing"
    description = "Re-capture app screenshots with Roborazzi and rebuild marketing assets (Music Player)"
    dependsOn("recordRoborazziDebug", "buildMarketingScreenshots")
}
