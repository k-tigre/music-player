object Application {
    const val SDK_COMPILE = 36
    const val SDK_MINIMUM = 26
    const val SDK_TARGET = 35

    data class Version(private val major: Int, private val minor: Int, private val patch: Int) {
        val code = 10000 * major + 100 * minor + patch
        val name = "$major.$minor.$patch"
    }

    object AudioBook {
        const val id: String = "by.tigre.audiobook"
        val version: Version = Version(0, 1, 0)
        const val name: String = "AudioBook"

        const val SDK_COMPILE = Application.SDK_COMPILE
        const val SDK_MINIMUM = Application.SDK_MINIMUM
        const val SDK_TARGET = Application.SDK_TARGET
    }

    object MusicPlayer {
        const val id: String = "by.tigre.musicplayer"
        val version: Version = Version(0, 16, 0)
        const val name: String = "Music"

        const val SDK_COMPILE = Application.SDK_COMPILE
        const val SDK_MINIMUM = Application.SDK_MINIMUM
        const val SDK_TARGET = Application.SDK_TARGET
    }
}
