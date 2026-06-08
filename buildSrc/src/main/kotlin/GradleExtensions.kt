import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Gets a value from Gradle properties or Environment variables as a [Provider].
 * This is compatible with Configuration Cache and follows the pattern used in `settings.gradle.kts`.
 */
fun Project.envOrProperty(key: String): Provider<String> {
    return providers.gradleProperty(key)
        .orElse(providers.environmentVariable(key))
}

/**
 * Gets a value from Gradle properties or Environment variables, defaulting to Null if not found.
 */
fun Project.envOrPropertyNullable(key: String): String? {
    return envOrProperty(key).orNull
}
