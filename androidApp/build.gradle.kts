plugins {
    id(Plugin.Id.AndroidApplication.value)
    id(Plugin.Id.KotlinAndroid.value)
//    id(Plugin.Id.Crashlytics.value)
}

android {
    defaultConfig {
        applicationId = Application.id
        namespace = Application.id

        versionName = Application.version.name
        versionCode = Application.version.code
        resourceConfigurations.addAll(listOf("en", "ru"))
    }

    buildTypes {
        Environment.values().forEach { env ->
            named(env.gradleName) {
                isDebuggable = env.debuggable
                isMinifyEnabled = env.useProguard
                isShrinkResources = env.useProguard

//                signingConfig = signingConfigs.findByName(env.gradleName) TODO

                applicationIdSuffix = env.suffix
                manifestPlaceholders["appName"] = "${Application.name}${env.appNameSuffix}"
                if (env.useProguard) {
                    proguardFiles("rules.proguard", getDefaultProguardFile(com.android.build.gradle.ProguardFiles.ProguardFile.DONT_OPTIMIZE.fileName))
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
    implementation(Library.MaterialComponents)
    implementation(Library.AndroidXSplash)
    implementation(Library.AccompanistPager)
    implementation(Library.AccompanistPagerInidcators)
    implementation(Project.Tools.Coroutines)

    implementation(Toolkit.UI)

    // debugImplementation because LeakCanary should only run in debug builds.
    debugImplementation(Library.Leakcanary)
}
