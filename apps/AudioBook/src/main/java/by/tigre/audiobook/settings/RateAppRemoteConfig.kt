package by.tigre.audiobook.settings

object RateAppRemoteConfig {
    const val KEY = "audiobook_show_rate_app"

    fun defaultsMap(): Map<String, Any> = mapOf(KEY to false)
}
