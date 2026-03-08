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
}

android {
    defaultConfig {
        with(Application.AudioBook) {
            applicationId = id
            namespace = id

            versionName = version.name
            versionCode = version.code
        }

        androidResources.localeFilters.addAll(listOf("en", "ru"))
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

        val releaseStorePassword = System.getenv("AUDIO_BOOK_RELEASE_JKS_STORE_PASSWORD")
        val releaseKeyPassword = System.getenv("AUDIO_BOOK_RELEASE_JKS_KEY_PASSWORD")

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

        Environment.values().forEach { env ->
            named(env.gradleName) {
                isDebuggable = env.debuggable
                isMinifyEnabled = env.useProguard
                isShrinkResources = env.useProguard

                signingConfig = signingConfigs.findByName(env.gradleName)

                applicationIdSuffix = env.suffix
                manifestPlaceholders["appName"] = "${Application.AudioBook.name}${env.appNameSuffix}"
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
}

dependencies {
    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)

    implementation(Library.KotlinStd)
    implementation(Toolkit.UI)

    implementation(Project.Tools.Coroutines)
    implementation(Project.Core.Music.Presentation.Catalog)
    implementation(Project.Core.Book.Presentation.Catalog)
    implementation(Project.Core.Book.Data.Catalog)
    implementation(Project.Core.Book.Data.Playback)
    implementation(Project.Core.Book.Data.Storage.Database)
    implementation(Project.Core.Book.Entity.Catalog)
    implementation(Project.Core.Base.Presentation.Player)
    implementation(Project.Core.Music.Presentation.PlaylistCurrentQueue)
    implementation(Project.Core.Base.Presentation.BackgroundPlayer)
    implementation(Project.Core.Platform.Permission)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation(Project.Core.Music.Data.Storage.Database)
    implementation(Project.Core.Music.Data.Playback)
    implementation(Project.Core.Music.Data.Catalog)
    implementation(Project.Core.Music.Entity.Catalog)
    implementation(FirebaseLibrary.FirebaseAnalytics, FirebaseLibrary.FirebaseCrashLytics)

    implementation(Project.Logger.Core)
    implementation(Project.Logger.Crashlytics)
    implementation(Project.Logger.Logcat)
    implementation(Project.Logger.InternalStore)
    implementation("androidx.compose.ui:ui-text-google-fonts:1.10.3")

    debugImplementation(Project.DebugSettings)
}

play {
    track.set("internal")
    userFraction.set(1.0)
    releaseStatus.set(ReleaseStatus.COMPLETED)
}
