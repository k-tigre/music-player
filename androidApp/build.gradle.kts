plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 32
    defaultConfig {
        applicationId = "by.tigre.music.player.android"
        minSdk = 24
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            manifestPlaceholders["appName"] = "Music"
        }

        getByName("debug") {
            isMinifyEnabled = false
            manifestPlaceholders["appName"] = "Music Dev"
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
}
