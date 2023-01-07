import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    dependencies {
        plugin(Plugin.Android)
        plugin(Plugin.Kotlin)
        plugin(Plugin.Google)
        plugin(Plugin.Crashlytics)
        plugin(Plugin.Versions)
    }
}

subprojects {
    plugins.matching { it is com.android.build.gradle.AppPlugin || it is com.android.build.gradle.LibraryPlugin }.whenPluginAdded {
        configure<com.android.build.gradle.BaseExtension> {

            buildToolsVersion(Tools.Build.version)
            compileSdkVersion(Application.SDK_COMPILE)

            defaultConfig {
                minSdk = Application.SDK_MINIMUM
                targetSdk = Application.SDK_TARGET
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }

            testOptions {
                unitTests.isReturnDefaultValues = true
            }

            buildFeatures.buildConfig = false

            composeOptions.kotlinCompilerExtensionVersion = KotlinCompilerExtensionVersion

            namespace = "${Application.id}${name.replace(":", ".")}"
        }
    }

    plugins.matching { it is JavaPlugin }.whenPluginAdded {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = false
            jvmTarget = "11"
        }
    }

    apply {
        plugin(Plugin.Id.Versions.value)
    }

    tasks.withType<DependencyUpdatesTask> {
        fun isNonStable(version: String): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(version)
            return isStable.not()
        }

        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                        reject("Release candidate")
                    }
                }
            }
        }
    }
}
