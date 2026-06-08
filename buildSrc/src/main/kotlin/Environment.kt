enum class Environment(
    val gradleName: String,
    val debuggable: Boolean = false,
    val useProguard: Boolean = true,
    val remoteAnalytics: Boolean = false,
    val suffix: String,
    val appNameSuffix: String
) {
    Debug(
        gradleName = "debug",
        suffix = ".dev",
        debuggable = true,
        useProguard = false,
        remoteAnalytics = false,
        appNameSuffix = " Dev"
    ),
    Qa(
        gradleName = "qa",
        suffix = ".dev",
        debuggable = true,
        useProguard = true,
        remoteAnalytics = true,
        appNameSuffix = " Qa"
    ),
    Release(
        gradleName = "release",
        suffix = "",
        appNameSuffix = "",
        remoteAnalytics = true,
    );
}
