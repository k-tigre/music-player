import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        plugin(Plugin.KotlinCompose)
        plugin(Plugin.KotlinSerialization)
        plugin(Plugin.Google)
        plugin(Plugin.Crashlytics)
        plugin(Plugin.Versions)
        plugin(Plugin.SQLDelight)
        plugin(Plugin.GooglePlayPublisher)
        plugin(Plugin.FirebasePublisher)
    }
}

subprojects {
    plugins.matching { it is com.android.build.gradle.AppPlugin || it is com.android.build.gradle.LibraryPlugin }
        .whenPluginAdded {
            configure<com.android.build.gradle.BaseExtension> {

                buildToolsVersion(Tools.Build.version)
                compileSdkVersion(Application.SDK_COMPILE)

                defaultConfig {
                    minSdk = Application.SDK_MINIMUM
                    targetSdk = Application.SDK_TARGET
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }

                testOptions {
                    unitTests.isReturnDefaultValues = true
                }

                buildFeatures.buildConfig = false
                buildFeatures.viewBinding = false
                buildFeatures.compose = false

                namespace = "by.tigre.${name.replace(":", ".").replace("-", "_")}"
            }
        }

    // Apply serialization plugin to all Kotlin Android modules (bundled in kotlin-gradle-plugin)
    plugins.withId(Plugin.Id.KotlinAndroid.value) {
        pluginManager.apply(Plugin.Id.KotlinSerialization.value)
    }

    plugins.matching { it is JavaPlugin }.whenPluginAdded {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget = JvmTarget.JVM_21
        }
    }

    apply {
        plugin(Plugin.Id.Versions.value)
    }

    tasks.withType<DependencyUpdatesTask> {
        fun isNonStable(version: String): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(version)
            return isStable.not()
        }

        outputDir = File(rootDir, "build/${project.group.toString().replace(".", "/")}/${project.name}").absolutePath

//        resolutionStrategy {
//            componentSelection {
//                all {
//                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
//                        reject("Release candidate")
//                    }
//                }
//            }
//        }
    }
}
