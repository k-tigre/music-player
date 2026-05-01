@file:Suppress("UnstableApiUsage")

import java.io.File

plugins {
    id(Plugin.Id.AndroidApplication.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.GoogleServices.value)
    id(Plugin.Id.Crashlytics.value)
}

android {
    namespace = "by.tigre.audiobook"
    defaultConfig {
        with(Application.AudioBook) {
            applicationId = id
            versionName = version.name
            versionCode = version.code
        }
        androidResources.localeFilters.addAll(listOf("en", "ru"))
    }

    sourceSets {
        getByName("main") {
            res.srcDir(rootProject.file("apps/AudioBook/src/main/res"))
        }
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

        Environment.entries.forEach { env ->
            named(env.gradleName) {
                isDebuggable = env.debuggable
                isMinifyEnabled = env.useProguard
                isShrinkResources = env.useProguard

                signingConfig = signingConfigs.findByName(env.gradleName)

                applicationIdSuffix = env.suffix
                manifestPlaceholders["appName"] = "${Application.AudioBook.name}${env.appNameSuffix} (Car)"
                if (env.useProguard) {
                    proguardFiles(
                        "rules.proguard",
                        getDefaultProguardFile(com.android.build.gradle.ProguardFiles.ProguardFile.DONT_OPTIMIZE.fileName)
                    )
                }

                matchingFallbacks.add(Environment.Release.gradleName)
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = false
        viewBinding = false
    }
}

dependencies {
    implementation(project(":apps:audiobook-shared"))
    implementation(Project.Core.Base.Presentation.BackgroundPlayer)

    implementation(Library.AndroidXCore)
    implementation(Library.AndroidXAppcompat)
    implementation(Library.CoroutinesAndroid)
    implementation(Library.KotlinStd)

    implementation(FirebaseLibrary.FirebaseAnalytics, FirebaseLibrary.FirebaseCrashLytics)

    implementation(Project.Logger.Core)
    implementation(Project.Logger.Crashlytics)
    implementation(Project.Logger.Logcat)
    implementation(Project.Logger.InternalStore)
}
