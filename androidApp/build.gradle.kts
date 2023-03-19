@file:Suppress("UnstableApiUsage")

plugins {
    id(Plugin.Id.AndroidApplication.value)
    id(Plugin.Id.KotlinAndroid.value)
    id(Plugin.Id.GoogleServices.value)
    id(Plugin.Id.Crashlytics.value)
}

android {
    defaultConfig {
        applicationId = Application.id
        namespace = Application.id

        versionName = Application.version.name
        versionCode = Application.version.code
        resourceConfigurations.addAll(listOf("en", "ru"))
    }

    signingConfigs {
        named(Environment.Debug.gradleName) {
            storeFile = File(rootDir, "/keys/debug.jks")
            storePassword = "debug123"
            keyAlias = "debug"
            keyPassword = "debug123"
        }

        val releaseStorePassword = System.getenv("MUSIC_PLAYER_RELEASE_JKS_STORE_PASSWORD")
        val releaseKeyPassword = System.getenv("MUSIC_PLAYER_RELEASE_JKS_KEY_PASSWORD")

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
        Environment.values().forEach { env ->
            named(env.gradleName) {
                isDebuggable = env.debuggable
                isMinifyEnabled = env.useProguard
                isShrinkResources = env.useProguard

                signingConfig = signingConfigs.findByName(env.gradleName)

                applicationIdSuffix = env.suffix
                manifestPlaceholders["appName"] = "${Application.name}${env.appNameSuffix}"
                if (env.useProguard) {
                    proguardFiles(
                        "rules.proguard",
                        getDefaultProguardFile(com.android.build.gradle.ProguardFiles.ProguardFile.DONT_OPTIMIZE.fileName)
                    )
                }

                matchingFallbacks.add(Environment.Debug.gradleName)
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
    implementation(Project.Core.Presentation.Catalog)
    implementation(Project.Core.Presentation.Player)
    implementation(Project.Core.Presentation.BackgroundPlayer)
    implementation(Project.Core.Platform.Permossion)
    implementation(Project.Core.Data.Storage.Preferences)
    implementation(Project.Core.Data.Storage.Database.Music)
    implementation(Project.Core.Data.Playback)
    implementation(Project.Core.Data.Catalog)
    implementation(Project.Core.Entity.Catalog)
    implementation(Library.AccompanistPermission)
    implementation(FirebaseLibrary.FirebaseAnalytics, FirebaseLibrary.FirebaseCrashLytics)

    // debugImplementation because LeakCanary should only run in debug builds.
    debugImplementation(Library.Leakcanary)
}
