import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
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
        plugin(Plugin.ComposeMultiplatform)
        plugin(Plugin.Google)
        plugin(Plugin.Crashlytics)
        plugin(Plugin.Versions)
        plugin(Plugin.SQLDelight)
        plugin(Plugin.GooglePlayPublisher)
        plugin(Plugin.FirebasePublisher)
        plugin(Plugin.Roborazzi)
    }
}

subprojects {
    plugins.withId(Plugin.Id.AndroidApplication.value) {
        extensions.configure<ApplicationExtension>("android") {
            compileSdk = Application.SDK_COMPILE
            buildToolsVersion = Tools.Build.version
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
            buildFeatures {
                buildConfig = false
                viewBinding = false
                compose = false
            }
            namespace = "by.tigre.${name.replace(":", ".").replace("-", "_")}"
        }
        pluginManager.apply(Plugin.Id.KotlinSerialization.value)
    }

    plugins.withId(Plugin.Id.AndroidLibrary.value) {
        extensions.configure<LibraryExtension>("android") {
            compileSdk = Application.SDK_COMPILE
            buildToolsVersion = Tools.Build.version
            defaultConfig {
                minSdk = Application.SDK_MINIMUM
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            testOptions {
                unitTests.isReturnDefaultValues = true
            }
            buildFeatures {
                buildConfig = false
                viewBinding = false
                compose = false
            }
            namespace = "by.tigre.${name.replace(":", ".").replace("-", "_")}"
        }
        pluginManager.apply(Plugin.Id.KotlinSerialization.value)
    }

    plugins.matching { it is JavaPlugin }.whenPluginAdded {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    plugins.withId(Plugin.Id.KotlinJvm.value) {
        tasks.withType<KotlinCompile> {
            compilerOptions {
                allWarningsAsErrors = false
                jvmTarget = JvmTarget.JVM_21
            }
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
    }
}
