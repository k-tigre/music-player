object Application {
    const val SDK_COMPILE = 37
    const val SDK_MINIMUM = 26
    const val SDK_TARGET = 37

    data class Version(val major: Int, val minor: Int, val patch: Int) {
        val code = 10000 * major + 100 * minor + patch
        val name = "$major.$minor.$patch"

        /** MSI ProductVersion: Android MAJOR.MINOR + (patch + minute-of-week). BUILD ≤ 65535. */
        fun desktopPackageVersion(
            minuteOfWeek: Int = Application.minuteOfWeekNow(),
        ): String = "$major.$minor.${patch + minuteOfWeek.coerceIn(0, 10_079)}"
    }

    /** Monday 00:00 → 0 … Sunday 23:59 → 10079 */
    fun minuteOfWeekNow(
        now: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    ): Int = (now.dayOfWeek.value - 1) * 24 * 60 + now.toLocalTime().toSecondOfDay() / 60

    object AudioBook {
        const val id: String = "by.tigre.audiobook"
        val version: Version = Version(1, 0, 0)
        const val name: String = "AudioBook"
    }

    object MusicPlayer {
        const val id: String = "by.tigre.musicplayer"
        val version: Version = Version(1, 0, 0)
        const val name: String = "Music"
    }
}
