enum class Environment(
    val gradleName: String,
    val debuggable: Boolean = false,
    val useProguard: Boolean = true,
    val suffix: String,
    val appNameSuffix: String
) {
    Debug(
        gradleName = "debug",
        suffix = ".dev",
        debuggable = true,
        useProguard = false,
        appNameSuffix = " Dev"
    ),
    Release(
        gradleName = "release",
        suffix = "",
        appNameSuffix = ""
    );
}
